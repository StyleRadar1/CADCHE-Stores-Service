package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;
import edu.dosw.styleradar.tiendas.domain.port.out.AlmacenEventPublisherPort;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Publica los eventos de almacenes en {@code styleradar.exchange}; si el broker no responde, solo se registra. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQAlmacenEventPublisherAdapter implements AlmacenEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Value("${styleradar.rabbitmq.exchange.fundaciones:" + RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES + "}")
    private String exchangeName = RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES;

    @Override
    public void publicarAlmacenEliminado(AlmacenEliminadoEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, RabbitMQConfig.DEFAULT_ROUTING_KEY_ALMACEN_ELIMINADO, cuerpo(evento));
            log.info("Evento store.deleted publicado para el almacén {}", evento.getAlmacenNit());
        } catch (AmqpException ex) {
            log.warn("No fue posible entregar store.deleted a RabbitMQ (broker no disponible): {}. "
                    + "El almacén ya quedó eliminado en base de datos.", ex.getMessage());
        }
    }

    /** Payload of store.deleted (requirements analysis v6, SR-RF-119). */
    static Map<String, Object> cuerpo(AlmacenEliminadoEvent evento) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventId", evento.getEventoId());
        m.put("storeNit", evento.getAlmacenNit());
        m.put("deletedAt", RabbitMQFundacionEventPublisherAdapter.fechaUtc(evento.getFechaEliminacion()));
        return m;
    }
}
