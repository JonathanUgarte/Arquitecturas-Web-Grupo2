package com.ticketya.fila.config;

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
    public OpenAPI filaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - Servicio de Fila Virtual")
                        .version("2.0")
                        .description("Fila virtual para eventos de alta demanda (Redis). El cliente entra a la fila, consulta su "
                                + "posicion y, cuando le llega su tanda, recibe un turno: un token firmado que tiene que enviar "
                                + "en el header 'X-Turno-Fila' al reservar.\n\nSeguridad: por el gateway con "
                                + "'Authorization: Bearer <JWT>'."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
