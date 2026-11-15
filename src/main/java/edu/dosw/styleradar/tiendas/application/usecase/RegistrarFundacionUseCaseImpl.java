package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrarFundacionUseCaseImpl implements RegistrarFundacionUseCase {

    private final FundacionRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;

    @Override
    @Transactional
    public FundacionResponseDTO registrar(FundacionRequestDTO request, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        if (request == null) {
            throw new ReglaDeNegocioException("La información de registro de la fundación no puede ser nula");
        }

        String nombreNormalizado = request.getNombre() != null ? request.getNombre().strip() : null;

        if (nombreNormalizado != null && repositoryPort.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new FundacionYaRegistradaException("Ya existe una fundación aliada registrada con el nombre: " + nombreNormalizado);
        }

        if (request.getRepresentanteId() != null && repositoryPort.existsByRepresentanteId(request.getRepresentanteId())) {
            throw new FundacionYaRegistradaException("El usuario con ID " + request.getRepresentanteId()
                    + " ya se encuentra asignado como representante de otra fundación aliada");
        }

        Fundacion fundacion = Fundacion.builder()
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
                .activa(true)
                .fechaRegistro(Instant.now())
                .build();

        fundacion.validarDatosObligatoriosRegistro();

        log.info("Registrando nueva fundación aliada '{}' con representanteId={}",
                fundacion.getNombre(), fundacion.getRepresentanteId());

        Fundacion guardada = repositoryPort.save(fundacion);

        log.info("Fundación '{}' registrada exitosamente con ID={}", guardada.getNombre(), guardada.getId());
        return toResponseDTO(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundacionResponseDTO> listarTodas(String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);
        return repositoryPort.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundacionResponseDTO> consultarCercanas(ConsultaFundacionesRequestDTO request) {
        if (request == null || request.getLatitud() == null || request.getLongitud() == null) {
            throw new ReglaDeNegocioException("Las coordenadas geográficas de latitud y longitud son obligatorias");
        }
        return repositoryPort.findAll().stream()
                .filter(Fundacion::isActiva)
                .filter(f -> request.getTipo() == null
                        || (f.getCategoriasAceptadas() != null && f.getCategoriasAceptadas().contains(request.getTipo())))
                .sorted(Comparator.comparingDouble(f -> calcularDistanciaKm(
                        request.getLatitud(),
                        request.getLongitud(),
                        f.getLatitud() != null ? f.getLatitud() : 0.0,
                        f.getLongitud() != null ? f.getLongitud() : 0.0)))
                .map(this::toResponseDTO)
                .toList();
    }

    private double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final double radioTierraKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radioTierraKm * c;
    }

    private FundacionResponseDTO toResponseDTO(Fundacion f) {
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
