package com.ticketya.ventas;

import com.sun.net.httpserver.HttpServer;
import com.ticketya.ventas.client.CatalogoClient;
import com.ticketya.ventas.dto.SectorVentaDTO;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// Resiliencia de ventas -> catalogo con el cliente HTTP REAL contra un catalogo simulado (un servidor HTTP minimo
// del JDK que responde lo que cada test necesita): reintentos, que un 404 no se reintente, y circuit breaker.
@SpringBootTest(classes = {VentasServiceApplication.class, ConfigPrueba.class})
@ActiveProfiles("test")
class ResilienciaCatalogoTest {

    private static final String SECTOR_JSON = "{\"eventoId\":1,\"evento\":\"Recital\",\"estadoEvento\":\"PUBLICADO\","
            + "\"fechaHora\":\"2030-01-01T21:00:00\",\"sectorId\":2,\"sector\":\"Platea\",\"tipo\":\"NUMERADO\","
            + "\"capacidadTotal\":50,\"filas\":5,\"asientosPorFila\":10,\"precio\":30000,\"filaVirtual\":false}";

    // Comportamiento del catalogo simulado: cuantas respuestas con error 500 da antes de responder bien
    private static final AtomicInteger erroresPendientes = new AtomicInteger();
    private static volatile boolean responder404 = false;
    private static final AtomicInteger pedidos = new AtomicInteger();
    private static final HttpServer catalogo = iniciarCatalogoSimulado();

    @DynamicPropertySource
    static void urlDelCatalogo(DynamicPropertyRegistry registry) {
        registry.add("ventas.catalogo-url", () -> "http://localhost:" + catalogo.getAddress().getPort());
    }

    @Autowired CatalogoClient catalogoClient;
    @Autowired CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void reiniciar() {
        circuitBreakers.circuitBreaker("catalogo").reset();
        erroresPendientes.set(0);
        responder404 = false;
        pedidos.set(0);
    }

    @AfterAll
    static void detenerCatalogoSimulado() {
        catalogo.stop(0);
    }

    @Test
    void dosFallasTemporalesSeReintentanYLaTerceraVezResponde() {
        erroresPendientes.set(2);

        SectorVentaDTO sector = catalogoClient.obtenerSector(1L, 2L);

        assertEquals("Platea", sector.sector());
        assertEquals(3, pedidos.get());
    }

    @Test
    void unNoEncontradoNoSeReintenta() {
        responder404 = true;

        assertThrows(NotFoundException.class, () -> catalogoClient.obtenerSector(1L, 2L));
        assertEquals(1, pedidos.get());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreakers.circuitBreaker("catalogo").getState());
    }

    @Test
    void siSigueFallandoSeRindeDespuesDeTresIntentos() {
        erroresPendientes.set(Integer.MAX_VALUE);

        assertThrows(ServicioNoDisponibleException.class, () -> catalogoClient.obtenerSector(1L, 2L));
        assertEquals(3, pedidos.get());
    }

    @Test
    void conFallasSeguidasElCircuitoSeAbreYNoSeLlamaMasAlCatalogo() {
        erroresPendientes.set(Integer.MAX_VALUE);

        assertThrows(ServicioNoDisponibleException.class, () -> catalogoClient.obtenerSector(1L, 2L));
        assertThrows(RuntimeException.class, () -> catalogoClient.obtenerSector(1L, 2L));
        assertEquals(CircuitBreaker.State.OPEN, circuitBreakers.circuitBreaker("catalogo").getState());

        int antes = pedidos.get();
        assertThrows(CallNotPermittedException.class, () -> catalogoClient.obtenerSector(1L, 2L));
        assertEquals(antes, pedidos.get(), "con el circuito abierto no se llama al catalogo");
    }

    // ---------- catalogo simulado ----------
    private static HttpServer iniciarCatalogoSimulado() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                pedidos.incrementAndGet();
                int status;
                String cuerpo;
                if (responder404) {
                    status = 404;
                    cuerpo = "{}";
                } else if (erroresPendientes.get() > 0) {
                    erroresPendientes.decrementAndGet();
                    status = 500;
                    cuerpo = "{}";
                } else {
                    status = 200;
                    cuerpo = SECTOR_JSON;
                }
                byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
