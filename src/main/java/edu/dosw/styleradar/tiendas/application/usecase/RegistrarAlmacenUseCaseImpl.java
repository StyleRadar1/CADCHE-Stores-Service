package edu.dosw.styleradar.tiendas.application.usecase;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ConsultaAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.RegistrarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.AlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.DocumentoSubidoResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.VerificacionAlmacenCambiadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenYaRegistradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.Pagina;
import edu.dosw.styleradar.tiendas.domain.model.SolicitudPagina;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.PeriodoNuevaLlegada;
import edu.dosw.styleradar.tiendas.domain.model.UmbralUltimasUnidades;
import edu.dosw.styleradar.tiendas.domain.port.in.RegistrarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenamientoBlobPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistrarAlmacenUseCaseImpl implements RegistrarAlmacenUseCase {

    public static final long MAX_BYTES_RUT_PDF = 5L * 1024L * 1024L;

    private final AlmacenRepositoryPort repositoryPort;
    private final AlmacenamientoBlobPort almacenamientoBlobPort;
    private final AutorizacionAlmacenValidator autorizacionValidator;
    private final AlmacenEventPublisherPort eventPublisher;

    public RegistrarAlmacenUseCaseImpl(AlmacenRepositoryPort repositoryPort) {
        this(repositoryPort, null, new AutorizacionAlmacenValidator());
    }

    public RegistrarAlmacenUseCaseImpl(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            AutorizacionAlmacenValidator autorizacionValidator) {
        this(repositoryPort, almacenamientoBlobPort, autorizacionValidator, null);
    }

    public RegistrarAlmacenUseCaseImpl(
            AlmacenRepositoryPort repositoryPort,
            AlmacenamientoBlobPort almacenamientoBlobPort,
            AutorizacionAlmacenValidator autorizacionValidator,
            AlmacenEventPublisherPort eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.repositoryPort = repositoryPort;
        this.almacenamientoBlobPort = almacenamientoBlobPort;
        this.autorizacionValidator = autorizacionValidator != null ? autorizacionValidator : new AutorizacionAlmacenValidator();
    }

    /**
     * SR-RF-62-A: solo una Tienda autenticada registra su almacén. Sin identidad responde 401 y con otro rol 403;
     * el almacén nace PENDING y queda asociado a quien lo registra.
     */
    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO registrarAlmacen(RegistrarAlmacenRequestDTO request, Long usuarioId, String rol) {
        exigirTiendaAutenticada(usuarioId, rol);
        if (request == null) {
            throw new ReglaDeNegocioException("La solicitud de registro del almacén no puede ser nula");
        }

        if (repositoryPort.existeAlmacenParaUsuario(usuarioId)) {
            throw new AlmacenYaRegistradoException(
                    "El usuario ya administra un almacén",
                    Map.of("userId", "El usuario ya administra un almacén"));
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
                .estadoVerificacion(EstadoVerificacionAlmacen.PENDING)
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

        if (eventPublisher != null) {
            eventPublisher.publicarVerificacionCambiada(VerificacionAlmacenCambiadaEvent.de(
                    guardado.getNit(), null, guardado.getEstadoVerificacion(), Instant.now()));
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
        exigirTiendaAutenticada(usuarioId, rol);
        validarArchivoPdfRut(nombreOriginal, contentType, tamano);

        String rutaDestino = "rut/" + usuarioId + "/" + UUID.randomUUID() + ".pdf";
        String url = almacenamientoBlobPort.subirArchivo(
                "documentos-almacenes", rutaDestino, contenido, "application/pdf", tamano);

        return new DocumentoSubidoResponseDTO(url, nombreOriginal, "application/pdf", tamano, Instant.now());
    }

    private void exigirTiendaAutenticada(Long usuarioId, String rol) {
        if (usuarioId == null) {
            throw new FalloAutenticacionException("Token de autenticación ausente, inválido o expirado");
        }
        autorizacionValidator.validarRolTienda(rol);
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
        SolicitudPagina solicitud = request != null
                ? SolicitudPagina.de(request.getPage(), request.getSize(),
                        "Parámetros de paginación inválidos para la consulta de almacenes")
                : new SolicitudPagina(0, 20);

        Pagina<Almacen> page = repositoryPort.findByEstadoVerificacion(EstadoVerificacionAlmacen.APPROVED, solicitud);

        List<AlmacenResponseDTO> content = page.contenido().stream()
                .filter(a -> request == null
                        || request.getCategory() == null
                        || (a.getCategorias() != null && a.getCategorias().contains(request.getCategory())))
                .map(this::toAlmacenResponseDTO)
                .toList();

        return new PageResponseDTO<>(
                content,
                page.numero(),
                page.tamano(),
                page.totalElementos(),
                page.totalPaginas(),
                page.esPrimera(),
                page.esUltima()
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
                a.getCategorias(),
                a.getTelefono(),
                a.getCorreoContacto(),
                a.getDireccionComercial(),
                a.getBarrio(),
                a.getHorarioAtencion(),
                a.obtenerAlmacenId()
        );
    }

    private VerificacionAlmacenResponseDTO toVerificacionResponseDTO(Almacen a, String representanteFallback) {
        String representante = (a.getRepresentanteLegal() != null && !a.getRepresentanteLegal().isBlank())
                ? a.getRepresentanteLegal()
                : representanteFallback;
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
                representante,
                a.getCuentaPagos(),
                a.getDireccionComercial(),
                a.getBarrio(),
                a.getHorarioAtencion(),
                a.obtenerAlmacenId()
        );
    }
}
