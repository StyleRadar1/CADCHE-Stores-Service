package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.ReactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ReactivarFundacionUseCaseImpl implements ReactivarFundacionUseCase {

    public static final String MOTIVO_DEFAULT = "Reactivación institucional por administrador StyleRadar";

    private final FundacionRepositoryPort repositoryPort;
    private final FundacionEventPublisherPort eventPublisherPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public FundacionResponseDTO reactivar(Long id, String rolUsuario) {
        return reactivar(id, MOTIVO_DEFAULT, rolUsuario);
    }

    @Override
    @Transactional
    public FundacionResponseDTO reactivar(Long id, String motivo, String rolUsuario) {
        adminValidator.validarEsAdministradorParaFundaciones(rolUsuario);
        Fundacion.validarMotivoCambioEstado(motivo);

        if (id == null) {
            throw new ReglaDeNegocioException("El identificador de la fundación es obligatorio");
        }

        Fundacion existente = repositoryPort.findById(id)
                .orElseThrow(() -> new FundacionNoEncontradaException("No existe una fundación aliada con ID: " + id));

        existente.reactivar();

        Fundacion guardada = repositoryPort.save(existente);
        log.info("Fundación aliada ID={} ('{}') reactivada exitosamente en BD",
                guardada.getId(), guardada.getNombre());

        FundacionReactivadaEvent evento = FundacionReactivadaEvent.builder()
                .fundacionId(guardada.getId())
                .nombreFundacion(guardada.getNombre())
                .representanteId(guardada.getRepresentanteId())
                .fechaReactivacion(Instant.now())
                .motivo(motivo != null && !motivo.isBlank() ? motivo : MOTIVO_DEFAULT)
                .build();

        eventPublisherPort.publicarFundacionReactivada(evento);
        eventPublisherPort.publicarEstadoCambiado(edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent.reactivada(guardada.getId(), Instant.now()));
        log.info("Evento FundacionReactivadaEvent publicado exitosamente");

        return FundacionResponseDTO.builder()
                .id(guardada.getId())
                .nit(guardada.getNit())
                .nombre(guardada.getNombre())
                .descripcion(guardada.getDescripcion())
                .direccion(guardada.getDireccion())
                .barrio(guardada.getBarrio())
                .telefono(guardada.getTelefono())
                .email(guardada.getEmail())
                .latitud(guardada.getLatitud())
                .longitud(guardada.getLongitud())
                .categoriasAceptadas(guardada.getCategoriasAceptadas())
                .representanteId(guardada.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(guardada.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(guardada.getCausasApoyadas())
                .horarioAtencion(guardada.getHorarioAtencion())
                .politicasDonacion(guardada.getPoliticasDonacion())
                .activa(guardada.isActiva())
                .fechaRegistro(guardada.getFechaRegistro())
                .build();
    }
}

