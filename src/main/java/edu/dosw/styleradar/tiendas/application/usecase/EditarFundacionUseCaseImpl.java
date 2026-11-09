package edu.dosw.styleradar.tiendas.application.usecase;

import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.EditarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditarFundacionUseCaseImpl implements EditarFundacionUseCase {

    private final FundacionRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public FundacionResponseDTO editar(Long id, FundacionRequestDTO request, String rolUsuario) {
        adminValidator.validarEsAdministradorStyleRadar(rolUsuario);

        if (id == null) {
            throw new ReglaDeNegocioException("El identificador de la fundación es obligatorio");
        }
        if (request == null) {
            throw new ReglaDeNegocioException("La información para actualizar la fundación no puede ser nula");
        }

        Fundacion existente = repositoryPort.findById(id)
                .orElseThrow(() -> new FundacionNoEncontradaException("No existe una fundación aliada con ID: " + id));

        String nombreNormalizado = request.getNombre() != null ? request.getNombre().strip() : null;
        if (nombreNormalizado != null && repositoryPort.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new FundacionYaRegistradaException("Ya existe otra fundación aliada registrada con el nombre: " + nombreNormalizado);
        }

        if (request.getRepresentanteId() != null && repositoryPort.existsByRepresentanteIdAndIdNot(request.getRepresentanteId(), id)) {
            throw new FundacionYaRegistradaException("El usuario con ID " + request.getRepresentanteId()
                    + " ya se encuentra asignado como representante de otra fundación aliada");
        }

        Fundacion cambios = Fundacion.builder()
                .nombre(nombreNormalizado)
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().strip() : null)
                .direccion(request.getDireccion() != null ? request.getDireccion().strip() : null)
                .telefono(request.getTelefono() != null ? request.getTelefono().strip() : null)
                .email(request.getEmail() != null ? request.getEmail().strip().toLowerCase() : null)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .categoriasAceptadas(request.getCategoriasAceptadas() != null ? new HashSet<>(request.getCategoriasAceptadas()) : new HashSet<>())
                .representanteId(request.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(request.getUrlCertificadoPersoneriaJuridica() != null
                        ? request.getUrlCertificadoPersoneriaJuridica().strip() : null)
                .causasApoyadas(request.getCausasApoyadas() != null ? request.getCausasApoyadas().strip() : null)
                .horarioAtencion(request.getHorarioAtencion() != null ? request.getHorarioAtencion().strip() : null)
                .politicasDonacion(request.getPoliticasDonacion() != null ? request.getPoliticasDonacion().strip() : null)
                .build();

        existente.actualizarDatosInstitucionales(cambios);

        Fundacion guardada = repositoryPort.save(existente);
        log.info("Fundación aliada ID={} ('{}') actualizada exitosamente por admin", guardada.getId(), guardada.getNombre());

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

