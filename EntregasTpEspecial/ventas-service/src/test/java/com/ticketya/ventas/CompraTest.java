package com.ticketya.ventas;

import com.ticketya.ventas.dto.CompraDTO;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.exception.ConflictException;
import com.ticketya.ventas.exception.ForbiddenException;
import com.ticketya.ventas.model.EstadoCompra;
import com.ticketya.ventas.model.EstadoEntrada;
import com.ticketya.ventas.model.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas de la logica de negocio de la compra: reserva temporal, pago, idempotencia y limites
class CompraTest extends VentasTestBase {

    @Test
    void compraAprobadaEmiteLaEntradaConCodigoQr() {
        stubPlatea(reloj.ahora().plusDays(30));

        ReservaDTO reserva = reservarAsiento(1L, 1, 1);
        assertEquals(EstadoReserva.PENDIENTE, reserva.estado());

        CompraDTO compra = pagar(1L, reserva.id(), "clave-1", "TARJETA");

        assertEquals(EstadoCompra.APROBADA, compra.estado());
        assertEquals(1, compra.entradas().size());
        EntradaDTO entrada = compra.entradas().get(0);
        assertNotNull(entrada.codigoQr());
        assertFalse(entrada.codigoQr().isBlank());
        assertEquals(EstadoEntrada.VENDIDA, entrada.estado());
        assertEquals(1, entrada.fila());
        assertEquals(1, entrada.numero());
        assertEquals(EstadoReserva.CONFIRMADA, reservaService.obtener(1L, reserva.id()).estado());
    }

    @Test
    void cadaEntradaTieneUnCodigoQrDistinto() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO a = comprarAsiento(1L, 1, 1);
        EntradaDTO b = comprarAsiento(2L, 1, 2);
        assertNotEquals(a.codigoQr(), b.codigoQr());
    }

    @Test
    void pagoRechazadoLiberaElAsiento() {
        stubPlatea(reloj.ahora().plusDays(30));

        ReservaDTO reserva = reservarAsiento(1L, 1, 1);
        CompraDTO compra = pagar(1L, reserva.id(), "clave-1", "TARJETA_RECHAZADA");

        assertEquals(EstadoCompra.RECHAZADA, compra.estado());
        assertTrue(compra.entradas().isEmpty());
        assertEquals(0, entradaRepo.count());
        // el asiento volvio a estar disponible para otro cliente
        assertDoesNotThrow(() -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void reintentoDelPagoConLaMismaClaveNoCobraDosVeces() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);

        CompraDTO primera = pagar(1L, reserva.id(), "clave-1", "TARJETA");
        CompraDTO reintento = pagar(1L, reserva.id(), "clave-1", "TARJETA");

        assertEquals(primera.id(), reintento.id());
        assertEquals(primera.entradas().get(0).codigoQr(), reintento.entradas().get(0).codigoQr());
        assertEquals(1, compraRepo.count());
        assertEquals(1, entradaRepo.count());
    }

    @Test
    void unaReservaYaPagadaNoSePuedePagarConOtraClave() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);
        pagar(1L, reserva.id(), "clave-1", "TARJETA");

        assertThrows(ConflictException.class, () -> pagar(1L, reserva.id(), "clave-2", "TARJETA"));
        assertEquals(1, entradaRepo.count());
    }

    @Test
    void reservaVencidaNoSePuedePagarYLiberaElAsiento() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);

        reloj.avanzar(Duration.ofMinutes(11));   // la reserva dura 10 minutos

        assertThrows(ConflictException.class, () -> pagar(1L, reserva.id(), "clave-1", "TARJETA"));
        assertEquals(0, entradaRepo.count());
        assertDoesNotThrow(() -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void unaReservaVencidaSinPagarLiberaElAsientoParaOtroCliente() {
        stubPlatea(reloj.ahora().plusDays(30));
        reservarAsiento(1L, 1, 1);

        // mientras no vence, nadie mas puede tomarlo
        assertThrows(ConflictException.class, () -> reservarAsiento(2L, 1, 1));

        reloj.avanzar(Duration.ofMinutes(11));
        assertDoesNotThrow(() -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void unAsientoYaReservadoNoSePuedeReservarDeNuevo() {
        stubPlatea(reloj.ahora().plusDays(30));
        reservarAsiento(1L, 1, 1);

        assertThrows(ConflictException.class, () -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void elClienteNoPuedeSuperarElLimiteDeEntradasPorEvento() {
        stubCampo(100, reloj.ahora().plusDays(30));

        reservarEnCampo(1L, 4);   // el maximo por evento es 4

        assertThrows(ConflictException.class, () -> reservarEnCampo(1L, 1));
    }

    @Test
    void laZonaSinCupoRechazaLaReserva() {
        stubCampo(2, reloj.ahora().plusDays(30));

        reservarEnCampo(1L, 2);

        assertThrows(ConflictException.class, () -> reservarEnCampo(2L, 1));
        assertEquals(0, stockRepo.findByEventoIdAndSectorId(EVENTO, CAMPO).orElseThrow().getDisponibles());
    }

    @Test
    void unEventoQueNoEstaPublicadoNoSePuedeComprar() {
        stubPlatea("CANCELADO", reloj.ahora().plusDays(30));

        assertThrows(ConflictException.class, () -> reservarAsiento(1L, 1, 1));
    }

    @Test
    void cancelarLaReservaLiberaElAsiento() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);

        reservaService.cancelar(1L, reserva.id());

        assertDoesNotThrow(() -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void nadiePuedePagarLaReservaDeOtroCliente() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(1L, 1, 1);

        assertThrows(ForbiddenException.class, () -> pagar(2L, reserva.id(), "clave-x", "TARJETA"));
    }
}
