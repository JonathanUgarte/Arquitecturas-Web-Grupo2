package com.ticketya.catalogo;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.config.CacheConfig;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.model.Evento;
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
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Comportamiento de la cache (en memoria en el test; en Docker es Redis, con las mismas anotaciones):
// las lecturas repetidas no van a la base, y cualquier cambio en un evento invalida lo cacheado.
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cache.type=simple")
class CacheTest {

    @MockitoBean VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired EventoRepository eventoRepo;
    @Autowired PrecioSectorRepository precioRepo;
    @Autowired LugarRepository lugarRepo;
    @Autowired CacheManager cacheManager;

    private LugarDTO lugar;

    @BeforeEach
    void preparar() {
        precioRepo.deleteAll();
        eventoRepo.deleteAll();
        lugarRepo.deleteAll();
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        lugar = lugarService.crear(new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1,
                List.of(new SectorRequest("Campo", TipoSector.NO_NUMERADO, 100, null, null))));
    }

    @Test
    void elDetalleDeUnEventoSeSirveDesdeLaCacheHastaQueElEventoCambia() {
        Long id = publicado("Recital Original");
        eventoService.obtener(id);   // primera lectura: va a la base y queda en cache

        renombrarDirectoEnLaBase(id, "Nombre Cambiado");
        assertEquals("Recital Original", eventoService.obtener(id).nombre(), "debe venir de la cache");

        eventoService.suspender(id);   // un cambio por el servicio invalida la cache
        assertEquals("Nombre Cambiado", eventoService.obtener(id).nombre());
        assertNotNull(cacheManager.getCache(CacheConfig.EVENTO).get(id), "al leer de nuevo vuelve a quedar en cache");
    }

    @Test
    void lasBusquedasSeCacheanYSeInvalidanAlPublicarOtroEvento() {
        publicado("Primero");
        assertEquals(1, buscarTodos().size());

        publicado("Segundo");   // publicar invalida las busquedas cacheadas
        assertEquals(2, buscarTodos().size());
    }

    @Test
    void unaBusquedaRepetidaNoVuelveALaBase() {
        Long id = publicado("Original");
        buscarTodos();

        renombrarDirectoEnLaBase(id, "Cambiado");
        assertEquals("Original", buscarTodos().get(0).nombre());
    }

    @Test
    void elListadoDeLugaresSeInvalidaAlCrearUnLugar() {
        assertEquals(1, lugarService.listar().size());
        lugarService.crear(new LugarRequest("Teatro", "Calle 2", "Azul", -36.7, -59.8,
                List.of(new SectorRequest("Platea", TipoSector.NUMERADO, null, 2, 5))));
        assertEquals(2, lugarService.listar().size());
    }

    // ---------- utilidades ----------

    private Long publicado(String nombre) {
        EventoDTO e = eventoService.crear(new EventoRequest(nombre, "x", TipoEvento.RECITAL, "Prod", List.of("Banda"),
                LocalDateTime.now().plusDays(10), lugar.id(), null), 3L);
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(lugar.sectores().get(0).id(), new BigDecimal("1000")))));
        eventoService.publicar(e.id());
        return e.id();
    }

    private List<EventoResumenDTO> buscarTodos() {
        return eventoService.buscar(null, null, null, null, null, null, "fecha");
    }

    // Cambia la base SIN pasar por el servicio (no invalida la cache): sirve para ver de donde viene el dato
    private void renombrarDirectoEnLaBase(Long id, String nombre) {
        Evento e = eventoRepo.findById(id).orElseThrow();
        e.setNombre(nombre);
        eventoRepo.save(e);
    }
}
