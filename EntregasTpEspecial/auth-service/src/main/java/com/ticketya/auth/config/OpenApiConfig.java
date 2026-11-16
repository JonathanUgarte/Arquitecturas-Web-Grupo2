package com.ticketya.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentacion OpenAPI: JSON en /v3/api-docs y pantalla interactiva en /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - Servicio de Autenticacion")
                        .version("2.0")
                        .description("Registro de clientes, login y alta de usuarios con rol. El login devuelve un JWT "
                                + "que se envia en el header 'Authorization: Bearer <token>' a traves del gateway. "
                                + "Roles: CLIENTE, ORGANIZADOR, ADMIN y ACCESO (personal de acceso)."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
