package edu.dosw.styleradar.tiendas.infrastructure.adapter.in.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import edu.dosw.styleradar.tiendas.application.dto.event.UsuarioEliminadoEventDTO;
import edu.dosw.styleradar.tiendas.domain.port.in.LimpiarUsuarioEliminadoUseCase;
import edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Consume la eliminación de cuentas de ms-usuarios y limpia los vínculos de tiendas y fundaciones. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsuarioEliminadoConsumer {

    private final LimpiarUsuarioEliminadoUseCase limpiarUsuarioEliminado;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USUARIO_ELIMINADO)
    public void recibir(UsuarioEliminadoEventDTO evento) {
        if (evento == null) {
            log.warn("Evento de cuenta eliminada nulo ignorado");
            return;
        }
        limpiarUsuarioEliminado.limpiar(evento.userId());
    }
}
