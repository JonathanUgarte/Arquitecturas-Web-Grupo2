package com.ticketya.catalogo;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.exception.BadRequestException;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import com.ticketya.catalogo.service.EventoService;
import com.ticketya.catalogo.service.LugarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// (9) Busqueda ordenada por relevancia: primero la coincidencia exacta, despues "empieza con", despues "contiene"
@SpringBootTest
@ActiveProfiles("test")
class BusquedaRelevanciaTest {

    @MockitoBean VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired PrecioSectorRepository precioRepo;
    @Autowired EventoRepository eventoRepo;
    @Autowired LugarRepository lugarRepo;

    private Long lugarId;
    private Long sectorId;

    @BeforeEach
    void preparar() {
        precioRepo.deleteAll();
        eventoRepo.deleteAll();
        lugarRepo.deleteAll();
        LugarDTO lugar = lugarService.crear(new LugarRequest("Teatro", "Calle 1", "Tandil", -37.3, -59.1, List.of(
                new SectorRequest("Campo", TipoSector.NO_NUMERADO, 100, null, null))));
        lugarId = lugar.id();
        sectorId = lugar.sectores().get(0).id();
        // el orden por fecha seria: La Gran Rock, Rock Nacional, Rock
        publicar("Contiene", "La Gran Rock", 5);
        publicar("Empieza", "Rock Nacional", 10);
        publicar("Exacto", "Rock", 15);
    }

    private void publicar(String nombre, String artista, int dias) {
        EventoDTO e = eventoService.crear(new EventoRequest(nombre, "x", TipoEvento.RECITAL, "Productora",
                List.of(artista), LocalDateTime.now().plusDays(dias), lugarId, null), 3L);
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(new PrecioRequest(sectorId, new BigDecimal("1000")))));
        eventoService.publicar(e.id());
    }

    private List<String> nombres(String orden) {
        return eventoService.buscar(null, null, "rock", null, null, null, orden).stream()
                .map(EventoResumenDTO::nombre).toList();
    }

    @Test
    void porRelevanciaPrimeroLaCoincidenciaExacta() {
        assertEquals(List.of("Exacto", "Empieza", "Contiene"), nombres("relevancia"));
    }

    @Test
    void porFechaSigueElOrdenCronologico() {
        assertEquals(List.of("Contiene", "Empieza", "Exacto"), nombres("fecha"));
    }

    @Test
    void aIgualRelevanciaGanaElMasProximo() {
        publicar("Exacto antes", "rock", 2);
        assertEquals(List.of("Exacto antes", "Exacto", "Empieza", "Contiene"), nombres("relevancia"));
    }

    @Test
    void unOrdenDesconocidoEsError() {
        assertThrows(BadRequestException.class,
                () -> eventoService.buscar(null, null, "rock", null, null, null, "popularidad"));
    }
}
