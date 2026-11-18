package edu.dosw.styleradar.tiendas.domain.validator;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lectura de los roles recibidos como lista separada por comas (por ejemplo {@code "STYLERADAR_ADMIN"} o
 * {@code "ROLE_STORE_ADMIN,BUYER"}). Compara roles completos, nunca subcadenas, para que un rol como
 * {@code NO_STYLERADAR_ADMIN} no se confunda con {@code STYLERADAR_ADMIN} (OWASP A01:2025).
 */
public final class RolesUsuario {

    private RolesUsuario() {
    }

    public static Set<String> leer(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(roles.split(","))
                .map(rol -> rol.trim().toUpperCase(Locale.ROOT))
                .filter(rol -> !rol.isEmpty())
                .map(RolesUsuario::nombreActual)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Traduce los nombres anteriores de los roles (ADMIN_ALMACEN, TIENDA, ADMIN_STYLERADAR) a los del análisis v2
     * (STORE_ADMIN, STYLERADAR_ADMIN), con o sin el prefijo ROLE_, para aceptar tokens emitidos antes del cambio.
     */
    static String nombreActual(String rol) {
        String prefijo = rol.startsWith("ROLE_") ? "ROLE_" : "";
        String nombre = rol.substring(prefijo.length());
        return switch (nombre) {
            case "ADMIN_ALMACEN", "TIENDA" -> prefijo + "STORE_ADMIN";
            case "ADMIN_STYLERADAR" -> prefijo + "STYLERADAR_ADMIN";
            case "COMPRADOR" -> prefijo + "BUYER";
            case "REPRESENTANTE_FUNDACION" -> prefijo + "FOUNDATION_REPRESENTATIVE";
            default -> rol;
        };
    }

    public static boolean contieneAlguno(String roles, String... aceptados) {
        Set<String> actuales = leer(roles);
        return Arrays.stream(aceptados).map(rol -> rol.toUpperCase(Locale.ROOT)).anyMatch(actuales::contains);
    }
}
