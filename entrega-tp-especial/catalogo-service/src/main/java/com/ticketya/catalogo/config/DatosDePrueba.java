package com.ticketya.catalogo.config;

import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.service.EventoService;
import com.ticketya.catalogo.service.LugarService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Carga datos de ejemplo la primera vez que arranca (si la base esta vacia)
@Component
@RequiredArgsConstructor
public class DatosDePrueba implements CommandLineRunner {

    private final LugarRepository lugarRepo;
    private final LugarService lugarService;
    private final EventoService eventoService;

    @Override
    public void run(String... args) {
        if (lugarRepo.count() > 0) {
            return;
        }

        LugarDTO lugar = lugarService.crear(new LugarRequest(
                "Estadio Municipal", "Av. Rivadavia 1000", "Tandil", -37.32, -59.13,
                List.of(new SectorRequest("Campo", TipoSector.NO_NUMERADO, 5000, null, null),
                        new SectorRequest("Platea", TipoSector.NUMERADO, null, 5, 10))));

        Long campo = lugar.sectores().get(0).id();
        Long platea = lugar.sectores().get(1).id();

        // Evento 1: publicado, listo para vender
        EventoDTO recital = eventoService.crear(new EventoRequest(
                "Recital de Prueba", "Show de rock en el estadio", TipoEvento.RECITAL, "Productora Sur",
                List.of("Banda Uno", "Banda Dos"), LocalDateTime.now().plusDays(30), lugar.id()));
        eventoService.configurarPrecios(recital.id(), new PreciosRequest(List.of(
                new PrecioRequest(campo, new BigDecimal("15000")),
                new PrecioRequest(platea, new BigDecimal("30000")))));
        eventoService.publicar(recital.id());

        // Evento 2: en borrador (para probar precios y publicacion)
        eventoService.crear(new EventoRequest(
                "Obra de Teatro de Prueba", "Comedia en el teatro", TipoEvento.TEATRO, "Compania Central",
                List.of("Elenco Estable"), LocalDateTime.now().plusDays(45), lugar.id()));
    }
}
