package edu.dosw.styleradar.tiendas.application.usecase;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.PeriodoNuevaLlegada;
import edu.dosw.styleradar.tiendas.domain.model.UmbralUltimasUnidades;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegistrarAlmacenUseCaseImpl implements RegistrarAlmacenUseCase {

    public static final long MAX_BYTES_RUT_PDF = 5L * 1024L * 1024L;

    private final AlmacenRepositoryPort repositoryPort;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    public RegistrarAlmacenUseCaseImpl(AlmacenRepositoryPort repositoryPort) {
        this(repositoryPort, null, new AutorizacionAlmacenValidator());
    }

    @Autowired
    public RegistrarAlmacenUseCaseImpl(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            AutorizacionAlmacenValidator autorizacionValidator) {
        this.repositoryPort = repositoryPort;
        this.almacenamientoBlobPort = almacenamientoBlobPort;
        this.autorizacionValidator = autorizacionValidator != null ? autorizacionValidator : new AutorizacionAlmacenValidator();
    }

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId) {
        return registrarAlmacenInterno(request, usuarioId, null, false);
    }

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId, String rol) {
        return registrarAlmacenInterno(request, usuarioId, rol, rol != null && !rol.isBlank());
    }

    private VerificacionAlmacenResponseDTO registrarAlmacenInterno(
            RegistrarAlmacenRequestDTO request, Long usuarioId, String rol, boolean validarRol) {
        if (validarRol) {
            autorizacionValidator.validarRolTienda(rol);
        }
        if (request == null) {
            throw new ReglaDeNegocioException("La solicitud de registro del almacén no puede ser nula");
        }

        if (usuarioId != null && repositoryPort.existeAlmacenParaUsuario(usuarioId)) {
            throw new AlmacenYaRegistradoException(
                    "El usuario ya administra un almacén",
                    Map.of("usuarioId", "El usuario ya administra un almacén"));
        }

        String nitNormalizado = stripOrNull(request.getNit());
        String correoNormalizado = stripOrNull(request.getCorreoContacto());

        Almacen almacen = Almacen.builder()
                .nit(nitNormalizado)
                .nombreComercial(stripOrNull(request.getNombreComercial()))
                .descripcion(stripOrNull(request.getDescripcion()))
                .telefono(stripOrNull(request.getTelefono()))
                .correoContacto(correoNormalizado != null ? correoNormalizado.toLowerCase(Locale.ROOT) : null)
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .reputacion(0.0)
                .categorias(request.getCategorias() != null ? new HashSet<>(request.getCategorias()) : new HashSet<>())
                .umbralUltimasUnidades(UmbralUltimasUnidades.VALOR_POR_DEFECTO)
                .diasNuevaLlegada(PeriodoNuevaLlegada.VALOR_POR_DEFECTO)
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDIENTE)
                .urlRutPdf(stripOrNull(request.getUrlRutPdf()))
                .build();

        almacen.validarDatosObligatoriosRegistro();

        if (repositoryPort.existsByNit(nitNormalizado)) {
            throw new AlmacenYaRegistradoException(
                    "Ya existe un almacén registrado con el NIT: " + nitNormalizado,
                    Map.of("nit", "Ya existe un almacén registrado con el NIT: " + nitNormalizado));
        }

        log.info("Registrando solicitud de vinculación de almacén aliado con NIT={} y nombre='{}'",
                almacen.getNit(), almacen.getNombreComercial());

        Almacen guardado = repositoryPort.save(almacen);

        if (usuarioId != null) {
            repositoryPort.asociarAdministrador(guardado.getNit(), usuarioId);
            log.info("Asociado usuarioId={} como administrador del almacén NIT={}", usuarioId, guardado.getNit());
        }

        String representante = usuarioId != null ? "Usuario #" + usuarioId : resolverRepresentante(guardado.getNit());
        return toVerificacionResponseDTO(guardado, representante);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionAlmacenResponseDTO obtenerMiAlmacen(Long usuarioId) {
        if (usuarioId == null) {
            throw new FalloAutenticacionException("Token de autenticación ausente, inválido o expirado");
        }
        Almacen almacen = repositoryPort.findByAdministradorUsuarioId(usuarioId)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén asociado al usuario autenticado: " + usuarioId));
        return toVerificacionResponseDTO(almacen, "Usuario #" + usuarioId);
    }

    @Override
    public DocumentoSubidoResponseDTO subirDocumentoRut(
            String nombreOriginal, String contentType, long tamano, InputStream contenido, Long usuarioId, String rol) {
        if (usuarioId == null) {
            throw new FalloAutenticacionException("Token de autenticación ausente, inválido o expirado");
        }
        if (rol != null && !rol.isBlank()) {
            autorizacionValidator.validarRolTienda(rol);
        }
        validarArchivoPdfRut(nombreOriginal, contentType, tamano);

        String rutaDestino = "rut/" + usuarioId + "/" + UUID.randomUUID() + ".pdf";
        String url = almacenamientoBlobPort != null
                ? almacenamientoBlobPort.subirArchivo("documentos-almacenes", rutaDestino, contenido, "application/pdf", tamano)
                : "https://styleradarblob.blob.core.windows.net/documentos-almacenes/" + rutaDestino;

        return new DocumentoSubidoResponseDTO(url, nombreOriginal, "application/pdf", tamano, Instant.now());
    }

    private void validarArchivoPdfRut(String nombreOriginal, String contentType, long tamano) {
        boolean esPdfMime = contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf");
        boolean esPdfExt = nombreOriginal != null && nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (tamano <= 0 || tamano > MAX_BYTES_RUT_PDF || (!esPdfMime && !esPdfExt)) {
            throw new ReglaDeNegocioException("El RUT debe ser un archivo PDF de máximo 5 MB");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AlmacenResponseDTO obtenerPorNit(String nit) {
        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));
        return toAlmacenResponseDTO(almacen);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<AlmacenResponseDTO> consultarAlmacenes(ConsultaAlmacenRequestDTO request) {
        int pageNum = request != null ? request.getPage() : 0;
        int size = request != null ? request.getSize() : 20;

        if (pageNum < 0 || size < 1 || size > 100) {
            throw new ReglaDeNegocioException("Parámetros de paginación inválidos para la consulta de almacenes");
        }

        Pageable pageable = PageRequest.of(
                pageNum,
                size,
                Sort.by("nombreComercial").ascending().and(Sort.by("nit").ascending()));

        Page<Almacen> page = repositoryPort.findByEstadoVerificacion(EstadoVerificacionAlmacen.APROBADO, pageable);

        List<AlmacenResponseDTO> content = page.getContent().stream()
                .filter(a -> request == null
                        || request.getCategoria() == null
                        || (a.getCategorias() != null && a.getCategorias().contains(request.getCategoria())))
                .map(this::toAlmacenResponseDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private String resolverRepresentante(String nit) {
        List<AdministradorAlmacen> admins = repositoryPort.listarAdministradoresPorNit(nit);
        if (admins == null || admins.isEmpty() || admins.get(0).getUsuarioId() == null) {
            return null;
        }
        return "Usuario #" + admins.get(0).getUsuarioId();
    }

    private static String stripOrNull(String val) {
        return val != null ? val.strip() : null;
    }

    private AlmacenResponseDTO toAlmacenResponseDTO(Almacen a) {
        return new AlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion() != null ? a.getReputacion() : 0.0,
                a.getCategorias()
        );
    }

    private VerificacionAlmacenResponseDTO toVerificacionResponseDTO(Almacen a, String representante) {
        return new VerificacionAlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getTelefono(),
                a.getCorreoContacto(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion() != null ? a.getReputacion() : 0.0,
                a.getCategorias(),
                a.getEstadoVerificacion(),
                a.getEstadoVerificacion() == null,
                a.getFechaDecisionVerificacion(),
                a.getAdministradorDecisionId(),
                a.getMotivoVerificacion(),
                a.getUrlRutPdf(),
                representante
        );
    }
}
