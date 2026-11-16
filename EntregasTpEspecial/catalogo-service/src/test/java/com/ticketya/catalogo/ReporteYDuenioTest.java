package com.ticketya.catalogo;

import com.ticketya.catalogo.client.EstadisticaVentasDTO;
import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.exception.ForbiddenException;
import com.ticketya.catalogo.exception.ServicioNoDisponibleException;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import com.ticketya.catalogo.service.EventoService;
import com.ticketya.catalogo.service.LugarService;
import com.ticketya.catalogo.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// (12) Reportes para organizadores y control de que cada organizador solo maneje sus propios eventos
@SpringBootTest
@ActiveProfiles("test")
class ReporteYDuenioTest {

    private static final Long DUENIO = 3L;
    private static final Long OTRO_ORGANIZADOR = 7L;

    @MockitoBean VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired ReporteService reporteService;
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
        LugarDTO lugar = lugarService.crear(new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1, List.of(
                new SectorRequest("Campo", TipoSector.NO_NUMERADO, 1000, null, null),
                new SectorRequest("Platea", TipoSector.NUMERADO, null, 2, 5))));
        campo = lugar.sectores().get(0).id();
        platea = lugar.sectores().get(1).id();
        EventoDTO e = eventoService.crear(new EventoRequest("Recital", "x", TipoEvento.RECITAL, "Productora Sur",
                List.of("Banda"), LocalDateTime.now().plusDays(20), lugar.id(), null), DUENIO);
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(campo, new BigDecimal("15000")),
                new PrecioRequest(platea, new BigDecimal("30000")))));
        eventoService.publicar(e.id());
        eventoId = e.id();
    }

    @Test
    void elReporteCalculaOcupacionYRecaudacionPorSectorYEnTotal() {
        when(ventasClient.estadisticas(eventoId)).thenReturn(List.of(
                new EstadisticaVentasDTO(campo, 250, 100, 5, 0, new BigDecimal("3750000"), new BigDecimal("75000")),
                new EstadisticaVentasDTO(platea, 10, 0, 0, 0, new BigDecimal("300000"), BigDecimal.ZERO)));

        ReporteEventoDTO r = reporteService.reporte(eventoId, DUENIO, "ORGANIZADOR");

        ReporteSectorDTO c = r.sectores().get(0);
        assertEquals("Campo", c.sector());
        assertEquals(1000, c.capacidad());
        assertEquals(750, c.disponibles());
        assertEquals(new BigDecimal("25.0"), c.ocupacionPorcentaje());
        assertEquals(100, c.utilizadas());
        assertEquals(5, c.devueltas());

        ReporteSectorDTO p = r.sectores().get(1);
        assertEquals(new BigDecimal("100.0"), p.ocupacionPorcentaje());   // 10 de 10 asientos

        assertEquals(1010, r.capacidadTotal());
        assertEquals(260, r.vendidas());
        assertEquals(new BigDecimal("25.7"), r.ocupacionPorcentaje());   // 260 / 1010
        assertEquals(0, new BigDecimal("4050000").compareTo(r.recaudacion()));
        assertEquals(0, new BigDecimal("75000").compareTo(r.montoReintegrado()));
    }

    @Test
    void unSectorSinVentasApareceEnCero() {
        when(ventasClient.estadisticas(eventoId)).thenReturn(List.of());

        ReporteEventoDTO r = reporteService.reporte(eventoId, DUENIO, "ORGANIZADOR");

        assertEquals(2, r.sectores().size());
        assertEquals(0, r.vendidas());
        assertEquals(new BigDecimal("0.0"), r.ocupacionPorcentaje());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.recaudacion()));
    }

    @Test
    void otroOrganizadorNoPuedeVerElReporteNiModificarElEvento() {
        assertThrows(ForbiddenException.class,
                () -> reporteService.reporte(eventoId, OTRO_ORGANIZADOR, "ORGANIZADOR"));
        assertThrows(ForbiddenException.class,
                () -> eventoService.verificarPermiso(eventoId, OTRO_ORGANIZADOR, "ORGANIZADOR"));
    }

    @Test
    void unAdministradorPuedeVerElReporteDeCualquierEvento() {
        when(ventasClient.estadisticas(eventoId)).thenReturn(List.of());
        assertNotNull(reporteService.reporte(eventoId, 999L, "ADMIN"));
    }

    @Test
    void siVentasNoRespondeElReporteInformaServicioNoDisponible() {
        when(ventasClient.estadisticas(eventoId)).thenThrow(new ServicioNoDisponibleException("caido"));
        assertThrows(ServicioNoDisponibleException.class,
                () -> reporteService.reporte(eventoId, DUENIO, "ORGANIZADOR"));
    }

    @Test
    void cadaOrganizadorVeSoloSusEventosYElAdministradorTodos() {
        assertEquals(1, eventoService.misEventos(DUENIO, "ORGANIZADOR").size());
        assertTrue(eventoService.misEventos(OTRO_ORGANIZADOR, "ORGANIZADOR").isEmpty());
        assertEquals(eventoRepo.count(), eventoService.misEventos(null, "ADMIN").size());
    }

    @Test
    void elEventoGuardaSuOrganizadorYSiUsaFilaVirtual() {
        EventoDTO e = eventoService.obtener(eventoId);
        assertEquals(DUENIO, e.organizadorId());
        assertFalse(e.filaVirtual());
    }
}
