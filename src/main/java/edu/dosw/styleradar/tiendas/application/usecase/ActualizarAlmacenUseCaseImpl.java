package edu.dosw.styleradar.tiendas.application.usecase;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.request.ActualizarAlmacenRequestDTO;
import edu.dosw.styleradar.tiendas.application.dto.response.VerificacionAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.model.Almacen;
import edu.dosw.styleradar.tiendas.domain.port.in.ActualizarAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import edu.dosw.styleradar.tiendas.domain.validator.RolesUsuario;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActualizarAlmacenUseCaseImpl implements ActualizarAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAlmacenValidator autorizacionValidator;

    public ActualizarAlmacenUseCaseImpl(
            AlmacenRepositoryPort repositoryPort,
            AutorizacionAlmacenValidator autorizacionValidator) {
        this.repositoryPort = repositoryPort;
        this.autorizacionValidator = autorizacionValidator != null
                ? autorizacionValidator
                : new AutorizacionAlmacenValidator();
    }

    @Override
    @Transactional
    public VerificacionAlmacenResponseDTO actualizarAlmacen(
            String nit,
            ActualizarAlmacenRequestDTO request,
            Long usuarioId,
            String rol) {
        if (usuarioId == null) {
            throw new FalloAutenticacionException(
                    "Se requiere autenticación válida (X-User-Id o token JWT) para administrar el almacén");
        }
        if (request == null) {
            throw new ReglaDeNegocioException("La solicitud de actualización del almacén no puede ser nula");
        }

        Almacen almacen = repositoryPort.findByNit(nit)
                .orElseThrow(() -> new AlmacenNoEncontradoException(
                        "No se encontró ningún almacén registrado con el NIT: " + nit));

        boolean esAdminStyleRadar = RolesUsuario.contieneAlguno(
                rol,
                AutorizacionAdminStyleRadarValidator.ROL_ADMIN_STYLERADAR,
                AutorizacionAdminStyleRadarValidator.ROL_ADMIN_CORTO);

        if (!esAdminStyleRadar) {
            autorizacionValidator.validarAcceso(nit, usuarioId, repositoryPort);
        }

        almacen.actualizarPerfilComercial(
                request.getNombreComercial(),
                request.getDescripcion(),
                request.getTelefono(),
                request.getCorreoContacto(),
                request.getRepresentanteLegal(),
                request.getCuentaPagos(),
                request.getDireccionComercial(),
                request.getBarrio(),
                request.getHorarioAtencion(),
                request.getCategorias());

        log.info("Actualizando perfil comercial de almacén NIT={} por usuarioId={}", nit, usuarioId);
        Almacen guardado = repositoryPort.save(almacen);

        return toVerificacionResponseDTO(guardado);
    }

    private String resolverRepresentante(Almacen almacen) {
        if (almacen.getRepresentanteLegal() != null && !almacen.getRepresentanteLegal().isBlank()) {
            return almacen.getRepresentanteLegal();
        }
        List<AdministradorAlmacen> admins = repositoryPort.listarAdministradoresPorNit(almacen.getNit());
        if (admins == null || admins.isEmpty() || admins.get(0).getUsuarioId() == null) {
            return null;
        }
        return "Usuario #" + admins.get(0).getUsuarioId();
    }

    private VerificacionAlmacenResponseDTO toVerificacionResponseDTO(Almacen a) {
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
                resolverRepresentante(a),
                a.getCuentaPagos(),
                a.getDireccionComercial(),
                a.getBarrio(),
                a.getHorarioAtencion(),
                a.obtenerAlmacenId()
        );
    }
}
