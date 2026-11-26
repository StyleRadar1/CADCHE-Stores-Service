package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.application.dto.response.ErrorResponseDTO;
import edu.dosw.styleradar.tiendas.domain.model.CodigoError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Protege {@code /api/v1/internal/**} con {@code Authorization: Basic} de servicio (hoy, ms-segunda-vida-donaciones).
 * Sin contraseña configurada ({@code DONACIONES_INTERNAL_PASSWORD}) las rutas internas responden 503 y nunca quedan
 * abiertas. La comparación es de tiempo constante.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@Slf4j
public class ServicioInternoAuthFilter extends OncePerRequestFilter {

    static final String PREFIJO = "/api/v1/internal/";
    private static final String BASIC = "Basic ";

    private final byte[] credencialEsperada;
    private final ObjectMapper objectMapper;

    public ServicioInternoAuthFilter(
            @Value("${styleradar.internal.donaciones.username:ms-donaciones}") String usuario,
            @Value("${styleradar.internal.donaciones.password:}") String clave,
            ObjectMapper objectMapper) {
        this.credencialEsperada = clave == null || clave.isBlank() || usuario == null || usuario.isBlank()
                ? null
                : (usuario.trim() + ":" + clave).getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIJO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (credencialEsperada == null) {
            log.error("Petición a {} rechazada: falta DONACIONES_INTERNAL_PASSWORD", request.getRequestURI());
            responder(response, request.getRequestURI(), CodigoError.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
                    "El acceso interno de servicios no está configurado");
            return;
        }
        if (!credencialValida(request.getHeader("Authorization"))) {
            log.warn("Petición a {} rechazada: credenciales de servicio ausentes o inválidas", request.getRequestURI());
            response.setHeader("WWW-Authenticate", "Basic realm=\"ms-tiendas internal\"");
            responder(response, request.getRequestURI(), CodigoError.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
                    "Se requieren credenciales de servicio válidas");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean credencialValida(String cabecera) {
        if (cabecera == null || !cabecera.startsWith(BASIC)) {
            return false;
        }
        try {
            byte[] recibida = Base64.getDecoder().decode(cabecera.substring(BASIC.length()).trim());
            return MessageDigest.isEqual(credencialEsperada, recibida);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void responder(HttpServletResponse response, String ruta, CodigoError codigo, HttpStatus estado,
            String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(ErrorResponseDTO.MEDIA_TYPE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorResponseDTO.problema(codigo, estado.getReasonPhrase(), mensaje, ruta, Map.of())));
    }
}
