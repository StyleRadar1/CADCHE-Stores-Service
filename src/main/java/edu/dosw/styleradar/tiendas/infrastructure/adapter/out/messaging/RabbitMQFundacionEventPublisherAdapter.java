package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.port.out.FundacionEventPublisherPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQFundacionEventPublisherAdapter implements FundacionEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Value("${styleradar.rabbitmq.exchange.fundaciones:" + RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES + "}")
    private String exchangeName;

    @Value("${styleradar.rabbitmq.routingkey.fundacion-desactivada:" + RabbitMQConfig.DEFAULT_ROUTING_KEY_DESACTIVADA + "}")
    private String routingKey;

    @Override
    public void publicarFundacionDesactivada(FundacionDesactivadaEvent evento) {
        try {
            log.info("Publicando evento FundacionDesactivadaEvent a exchange='{}' con routingKey='{}': {}",
                    exchangeName, routingKey, evento);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, evento);
            log.info("Evento FundacionDesactivadaEvent publicado exitosamente");
        } catch (AmqpException ex) {
            log.warn("No fue posible entregar el evento a RabbitMQ (broker no disponible): {}. Se conserva la desactivación en base de datos.",
                    ex.getMessage());
        }
    }
}

