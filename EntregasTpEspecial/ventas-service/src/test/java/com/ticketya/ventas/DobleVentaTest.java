package com.ticketya.ventas;

import com.ticketya.ventas.dto.AsientoRequest;
import com.ticketya.ventas.dto.ReservaRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// Prueba de la NO DOBLE VENTA: muchos clientes intentan comprar lo mismo AL MISMO TIEMPO
// y solo puede tener exito la cantidad que realmente hay disponible.
class DobleVentaTest extends VentasTestBase {

    private static final int CLIENTES = 20;

    @Test
    void veinteClientesQuierenElMismoAsientoYSoloUnoLoConsigue() throws Exception {
        stubPlatea(reloj.ahora().plusDays(30));
        ReservaRequest pedido = new ReservaRequest(EVENTO, PLATEA, List.of(new AsientoRequest(3, 5)), null);

        int exitosas = ejecutarEnParalelo(usuario -> reservaService.reservar(usuario, pedido));

        assertEquals(1, exitosas, "Solo un cliente puede quedarse con el asiento");
        assertEquals(1, asientoRepo.count(), "El asiento figura ocupado una sola vez");
        assertEquals(1, reservaRepo.count(), "Las reservas de los que perdieron se deshicieron");
    }

    @Test
    void veinteClientesQuierenElUltimoLugarDeUnaZonaYSoloUnoLoConsigue() throws Exception {
        stubCampo(1, reloj.ahora().plusDays(30));
        ReservaRequest pedido = new ReservaRequest(EVENTO, CAMPO, null, 1);

        int exitosas = ejecutarEnParalelo(usuario -> reservaService.reservar(usuario, pedido));

        assertEquals(1, exitosas, "Solo un cliente puede quedarse con el ultimo lugar");
        assertEquals(0, stockRepo.findByEventoIdAndSectorId(EVENTO, CAMPO).orElseThrow().getDisponibles());
    }

    @Test
    void conCupoParaCincoSeVendenExactamenteCincoYElCupoNuncaQuedaNegativo() throws Exception {
        stubCampo(5, reloj.ahora().plusDays(30));
        ReservaRequest pedido = new ReservaRequest(EVENTO, CAMPO, null, 1);

        int exitosas = ejecutarEnParalelo(usuario -> reservaService.reservar(usuario, pedido));

        assertEquals(5, exitosas);
        int disponibles = stockRepo.findByEventoIdAndSectorId(EVENTO, CAMPO).orElseThrow().getDisponibles();
        assertEquals(0, disponibles);
        assertTrue(disponibles >= 0, "El cupo nunca puede quedar negativo");
    }

    // Lanza CLIENTES hilos que arrancan juntos y devuelve cuantos lograron completar la accion.
    // Los que pierden reciben una excepcion de conflicto, que es lo esperado.
    private int ejecutarEnParalelo(Consumer<Long> accion) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CLIENTES);
        CountDownLatch listos = new CountDownLatch(CLIENTES);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();

        for (long usuario = 1; usuario <= CLIENTES; usuario++) {
            final long id = usuario;
            futuros.add(pool.submit(() -> {
                listos.countDown();
                try {
                    largada.await();
                    accion.accept(id);
                    exitosas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    // conflicto esperado: otro cliente llego antes
                }
            }));
        }

        listos.await();
        largada.countDown();
        for (Future<?> futuro : futuros) {
            futuro.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        return exitosas.get();
    }
}
