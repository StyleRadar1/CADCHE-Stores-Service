package edu.dosw.styleradar.tiendas.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * CADCHE-131: en PROD el servicio no arranca sin el host del broker. Spring deja el marcador {@code ${RABBITMQ_HOST}}
 * sin resolver como nombre de host y solo fallaría al publicar, perdiendo los eventos de fundaciones en silencio.
 */
@Component
@Profile("prod")
public class ConfiguracionProduccionValidator implements InitializingBean {

    private final String hostBroker;

    public ConfiguracionProduccionValidator(@Value("${spring.rabbitmq.host:}") String hostBroker) {
        this.hostBroker = hostBroker;
    }

    @Override
    public void afterPropertiesSet() {
        if (hostBroker == null || hostBroker.isBlank() || hostBroker.contains("${")) {
            throw new IllegalStateException(
                    "Falta RABBITMQ_HOST en PROD: configure el secreto RABBITMQ_HOST_PROD (CADCHE-131).");
        }
    }
}
