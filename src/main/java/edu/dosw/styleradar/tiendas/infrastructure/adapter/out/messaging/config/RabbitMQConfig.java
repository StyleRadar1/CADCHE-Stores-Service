package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DEFAULT_EXCHANGE_FUNDACIONES = "styleradar.exchange";
    public static final String DEFAULT_ROUTING_KEY_DESACTIVADA = "styleradar.tiendas.fundacion.desactivada";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

