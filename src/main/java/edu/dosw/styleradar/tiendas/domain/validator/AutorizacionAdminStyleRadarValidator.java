package edu.dosw.styleradar.tiendas.domain.validator;


import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;

/**
 * Autoriza las operaciones reservadas al administrador de StyleRadar. Sin identidad autenticada responde
 * {@code 401} y con otro rol {@code 403} (B-0.1, CADCHE-130).
 */
public class AutorizacionAdminStyleRadarValidator {

    public static final String ROL_ADMIN_STYLERADAR = "ROLE_ADMIN_STYLERADAR";
    public static final String ROL_ADMIN_CORTO = "ADMIN_STYLERADAR";

    public static final String MENSAJE_VERIFICAR_ALMACENES =
            "Se requiere el rol ROLE_ADMIN_STYLERADAR para verificar almacenes aliados";
    public static final String MENSAJE_SIN_IDENTIDAD =
            "Se requiere un token de acceso válido para esta operación";
    public static final String MENSAJE_GESTIONAR_FUNDACIONES =
            "Se requiere el rol ROLE_ADMIN_STYLERADAR para gestionar fundaciones aliadas";

    public void validarEsAdministradorStyleRadar(String roles) {
        validarRolConMensaje(roles, MENSAJE_VERIFICAR_ALMACENES);
    }

    public void validarEsAdministradorParaFundaciones(String roles) {
        validarRolConMensaje(roles, MENSAJE_GESTIONAR_FUNDACIONES);
    }

    private void validarRolConMensaje(String roles, String mensajeError) {
        if (roles == null || roles.isBlank()) {
            throw new FalloAutenticacionException(MENSAJE_SIN_IDENTIDAD);
        }
        if (!RolesUsuario.contieneAlguno(roles, ROL_ADMIN_STYLERADAR, ROL_ADMIN_CORTO)) {
            throw new AccesoDenegadoAlmacenException(mensajeError);
        }
    }
}
