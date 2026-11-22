package edu.dosw.styleradar.tiendas.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class PoolConexionesConfigTest {

    @ParameterizedTest(name = "{0} usa un pool pequeño en el PostgreSQL compartido")
    @ValueSource(strings = {"application.yml", "application-qa.yml", "application-prod.yml"})
    void poolPequeno(String archivo) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(archivo));
        Properties p = yaml.getObject();
        assertThat(Integer.parseInt(p.getProperty("spring.datasource.hikari.maximum-pool-size"))).isLessThanOrEqualTo(3);
        assertThat(p.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("1");
        assertThat(p.getProperty("spring.datasource.hikari.idle-timeout")).isEqualTo("30000");
    }
}
