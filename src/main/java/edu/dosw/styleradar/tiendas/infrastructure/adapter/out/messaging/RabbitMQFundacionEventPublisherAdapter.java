package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
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

    @Value("${styleradar.rabbitmq.routingkey.fundacion-reactivada:" + RabbitMQConfig.DEFAULT_ROUTING_KEY_REACTIVADA + "}")
    private String routingKeyReactivada;

    @Override
    public void publicarFundacionDesactivada(FundacionDesactivadaEvent evento) {
        try {
            log.info("Publicando evento FundacionDesactivadaEvent a exchange='{}' con routingKey='{}': {}",
                    exchangeName, routingKey, evento);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, cuerpo(evento));
            log.info("Evento FundacionDesactivadaEvent publicado exitosamente");
        } catch (AmqpException ex) {
            log.warn("No fue posible entregar el evento a RabbitMQ (broker no disponible): {}. Se conserva la desactivación en base de datos.",
                    ex.getMessage());
        }
    }

    @Override
    public void publicarFundacionReactivada(FundacionReactivadaEvent evento) {
        String targetRoutingKey = routingKeyReactivada != null ? routingKeyReactivada : RabbitMQConfig.DEFAULT_ROUTING_KEY_REACTIVADA;
        try {
            log.info("Publicando evento FundacionReactivadaEvent a exchange='{}' con routingKey='{}': {}",
                    exchangeName, targetRoutingKey, evento);
            rabbitTemplate.convertAndSend(exchangeName, targetRoutingKey, cuerpo(evento));
            log.info("Evento FundacionReactivadaEvent publicado exitosamente");
        } catch (AmqpException ex) {
            log.warn("No fue posible entregar el evento de reactivación a RabbitMQ (broker no disponible): {}. Se conserva la reactivación en base de datos.",
                    ex.getMessage());
        }
    }

    /** Payload of foundation.deactivated with the English field names of the requirements analysis v2. */
    static java.util.Map<String, Object> cuerpo(FundacionDesactivadaEvent evento) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("eventId", evento.getEventoId());
        m.put("foundationId", evento.getFundacionId());
        m.put("foundationName", evento.getNombreFundacion());
        m.put("representativeId", evento.getRepresentanteId());
        m.put("deactivatedAt", fechaUtc(evento.getFechaDesactivacion()));
        m.put("reason", evento.getMotivo());
        return m;
    }

    /** Payload of foundation.reactivated with the English field names of the requirements analysis v2. */
    static java.util.Map<String, Object> cuerpo(FundacionReactivadaEvent evento) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("eventId", evento.getEventoId());
        m.put("foundationId", evento.getFundacionId());
        m.put("foundationName", evento.getNombreFundacion());
        m.put("representativeId", evento.getRepresentanteId());
        m.put("reactivatedAt", fechaUtc(evento.getFechaReactivacion()));
        m.put("reason", evento.getMotivo());
        return m;
    }

    /** Fecha en UTC con formato ISO-8601 ("2026-11-21T22:35:00Z"); sin esto Jackson la envía como número. */
    static String fechaUtc(java.time.Instant fecha) {
        return fecha == null ? null
                : java.time.format.DateTimeFormatter.ISO_INSTANT.format(fecha.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    @Override
    public void publicarFundacionEliminada(edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent evento) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, RabbitMQConfig.DEFAULT_ROUTING_KEY_FUNDACION_ELIMINADA,
                    cuerpo(evento));
            log.info("Evento foundation.deleted publicado para la fundación {}", evento.getFundacionId());
        } catch (AmqpException ex) {
            log.warn("No fue posible entregar foundation.deleted a RabbitMQ (broker no disponible): {}. "
                    + "La fundación ya quedó eliminada en base de datos.", ex.getMessage());
        }
    }

    /** Payload of foundation.deleted (requirements analysis v6, SR-RF-120). */
    static java.util.Map<String, Object> cuerpo(edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent evento) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("eventId", evento.getEventoId());
        m.put("foundationId", evento.getFundacionId());
        m.put("deletedAt", fechaUtc(evento.getFechaEliminacion()));
        return m;
    }
}
