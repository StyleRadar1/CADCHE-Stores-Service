package edu.dosw.styleradar.tiendas.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
 * Protege {@code /api/v1/internal/**} con {@code Authorization: Basic} de servicio (ms-segunda-vida-donaciones y ms-auth).
 * Sin contraseñas configuradas las rutas internas responden 503 y nunca quedan abiertas. La comparación es de tiempo constante.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@Slf4j
public class ServicioInternoAuthFilter extends OncePerRequestFilter {

    static final String PREFIJO = "/api/v1/internal/";
    private static final String BASIC = "Basic ";

    private final List<byte[]> credencialesEsperadas;
    private final ObjectMapper objectMapper;

    public ServicioInternoAuthFilter(
            String usuario,
            String clave,
            ObjectMapper objectMapper) {
        this(usuario, clave, null, null, objectMapper);
    }

    @Autowired
    public ServicioInternoAuthFilter(
            @Value("${styleradar.internal.donaciones.username:ms-donaciones}") String donacionesUsuario,
            @Value("${styleradar.internal.donaciones.password:}") String donacionesClave,
            @Value("${styleradar.internal.auth.username:ms-auth}") String authUsuario,
            @Value("${styleradar.internal.auth.password:}") String authClave,
            ObjectMapper objectMapper) {
        List<byte[]> credenciales = new ArrayList<>();
        agregarSiConfigurada(credenciales, donacionesUsuario, donacionesClave);
        agregarSiConfigurada(credenciales, authUsuario, authClave);
        this.credencialesEsperadas = List.copyOf(credenciales);
        this.objectMapper = objectMapper;
    }

    private static void agregarSiConfigurada(List<byte[]> destino, String usuario, String clave) {
        if (usuario != null && !usuario.isBlank() && clave != null && !clave.isBlank()) {
            destino.add((usuario.trim() + ":" + clave).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIJO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (credencialesEsperadas.isEmpty()) {
            log.error("Petición a {} rechazada: faltan credenciales internas (DONACIONES_INTERNAL_PASSWORD / AUTH_INTERNAL_PASSWORD)",
                    request.getRequestURI());
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
            boolean coincide = false;
            for (byte[] esperada : credencialesEsperadas) {
                coincide |= MessageDigest.isEqual(esperada, recibida);
            }
            return coincide;
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
