package com.ticketya.ventas;

import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.dto.EstadisticaSectorDTO;
import com.ticketya.ventas.service.AccesoService;
import com.ticketya.ventas.service.EstadisticasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// (12) Datos de ventas por sector que usa catalogo para armar el reporte del organizador
class EstadisticasTest extends VentasTestBase {

    @Autowired EstadisticasService estadisticasService;
    @Autowired AccesoService accesoService;

    @Test
    void cuentaVendidasUtilizadasYDevueltasPorSector() {
        stubPlatea(reloj.ahora().plusDays(30));
        stubCampo(100, reloj.ahora().plusDays(30));

        EntradaDTO a = comprarAsiento(1L, 1, 1);
        comprarAsiento(1L, 1, 2);
        EntradaDTO c = comprarAsiento(2L, 2, 1);
        // en el campo, 3 entradas
        var reserva = reservarEnCampo(3L, 3);
        pagar(3L, reserva.id(), "clave-campo", "TARJETA");

        accesoService.validarIngreso(EVENTO, a.codigoQr(), 5L);                        // 1 ya ingreso
        DevolucionDTO dev = devolucionService.solicitar(2L, new DevolucionRequest(c.id(), "No puedo ir"));
        devolucionService.aprobar(dev.id());                                            // 1 devuelta

        List<EstadisticaSectorDTO> r = estadisticasService.porSector(EVENTO);

        EstadisticaSectorDTO campo = r.stream().filter(x -> x.sectorId().equals(CAMPO)).findFirst().orElseThrow();
        EstadisticaSectorDTO platea = r.stream().filter(x -> x.sectorId().equals(PLATEA)).findFirst().orElseThrow();

        assertEquals(3, campo.vendidas());
        assertEquals(0, new BigDecimal("45000").compareTo(campo.recaudacion()));

        assertEquals(2, platea.vendidas());      // la devuelta ya no cuenta como vendida
        assertEquals(1, platea.utilizadas());
        assertEquals(1, platea.devueltas());
        assertEquals(0, new BigDecimal("60000").compareTo(platea.recaudacion()));
        assertEquals(0, new BigDecimal("30000").compareTo(platea.montoReintegrado()));
    }

    @Test
    void laCancelacionPasaLasVentasAReintegrar() {
        stubPlatea(reloj.ahora().plusDays(30));
        comprarAsiento(1L, 1, 1);
        cancelacionEventoService.cancelarEvento(EVENTO);

        EstadisticaSectorDTO platea = estadisticasService.porSector(EVENTO).get(0);

        assertEquals(0, platea.vendidas());
        assertEquals(1, platea.aReintegrar());
        assertEquals(0, BigDecimal.ZERO.compareTo(platea.recaudacion()));
    }

    @Test
    void unEventoSinVentasDevuelveUnaListaVacia() {
        assertTrue(estadisticasService.porSector(EVENTO).isEmpty());
    }
}
