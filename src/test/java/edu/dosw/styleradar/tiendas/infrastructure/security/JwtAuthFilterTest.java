package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;

@DisplayName("Pruebas Unitarias de JwtAuthFilter y JwtSecurityValidator - OWASP A04 y A07:2025")
class JwtAuthFilterTest {

    private static final String SECRET_256_BITS = "styleradar_super_secret_jwt_key_2025_at_least_32_bytes_long";
    private static final String WEAK_SECRET = "short_secret";

    private ObjectMapper objectMapper;
    private JwtSecurityValidator jwtSecurityValidator;
    private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        jwtSecurityValidator = new JwtSecurityValidator(objectMapper, SECRET_256_BITS);
        jwtAuthFilter = new JwtAuthFilter(jwtSecurityValidator, objectMapper);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("A07: Debe permitir acceso cuando el token JWT es válido y extraer claims")
    void debePermitirAccesoConTokenValido() throws ServletException, IOException {
        String token = jwtSecurityValidator.generarToken(10L, "ROLE_ADMIN_ALMACEN", 3600, SECRET_256_BITS);
        request.addHeader("Authorization", "Bearer " + token);
        request.setRequestURI("/api/v1/almacenes/900123456");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(request.getAttribute("authenticatedUserId")).isEqualTo(10L);
        assertThat(request.getAttribute("authenticatedRoles")).isEqualTo("ROLE_ADMIN_ALMACEN");
    }

    @Test
    @DisplayName("A07: Debe retornar 401 Unauthorized ante un token JWT expirado")
    void debeRetornar401AnteTokenExpirado() throws ServletException, IOException {
        // Token expirado con vigencia negativa (-100 segundos)
        String tokenExpirado = jwtSecurityValidator.generarToken(10L, "ROLE_ADMIN_ALMACEN", -100, SECRET_256_BITS);
        request.addHeader("Authorization", "Bearer " + tokenExpirado);
        request.setRequestURI("/api/v1/almacenes/900123456/catalogo");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("El token de autenticación ha expirado");
    }

    @Test
    @DisplayName("A07: Debe retornar 401 Unauthorized ante un token con firma adulterada")
    void debeRetornar401AnteFirmaAdulterada() throws ServletException, IOException {
        String tokenValido = jwtSecurityValidator.generarToken(10L, "ROLE_ADMIN_ALMACEN", 3600, SECRET_256_BITS);
        // Alterar un carácter de la firma
        String tokenAlterado = tokenValido.substring(0, tokenValido.length() - 2) + "ZZ";
        request.addHeader("Authorization", "Bearer " + tokenAlterado);
        request.setRequestURI("/api/v1/almacenes/900123456/catalogo");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString()).contains("La firma criptográfica del token es inválida");
    }

    @Test
    @DisplayName("A07: Debe continuar la cadena cuando no se proporciona cabecera Authorization")
    void debeContinuarCadenaSinCabeceraAuthorization() throws ServletException, IOException {
        request.setRequestURI("/api/v1/almacenes");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(request.getAttribute("authenticatedUserId")).isNull();
    }

    @Test
    @DisplayName("A04: Debe rechazar claves secretas con entropía menor a 256 bits (32 bytes)")
    void debeRechazarClavesSecretasDebiles() {
        assertThatThrownBy(() -> jwtSecurityValidator.generarToken(10L, "ROLE_USER", 3600, WEAK_SECRET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OWASP A04:2025")
                .hasMessageContaining("256 bits");
    }
}
