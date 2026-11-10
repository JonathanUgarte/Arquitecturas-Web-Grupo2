package com.ticketya.ventas;

import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.exception.ConflictException;
import com.ticketya.ventas.exception.ForbiddenException;
import com.ticketya.ventas.model.EstadoDevolucion;
import com.ticketya.ventas.model.EstadoEntrada;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas de las devoluciones y de la cancelacion de eventos
class DevolucionTest extends VentasTestBase {

    @Test
    void noSePuedeSolicitarLaDevolucionAMenosDe72Horas() {
        stubPlatea(reloj.ahora().plusHours(48));
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);

        assertThrows(ConflictException.class,
                () -> devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "No puedo ir")));
    }

    @Test
    void unaDevolucionAprobadaLiberaElAsientoYPideElReintegro() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);

        DevolucionDTO solicitud = devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "No puedo ir"));
        assertEquals(EstadoDevolucion.PENDIENTE, solicitud.estado());

        DevolucionDTO aprobada = devolucionService.aprobar(solicitud.id());

        assertEquals(EstadoDevolucion.APROBADA, aprobada.estado());
        assertTrue(aprobada.reintegroSolicitado());
        assertEquals(EstadoEntrada.DEVUELTA, entradaRepo.findById(entrada.id()).orElseThrow().getEstado());
        // el asiento quedo libre para otro cliente
        assertDoesNotThrow(() -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void unaDevolucionRechazadaDejaLaEntradaVendida() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        DevolucionDTO solicitud = devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "No puedo ir"));

        DevolucionDTO rechazada = devolucionService.rechazar(solicitud.id());

        assertEquals(EstadoDevolucion.RECHAZADA, rechazada.estado());
        assertEquals(EstadoEntrada.VENDIDA, entradaRepo.findById(entrada.id()).orElseThrow().getEstado());
        assertThrows(ConflictException.class, () -> reservarAsiento(2L, 1, 1));
    }

    @Test
    void noSePuedeAprobarDosVecesLaMismaDevolucion() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        DevolucionDTO solicitud = devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "No puedo ir"));
        devolucionService.aprobar(solicitud.id());

        assertThrows(ConflictException.class, () -> devolucionService.aprobar(solicitud.id()));
    }

    @Test
    void noSePuedeDevolverLaEntradaDeOtroCliente() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);

        assertThrows(ForbiddenException.class,
                () -> devolucionService.solicitar(2L, new DevolucionRequest(entrada.id(), "Quiero la plata")));
    }

    @Test
    void laCancelacionDeUnEventoPasaLasEntradasVendidasAReintegro() {
        stubPlatea(reloj.ahora().plusDays(30));
        EntradaDTO a = comprarAsiento(1L, 1, 1);
        EntradaDTO b = comprarAsiento(2L, 1, 2);

        var resultado = cancelacionEventoService.cancelarEvento(EVENTO);

        assertEquals(2, resultado.entradasAReintegrar());
        assertEquals(EstadoEntrada.A_REINTEGRAR, entradaRepo.findById(a.id()).orElseThrow().getEstado());
        assertEquals(EstadoEntrada.A_REINTEGRAR, entradaRepo.findById(b.id()).orElseThrow().getEstado());
    }

    @Test
    void cancelarElMismoEventoDosVecesNoCambiaNada() {
        stubPlatea(reloj.ahora().plusDays(30));
        comprarAsiento(1L, 1, 1);

        cancelacionEventoService.cancelarEvento(EVENTO);
        var segundaVez = cancelacionEventoService.cancelarEvento(EVENTO);

        assertEquals(0, segundaVez.entradasAReintegrar());
    }
}
