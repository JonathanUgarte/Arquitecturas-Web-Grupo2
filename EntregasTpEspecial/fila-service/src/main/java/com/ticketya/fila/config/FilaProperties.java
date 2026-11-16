package com.ticketya.fila.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fila")
@Getter
@Setter
public class FilaProperties {

    // Cuantos clientes se habilitan en cada tanda
    private int tamanioTanda = 50;
    // Cada cuanto se habilita una tanda (milisegundos)
    private long intervaloMs = 5000;
    // Cuanto dura el turno de un cliente habilitado para reservar
    private int minutosTurno = 10;
}
