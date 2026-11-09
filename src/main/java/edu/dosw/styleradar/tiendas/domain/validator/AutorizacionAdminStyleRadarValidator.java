package edu.dosw.styleradar.tiendas.domain.validator;

import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.exception.AccesoDenegadoAlmacenException;

@Component
public class AutorizacionAdminStyleRadarValidator {

    public static final String ROL_ADMIN_STYLERADAR = "ROLE_ADMIN_STYLERADAR";
    public static final String ROL_ADMIN_CORTO = "ADMIN_STYLERADAR";

    public void validarEsAdministradorStyleRadar(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR para verificar almacenes aliados");
        }
        boolean esAdmin = roles.contains(ROL_ADMIN_STYLERADAR) || roles.contains(ROL_ADMIN_CORTO);
        if (!esAdmin) {
            throw new AccesoDenegadoAlmacenException("Se requiere el rol ROLE_ADMIN_STYLERADAR para verificar almacenes aliados");
        }
    }
}
