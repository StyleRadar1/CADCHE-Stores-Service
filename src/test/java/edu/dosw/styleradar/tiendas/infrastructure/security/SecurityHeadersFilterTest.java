package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

@DisplayName("Pruebas Unitarias de SecurityHeadersFilter - OWASP A02:2025 Security Misconfiguration")
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter securityHeadersFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        securityHeadersFilter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    @DisplayName("Debe inyectar todas las cabeceras de seguridad requeridas por OWASP Top 10:2025")
    void debeInyectarCabecerasDeSeguridad() throws IOException, ServletException {
        request.setRequestURI("/api/v1/almacenes");

        securityHeadersFilter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_X_CONTENT_TYPE_OPTIONS))
                .isEqualTo("nosniff");
        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_X_FRAME_OPTIONS))
                .isEqualTo("DENY");
        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_X_XSS_PROTECTION))
                .isEqualTo("1; mode=block");
        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_STRICT_TRANSPORT_SECURITY))
                .contains("max-age=31536000")
                .contains("includeSubDomains");
        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_CONTENT_SECURITY_POLICY))
                .contains("default-src 'self'")
                .contains("frame-ancestors 'none'");
        assertThat(response.getHeader(SecurityHeadersFilter.HEADER_REFERRER_POLICY))
                .isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Debe continuar la cadena sin fallar cuando la respuesta no es HttpServletResponse")
    void debeContinuarCadenaSiRespuestaNoEsHttpResponse() throws IOException, ServletException {
        jakarta.servlet.ServletResponse genericResponse = org.mockito.Mockito.mock(jakarta.servlet.ServletResponse.class);
        jakarta.servlet.FilterChain mockChain = org.mockito.Mockito.mock(jakarta.servlet.FilterChain.class);

        securityHeadersFilter.doFilter(request, genericResponse, mockChain);

        org.mockito.Mockito.verify(mockChain).doFilter(request, genericResponse);
    }
}

