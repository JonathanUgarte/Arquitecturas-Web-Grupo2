package com.ticketya.ventas;

import com.ticketya.ventas.client.PasarelaPagoMock;
import com.ticketya.ventas.dto.CompraDTO;
import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.exception.ServicioNoDisponibleException;
import com.ticketya.ventas.model.EstadoCompra;
import com.ticketya.ventas.model.EstadoReserva;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

// Resiliencia ante la pasarela de pagos (servicio externo): reintentos, circuit breaker, fallback que conserva la
// reserva, idempotencia del cobro y reintento programado de reintegros pendientes.
// En los tests el circuito se abre con 4 llamadas y 50% de fallas (application-test.properties).
class PagoResilienteTest extends VentasTestBase {

    @Autowired PasarelaPagoMock pasarelaMock;
    @Autowired CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void pasarelaSana() {
        circuitBreakers.circuitBreaker("pasarela").reset();
        pasarelaMock.simularReintegrosCaidos(false);
        stubPlatea(reloj.ahora().plusDays(30));
    }

    @AfterEach
    void cerrarCircuito() {
        circuitBreakers.circuitBreaker("pasarela").reset();
        pasarelaMock.simularReintegrosCaidos(false);
    }

    @Test
    void unaFallaTemporalSeReintentaYElPagoSeApruebaCobrandoUnaSolaVez() {
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);
        int cobrosAntes = pasarelaMock.cobrosReales();

        CompraDTO compra = pagar(1L, reserva.id(), "clave-inestable", "TARJETA_INESTABLE");

        assertEquals(EstadoCompra.APROBADA, compra.estado());
        assertEquals(1, compra.entradas().size());
        assertEquals(cobrosAntes + 1, pasarelaMock.cobrosReales(), "el reintento no debe cobrar dos veces");
    }

    @Test
    void siLaPasarelaNoRespondeLaReservaSeConservaYSePuedeReintentarElPago() {
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);

        ServicioNoDisponibleException ex = assertThrows(ServicioNoDisponibleException.class,
                () -> pagar(1L, reserva.id(), "clave-caida", "PASARELA_CAIDA"));
        assertTrue(ex.getMessage().contains("sigue vigente"));

        // La reserva no se perdio: sigue PENDIENTE y el asiento sigue tomado
        assertEquals(EstadoReserva.PENDIENTE, reservaRepo.findById(reserva.id()).orElseThrow().getEstado());
        assertEquals(1, asientoRepo.count());

        // Cuando la pasarela vuelve, el cliente paga con una nueva clave
        CompraDTO compra = pagar(1L, reserva.id(), "clave-reintento", "TARJETA");
        assertEquals(EstadoCompra.APROBADA, compra.estado());
    }

    @Test
    void conLaPasarelaCaidaElCircuitoSeAbreYLosPagosFallanAlInstante() {
        ReservaDTO primera = reservarAsiento(1L, 1, 1);
        ReservaDTO segunda = reservarAsiento(2L, 1, 2);
        ReservaDTO tercera = reservarAsiento(3L, 1, 3);

        assertThrows(ServicioNoDisponibleException.class, () -> pagar(1L, primera.id(), "c1", "PASARELA_CAIDA"));
        assertThrows(ServicioNoDisponibleException.class, () -> pagar(2L, segunda.id(), "c2", "PASARELA_CAIDA"));
        assertEquals(CircuitBreaker.State.OPEN, circuitBreakers.circuitBreaker("pasarela").getState());

        // Con el circuito abierto ni siquiera se llama a la pasarela: se responde 503 enseguida
        int cobrosAntes = pasarelaMock.cobrosReales();
        assertThrows(ServicioNoDisponibleException.class, () -> pagar(3L, tercera.id(), "c3", "TARJETA"));
        assertEquals(cobrosAntes, pasarelaMock.cobrosReales());
        assertEquals(EstadoReserva.PENDIENTE, reservaRepo.findById(tercera.id()).orElseThrow().getEstado());
    }

    @Test
    void unReintegroQueLaPasarelaNoAceptoSeReintentaDespues() {
        EntradaDTO entrada = comprarAsiento(1L, 2, 2);
        DevolucionDTO solicitada = devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "No puedo ir"));

        pasarelaMock.simularReintegrosCaidos(true);
        DevolucionDTO aprobada = devolucionService.aprobar(solicitada.id());
        assertFalse(aprobada.reintegroSolicitado(), "queda pendiente: la pasarela no respondio");

        // La pasarela se recupera (y pasa el tiempo de circuito abierto)
        pasarelaMock.simularReintegrosCaidos(false);
        circuitBreakers.circuitBreaker("pasarela").reset();

        assertEquals(1, devolucionService.reintentarReintegrosPendientes());
        assertTrue(devolucionRepo.findById(solicitada.id()).orElseThrow().isReintegroSolicitado());
        assertEquals(0, devolucionService.reintentarReintegrosPendientes(), "no se pide dos veces");
    }
}
