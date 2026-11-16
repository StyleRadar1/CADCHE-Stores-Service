package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.domain.exception.FalloAutenticacionException;
import edu.dosw.styleradar.tiendas.domain.model.CodigoError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Normaliza la identidad de cada petición (contrato Front ↔ ms-tiendas, B-0.1 y B-0.3).
 * <ul>
 *   <li>Con {@code Authorization: Bearer}: valida el JWT de ms-auth y la identidad sale del token; las
 *       cabeceras {@code X-User-*} que envíe el cliente se ignoran. Token inválido o vencido → 401.</li>
 *   <li>Sin token: las cabeceras {@code X-User-*} solo se aceptan si
 *       {@code styleradar.security.trust-gateway-headers=true} (dev/test, o QA/PROD con el servicio
 *       restringido a la red del API Gateway). En caso contrario se ocultan y las funciones restringidas
 *       responden 401/403.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    static final String ATRIBUTO_USUARIO = "authenticatedUserId";
    static final String ATRIBUTO_ROLES = "authenticatedRoles";
    private static final String BEARER = "Bearer ";

    private final JwtSecurityValidator jwtSecurityValidator;
    private final ObjectMapper objectMapper;
    private final boolean confiarCabecerasGateway;

    public JwtAuthFilter(
            JwtSecurityValidator jwtSecurityValidator,
            ObjectMapper objectMapper,
            @Value("${styleradar.security.trust-gateway-headers:false}") boolean confiarCabecerasGateway) {
        this.jwtSecurityValidator = jwtSecurityValidator;
        this.objectMapper = objectMapper;
        this.confiarCabecerasGateway = confiarCabecerasGateway;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(confiarCabecerasGateway ? request : IdentidadVerificadaRequest.sinIdentidad(request),
                    response);
            return;
        }

        JwtSecurityValidator.IdentidadToken identidad;
        try {
            identidad = jwtSecurityValidator.validar(authHeader.substring(BEARER.length()));
        } catch (FalloAutenticacionException ex) {
            log.warn("Falla de autenticación en {}: {}", request.getRequestURI(), ex.getMessage());
            escribirRespuestaUnauthorized(response, request.getRequestURI(), ex.getMessage());
            return;
        }

        HttpServletRequest verificada =
                IdentidadVerificadaRequest.desdeToken(request, identidad.usuarioId(), identidad.rolesCsv());
        verificada.setAttribute(ATRIBUTO_USUARIO, identidad.usuarioId());
        verificada.setAttribute(ATRIBUTO_ROLES, identidad.rolesCsv());
        filterChain.doFilter(verificada, response);
    }

    private void escribirRespuestaUnauthorized(HttpServletResponse response, String path, String mensaje)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(ErrorResponseDTO.MEDIA_TYPE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDTO errorResponse = ErrorResponseDTO.problema(CodigoError.NO_AUTENTICADO,
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), mensaje, path, Map.of());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
