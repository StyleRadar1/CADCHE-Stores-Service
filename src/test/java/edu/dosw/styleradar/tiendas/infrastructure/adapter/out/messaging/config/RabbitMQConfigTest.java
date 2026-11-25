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
    @DisplayName("La cola de cuentas eliminadas escucha la clave actual de ms-usuarios y user.deleted, con DLQ por el exchange por defecto")
    void colaDeCuentasEliminadas() {
        RabbitMQConfig config = new RabbitMQConfig();
        var exchange = config.eventosExchange(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);
        var cola = config.usuarioEliminadoQueue();
        var dlq = config.usuarioEliminadoDeadLetterQueue();

        assertThat(cola.getName()).isEqualTo("styleradar.tiendas.user-deleted.q");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments()).containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", "styleradar.tiendas.user-deleted.dlq");
        assertThat(dlq.getName()).isEqualTo("styleradar.tiendas.user-deleted.dlq");
        assertThat(dlq.isDurable()).isTrue();
        assertThat(config.cuentaEliminadaBinding(cola, exchange).getRoutingKey())
                .isEqualTo("styleradar.usuarios.cuenta.eliminada");
        assertThat(config.usuarioEliminadoBinding(cola, exchange).getRoutingKey()).isEqualTo("user.deleted");
    }

    @Test
    @DisplayName("El broker no puede tumbar el arranque: declaraciones toleradas y consumidores que reintentan")
    void brokerNoTumbaElArranque() {
        RabbitMQConfig config = new RabbitMQConfig();
        var admin = config.rabbitAdmin(org.mockito.Mockito.mock(
                org.springframework.amqp.rabbit.connection.ConnectionFactory.class));
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(admin, "ignoreDeclarationExceptions")).isEqualTo(true);

        var contenedor = new org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer();
        config.consumidoresNoFatales().configure(contenedor);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(contenedor, "missingQueuesFatal")).isEqualTo(false);
        assertThat(contenedor.isPossibleAuthenticationFailureFatal()).isFalse();
    }
}
