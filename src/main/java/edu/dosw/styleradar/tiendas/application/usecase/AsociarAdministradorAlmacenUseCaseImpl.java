package edu.dosw.styleradar.tiendas.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.dosw.styleradar.tiendas.application.dto.response.AdministradorAlmacenResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.AlmacenNoEncontradoException;
import edu.dosw.styleradar.tiendas.domain.exception.ReglaDeNegocioException;
import edu.dosw.styleradar.tiendas.domain.model.AdministradorAlmacen;
import edu.dosw.styleradar.tiendas.domain.port.in.AsociarAdministradorAlmacenUseCase;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAdminStyleRadarValidator;
import edu.dosw.styleradar.tiendas.domain.validator.AutorizacionAlmacenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsociarAdministradorAlmacenUseCaseImpl implements AsociarAdministradorAlmacenUseCase {

    private final AlmacenRepositoryPort repositoryPort;
    private final AutorizacionAdminStyleRadarValidator adminValidator;
    private final AutorizacionAlmacenValidator almacenValidator;

    @Override
    @Transactional
    public AdministradorAlmacenResponseDTO asociar(String nit, Long usuarioId, Long adminId, String rol) {
        if (rol != null && !rol.isBlank()) {
            adminValidator.validarEsAdministradorStyleRadar(rol);
        }
        if (usuarioId == null || usuarioId <= 0) {
            throw new ReglaDeNegocioException("El identificador del usuario administrador debe ser mayor a cero");
        }
        if (!repositoryPort.existsByNit(nit)) {
            throw new AlmacenNoEncontradoException("No se encontró ningún almacén registrado con el NIT: " + nit);
        }

        log.info("Asociando usuarioId={} como administrador del almacén NIT={}", usuarioId, nit);
        AdministradorAlmacen vinculo = repositoryPort.guardarAsociacionAdministrador(nit, usuarioId);
        Long asignadoPor = adminId != null ? adminId : (vinculo.getAsignadoPorId() != null ? vinculo.getAsignadoPorId() : 1L);

        return new AdministradorAlmacenResponseDTO(
                vinculo.getId(),
                vinculo.getUsuarioId(),
                vinculo.getAlmacenNit(),
                asignadoPor,
                vinculo.getFechaAsignacion()
        );
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
                .map(v -> new AdministradorAlmacenResponseDTO(
                        v.getId(),
                        v.getUsuarioId(),
                        v.getAlmacenNit(),
                        v.getAsignadoPorId() != null ? v.getAsignadoPorId() : 1L,
                        v.getFechaAsignacion()))
                .toList();
    }
}

