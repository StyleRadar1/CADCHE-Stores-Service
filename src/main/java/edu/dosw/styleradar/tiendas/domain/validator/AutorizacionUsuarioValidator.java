package edu.dosw.styleradar.tiendas.domain.validator;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;

/**
 * Autoriza las operaciones sobre los recursos de un usuario identificado en la ruta ({@code /users/{userId}/...}).
 * El proyecto no usa {@code /me}: la ruta nombra al usuario de forma explícita y el servicio comprueba que quien
 * llama sea ese mismo usuario o un administrador de StyleRadar. Sin identidad responde 401 y con otra identidad 403.
 */
public class AutorizacionUsuarioValidator {

    public void validarAccesoAlUsuario(Long solicitanteId, String roles, Long usuarioRuta) {
        if (solicitanteId == null) {
            throw new FalloAutenticacionException(AutorizacionAdminStyleRadarValidator.MENSAJE_SIN_IDENTIDAD);
        }
        boolean esAdmin = roles != null && RolesUsuario.contieneAlguno(roles,
                AutorizacionAdminStyleRadarValidator.ROL_ADMIN_STYLERADAR, AutorizacionAdminStyleRadarValidator.ROL_ADMIN_CORTO);
        if (!solicitanteId.equals(usuarioRuta) && !esAdmin) {
            throw new AccesoDenegadoAlmacenException("El usuario autenticado (" + solicitanteId
                    + ") no tiene permisos para consultar los recursos del usuario " + usuarioRuta);
        }
    }
}
