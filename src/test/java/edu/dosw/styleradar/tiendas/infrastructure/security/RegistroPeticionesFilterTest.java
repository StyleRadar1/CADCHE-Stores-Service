package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RegistroPeticionesFilterTest {

    @Test
    @DisplayName("Registra solo las rutas /api/ y deja pasar la petición")
    void registraSoloApi() throws Exception {
        RegistroPeticionesFilter filtro = new RegistroPeticionesFilter(true);
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/v1/stores/900555777-1");
        api.addHeader("Authorization", "Bearer x");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(api, respuesta, cadena);
        assertThat(cadena.getRequest()).isSameAs(api);
        assertThat(filtro.shouldNotFilter(new MockHttpServletRequest("GET", "/actuator/health"))).isTrue();
        assertThat(filtro.shouldNotFilter(api)).isFalse();
        assertThat(new RegistroPeticionesFilter(false).shouldNotFilter(api)).isTrue();
    }
}
