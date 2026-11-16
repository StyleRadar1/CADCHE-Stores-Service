package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import edu.dosw.styleradar.tiendas.domain.event.FundacionDesactivadaEvent;
import edu.dosw.styleradar.tiendas.domain.event.FundacionReactivadaEvent;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config.RabbitMQConfig;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias e Integración - RabbitMQFundacionEventPublisherAdapter (SR-RF-65)")
class RabbitMQFundacionEventPublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQFundacionEventPublisherAdapter adapter;

    private FundacionDesactivadaEvent evento;
    private FundacionReactivadaEvent eventoReactivada;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adapter, "exchangeName", RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES);
        ReflectionTestUtils.setField(adapter, "routingKey", RabbitMQConfig.DEFAULT_ROUTING_KEY_DESACTIVADA);
        ReflectionTestUtils.setField(adapter, "routingKeyReactivada", RabbitMQConfig.DEFAULT_ROUTING_KEY_REACTIVADA);

        evento = FundacionDesactivadaEvent.builder()
                .fundacionId(10L)
                .nombreFundacion("Fundación Esperanza")
                .representanteId(22L)
                .fechaDesactivacion(Instant.now())
                .motivo("Cierre administrativo")
                .build();

        eventoReactivada = FundacionReactivadaEvent.builder()
                .fundacionId(10L)
                .nombreFundacion("Fundación Esperanza")
                .representanteId(22L)
                .fechaReactivacion(Instant.now())
                .motivo("Reapertura")
                .build();
    }

    @Test
    @DisplayName("SR-RF-65 Integración: Debe convertir y publicar eventos de desactivación y reactivación al exchange y routing keys configurados")
    void debePublicarEventoExitosamente() {
        assertDoesNotThrow(() -> adapter.publicarFundacionDesactivada(evento));
        assertDoesNotThrow(() -> adapter.publicarFundacionReactivada(eventoReactivada));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq(RabbitMQConfig.DEFAULT_ROUTING_KEY_DESACTIVADA),
                eq(evento));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DEFAULT_EXCHANGE_FUNDACIONES),
                eq(RabbitMQConfig.DEFAULT_ROUTING_KEY_REACTIVADA),
                eq(eventoReactivada));
    }

    @Test
    @DisplayName("SR-RF-65 Resiliencia: Si el broker de RabbitMQ no está disponible, no debe fallar la ejecución")
    void debeManejarExcepcionDeBrokerSinFallar() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused to localhost:5672")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> adapter.publicarFundacionDesactivada(evento));
        assertDoesNotThrow(() -> adapter.publicarFundacionReactivada(eventoReactivada));
    }

    @Test
    @DisplayName("SR-RF-65 Contrato: Constructores de 5 parámetros generan eventoId único automáticamente")
    void debeGenerarEventoIdEnConstructorDeCincoParametros() {
        FundacionDesactivadaEvent ev = new FundacionDesactivadaEvent(
                10L, "Fundación Esperanza", 22L, Instant.now(), "Cierre");
        FundacionReactivadaEvent evRe = new FundacionReactivadaEvent(
                10L, "Fundación Esperanza", 22L, Instant.now(), "Reapertura");
        assertNotNull(ev.getEventoId());
        assertNotNull(evRe.getEventoId());
    }
}
