package edu.dosw.styleradar.tiendas.infrastructure.adapter.out.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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

    /** Cola propia de ms-tiendas para la eliminación de cuentas que publica ms-usuarios. */
    public static final String QUEUE_USUARIO_ELIMINADO = "styleradar.tiendas.user-deleted.q";
    public static final String DLQ_USUARIO_ELIMINADO = "styleradar.tiendas.user-deleted.dlq";
    /** Exchange por defecto de RabbitMQ: enruta a la cola cuyo nombre es la routing key (no hay que declararlo). */
    public static final String DLX_EXCHANGE = "";
    /** Clave que publica hoy ms-usuarios y la clave en inglés acordada para cuando la cambie. */
    public static final String ROUTING_KEY_CUENTA_ELIMINADA = "styleradar.usuarios.cuenta.eliminada";
    public static final String ROUTING_KEY_USUARIO_ELIMINADO = "user.deleted";

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

    /**
     * Un fallo al declarar colas o bindings (p. ej. permisos del usuario en el broker) se registra en el log pero no
     * impide que ms-tiendas arranque ni que publique eventos.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    /**
     * Si la cola del consumidor no existe o el broker rechaza al consumidor, el contenedor reintenta en segundo plano
     * en lugar de abortar el arranque de la aplicación (por defecto Spring AMQP lo trata como fatal).
     */
    @Bean
    public ContainerCustomizer<SimpleMessageListenerContainer> consumidoresNoFatales() {
        return contenedor -> {
            contenedor.setMissingQueuesFatal(false);
            contenedor.setPossibleAuthenticationFailureFatal(false);
        };
    }

    /** Un mensaje que falla (p. ej. base de datos caída) va a la DLQ en vez de reintentarse sin fin. */
    @Bean
    public Queue usuarioEliminadoQueue() {
        return QueueBuilder.durable(QUEUE_USUARIO_ELIMINADO)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_USUARIO_ELIMINADO)
                .build();
    }

    @Bean
    public Queue usuarioEliminadoDeadLetterQueue() {
        return QueueBuilder.durable(DLQ_USUARIO_ELIMINADO).build();
    }

    @Bean
    public Binding cuentaEliminadaBinding(Queue usuarioEliminadoQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(usuarioEliminadoQueue).to(eventosExchange).with(ROUTING_KEY_CUENTA_ELIMINADA);
    }

    @Bean
    public Binding usuarioEliminadoBinding(Queue usuarioEliminadoQueue, TopicExchange eventosExchange) {
        return BindingBuilder.bind(usuarioEliminadoQueue).to(eventosExchange).with(ROUTING_KEY_USUARIO_ELIMINADO);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
