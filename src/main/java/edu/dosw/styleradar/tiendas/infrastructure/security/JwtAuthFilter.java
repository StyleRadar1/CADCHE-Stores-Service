package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro de autenticación JWT (OWASP A07:2025).
 * Intercepta la cabecera Authorization (Bearer <token>) cuando está presente,
 * valida criptográficamente su firma y expiración, y rechaza tokens alterados o
 * vencidos con HTTP 401 Unauthorized sin fuga de trazas internas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtSecurityValidator jwtSecurityValidator;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthFilter(
            @Autowired(required = false) JwtSecurityValidator jwtSecurityValidator,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.jwtSecurityValidator = jwtSecurityValidator != null
                ? jwtSecurityValidator
                : new JwtSecurityValidator(this.objectMapper, "styleradar_super_secret_jwt_key_2025_at_least_32_bytes_long");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Map<String, Object> claims = jwtSecurityValidator.validarToken(token);
                // Inyectar en atributos de solicitud para controladores o capas posteriores
                if (claims.containsKey("userId")) {
                    Object val = claims.get("userId");
                    Long userId = (val instanceof Number n) ? n.longValue() : Long.parseLong(val.toString());
                    request.setAttribute("authenticatedUserId", userId);
                }
                if (claims.containsKey("roles")) {
                    request.setAttribute("authenticatedRoles", claims.get("roles"));
                }
            } catch (FalloAutenticacionException ex) {
                log.warn("Falla de autenticación en {}: {}", request.getRequestURI(), ex.getMessage());
                escribirRespuestaUnauthorized(response, request.getRequestURI(), ex.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void escribirRespuestaUnauthorized(HttpServletResponse response, String path, String mensaje)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new java.util.LinkedHashMap<>();
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.put("message", mensaje);
        errorResponse.put("path", path);
        errorResponse.put("validationErrors", java.util.Map.of());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
