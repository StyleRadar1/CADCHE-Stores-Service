package edu.dosw.styleradar.tiendas.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

@DisplayName("Pruebas Unitarias de CorrelationIdFilter - BUG-031")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Propaga X-Correlation-Id a MDC y cabeceras de respuesta y limpia MDC al finalizar")
    void propagaXCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        request.addHeader(CorrelationIdFilter.HEADER_CORRELATION_ID, "store-corr-777");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDurante = new AtomicReference<>();

        FilterChain chain = (req, res) -> mdcDurante.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);

        assertThat(mdcDurante.get()).isEqualTo("store-corr-777");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo("store-corr-777");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_REQUEST_ID)).isEqualTo("store-corr-777");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Usa X-Request-Id si no llega X-Correlation-Id y genera UUID si faltan ambos")
    void usaXRequestIdOGeneraUuid() throws Exception {
        MockHttpServletRequest reqConRequestId = new MockHttpServletRequest("GET", "/api/v1/foundations");
        reqConRequestId.addHeader(CorrelationIdFilter.HEADER_REQUEST_ID, "req-store\r\n");
        MockHttpServletResponse resConRequestId = new MockHttpServletResponse();

        filter.doFilter(reqConRequestId, resConRequestId, new MockFilterChain());
        assertThat(resConRequestId.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo("req-store");

        MockHttpServletRequest reqVacia = new MockHttpServletRequest("GET", "/api/v1/foundations");
        MockHttpServletResponse resVacia = new MockHttpServletResponse();

        filter.doFilter(reqVacia, resVacia, new MockFilterChain());
        assertThat(resVacia.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
