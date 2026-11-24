package edu.dosw.styleradar.tiendas.application.usecase;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.DecisionVerificacionRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.request.VerificacionConsultaRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.PageResponseDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.event.VerificacionAlmacenCambiadaEvent;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Pagina;
import edu.dosw.styleradar.tiendas.domain.model.SolicitudPagina;
import edu.dosw.styleradar.tiendas.domain.port.in.VerificarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificarAlmacenUseCaseImpl implements VerificarAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;
    private final AlmacenEventPublisherPort eventPublisher;

    public VerificarAlmacenUseCaseImpl(AlmacenRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator) {
        this(repositoryPort, adminValidator, null);
    }

    public VerificarAlmacenUseCaseImpl(AlmacenRepositoryPort repositoryPort,
            AutorizacionAdminStyleRadarValidator adminValidator, AlmacenEventPublisherPort eventPublisher) {
        this.repositoryPort = repositoryPort;
        this.adminValidator = adminValidator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO decidir(
            String nit, DecisionVerificacionRequestDTO request, Long adminId, String rol) {
        if (adminId == null) {
            throw new FalloAutenticacionException("Token de autenticación ausente, inválido o expirado");
        }
        adminValidator.validarEsAdministradorStyleRadar(rol);

        if (request == null || request.getDecision() == null || !request.isMotivoValido() || !request.isMotivoLongitudValida()) {
            throw new ReglaDeNegocioException("La decisión de verificación o el motivo es inválido");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        log.info("Decidiendo verificación para almacén NIT={} decisión={} adminId={}",
                nit, request.getDecision(), adminId);

        EstadoVerificacionAlmacen anterior = almacen.getEstadoVerificacion();
        almacen.decidirVerificacion(request.getDecision(), request.getMotivo(), adminId, Instant.now());
        Almacen guardado = repositoryPort.save(almacen);
        publicarCambio(guardado, anterior);

        log.info("Almacén NIT={} decidido exitosamente como {}", nit, guardado.getEstadoVerificacion());
        return toResponseDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificacionAlmacenResponseDTO consultarUno(String nit, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        return toResponseDTO(almacen);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<VerificacionAlmacenResponseDTO> consultar(
            VerificacionConsultaRequestDTO request, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);

        SolicitudPagina solicitud = SolicitudPagina.de(
                request.getPage(), request.getSize(), "Paginación de verificación inválida");

        Pagina<Almacen> page = request.getStatus() != null
                ? repositoryPort.findByEstadoVerificacion(request.getStatus(), solicitud)
                : repositoryPort.findAllVerificacion(solicitud);

        List<VerificacionAlmacenResponseDTO> content = page.contenido().stream()
                .map(this::toResponseDTO)
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

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO solicitarRevision(String nit, Long adminId, String rol) {
        if (adminId == null) {
            throw new FalloAutenticacionException("Token de autenticación ausente, inválido o expirado");
        }
        adminValidator.validarEsAdministradorStyleRadar(rol);

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit));

        log.info("Solicitando revisión de almacén NIT={} por adminId={}", nit, adminId);
        EstadoVerificacionAlmacen anterior = almacen.getEstadoVerificacion();
        almacen.solicitarRevision();
        Almacen guardado = repositoryPort.save(almacen);
        publicarCambio(guardado, anterior);

        return toResponseDTO(guardado);
    }

    private void publicarCambio(Almacen guardado, EstadoVerificacionAlmacen anterior) {
        if (eventPublisher != null) {
            eventPublisher.publicarVerificacionCambiada(VerificacionAlmacenCambiadaEvent.de(
                    guardado.getNit(), anterior, guardado.getEstadoVerificacion(), Instant.now()));
        }
    }

    private String resolverRepresentante(String nit) {
        List<AdministradorAlmacen> admins = repositoryPort.listarAdministradoresPorNit(nit);
        if (admins == null || admins.isEmpty() || admins.get(0).getUsuarioId() == null) {
            return null;
        }
        return "Usuario #" + admins.get(0).getUsuarioId();
    }

    private VerificacionAlmacenResponseDTO toResponseDTO(Almacen a) {
        return new VerificacionAlmacenResponseDTO(
                a.getNit(),
                a.getNombreComercial(),
                a.getDescripcion(),
                a.getTelefono(),
                a.getCorreoContacto(),
                a.getLatitud(),
                a.getLongitud(),
                a.getReputacion(),
                a.getCategorias(),
                a.getEstadoVerificacion(),
                a.getEstadoVerificacion() == null,
                a.getFechaDecisionVerificacion(),
                a.getAdministradorDecisionId(),
                a.getMotivoVerificacion(),
                a.getUrlRutPdf(),
                resolverRepresentante(a.getNit())
        );
    }
}
