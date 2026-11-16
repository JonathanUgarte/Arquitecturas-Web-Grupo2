package com.ticketya.preferencias.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentacion OpenAPI: JSON en /v3/api-docs y pantalla interactiva en /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI preferenciasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - Servicio de Preferencias y Recomendaciones")
                        .version("2.0")
                        .description("Preferencias de cada cliente e historial de compras (MongoDB), y recomendaciones de "
                                + "eventos. El historial se arma consumiendo el mensaje 'compra.confirmada' de RabbitMQ.\n\n"
                                + "Seguridad: por el gateway con 'Authorization: Bearer <JWT>', rol CLIENTE."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
