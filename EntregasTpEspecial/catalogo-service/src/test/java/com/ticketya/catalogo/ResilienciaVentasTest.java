package com.ticketya.catalogo;

import com.sun.net.httpserver.HttpServer;
import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.exception.ServicioNoDisponibleException;
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

// Resiliencia de catalogo -> ventas con el cliente HTTP REAL contra un ventas simulado (servidor HTTP del JDK)
@SpringBootTest
@ActiveProfiles("test")
class ResilienciaVentasTest {

    private static final AtomicInteger erroresPendientes = new AtomicInteger();
    private static final AtomicInteger pedidos = new AtomicInteger();
    private static final HttpServer ventas = iniciarVentasSimulado();

    @DynamicPropertySource
    static void urlDeVentas(DynamicPropertyRegistry registry) {
        registry.add("ventas.url", () -> "http://localhost:" + ventas.getAddress().getPort());
    }

    @Autowired VentasClient ventasClient;
    @Autowired CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void reiniciar() {
        circuitBreakers.circuitBreaker("ventas").reset();
        erroresPendientes.set(0);
        pedidos.set(0);
    }

    @AfterAll
    static void detener() {
        ventas.stop(0);
    }

    @Test
    void elAvisoDeCancelacionSeReintentaSiVentasFallaUnaVez() {
        erroresPendientes.set(1);

        assertTrue(ventasClient.notificarCancelacion(1L));
        assertEquals(2, pedidos.get());
    }

    @Test
    void siVentasNoRespondeElAvisoDevuelveFalseSinRomperLaCancelacion() {
        erroresPendientes.set(Integer.MAX_VALUE);

        assertFalse(ventasClient.notificarCancelacion(1L));   // fallback
        assertEquals(3, pedidos.get());
    }

    @Test
    void lasEstadisticasSeReintentanYSiVentasSigueCaidoSeInforma503() {
        erroresPendientes.set(2);
        assertNotNull(ventasClient.estadisticas(1L));
        assertEquals(3, pedidos.get());

        // Las 2 fallas de recien ya cuentan para el circuit breaker (en el test se abre con 4 llamadas y 50% de
        // fallas). Se lo reinicia para probar solo los reintentos.
        circuitBreakers.circuitBreaker("ventas").reset();
        pedidos.set(0);
        erroresPendientes.set(Integer.MAX_VALUE);
        assertThrows(ServicioNoDisponibleException.class, () -> ventasClient.estadisticas(1L));
        assertEquals(3, pedidos.get());
    }

    private static HttpServer iniciarVentasSimulado() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                pedidos.incrementAndGet();
                boolean falla = erroresPendientes.get() > 0;
                if (falla) {
                    erroresPendientes.decrementAndGet();
                }
                byte[] bytes = (falla ? "{}" : (exchange.getRequestMethod().equals("GET") ? "[]" : "{}"))
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(falla ? 500 : 200, bytes.length);
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
