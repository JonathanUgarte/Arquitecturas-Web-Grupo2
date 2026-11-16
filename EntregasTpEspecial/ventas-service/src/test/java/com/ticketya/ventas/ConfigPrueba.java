package com.ticketya.ventas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ConfigPrueba {

    @Bean
    @Primary
    public RelojDePrueba relojDePrueba() {
        return new RelojDePrueba();
    }
}
