package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.rest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuthRequestContextResolver {

    private AuthRequestContextResolver() {
    }

    public static Long resolveUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra
                && sra.getRequest().getAttribute("authenticatedUserId") instanceof Long id) {
            return id;
        }
        return null;
    }

    public static String resolveRole(String rolesHeader, String roleHeader) {
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            return rolesHeader;
        }
        if (roleHeader != null && !roleHeader.isBlank()) {
            return roleHeader;
        }
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            Object attr = sra.getRequest().getAttribute("authenticatedRoles");
            if (attr != null && !attr.toString().isBlank()) {
                return attr.toString();
            }
        }
        return null;
    }
}

