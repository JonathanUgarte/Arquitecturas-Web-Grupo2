package com.ticketya.ventas;

import com.ticketya.ventas.dto.AsientoRequest;
import com.ticketya.ventas.dto.CompraDTO;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.dto.ReservaRequest;
import com.ticketya.ventas.mensajeria.CompraConfirmadaEvento;
import com.ticketya.ventas.mensajeria.EventoCanceladoEvento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica QUE eventos se publican hacia RabbitMQ y CUANDO (el envio real lo hace PublicadorDeEventos,
// deshabilitado en los tests). Lo importante: un reintento del pago o de la cancelacion no duplica avisos.
@RecordApplicationEvents
class EventosPublicadosTest extends VentasTestBase {

    @Autowired ApplicationEvents eventos;

    @Test
    void unPagoAprobadoPublicaUnaSolaCompraConfirmadaAunqueSeReintente() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservaService.reservar(7L, new ReservaRequest(EVENTO, PLATEA,
                List.of(new AsientoRequest(1, 1), new AsientoRequest(1, 2)), null));

        CompraDTO compra = pagar(7L, reserva.id(), "clave-eventos", "TARJETA");
        pagar(7L, reserva.id(), "clave-eventos", "TARJETA");   // reintento con la misma clave

        List<CompraConfirmadaEvento> publicados = eventos.stream(CompraConfirmadaEvento.class).toList();
        assertEquals(1, publicados.size());
        CompraConfirmadaEvento evento = publicados.get(0);
        assertEquals(compra.id(), evento.compraId());
        assertEquals(7L, evento.usuarioId());
        assertEquals(EVENTO, evento.eventoId());
        assertEquals(2, evento.cantidadEntradas());
        assertEquals(0, new BigDecimal("60000").compareTo(evento.total()));
    }

    @Test
    void unPagoRechazadoNoPublicaNada() {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaDTO reserva = reservarAsiento(7L, 2, 2);

        pagar(7L, reserva.id(), "clave-rechazo", "TARJETA_RECHAZADA");

        assertEquals(0, eventos.stream(CompraConfirmadaEvento.class).count());
    }

    @Test
    void laCancelacionPublicaLosCompradoresUnaSolaVez() {
        stubPlatea(reloj.ahora().plusDays(30));
        comprarAsiento(1L, 3, 1);
        comprarAsiento(1L, 3, 2);
        comprarAsiento(2L, 3, 3);

        cancelacionEventoService.cancelarEvento(EVENTO);
        cancelacionEventoService.cancelarEvento(EVENTO);   // catalogo reintenta el aviso

        List<EventoCanceladoEvento> publicados = eventos.stream(EventoCanceladoEvento.class).toList();
        assertEquals(1, publicados.size());
        assertEquals(3, publicados.get(0).entradasAfectadas());
        assertEquals(List.of(1L, 2L), publicados.get(0).compradores().stream().sorted().toList());
    }
}
