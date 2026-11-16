package com.ticketya.fila.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Cada 'fila.intervalo-ms' habilita una tanda en cada evento que tiene gente esperando
@Component
@ConditionalOnProperty(name = "fila.planificador.habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PlanificadorDeTandas {

    private final FilaService filaService;

    @Scheduled(fixedDelayString = "${fila.intervalo-ms}")
    public void habilitarTandas() {
        for (String eventoId : filaService.eventosConFilaActiva()) {
            filaService.habilitarSiguienteTanda(Long.valueOf(eventoId));
        }
    }
}
