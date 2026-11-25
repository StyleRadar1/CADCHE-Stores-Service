package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DEFAULT_EXCHANGE_FUNDACIONES = "styleradar.exchange";
    public static final String DEFAULT_ROUTING_KEY_DESACTIVADA = "foundation.deactivated";
    public static final String DEFAULT_ROUTING_KEY_REACTIVADA = "foundation.reactivated";
    /** SR-RF-120 (análisis v6). */
    public static final String DEFAULT_ROUTING_KEY_FUNDACION_ELIMINADA = "foundation.deleted";
    public static final String DEFAULT_ROUTING_KEY_FUNDACION_ESTADO = "foundation.status.changed";
    /** SR-RF-119 (análisis v6). */
    public static final String DEFAULT_ROUTING_KEY_ALMACEN_ELIMINADO = "store.deleted";
    public static final String DEFAULT_ROUTING_KEY_VERIFICACION_ALMACEN = "store.verification.changed";

    /**
     * Declara el exchange de eventos ({@code topic}, durable), igual al creado en el broker del curso. RabbitAdmin lo
     * declara al conectarse de forma idempotente: sin él, en un broker nuevo los eventos de fundaciones se
     * descartarían en silencio (RabbitMQ no entrega mensajes a un exchange inexistente).
     */
    @Bean
    public TopicExchange eventosExchange(
            @Value("${styleradar.rabbitmq.exchange.fundaciones:" + DEFAULT_EXCHANGE_FUNDACIONES + "}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
