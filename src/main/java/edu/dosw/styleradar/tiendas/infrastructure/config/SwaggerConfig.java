package edu.dosw.styleradar.tiendas.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:}")
    private String customServerUrl;

    @Bean
    OpenAPI storesServiceOpenAPI() {
        List<Server> servers = new ArrayList<>();

        if (customServerUrl != null && !customServerUrl.isBlank()) {
            servers.add(new Server().url(customServerUrl).description("Servidor Activo Cloud (Configurado)"));
        }

        // Entornos Cloud en Azure (QA y PROD)
        servers.add(new Server().url("https://styleradar-tiendas-qa.azurewebsites.net").description("Azure QA - Microservicio Tiendas (:8083)"));
        servers.add(new Server().url("https://styleradar-backend-qa.azurewebsites.net").description("Azure QA - API Gateway (:8080)"));
        servers.add(new Server().url("https://styleradar-tiendas-prod.azurewebsites.net").description("Azure PROD - Microservicio Tiendas (:8083)"));
        servers.add(new Server().url("https://styleradar-backend-prod.azurewebsites.net").description("Azure PROD - API Gateway (:8080)"));

        // Entornos Locales
        servers.add(new Server().url("http://localhost:8083").description("Servidor Local ms-tiendas"));
        servers.add(new Server().url("http://localhost:8080").description("API Gateway Local"));

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT emitido por ms-auth (HS256 / RS256)")))
                .servers(servers)
                .info(new Info()
                        .title("StyleRadar - Stores Service API (ms-tiendas)")
                        .description("Especificación OpenAPI 3.0 del microservicio de gestión de tiendas y fundaciones aliadas de StyleRadar. "
                                + "Gobierna los umbrales de últimas unidades (SR-RF-59), días de nueva llegada (SR-RF-60), verificación de almacenes aliados (SR-RF-62), "
                                + "registro, edición y desactivación de fundaciones aliadas (SR-RF-63, SR-RF-64, SR-RF-65) y galería de hasta 6 fotografías de tienda física (SR-RF-74).")
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipo CADche - StyleRadar DOSW")
                                .email("cadche@styleradar.com")));
    }

    @Bean
    OpenApiCustomizer storesAuthenticationDocumentation() {
        Set<String> publicGets = Set.of(
                "/api/v1/almacenes",
                "/api/v1/almacenes/{nit}/fotografias");
        return api -> {
            if (api.getPaths() == null) {
                return;
            }
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                boolean publicRoute = method == PathItem.HttpMethod.GET && publicGets.contains(path);
                operation.setSecurity(publicRoute
                        ? List.of()
                        : List.of(new SecurityRequirement().addList("bearerAuth")));
            }));
        };
    }
}

