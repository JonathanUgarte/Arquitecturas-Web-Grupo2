package com.ticketya.ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// El reloj es un bean para poder "adelantar el tiempo" en los tests (vencimientos, plazo de 72 hs)
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
