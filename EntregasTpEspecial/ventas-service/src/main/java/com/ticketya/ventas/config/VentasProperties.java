package com.ticketya.ventas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Se carga desde application.properties (prefijo "ventas")
@Component
@ConfigurationProperties(prefix = "ventas")
@Getter
@Setter
public class VentasProperties {

    private String catalogoUrl = "http://localhost:8081";
    private long reservaMinutos = 10;
    private int maxEntradasPorEvento = 4;
}
