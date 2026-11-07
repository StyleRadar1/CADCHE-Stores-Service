package edu.dosw.styleradar.tiendas.infrastructure.config;

import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
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

    @Bean
    OpenAPI storesServiceOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT emitido por ms-auth (HS256)")))
                .servers(List.of(
                        new Server().url("http://localhost:8140").description("Servidor local ms-tiendas"),
                        new Server().url("http://localhost:8080").description("API Gateway StyleRadar")))
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

