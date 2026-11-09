package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.DesactivarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesactivarFundacionUseCaseImpl implements DesactivarFundacionUseCase {

    public static final String MOTIVO_DEFAULT = "Desactivación institucional por administrador StyleRadar";

    private final FundacionRepositoryPort repositoryPort;
    private final FundacionEventPublisherPort eventPublisherPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public FundacionResponseDTO desactivar(Long id, String rolUsuario) {
        return desactivar(id, MOTIVO_DEFAULT, rolUsuario);
    }

    @Override
    @Transactional
    public FundacionResponseDTO desactivar(Long id, String motivo, String rolUsuario) {
        adminValidator.validarEsAdministradorStyleRadar(rolUsuario);

        if (id == null) {
            throw new ReglaDeNegocioException("El identificador de la fundación es obligatorio");
        }

        Fundacion existente = repositoryPort.findById(id)
                .orElseThrow(() -> new FundacionNoEncontradaException("No existe una fundación aliada con ID: " + id));

        // Transición de dominio: si ya está inactiva, lanza FundacionYaDesactivadaException (409)
        existente.desactivar();

        Fundacion guardada = repositoryPort.save(existente);
        log.info("Fundación aliada ID={} ('{}') desactivada exitosamente en BD (historial conservado)",
                guardada.getId(), guardada.getNombre());

        // Notificación asíncrona mediante publicación de evento de dominio a RabbitMQ
        FundacionDesactivadaEvent evento = FundacionDesactivadaEvent.builder()
                .fundacionId(guardada.getId())
                .nombreFundacion(guardada.getNombre())
                .representanteId(guardada.getRepresentanteId())
                .fechaDesactivacion(Instant.now())
                .motivo(motivo != null && !motivo.isBlank() ? motivo : MOTIVO_DEFAULT)
                .build();

        eventPublisherPort.publicarFundacionDesactivada(evento);
        log.info("Evento FundacionDesactivadaEvent publicado para reasignar donaciones y notificar a usuarios");

        return mapearResponse(guardada);
    }

    private FundacionResponseDTO mapearResponse(Fundacion f) {
        return FundacionResponseDTO.builder()
                .id(f.getId())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .direccion(f.getDireccion())
                .telefono(f.getTelefono())
                .email(f.getEmail())
                .latitud(f.getLatitud())
                .longitud(f.getLongitud())
                .categoriasAceptadas(f.getCategoriasAceptadas())
                .representanteId(f.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(f.getUrlCertificadoPersoneriaJuridica())
                .causasApoyadas(f.getCausasApoyadas())
                .horarioAtencion(f.getHorarioAtencion())
                .politicasDonacion(f.getPoliticasDonacion())
                .activa(f.isActiva())
                .fechaRegistro(f.getFechaRegistro())
                .build();
    }
}

