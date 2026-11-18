package edu.dosw.styleradar.tiendas.domain.validator;



import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenRepositoryPort;

public class AutorizacionAlmacenValidator {

    public static final String ROL_ADMIN_ALMACEN = "STORE_ADMIN";
    public static final String ROL_TIENDA = "TIENDA";

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
            throw new FalloAutenticacionException(AutorizacionAdminStyleRadarValidator.MENSAJE_SIN_IDENTIDAD);
        }
        if (!RolesUsuario.contieneAlguno(roles, ROL_ADMIN_ALMACEN, "ROLE_" + ROL_ADMIN_ALMACEN, ROL_TIENDA, "ROLE_" + ROL_TIENDA)) {
            throw new AccesoDenegadoAlmacenException("Solo una cuenta con rol de tienda puede registrar un almacén");
        }
    }
}
