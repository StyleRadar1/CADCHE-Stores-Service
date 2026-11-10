package edu.dosw.styleradar.tiendas.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Cross-Origin Resource Sharing (CORS) restringida a dominios
 * autorizados de StyleRadar (OWASP A02:2025).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:8140",
                        "https://styleradar.com",
                        "https://*.styleradar.com",
                        "https://*.azurewebsites.net")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-User-Id", "X-User-Roles", "X-User-Role")
                .exposedHeaders("X-Total-Count", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

