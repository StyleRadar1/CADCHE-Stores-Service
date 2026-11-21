package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Deja una línea de log por cada petición a la API (método, ruta, estado, duración y si llegó con token), para
 * comprobar en el Log stream de Azure que otro microservicio sí llamó a ms-tiendas. No registra el token, el cuerpo
 * ni parámetros de consulta. Se desactiva con {@code styleradar.logging.requests=false}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroPeticionesFilter extends OncePerRequestFilter {

    private final boolean activo;

    public RegistroPeticionesFilter(@Value("${styleradar.logging.requests:true}") boolean activo) {
        this.activo = activo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !activo || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            String autorizacion = request.getHeader("Authorization");
            log.info("HTTP {} {} -> {} ({} ms, token={})",
                    request.getMethod(),
                    request.getRequestURI().replaceAll("[\r\n]", "_"),
                    response.getStatus(),
                    ms,
                    autorizacion != null && autorizacion.startsWith("Bearer ") ? "si" : "no");
        }
    }
}
