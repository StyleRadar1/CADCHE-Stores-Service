package edu.dosw.styleradar.tiendas.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CADCHE-131: PROD no arranca sin el host del broker")
class ConfiguracionProduccionValidatorTest {

    @Test
    @DisplayName("Sin host, vacío o con el marcador sin resolver: falla al arrancar")
    void sinHostFalla() {
        for (String host : new String[] {null, " ", "${RABBITMQ_HOST}"}) {
            assertThatThrownBy(() -> new ConfiguracionProduccionValidator(host).afterPropertiesSet())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RABBITMQ_HOST_PROD");
        }
    }

    @Test
    @DisplayName("Con un host configurado arranca")
    void conHostArranca() {
        assertThatCode(() -> new ConfiguracionProduccionValidator("broker.styleradar.com").afterPropertiesSet())
                .doesNotThrowAnyException();
    }
}
