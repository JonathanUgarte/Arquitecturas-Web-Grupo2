package com.ticketya.catalogo;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.exception.BadRequestException;
import com.ticketya.catalogo.exception.ConflictException;
import com.ticketya.catalogo.exception.NotFoundException;
import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import com.ticketya.catalogo.service.CancelacionService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas de la logica de negocio del catalogo: busqueda por multiples criterios, publicacion,
// precios, suspension y cancelacion de eventos. El servicio de ventas se simula (mock).
@SpringBootTest
@ActiveProfiles("test")
class EventoServiceTest {

    @MockitoBean
    VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired CancelacionService cancelacionService;
    @Autowired PrecioSectorRepository precioRepo;
    @Autowired EventoRepository eventoRepo;
    @Autowired LugarRepository lugarRepo;

    private LugarDTO tandil;
    private LugarDTO azul;
    private LocalDateTime base;

    @BeforeEach
    void prepararBase() {
        precioRepo.deleteAll();
        eventoRepo.deleteAll();
        lugarRepo.deleteAll();
        tandil = lugarService.crear(lugar("Estadio Municipal", "Tandil"));
        azul = lugarService.crear(lugar("Teatro Espanol", "Azul"));
        base = LocalDateTime.now();
    }

    // ---------- helpers ----------
    private LugarRequest lugar(String nombre, String ciudad) {
        return new LugarRequest(nombre, "Calle 123", ciudad, -37.3, -59.1, List.of(
                new SectorRequest("Campo", TipoSector.NO_NUMERADO, 1000, null, null),
                new SectorRequest("Platea", TipoSector.NUMERADO, null, 2, 5)));
    }

    private EventoDTO borrador(LugarDTO lugar, String nombre, TipoEvento tipo, String organizador,
                               List<String> participantes, LocalDateTime fecha) {
        return eventoService.crear(new EventoRequest(nombre, "descripcion", tipo, organizador,
                participantes, fecha, lugar.id()));
    }

