package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DEFAULT_EXCHANGE_FUNDACIONES = "styleradar.fundaciones.exchange";
    public static final String DEFAULT_ROUTING_KEY_DESACTIVADA = "fundacion.desactivada";

    @Bean
    public TopicExchange fundacionesExchange(
            @Value("${styleradar.rabbitmq.exchange.fundaciones:" + DEFAULT_EXCHANGE_FUNDACIONES + "}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

