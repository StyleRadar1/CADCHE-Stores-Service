package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("Credenciales de servicio en /api/v1/internal/**")
class ServicioInternoAuthFilterTest {

    private final ServicioInternoAuthFilter filtro =
            new ServicioInternoAuthFilter("ms-donaciones", "clave-de-prueba", new ObjectMapper().findAndRegisterModules());

    private static String basic(String usuario, String clave) {
        return "Basic " + Base64.getEncoder().encodeToString((usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
    }

    private MockHttpServletResponse ejecutar(ServicioInternoAuthFilter f, String ruta, String autorizacion,
            MockFilterChain cadena) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", ruta);
        req.setRequestURI(ruta);
        if (autorizacion != null) {
            req.addHeader("Authorization", autorizacion);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, cadena);
        return res;
    }

    @Test
    @DisplayName("Con las credenciales correctas la petición continúa")
    void credencialesCorrectas() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse res = ejecutar(filtro, "/api/v1/internal/foundations/7",
                basic("ms-donaciones", "clave-de-prueba"), cadena);
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(cadena.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Sin credenciales, con otra clave, con Bearer o con Base64 inválido responde 401 ProblemDetail")
    void credencialesInvalidas() throws Exception {
        for (String cabecera : new String[]{null, basic("ms-donaciones", "otra"), basic("otro", "clave-de-prueba"),
                "Bearer abc.def.ghi", "Basic %%%"}) {
            MockFilterChain cadena = new MockFilterChain();
            MockHttpServletResponse res = ejecutar(filtro, "/api/v1/internal/foundations", cabecera, cadena);
            assertThat(res.getStatus()).as(String.valueOf(cabecera)).isEqualTo(401);
            assertThat(res.getHeader("WWW-Authenticate")).startsWith("Basic");
            assertThat(res.getContentAsString(StandardCharsets.UTF_8))
                    .contains("UNAUTHENTICATED").contains("Se requieren credenciales de servicio válidas");
            assertThat(cadena.getRequest()).isNull();
        }
    }

    @Test
    @DisplayName("Sin contraseña configurada las rutas internas responden 503 y nunca quedan abiertas")
    void sinConfigurar() throws Exception {
        for (ServicioInternoAuthFilter sinClave : new ServicioInternoAuthFilter[]{
                new ServicioInternoAuthFilter("ms-donaciones", " ", new ObjectMapper().findAndRegisterModules()),
                new ServicioInternoAuthFilter(null, "clave", new ObjectMapper().findAndRegisterModules())}) {
            MockFilterChain cadena = new MockFilterChain();
            MockHttpServletResponse res = ejecutar(sinClave, "/api/v1/internal/foundations/7",
                    basic("ms-donaciones", ""), cadena);
            assertThat(res.getStatus()).isEqualTo(503);
            assertThat(res.getContentAsString(StandardCharsets.UTF_8)).contains("SERVICE_UNAVAILABLE");
            assertThat(cadena.getRequest()).isNull();
        }
    }

    @Test
    @DisplayName("Las rutas que no son internas no pasan por este filtro")
    void otrasRutas() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        MockHttpServletResponse res = ejecutar(filtro, "/api/v1/foundations", null, cadena);
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(cadena.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("BUG-005: Acepta tanto ms-donaciones como ms-auth cuando ambos están configurados")
    void aceptaMultiplesServiciosInternos() throws Exception {
        ServicioInternoAuthFilter multi = new ServicioInternoAuthFilter(
                "ms-donaciones", "clave-don", "ms-auth", "clave-auth",
                new ObjectMapper().findAndRegisterModules());

        MockFilterChain cadenaDon = new MockFilterChain();
        MockHttpServletResponse resDon = ejecutar(multi, "/api/v1/internal/foundations?representativeId=24",
                basic("ms-donaciones", "clave-don"), cadenaDon);
        assertThat(resDon.getStatus()).isEqualTo(200);
        assertThat(cadenaDon.getRequest()).isNotNull();

        MockFilterChain cadenaAuth = new MockFilterChain();
        MockHttpServletResponse resAuth = ejecutar(multi, "/api/v1/internal/stores?ownerUserId=15",
                basic("ms-auth", "clave-auth"), cadenaAuth);
        assertThat(resAuth.getStatus()).isEqualTo(200);
        assertThat(cadenaAuth.getRequest()).isNotNull();

        MockFilterChain cadenaMala = new MockFilterChain();
        MockHttpServletResponse resMala = ejecutar(multi, "/api/v1/internal/stores?ownerUserId=15",
                basic("ms-auth", "incorrecta"), cadenaMala);
        assertThat(resMala.getStatus()).isEqualTo(401);
        assertThat(cadenaMala.getRequest()).isNull();
    }
}
