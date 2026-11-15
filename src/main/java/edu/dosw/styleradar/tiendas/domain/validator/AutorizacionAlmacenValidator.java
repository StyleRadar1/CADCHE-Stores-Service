package edu.dosw.styleradar.tiendas.domain.validator;

import java.util.Locale;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

@Component
public class AutorizacionAlmacenValidator {

    public void validarAcceso(String nit, Long usuarioId, AlmacenRepositoryPort repositoryPort) {
        if (usuarioId == null) {
            throw new FalloAutenticacionException(
                    "Se requiere autenticación válida (X-User-Id o token JWT) para administrar el almacén");
        }
        boolean autorizado = repositoryPort.esAdministradorDeAlmacen(nit, usuarioId);
        if (!autorizado) {
            throw new AccesoDenegadoAlmacenException(
                    "El usuario autenticado (" + usuarioId + ") no tiene permisos para administrar el almacén con NIT " + nit);
        }
    }

    public void validarRolTienda(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new AccesoDenegadoAlmacenException("Solo una cuenta con rol de tienda puede registrar un almacén");
        }
        String upper = roles.toUpperCase(Locale.ROOT);
        boolean esTienda = upper.contains("TIENDA")
                || upper.contains("ADMIN_ALMACEN")
                || upper.contains("ALMACEN");
        if (!esTienda) {
            throw new AccesoDenegadoAlmacenException("Solo una cuenta con rol de tienda puede registrar un almacén");
        }
    }
}
