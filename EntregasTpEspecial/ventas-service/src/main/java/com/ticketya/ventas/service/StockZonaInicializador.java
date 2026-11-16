package com.ticketya.ventas.service;

import com.ticketya.ventas.model.StockZona;
import com.ticketya.ventas.repository.StockZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Crea (una sola vez) la fila de cupo de una zona. Si dos hilos la crean a la vez, la restriccion
// de unicidad rechaza a uno: quien llama atrapa esa excepcion y sigue, porque la fila ya existe.
@Component
@RequiredArgsConstructor
public class StockZonaInicializador {

    private final StockZonaRepository stockRepo;

    @Transactional
    public void asegurar(Long eventoId, Long sectorId, int capacidad) {
        if (stockRepo.findByEventoIdAndSectorId(eventoId, sectorId).isEmpty()) {
            StockZona stock = new StockZona();
            stock.setEventoId(eventoId);
            stock.setSectorId(sectorId);
            stock.setDisponibles(capacidad);
            stockRepo.saveAndFlush(stock);
        }
    }
}
