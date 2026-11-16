package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.dosw.styleradar.tiendas.support.TokensDePrueba;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@DisplayName("JwtAuthFilter · identidad no falsificable (contrato B-0.1/B-0.3, OWASP A01/A07:2025)")
class JwtAuthFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtSecurityValidator validator = new JwtSecurityValidator(TokensDePrueba.SECRETO_DEV, null);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/almacenes/900123456/umbral-ultimas-unidades");
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    private JwtAuthFilter filtro(boolean confiarEnGateway) {
        return new JwtAuthFilter(validator, objectMapper, confiarEnGateway);
    }

    private HttpServletRequest peticionQueLlegoAlControlador() {
        return (HttpServletRequest) filterChain.getRequest();
    }

    @Test
    @DisplayName("Con JWT válido la identidad sale del token aunque el cliente envíe otras cabeceras X-User-*")
    void conTokenValido_identidadDelTokenIgnoraCabecerasFalsas() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + TokensDePrueba.token(10L, List.of("ADMIN_ALMACEN"), 3600));
        request.addHeader("X-User-Id", "999");
        request.addHeader("X-User-Roles", "ADMIN_STYLERADAR");

        filtro(true).doFilter(request, response, filterChain);

        HttpServletRequest llegada = peticionQueLlegoAlControlador();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(llegada.getHeader("X-User-Id")).isEqualTo("10");
        assertThat(llegada.getHeader("x-user-roles")).isEqualTo("ADMIN_ALMACEN");
        assertThat(llegada.getHeader("X-User-Role")).isEqualTo("ADMIN_ALMACEN");
        assertThat(Collections.list(llegada.getHeaders("X-User-Roles"))).containsExactly("ADMIN_ALMACEN");
        assertThat(llegada.getAttribute(JwtAuthFilter.ATRIBUTO_USUARIO)).isEqualTo(10L);
        assertThat(llegada.getAttribute(JwtAuthFilter.ATRIBUTO_ROLES)).isEqualTo("ADMIN_ALMACEN");
        assertThat(Collections.list(llegada.getHeaderNames())).contains("x-user-id", "Authorization");
    }

    @Test
    @DisplayName("Sin token y sin confianza en el Gateway (QA/PROD) las cabeceras X-User-* se ocultan")
    void sinTokenNiConfianza_ocultaCabecerasDeIdentidad() throws ServletException, IOException {
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-User-Roles", "ADMIN_STYLERADAR");
        request.addHeader("Accept", "application/json");

        filtro(false).doFilter(request, response, filterChain);

        HttpServletRequest llegada = peticionQueLlegoAlControlador();
        assertThat(llegada.getHeader("X-User-Id")).isNull();
        assertThat(llegada.getHeader("X-User-Roles")).isNull();
        assertThat(Collections.list(llegada.getHeaders("X-User-Roles"))).isEmpty();
        assertThat(llegada.getHeader("Accept")).isEqualTo("application/json");
        assertThat(Collections.list(llegada.getHeaders("Accept"))).containsExactly("application/json");
        assertThat(Collections.list(llegada.getHeaderNames())).doesNotContain("X-User-Id", "X-User-Roles");
    }

    @Test
    @DisplayName("Sin token y con confianza en el Gateway (dev/test o red restringida) las cabeceras pasan tal cual")
    void sinTokenConConfianza_respetaCabecerasDelGateway() throws ServletException, IOException {
        request.addHeader("X-User-Id", "7");
        request.addHeader("Authorization", "Basic abc");

        filtro(true).doFilter(request, response, filterChain);

        assertThat(peticionQueLlegoAlControlador().getHeader("X-User-Id")).isEqualTo("7");
    }

    @Test
    @DisplayName("Token vencido o firmado con otro secreto responde 401 con el formato de error del contrato")
    void conTokenInvalido_responde401() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + TokensDePrueba.token(
                TokensDePrueba.SECRETO_AJENO, 1L, List.of("ADMIN_STYLERADAR"), 3600));

        filtro(true).doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("inválida o ha sido alterada")
                .contains("\"validationErrors\":{}");
        assertThat(filterChain.getRequest()).isNull();

        MockHttpServletResponse respuestaVencido = new MockHttpServletResponse();
        MockHttpServletRequest vencido = new MockHttpServletRequest();
        vencido.addHeader("Authorization", "Bearer " + TokensDePrueba.token(1L, List.of("COMPRADOR"), -60));
        filtro(false).doFilter(vencido, respuestaVencido, new MockFilterChain());
        assertThat(respuestaVencido.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(respuestaVencido.getContentAsString()).contains("ha expirado");
    }

    @Test
    @DisplayName("B-0.1: una escritura sin token responde 401 antes de validar el cuerpo")
    void escrituraSinToken_responde401() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/api/v1/almacenes");
        request.addHeader("X-User-Id", "1");

        filtro(false).doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString()).contains("Se requiere un token de acceso válido para esta operación");
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Las lecturas sin token siguen pasando (rutas públicas) y el Gateway de confianza puede escribir")
    void lecturaSinTokenYGatewayDeConfianza_pasan() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/v1/fundaciones");
        filtro(false).doFilter(request, response, filterChain);
        assertThat(filterChain.getRequest()).isNotNull();

        MockHttpServletRequest escritura = new MockHttpServletRequest("PUT", "/api/v1/almacenes/900123456-1/configuracion-catalogo");
        escritura.addHeader("X-User-Id", "7");
        MockFilterChain cadena = new MockFilterChain();
        filtro(true).doFilter(escritura, new MockHttpServletResponse(), cadena);
        assertThat(cadena.getRequest()).isNotNull();
    }
}
