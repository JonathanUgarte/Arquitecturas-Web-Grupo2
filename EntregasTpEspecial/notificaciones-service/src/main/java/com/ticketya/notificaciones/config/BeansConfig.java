package com.ticketya.notificaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeansConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
