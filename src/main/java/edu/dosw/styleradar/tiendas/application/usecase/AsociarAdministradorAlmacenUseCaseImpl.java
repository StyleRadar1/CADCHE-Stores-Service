package edu.dosw.styleradar.tiendas.application.usecase;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.AsociarAdministradorAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class AsociarAdministradorAlmacenUseCaseImpl implements AsociarAdministradorAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;
    private final AutorizacionAlmacenValidator almacenValidator;

    @Override
    @Transactional
    public AdministradorAlmacenResponseDTO asociar(String nit, Long usuarioId, Long adminId, String rol) {
        adminValidator.validarEsAdministradorStyleRadar(rol);
        if (adminId == null || adminId <= 0) {
            throw new FalloAutenticacionException(AutorizacionAdminStyleRadarValidator.MENSAJE_SIN_IDENTIDAD);
        }
        if (usuarioId == null || usuarioId <= 0) {
            throw new ReglaDeNegocioException("El identificador del usuario administrador debe ser mayor a cero");
        }
        if (!repositoryPort.existsByNit(nit)) {
            throw new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit);
        }

        log.info("Asociando usuarioId={} como administrador del almacén NIT={}", usuarioId, nit);
        return aRespuesta(repositoryPort.guardarAsociacionAdministrador(nit, usuarioId, adminId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministradorAlmacenResponseDTO> listarAdministradores(String nit, Long usuarioId, String rol) {
        if (!repositoryPort.existsByNit(nit)) {
            throw new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit);
        }

        boolean esAdminGlobal = rol != null && (rol.contains(AutorizacionAdminStyleRadarValidator.ROL_ADMIN_STYLERADAR)
                || rol.contains(AutorizacionAdminStyleRadarValidator.ROL_ADMIN_CORTO));
        if (!esAdminGlobal) {
            almacenValidator.validarAcceso(nit, usuarioId, repositoryPort);
        }

        return repositoryPort.listarAdministradoresPorNit(nit).stream()
                .map(AsociarAdministradorAlmacenUseCaseImpl::aRespuesta)
                .toList();
    }

    private static AdministradorAlmacenResponseDTO aRespuesta(AdministradorAlmacen vinculo) {
        return new AdministradorAlmacenResponseDTO(
                vinculo.getId(),
                vinculo.getUsuarioId(),
                vinculo.getAlmacenNit(),
                vinculo.getAsignadoPorId(),
                vinculo.getFechaAsignacion());
    }
}

