package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.amqp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import edu.dosw.styleradar.tiendas.application.dto.event.UsuarioEliminadoEventDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.LimpiarUsuarioEliminadoUseCase;

@DisplayName("Consumidor de cuentas eliminadas de ms-usuarios")
class UsuarioEliminadoConsumerTest {

    private final LimpiarUsuarioEliminadoUseCase useCase = mock(LimpiarUsuarioEliminadoUseCase.class);
    private final UsuarioEliminadoConsumer consumer = new UsuarioEliminadoConsumer(useCase);

    @Test
    @DisplayName("Entrega el userId del evento al caso de uso")
    void entregaUserId() {
        consumer.recibir(new UsuarioEliminadoEventDTO(42L, "2026-11-25T10:00:00Z"));
        verify(useCase).limpiar(42L);
    }

    @Test
    @DisplayName("Ignora un evento nulo")
    void ignoraNulo() {
        consumer.recibir(null);
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Lee el JSON de ms-usuarios {userId, deletedAt} e ignora campos adicionales")
    void leeJsonDeUsuarios() {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setInferredArgumentType(UsuarioEliminadoEventDTO.class);
        byte[] cuerpo = "{\"eventId\":\"e-1\",\"userId\":42,\"deletedAt\":\"2026-11-25T10:00:00Z\",\"extra\":true}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        Object evento = new Jackson2JsonMessageConverter().fromMessage(new Message(cuerpo, props));

        assertThat(evento).isEqualTo(new UsuarioEliminadoEventDTO(42L, "2026-11-25T10:00:00Z"));
    }
}
