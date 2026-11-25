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

    @Test
    @DisplayName("La cola de cuentas eliminadas escucha la clave actual de ms-usuarios y user.deleted, con DLQ")
    void colaDeCuentasEliminadas() {
        RabbitMQConfig config = new RabbitMQConfig();
        var exchange = config.eventosExchange(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);
        var cola = config.usuarioEliminadoQueue();
        var dlq = config.usuarioEliminadoDeadLetterQueue();
        var dlx = config.deadLetterExchange();

        assertThat(cola.getName()).isEqualTo("styleradar.tiendas.user-deleted.q");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments()).containsEntry("x-dead-letter-exchange", "styleradar.dlx")
                .containsEntry("x-dead-letter-routing-key", "styleradar.tiendas.user-deleted.dlq");
        assertThat(dlq.isDurable()).isTrue();
        assertThat(dlx.getType()).isEqualTo("direct");
        assertThat(config.cuentaEliminadaBinding(cola, exchange).getRoutingKey())
                .isEqualTo("styleradar.usuarios.cuenta.eliminada");
        assertThat(config.usuarioEliminadoBinding(cola, exchange).getRoutingKey()).isEqualTo("user.deleted");
        var dlqBinding = config.usuarioEliminadoDlqBinding(dlq, dlx);
        assertThat(dlqBinding.getExchange()).isEqualTo("styleradar.dlx");
        assertThat(dlqBinding.getDestination()).isEqualTo("styleradar.tiendas.user-deleted.dlq");
    }
}
