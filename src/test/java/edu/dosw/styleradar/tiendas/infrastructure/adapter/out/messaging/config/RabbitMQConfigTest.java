package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Contrato 6.3: el exchange de eventos es styleradar.exchange (topic, durable)")
class RabbitMQConfigTest {

    @Test
    @DisplayName("Declara el exchange topic durable con el nombre del contrato")
    void declaraExchangeDelContrato() {
        var exchange = new RabbitMQConfig().eventosExchange(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);

        assertThat(exchange.getName()).isEqualTo("styleradar.exchange");
        assertThat(exchange.getType()).isEqualTo("topic");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }
}
