package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Envoltura de la petición que fija las cabeceras de identidad {@code X-User-*}.
 * <p>
 * Con un JWT válido las cabeceras salen del token; sin token y sin confianza en el Gateway se ocultan.
 * Así un cliente nunca puede suplantar a otro usuario enviando {@code X-User-Roles} a mano (OWASP A01:2025).
 */
final class IdentidadVerificadaRequest extends HttpServletRequestWrapper {

    static final String X_USER_ID = "X-User-Id";
    static final String X_USER_ROLES = "X-User-Roles";
    static final String X_USER_ROLE = "X-User-Role";

    private static final Set<String> CABECERAS_IDENTIDAD = Set.of(
            X_USER_ID.toLowerCase(Locale.ROOT), X_USER_ROLES.toLowerCase(Locale.ROOT), X_USER_ROLE.toLowerCase(Locale.ROOT));

    private final Map<String, String> identidad;

    private IdentidadVerificadaRequest(HttpServletRequest request, Map<String, String> identidad) {
        super(request);
        this.identidad = identidad;
    }

    static IdentidadVerificadaRequest desdeToken(HttpServletRequest request, Long usuarioId, String rolesCsv) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put(X_USER_ID.toLowerCase(Locale.ROOT), String.valueOf(usuarioId));
        valores.put(X_USER_ROLES.toLowerCase(Locale.ROOT), rolesCsv);
        valores.put(X_USER_ROLE.toLowerCase(Locale.ROOT), rolesCsv);
        return new IdentidadVerificadaRequest(request, valores);
    }

    static IdentidadVerificadaRequest sinIdentidad(HttpServletRequest request) {
        return new IdentidadVerificadaRequest(request, Map.of());
    }

    @Override
    public String getHeader(String name) {
        String clave = name.toLowerCase(Locale.ROOT);
        return CABECERAS_IDENTIDAD.contains(clave) ? identidad.get(clave) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String clave = name.toLowerCase(Locale.ROOT);
        if (!CABECERAS_IDENTIDAD.contains(clave)) {
            return super.getHeaders(name);
        }
        String valor = identidad.get(clave);
        return valor == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(valor));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> nombres = Collections.list(super.getHeaderNames()).stream()
                .filter(nombre -> !CABECERAS_IDENTIDAD.contains(nombre.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        nombres.addAll(identidad.keySet());
        return Collections.enumeration(nombres);
    }
}
