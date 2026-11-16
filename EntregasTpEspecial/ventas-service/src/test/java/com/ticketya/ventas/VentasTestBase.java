package com.ticketya.ventas;

import com.ticketya.ventas.client.CatalogoClient;
import com.ticketya.ventas.dto.*;
import com.ticketya.ventas.model.TipoSector;
import com.ticketya.ventas.repository.*;
import com.ticketya.ventas.service.CancelacionEventoService;
import com.ticketya.ventas.service.DevolucionService;
import com.ticketya.ventas.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

// Base de los tests: levanta el servicio con una base en memoria, un reloj controlable y el
// servicio de catalogo simulado (mock). La pasarela de pagos es la simulada real del proyecto.
@SpringBootTest(classes = {VentasServiceApplication.class, ConfigPrueba.class})
@ActiveProfiles("test")
abstract class VentasTestBase {

    protected static final Long EVENTO = 1L;
    protected static final Long CAMPO = 1L;    // zona sin asiento
    protected static final Long PLATEA = 2L;   // sector numerado: 5 filas x 10 asientos

    @MockitoBean
    protected CatalogoClient catalogoClient;

    @Autowired protected RelojDePrueba reloj;
    @Autowired protected ReservaService reservaService;
    @Autowired protected DevolucionService devolucionService;
    @Autowired protected CancelacionEventoService cancelacionEventoService;

    @Autowired protected ReservaRepository reservaRepo;
    @Autowired protected AsientoOcupadoRepository asientoRepo;
    @Autowired protected StockZonaRepository stockRepo;
    @Autowired protected CompraRepository compraRepo;
    @Autowired protected EntradaRepository entradaRepo;
    @Autowired protected DevolucionRepository devolucionRepo;
    @Autowired protected EventoCanceladoRepository eventoCanceladoRepo;

    @BeforeEach
    void limpiarBase() {
        devolucionRepo.deleteAll();
        entradaRepo.deleteAll();
        compraRepo.deleteAll();
        asientoRepo.deleteAll();
        reservaRepo.deleteAll();
        stockRepo.deleteAll();
        eventoCanceladoRepo.deleteAll();
        reloj.fijarEn(Instant.now());
    }

    // ---------- catalogo simulado ----------
    protected void stubPlatea(LocalDateTime fechaEvento) {
        stubPlatea("PUBLICADO", fechaEvento);
    }

    protected void stubPlatea(String estadoEvento, LocalDateTime fechaEvento) {
        when(catalogoClient.obtenerSector(EVENTO, PLATEA)).thenReturn(new SectorVentaDTO(
                EVENTO, "Recital de Prueba", estadoEvento, fechaEvento, PLATEA, "Platea",
                TipoSector.NUMERADO, 50, 5, 10, new BigDecimal("30000")));
    }

    protected void stubCampo(int capacidad, LocalDateTime fechaEvento) {
        when(catalogoClient.obtenerSector(EVENTO, CAMPO)).thenReturn(new SectorVentaDTO(
                EVENTO, "Recital de Prueba", "PUBLICADO", fechaEvento, CAMPO, "Campo",
                TipoSector.NO_NUMERADO, capacidad, null, null, new BigDecimal("15000")));
    }

    // ---------- atajos ----------
    protected ReservaDTO reservarAsiento(long usuario, int fila, int numero) {
        return reservaService.reservar(usuario,
                new ReservaRequest(EVENTO, PLATEA, List.of(new AsientoRequest(fila, numero)), null));
    }

    protected ReservaDTO reservarEnCampo(long usuario, int cantidad) {
        return reservaService.reservar(usuario, new ReservaRequest(EVENTO, CAMPO, null, cantidad));
    }

    protected CompraDTO pagar(long usuario, long reservaId, String clave, String medioPago) {
        return reservaService.pagar(usuario, reservaId, clave, new PagoRequest(medioPago));
    }

    protected EntradaDTO comprarAsiento(long usuario, int fila, int numero) {
        ReservaDTO reserva = reservarAsiento(usuario, fila, numero);
        CompraDTO compra = pagar(usuario, reserva.id(), "clave-" + usuario + "-" + fila + "-" + numero, "TARJETA");
        return compra.entradas().get(0);
    }
}
