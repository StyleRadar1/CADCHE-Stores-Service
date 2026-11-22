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
            servers.add(new Server().url(customServerUrl).description("Active Cloud Server (Configured)"));
        }

        // Cloud environments on Azure (QA and PROD)
        servers.add(new Server().url("https://styleradar-tiendas-qa.azurewebsites.net").description("Azure QA - Stores Service (:8140)"));
        servers.add(new Server().url("https://styleradar-backend-qa.azurewebsites.net").description("Azure QA - API Gateway (:8080)"));
        servers.add(new Server().url("https://styleradar-tiendas-prod.azurewebsites.net").description("Azure PROD - Stores Service (:8140)"));
        servers.add(new Server().url("https://styleradar-backend-prod.azurewebsites.net").description("Azure PROD - API Gateway (:8080)"));

        // Local environments
        servers.add(new Server().url("http://localhost:8140").description("Local ms-tiendas server (:8140)"));
        servers.add(new Server().url("http://localhost:8080").description("Local API Gateway (:8080)"));

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT issued by ms-auth (HS256)")))
                .servers(servers)
                .info(new Info()
                        .title("StyleRadar - Stores Service API (ms-tiendas)")
                        .description("OpenAPI 3.0 specification of the StyleRadar stores and partner foundations microservice: low-stock threshold (SR-RF-59), new-arrival days (SR-RF-60), store verification (SR-RF-62), registration, edition, deactivation and reactivation of partner foundations (SR-RF-63, SR-RF-64, SR-RF-65) and a gallery of up to 6 physical-store photos (SR-RF-74). Routes, fields, values and error codes are in English; only the user messages inside the JSON are in Spanish.")
                        .version("v1")
                        .contact(new Contact()
                                .name("CADche team - StyleRadar DOSW")
                                .email("cadche@styleradar.com")));
    }

    @Bean
    OpenApiCustomizer storesAuthenticationDocumentation() {
        Set<String> publicGets = Set.of(
                "/api/v1/stores",
                "/api/v1/stores/{nit}",
                "/api/v1/stores/{nit}/catalog-settings",
                "/api/v1/stores/{nit}/low-stock-threshold",
                "/api/v1/stores/{nit}/new-arrival-days",
                "/api/v1/stores/{nit}/photos",
                "/api/v1/foundations");
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

