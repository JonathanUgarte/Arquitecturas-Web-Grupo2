package com.ticketya.catalogo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentacion OpenAPI: JSON en /v3/api-docs y pantalla interactiva en /swagger-ui.html
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("TicketYa - Servicio de Catalogo")
                .version("1.0")
                .description("Lugares, sectores y eventos: alta, precios por sector, publicacion, "
                        + "suspension, cancelacion y busqueda por multiples criterios. "
                        + "Sin autenticacion en la entrega 1: las acciones de administrador exigen el header X-Rol: ADMIN."));
    }
}
