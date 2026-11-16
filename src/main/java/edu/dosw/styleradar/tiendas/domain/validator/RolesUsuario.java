package edu.dosw.styleradar.tiendas.domain.validator;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lectura de los roles recibidos como lista separada por comas (por ejemplo {@code "ADMIN_STYLERADAR"} o
 * {@code "ROLE_ADMIN_ALMACEN,COMPRADOR"}). Compara roles completos, nunca subcadenas, para que un rol como
 * {@code NO_ADMIN_STYLERADAR} no se confunda con {@code ADMIN_STYLERADAR} (OWASP A01:2025).
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
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean contieneAlguno(String roles, String... aceptados) {
        Set<String> actuales = leer(roles);
        return Arrays.stream(aceptados).map(rol -> rol.toUpperCase(Locale.ROOT)).anyMatch(actuales::contains);
    }
}