    private EventoDTO publicado(LugarDTO lugar, String nombre, TipoEvento tipo, String organizador,
                                List<String> participantes, LocalDateTime fecha, String precioCampo) {
        EventoDTO e = borrador(lugar, nombre, tipo, organizador, participantes, fecha);
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(lugar.sectores().get(0).id(), new BigDecimal(precioCampo)),
                new PrecioRequest(lugar.sectores().get(1).id(), new BigDecimal("30000")))));
        return eventoService.publicar(e.id());
    }

    private List<EventoResumenDTO> buscar(TipoEvento tipo, String organizador, String participante,
                                          String ciudad, LocalDateTime desde, LocalDateTime hasta, String orden) {
        return eventoService.buscar(tipo, organizador, participante, ciudad, desde, hasta, orden);
    }

    private List<String> nombres(List<EventoResumenDTO> eventos) {
        return eventos.stream().map(EventoResumenDTO::nombre).toList();
    }

    // ---------- busqueda ----------
    @Test
    void laBusquedaSoloDevuelveEventosALaVenta() {
        publicado(tandil, "Publicado", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(10), "10000");
        borrador(tandil, "Borrador", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(11));

        assertEquals(List.of("Publicado"), nombres(buscar(null, null, null, null, null, null, "fecha")));
    }

    @Test
    void buscarPorTipoDeEvento() {
        publicado(tandil, "Recital", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(10), "10000");
        publicado(azul, "Obra", TipoEvento.TEATRO, "Compania Central", List.of("Elenco"), base.plusDays(11), "8000");

        assertEquals(List.of("Obra"), nombres(buscar(TipoEvento.TEATRO, null, null, null, null, null, "fecha")));
    }

    @Test
    void buscarPorOrganizadorNoDistingueMayusculasNiExigeElNombreCompleto() {
        publicado(tandil, "Recital", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(10), "10000");
        publicado(azul, "Obra", TipoEvento.TEATRO, "Compania Central", List.of("Elenco"), base.plusDays(11), "8000");

        assertEquals(List.of("Recital"), nombres(buscar(null, "productora", null, null, null, null, "fecha")));
    }

    @Test
    void buscarPorParticipante() {
        publicado(tandil, "Recital", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno", "Banda Dos"), base.plusDays(10), "10000");
        publicado(azul, "Obra", TipoEvento.TEATRO, "Compania Central", List.of("Elenco Estable"), base.plusDays(11), "8000");

        assertEquals(List.of("Recital"), nombres(buscar(null, null, "banda dos", null, null, null, "fecha")));
        assertEquals(List.of("Obra"), nombres(buscar(null, null, "elenco", null, null, null, "fecha")));
    }

    @Test
    void buscarPorCiudad() {
        publicado(tandil, "Recital", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(10), "10000");
        publicado(azul, "Obra", TipoEvento.TEATRO, "Compania Central", List.of("Elenco"), base.plusDays(11), "8000");

        assertEquals(List.of("Recital"), nombres(buscar(null, null, null, "tandil", null, null, "fecha")));
        assertEquals(List.of("Obra"), nombres(buscar(null, null, null, "AZUL", null, null, "fecha")));
    }

    @Test
    void buscarPorRangoDeFechas() {
        publicado(tandil, "Primero", TipoEvento.RECITAL, "Productora Sur", List.of("A"), base.plusDays(10), "10000");
        publicado(tandil, "Segundo", TipoEvento.RECITAL, "Productora Sur", List.of("B"), base.plusDays(20), "10000");
        publicado(tandil, "Tercero", TipoEvento.RECITAL, "Productora Sur", List.of("C"), base.plusDays(30), "10000");

        List<EventoResumenDTO> r = buscar(null, null, null, null, base.plusDays(15), base.plusDays(25), "fecha");

        assertEquals(List.of("Segundo"), nombres(r));
    }

    @Test
    void buscarCombinandoVariosCriterios() {
        publicado(tandil, "Recital Tandil", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(10), "10000");
        publicado(azul, "Recital Azul", TipoEvento.RECITAL, "Productora Sur", List.of("Banda Uno"), base.plusDays(12), "10000");
        publicado(tandil, "Obra Tandil", TipoEvento.TEATRO, "Compania Central", List.of("Elenco"), base.plusDays(11), "8000");

        List<EventoResumenDTO> r = buscar(TipoEvento.RECITAL, null, "banda", "Tandil", null, null, "fecha");

        assertEquals(List.of("Recital Tandil"), nombres(r));
    }

    @Test
    void sinCriteriosLosEventosVienenOrdenadosPorFecha() {
        publicado(tandil, "C", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(30), "10000");
        publicado(tandil, "A", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");
        publicado(tandil, "B", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(20), "10000");

        assertEquals(List.of("A", "B", "C"), nombres(buscar(null, null, null, null, null, null, "fecha")));
    }

    @Test
    void ordenarPorPrecioUsaElPrecioMasBajoDeCadaEvento() {
        publicado(tandil, "Caro", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "20000");
        publicado(tandil, "Barato", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(11), "10000");
        publicado(tandil, "Medio", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(12), "15000");

        List<EventoResumenDTO> r = buscar(null, null, null, null, null, null, "precio");

        assertEquals(List.of("Barato", "Medio", "Caro"), nombres(r));
        assertEquals(0, new BigDecimal("10000").compareTo(r.get(0).precioDesde()));
    }

    @Test
    void unOrdenInvalidoSeRechaza() {
        assertThrows(BadRequestException.class, () -> buscar(null, null, null, null, null, null, "relevancia"));
    }

    // ---------- precios y publicacion ----------
    @Test
    void noSePuedePublicarUnEventoSinPrecios() {
        EventoDTO e = borrador(tandil, "Sin precios", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10));

        assertThrows(ConflictException.class, () -> eventoService.publicar(e.id()));
    }

    @Test
    void noSePuedePublicarSiFaltaElPrecioDeUnSector() {
        EventoDTO e = borrador(tandil, "Precios parciales", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10));
        eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(tandil.sectores().get(0).id(), new BigDecimal("10000")))));

        assertThrows(ConflictException.class, () -> eventoService.publicar(e.id()));
    }

    @Test
    void conTodosLosPreciosElEventoSePublica() {
        EventoDTO e = publicado(tandil, "Completo", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");

        assertEquals(EstadoEvento.PUBLICADO, e.estado());
        assertEquals(2, e.precios().size());
    }

    @Test
    void unEventoYaPublicadoNoSePublicaDeNuevoNiAdmitePrecios() {
        EventoDTO e = publicado(tandil, "Publicado", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");

        assertThrows(ConflictException.class, () -> eventoService.publicar(e.id()));
        assertThrows(ConflictException.class, () -> eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(tandil.sectores().get(0).id(), new BigDecimal("999"))))));
    }

    @Test
    void unSectorDeOtroLugarNoSePuedePreciar() {
        EventoDTO e = borrador(tandil, "Evento", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10));
        Long sectorDeAzul = azul.sectores().get(0).id();

        assertThrows(BadRequestException.class, () -> eventoService.configurarPrecios(e.id(), new PreciosRequest(List.of(
                new PrecioRequest(sectorDeAzul, new BigDecimal("10000"))))));
    }

    @Test
    void unSectorNumeradoNecesitaFilasYAsientos() {
        LugarRequest invalido = new LugarRequest("Sala", "Calle 1", "Tandil", null, null,
                List.of(new SectorRequest("Platea", TipoSector.NUMERADO, null, null, null)));

        assertThrows(BadRequestException.class, () -> lugarService.crear(invalido));
    }

    // ---------- suspension y cancelacion ----------
    @Test
    void unEventoSuspendidoDejaDeAparecerEnLaBusqueda() {
        EventoDTO e = publicado(tandil, "A suspender", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");

        EventoDTO suspendido = eventoService.suspender(e.id());

        assertEquals(EstadoEvento.SUSPENDIDO, suspendido.estado());
        assertTrue(buscar(null, null, null, null, null, null, "fecha").isEmpty());
    }

    @Test
    void unEventoEnBorradorNoSePuedeSuspender() {
        EventoDTO e = borrador(tandil, "Borrador", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10));

        assertThrows(ConflictException.class, () -> eventoService.suspender(e.id()));
    }

    @Test
    void cancelarUnEventoLoDejaCanceladoYAvisaAVentas() {
        when(ventasClient.notificarCancelacion(anyLong())).thenReturn(true);
        EventoDTO e = publicado(tandil, "A cancelar", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");

        CancelacionDTO r = cancelacionService.cancelar(e.id(), "Mal tiempo");

        assertEquals(EstadoEvento.CANCELADO, r.estado());
        assertTrue(r.reintegroNotificado());
        verify(ventasClient).notificarCancelacion(e.id());
        assertTrue(buscar(null, null, null, null, null, null, "fecha").isEmpty());
    }

    @Test
    void siVentasNoRespondeElEventoIgualQuedaCancelado() {
        when(ventasClient.notificarCancelacion(anyLong())).thenReturn(false);
        EventoDTO e = publicado(tandil, "A cancelar", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");

        CancelacionDTO r = cancelacionService.cancelar(e.id(), "Mal tiempo");

        assertEquals(EstadoEvento.CANCELADO, r.estado());
        assertFalse(r.reintegroNotificado(), "Queda marcado que el reintegro esta pendiente de avisar");
        assertEquals(EstadoEvento.CANCELADO, eventoService.obtener(e.id()).estado());
    }

    @Test
    void noSePuedeCancelarDosVecesElMismoEvento() {
        when(ventasClient.notificarCancelacion(anyLong())).thenReturn(true);
        EventoDTO e = publicado(tandil, "A cancelar", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "10000");
        cancelacionService.cancelar(e.id(), "Mal tiempo");

        assertThrows(ConflictException.class, () -> cancelacionService.cancelar(e.id(), "Otra vez"));
    }

    // ---------- datos para el servicio de ventas ----------
    @Test
    void ventasPuedeConsultarElPrecioYElEstadoDeUnSector() {
        EventoDTO e = publicado(tandil, "Con precio", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10), "15000");

        SectorVentaDTO s = eventoService.obtenerSectorVenta(e.id(), tandil.sectores().get(0).id());

        assertEquals("Campo", s.sector());
        assertEquals(EstadoEvento.PUBLICADO, s.estadoEvento());
        assertEquals(0, new BigDecimal("15000").compareTo(s.precio()));
        assertEquals(1000, s.capacidadTotal());
    }

    @Test
    void unSectorSinPrecioEnElEventoNoSeEncuentra() {
        EventoDTO e = borrador(tandil, "Sin precio", TipoEvento.RECITAL, "Productora Sur", List.of("X"), base.plusDays(10));

        assertThrows(NotFoundException.class,
                () -> eventoService.obtenerSectorVenta(e.id(), tandil.sectores().get(0).id()));
    }
}
