package com.ticketya.ventas.config;

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
    public OpenAPI ventasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - Servicio de Ventas")
                        .version("2.0")
                        .description("Reserva temporal de lugares, compra con pago simulado (idempotente), entradas con QR, "
                                + "devoluciones y cancelacion de eventos. Garantiza que una entrada nunca se venda dos veces.\n\n"
                                + "Seguridad (entrega 2): se accede por el gateway con 'Authorization: Bearer <JWT>' "
                                + "(POST /auth/login). Reservas, pagos, entradas y solicitar devoluciones requieren CLIENTE; "
                                + "listar, aprobar y rechazar devoluciones, ADMIN. La disponibilidad es publica. "
                                + "El gateway valida el token y reenvia la identidad en X-Usuario-Id y el rol en X-Rol."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
