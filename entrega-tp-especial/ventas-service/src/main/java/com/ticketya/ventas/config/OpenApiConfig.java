package com.ticketya.ventas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentacion OpenAPI: JSON en /v3/api-docs y pantalla interactiva en /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ventasOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("TicketYa - Servicio de Ventas")
                .version("1.0")
                .description("Reserva temporal de lugares, compra con pago simulado (idempotente), entradas con QR, "
                        + "devoluciones y cancelacion de eventos. Garantiza que una entrada nunca se venda dos veces. "
                        + "Sin autenticacion en la entrega 1: el cliente se identifica con el header X-Usuario-Id "
                        + "y las acciones de administrador exigen X-Rol: ADMIN."));
    }
}
