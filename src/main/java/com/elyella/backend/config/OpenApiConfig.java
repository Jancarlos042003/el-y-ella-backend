package com.elyella.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Springdoc OpenAPI 3.0.3.
 * Disponible en /swagger-ui.html y /api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("El y Ella Detalles — API")
                        .description("API REST del e-commerce de arreglos florales. " +
                                "Los endpoints marcados con el candado requieren un JWT en el header Authorization.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("El y Ella Detalles")
                                .email("contacto@elyella.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo")))
                /*
                 * Esquema de seguridad global: Bearer JWT.
                 * Los controllers usan @SecurityRequirement(name = "Bearer Auth")
                 * para indicar qué endpoints lo requieren.
                 */
                .components(new Components()
                        .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa el token JWT obtenido en /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"));
    }
}
