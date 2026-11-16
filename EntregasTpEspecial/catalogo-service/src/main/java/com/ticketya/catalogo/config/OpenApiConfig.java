package com.ticketya.catalogo.config;

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
    public OpenAPI catalogoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - Servicio de Catalogo")
                        .version("2.0")
                        .description("Lugares, sectores y eventos: alta, precios por sector, publicacion, "
                                + "suspension, cancelacion y busqueda por multiples criterios.\n\n"
                                + "Seguridad (entrega 2): se accede por el gateway con 'Authorization: Bearer <JWT>' "
                                + "(POST /auth/login). Las consultas (GET) son publicas. Crear lugares y eventos, "
                                + "configurar precios y publicar requiere ORGANIZADOR o ADMIN; suspender y cancelar, ADMIN. "
                                + "El gateway valida el token y reenvia el rol en el header X-Rol."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
