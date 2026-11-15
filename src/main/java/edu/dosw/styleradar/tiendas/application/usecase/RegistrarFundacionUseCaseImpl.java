package edu.dosw.styleradar.tiendas.application.usecase;

import java.io.InputStream;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaFundacionesRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.FundacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.FundacionResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionNoEncontradaException;
import edu.dosw.styleradar.tiendas.domain.exception.FundacionYaRegistradaException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.Fundacion;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarFundacionUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegistrarFundacionUseCaseImpl implements RegistrarFundacionUseCase {

    public static final long MAX_BYTES_CERTIFICADO_PDF = 5L * 1024L * 1024L;

    private final FundacionRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;

    public RegistrarFundacionUseCaseImpl(
            FundacionRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        this(repositoryPort, adminValidator, null);
    }

    @Autowired
    public RegistrarFundacionUseCaseImpl(
            FundacionRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator,
            AlmacenamientoBlobPort almacenamientoBlobPort) {
        this.repositoryPort = repositoryPort;
        this.adminValidator = adminValidator;
        this.almacenamientoBlobPort = almacenamientoBlobPort;
    }

    @Override
    @Transactional
    public FundacionResponseDTO registrar(FundacionRequestDTO request, String rol) {
        adminValidator.validarEsAdministradorParaFundaciones(rol);

        if (request == null) {
            throw new ReglaDeNegocioException("La información de registro de la fundación no puede ser nula");
        }

        String nitNormalizado = stripOrNull(request.getNit());
        String nombreNormalizado = stripOrNull(request.getNombre());

        if (nitNormalizado != null && repositoryPort.existsByNitIgnoreCase(nitNormalizado)) {
            throw new FundacionYaRegistradaException(
                    "Ya existe una fundación aliada registrada con el NIT: " + nitNormalizado,
                    Map.of("nit", "Ya existe una fundación aliada registrada con el NIT: " + nitNormalizado));
        }

        if (nombreNormalizado != null && repositoryPort.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new FundacionYaRegistradaException(
                    "Ya existe una fundación aliada registrada con el nombre: " + nombreNormalizado,
                    Map.of("nombre", "Ya existe una fundación aliada registrada con el nombre: " + nombreNormalizado));
        }

        if (request.getRepresentanteId() != null && repositoryPort.existsByRepresentanteId(request.getRepresentanteId())) {
            throw new FundacionYaRegistradaException(
                    "El usuario con ID " + request.getRepresentanteId()
                            + " ya se encuentra asignado como representante de otra fundación aliada",
                    Map.of("representanteId", "El usuario ya se encuentra asignado como representante de otra fundación aliada"));
        }

        String emailNorm = stripOrNull(request.getEmail());
        Fundacion fundacion = Fundacion.builder()
                .nit(nitNormalizado)
                .nombre(nombreNormalizado)
                .descripcion(stripOrNull(request.getDescripcion()))
                .direccion(stripOrNull(request.getDireccion()))
                .barrio(stripOrNull(request.getBarrio()))
                .telefono(stripOrNull(request.getTelefono()))
                .email(emailNorm != null ? emailNorm.toLowerCase(Locale.ROOT) : null)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .categoriasAceptadas(request.getCategoriasAceptadas() != null ? new HashSet<>(request.getCategoriasAceptadas()) : new HashSet<>())
                .representanteId(request.getRepresentanteId())
                .urlCertificadoPersoneriaJuridica(stripOrNull(request.getUrlCertificadoPersoneriaJuridica()))
                .causasApoyadas(stripOrNull(request.getCausasApoyadas()))
                .horarioAtencion(stripOrNull(request.getHorarioAtencion()))
                .politicasDonacion(stripOrNull(request.getPoliticasDonacion()))
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
        adminValidator.validarEsAdministradorParaFundaciones(rol);
        return repositoryPort.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FundacionResponseDTO obtenerPorId(Long id, String rol) {
        adminValidator.validarEsAdministradorParaFundaciones(rol);
        if (id == null) {
            throw new ReglaDeNegocioException("El identificador de la fundación es obligatorio");
        }
        Fundacion fundacion = repositoryPort.findById(id)
                .orElseThrow(() -> new FundacionNoEncontradaException("No existe una fundación aliada con ID: " + id));
        return toResponseDTO(fundacion);
    }

    @Override
    public DocumentoSubidoResponseDTO subirCertificado(
            String nombreOriginal, String contentType, long tamano, InputStream contenido, String rol) {
        adminValidator.validarEsAdministradorParaFundaciones(rol);

        boolean esPdfMime = contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf");
        boolean esPdfExt = nombreOriginal != null && nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (tamano <= 0 || tamano > MAX_BYTES_CERTIFICADO_PDF || (!esPdfMime && !esPdfExt)) {
            throw new ReglaDeNegocioException("El certificado debe ser un archivo PDF de máximo 5 MB");
        }

        String rutaDestino = "certificados/" + UUID.randomUUID() + ".pdf";
        String url = almacenamientoBlobPort != null
                ? almacenamientoBlobPort.subirArchivo("documentos-fundaciones", rutaDestino, contenido, "application/pdf", tamano)
                : "https://styleradarblob.blob.core.windows.net/documentos-fundaciones/" + rutaDestino;

        return new DocumentoSubidoResponseDTO(url, nombreOriginal, "application/pdf", tamano, Instant.now());
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

    private static String stripOrNull(String val) {
        return val != null ? val.strip() : null;
    }

    private FundacionResponseDTO toResponseDTO(Fundacion f) {
        return FundacionResponseDTO.builder()
                .id(f.getId())
                .nit(f.getNit())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .direccion(f.getDireccion())
                .barrio(f.getBarrio())
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
