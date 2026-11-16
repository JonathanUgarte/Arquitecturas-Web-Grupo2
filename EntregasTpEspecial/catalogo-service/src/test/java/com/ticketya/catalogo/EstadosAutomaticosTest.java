package com.ticketya.catalogo;

import com.ticketya.catalogo.client.EstadisticaVentasDTO;
import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.Evento;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import com.ticketya.catalogo.service.DisponibilidadService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// (1) Estados que cambian solos: AGOTADO cuando ventas vendio toda la capacidad (y vuelta a PUBLICADO si una
// devolucion libera un lugar), y FINALIZADO cuando la fecha del evento ya paso.
@SpringBootTest
@ActiveProfiles("test")
class EstadosAutomaticosTest {

    @MockitoBean VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired DisponibilidadService disponibilidadService;
    @Autowired PrecioSectorRepository precioRepo;
    @Autowired EventoRepository eventoRepo;
    @Autowired LugarRepository lugarRepo;

    private Long eventoId;
    private Long campo;
    private Long platea;

    @BeforeEach
    void preparar() {
        precioRepo.deleteAll();
        eventoRepo.deleteAll();
        lugarRepo.deleteAll();
        LugarDTO lugar = lugarService.crear(new LugarRequest("Teatro", "Calle 1", "Tandil", -37.3, -59.1, List.of(
                new SectorRequest("Campo", TipoSector.NO_NUMERADO, 100, null, null),
                new SectorRequest("Platea", TipoSector.NUMERADO, null, 2, 5))));   // 100 + 10 = 110 lugares
        campo = lugar.sectores().get(0).id();
        platea = lugar.sectores().get(1).id();
        EventoDTO e = eventoService.crear(new EventoRequest("Recital", "x", TipoEvento.RECITAL, "Productora Sur",
                List.of("Banda"), LocalDateTime.now().plusDays(20), lugar.id(), null), 3L);
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(campo, new BigDecimal("15000")),
                new PrecioRequest(platea, new BigDecimal("30000")))));
        eventoService.publicar(e.id());
        eventoId = e.id();
    }

    private void vendidas(long enCampo, long enPlatea) {
        when(ventasClient.estadisticas(eventoId)).thenReturn(List.of(
                new EstadisticaVentasDTO(campo, enCampo, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO),
                new EstadisticaVentasDTO(platea, enPlatea, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    private EstadoEvento estado() {
        return eventoRepo.findById(eventoId).orElseThrow().getEstado();
    }

    @Test
    void conLugaresLibresSigueAlaVenta() {
        vendidas(100, 9);
        disponibilidadService.reevaluar(eventoId);
        assertEquals(EstadoEvento.PUBLICADO, estado());
    }

    @Test
    void alVenderseTodoPasaAAgotadoYUnaDevolucionLoVuelveAPublicar() {
        vendidas(100, 10);
        disponibilidadService.reevaluar(eventoId);
        assertEquals(EstadoEvento.AGOTADO, estado());

        // el evento agotado sigue apareciendo en la busqueda (se muestra como agotado)
        assertTrue(eventoService.buscar(null, null, null, null, null, null, null).stream()
                .anyMatch(r -> r.id().equals(eventoId) && r.estado() == EstadoEvento.AGOTADO));

        vendidas(100, 9);   // se aprobo una devolucion
        disponibilidadService.reevaluar(eventoId);
        assertEquals(EstadoEvento.PUBLICADO, estado());
    }

    @Test
    void unEventoSuspendidoNoCambiaAunqueSeVendaTodo() {
        eventoService.suspender(eventoId);
        vendidas(100, 10);
        disponibilidadService.reevaluar(eventoId);
        assertEquals(EstadoEvento.SUSPENDIDO, estado());
    }

    @Test
    void losEventosQueYaOcurrieronPasanAFinalizado() {
        Evento e = eventoRepo.findById(eventoId).orElseThrow();
        e.setFechaHora(LocalDateTime.now().minusHours(3));
        eventoRepo.save(e);

        int finalizados = eventoService.finalizarEventosPasados();

        assertEquals(1, finalizados);
        assertEquals(EstadoEvento.FINALIZADO, estado());
        assertTrue(eventoService.buscar(null, null, null, null, null, null, null).isEmpty());
        assertEquals(0, eventoService.finalizarEventosPasados());   // no se vuelve a tocar
    }
}
