package com.ticketya.ventas;

import com.ticketya.ventas.dto.AccesoDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.exception.ConflictException;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.model.EstadoEntrada;
import com.ticketya.ventas.model.ResultadoAcceso;
import com.ticketya.ventas.repository.RegistroAccesoRepository;
import com.ticketya.ventas.service.AccesoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// (8) Control de acceso con QR: validaciones, bitacora y que un QR no entre dos veces ni siquiera en paralelo
class AccesoTest extends VentasTestBase {

    private static final Long OTRO_EVENTO = 99L;
    private static final Long PERSONAL = 5L;

    @Autowired AccesoService accesoService;
    @Autowired RegistroAccesoRepository registroRepo;

    @BeforeEach
    void limpiarBitacora() {
        registroRepo.deleteAll();
        stubPlatea(reloj.ahora().plusDays(30));
    }

    @Test
    void unaEntradaVendidaIngresaYQuedaMarcadaComoUsada() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);

        AccesoDTO acceso = accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL);

        assertEquals(ResultadoAcceso.PERMITIDO, acceso.resultado());
        assertEquals(1, acceso.fila());
        assertEquals(1, acceso.numero());
        assertNotNull(acceso.fechaUso());
        assertEquals(EstadoEntrada.USADA, entradaRepo.findById(entrada.id()).orElseThrow().getEstado());
    }

    @Test
    void unQrNoPuedeIngresarDosVeces() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL));
        assertTrue(ex.getMessage().contains("ya fue utilizada"));
    }

    @Test
    void unaEntradaDeOtroEventoSeRechazaYNoSeConsume() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> accesoService.validarIngreso(OTRO_EVENTO, entrada.codigoQr(), PERSONAL));
        assertTrue(ex.getMessage().contains("otro evento"));
        assertEquals(EstadoEntrada.VENDIDA, entradaRepo.findById(entrada.id()).orElseThrow().getEstado());

        // en la puerta correcta entra sin problema
        assertEquals(ResultadoAcceso.PERMITIDO,
                accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL).resultado());
    }

    @Test
    void unCodigoInexistenteDevuelveNoEncontrado() {
        assertThrows(NotFoundException.class,
                () -> accesoService.validarIngreso(EVENTO, "codigo-falso", PERSONAL));
    }

    @Test
    void unaEntradaDeUnEventoCanceladoNoEstaVigente() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        cancelacionEventoService.cancelarEvento(EVENTO);   // la entrada pasa a A_REINTEGRAR

        ConflictException ex = assertThrows(ConflictException.class,
                () -> accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL));
        assertTrue(ex.getMessage().contains("no esta vigente"));
    }

    @Test
    void unaEntradaUsadaNoSePuedeDevolver() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL);

        assertThrows(ConflictException.class,
                () -> devolucionService.solicitar(1L, new DevolucionRequest(entrada.id(), "Ya entre")));
    }

    @Test
    void cadaEscaneoQuedaEnLaBitacoraConSuResultado() {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL);
        assertThrows(ConflictException.class, () -> accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL));
        assertThrows(NotFoundException.class, () -> accesoService.validarIngreso(EVENTO, "codigo-falso", PERSONAL));

        List<ResultadoAcceso> resultados = registroRepo.findByEventoIdOrderByFechaDesc(EVENTO).stream()
                .map(r -> r.getResultado()).toList();
        assertEquals(3, resultados.size());
        assertTrue(resultados.containsAll(List.of(ResultadoAcceso.PERMITIDO, ResultadoAcceso.YA_UTILIZADA,
                ResultadoAcceso.NO_EXISTE)));
    }

    @Test
    void elMismoQrEscaneadoEnVariasPuertasALaVezIngresaUnaSolaVez() throws Exception {
        EntradaDTO entrada = comprarAsiento(1L, 1, 1);
        int puertas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(puertas);
        CountDownLatch listos = new CountDownLatch(puertas);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger permitidos = new AtomicInteger();
        AtomicInteger rechazados = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();

        for (int i = 0; i < puertas; i++) {
            tareas.add(pool.submit(() -> {
                listos.countDown();
                largada.await();
                try {
                    accesoService.validarIngreso(EVENTO, entrada.codigoQr(), PERSONAL);
                    permitidos.incrementAndGet();
                } catch (ConflictException e) {
                    rechazados.incrementAndGet();
                }
                return null;
            }));
        }
        listos.await();
        largada.countDown();   // todas las puertas escanean a la vez
        for (Future<?> t : tareas) {
            t.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, permitidos.get(), "el QR debe ingresar una sola vez");
        assertEquals(puertas - 1, rechazados.get());
        assertEquals(1, registroRepo.countByCodigoQrAndResultado(entrada.codigoQr(), ResultadoAcceso.PERMITIDO));
    }
}
