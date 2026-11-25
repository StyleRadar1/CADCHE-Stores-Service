package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import edu.dosw.styleradar.tiendas.domain.event.AlmacenEliminadoEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionEliminadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.VerificacionAlmacenCambiadaEvent;
import edu.dosw.styleradar.tiendas.domain.model.EstadoVerificacionAlmacen;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config.RabbitMQConfig;

@DisplayName("Eventos store.deleted y foundation.deleted (análisis v6)")
class EventosEliminacionPublisherTest {

    private final Instant fecha = Instant.parse("2026-11-24T15:04:05.123Z");

    @Test
    @DisplayName("SR-RF-119: store.deleted en styleradar.exchange con eventId, storeNit y deletedAt en UTC")
    void almacenEliminado() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitMQAlmacenEventPublisherAdapter adapter = new RabbitMQAlmacenEventPublisherAdapter(template);
        AlmacenEliminadoEvent evento = AlmacenEliminadoEvent.de("900123456-1", fecha);

        adapter.publicarAlmacenEliminado(evento);

        verify(template).convertAndSend(eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq(RabbitMQConfig.DEFAULT_ROUTING_KEY_ALMACEN_ELIMINADO), any(Object.class));
        Map<String, Object> cuerpo = RabbitMQAlmacenEventPublisherAdapter.cuerpo(evento);
        assertThat(cuerpo).containsOnlyKeys("eventId", "storeNit", "deletedAt")
                .containsEntry("storeNit", "900123456-1").containsEntry("deletedAt", "2026-11-24T15:04:05Z");

        doThrow(new AmqpConnectException(new RuntimeException("broker caído"))).when(template)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));
        assertThatCode(() -> adapter.publicarAlmacenEliminado(evento)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SR-RF-120: foundation.deleted con eventId, foundationId y deletedAt; broker caído no rompe la eliminación")
    void fundacionEliminada() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitMQFundacionEventPublisherAdapter adapter = new RabbitMQFundacionEventPublisherAdapter(template);
        ReflectionTestUtils.setField(adapter, "exchangeName", RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);
        FundacionEliminadaEvent evento = FundacionEliminadaEvent.de(7L, fecha);

        adapter.publicarFundacionEliminada(evento);

        verify(template).convertAndSend(eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq(RabbitMQConfig.DEFAULT_ROUTING_KEY_FUNDACION_ELIMINADA), any(Object.class));
        assertThat(RabbitMQFundacionEventPublisherAdapter.cuerpo(evento)).containsOnlyKeys("eventId", "foundationId", "deletedAt")
                .containsEntry("foundationId", 7L).containsEntry("deletedAt", "2026-11-24T15:04:05Z");

        doThrow(new AmqpConnectException(new RuntimeException("broker caído"))).when(template)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));
        assertThatCode(() -> adapter.publicarFundacionEliminada(evento)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("store.verification.changed con eventType, storeNit, status, previousStatus y changedAt en UTC")
    void verificacionCambiada() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitMQAlmacenEventPublisherAdapter adapter = new RabbitMQAlmacenEventPublisherAdapter(template);
        VerificacionAlmacenCambiadaEvent evento = VerificacionAlmacenCambiadaEvent.de(
                "900123456-1", EstadoVerificacionAlmacen.PENDING, EstadoVerificacionAlmacen.APPROVED, fecha);

        adapter.publicarVerificacionCambiada(evento);

        verify(template).convertAndSend(eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq(RabbitMQConfig.DEFAULT_ROUTING_KEY_VERIFICACION_ALMACEN), any(Object.class));
        assertThat(RabbitMQConfig.DEFAULT_ROUTING_KEY_VERIFICACION_ALMACEN).isEqualTo("store.verification.changed");
        assertThat(RabbitMQAlmacenEventPublisherAdapter.cuerpo(evento))
                .containsOnlyKeys("eventId", "eventType", "storeNit", "status", "previousStatus", "changedAt")
                .containsEntry("eventType", "STORE_VERIFICATION_CHANGED")
                .containsEntry("storeNit", "900123456-1")
                .containsEntry("status", "APPROVED")
                .containsEntry("previousStatus", "PENDING")
                .containsEntry("changedAt", "2026-11-24T15:04:05Z");

        VerificacionAlmacenCambiadaEvent registro = VerificacionAlmacenCambiadaEvent.de(
                "900123456-1", null, EstadoVerificacionAlmacen.PENDING, fecha);
        assertThat(RabbitMQAlmacenEventPublisherAdapter.cuerpo(registro)).containsEntry("previousStatus", null);

        doThrow(new AmqpConnectException(new RuntimeException("broker caído"))).when(template)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));
        assertThatCode(() -> adapter.publicarVerificacionCambiada(evento)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("foundation.status.changed con eventType, foundationId, action, active, delta y occurredAt en UTC")
    void fundacionEstadoCambiado() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitMQFundacionEventPublisherAdapter adapter = new RabbitMQFundacionEventPublisherAdapter(template);
        ReflectionTestUtils.setField(adapter, "exchangeName", RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);
        edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent evento =
                edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent.desactivada(7L, fecha);

        adapter.publicarEstadoCambiado(evento);

        verify(template).convertAndSend(eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq("foundation.status.changed"), any(Object.class));
        assertThat(RabbitMQFundacionEventPublisherAdapter.cuerpo(evento))
                .containsOnlyKeys("eventId", "eventType", "foundationId", "action", "active", "delta", "occurredAt")
                .containsEntry("eventType", "FOUNDATION_STATUS_CHANGED").containsEntry("foundationId", 7L)
                .containsEntry("action", "DEACTIVATED").containsEntry("active", false).containsEntry("delta", -1)
                .containsEntry("occurredAt", "2026-11-24T15:04:05Z");
        assertThat(RabbitMQFundacionEventPublisherAdapter.cuerpo(
                edu.dosw.styleradar.tiendas.domain.event.FundacionEstadoCambiadoEvent.registrada(7L, fecha)))
                .containsEntry("action", "REGISTERED").containsEntry("active", true).containsEntry("delta", 1);

        doThrow(new AmqpConnectException(new RuntimeException("broker caído"))).when(template)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));
        assertThatCode(() -> adapter.publicarEstadoCambiado(evento)).doesNotThrowAnyException();
    }
}
