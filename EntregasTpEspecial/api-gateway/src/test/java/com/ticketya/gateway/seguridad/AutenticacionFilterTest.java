package com.ticketya.gateway.seguridad;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

// Prueba el filtro completo: token ausente, vencido, mal firmado, rol incorrecto y reenvio de la identidad
class AutenticacionFilterTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-treinta-y-dos-caracteres";

    private final AutenticacionFilter filtro = new AutenticacionFilter(new JwtValidador(SECRETO));

    @Test
    void sinTokenEnUnEndpointProtegidoResponde401YNoLlegaAlServicio() {
        Resultado r = ejecutar(MockServerHttpRequest.post("/reservas").build());
        assertEquals(HttpStatus.UNAUTHORIZED, r.status());
        assertNull(r.reenviado(), "el pedido no debe llegar al servicio");
    }

    @Test
    void conTokenVencidoResponde401() {
        String vencido = token(SECRETO, "1", "CLIENTE", Instant.now().minusSeconds(60));
        Resultado r = ejecutar(MockServerHttpRequest.get("/entradas").header("Authorization", "Bearer " + vencido).build());
        assertEquals(HttpStatus.UNAUTHORIZED, r.status());
    }

    @Test
    void conTokenFirmadoConOtroSecretoResponde401() {
        String falso = token("otro-secreto-cualquiera-de-mas-de-treinta-y-dos", "1", "ADMIN",
                Instant.now().plusSeconds(600));
        Resultado r = ejecutar(MockServerHttpRequest.get("/devoluciones").header("Authorization", "Bearer " + falso).build());
        assertEquals(HttpStatus.UNAUTHORIZED, r.status());
    }

    @Test
    void conRolIncorrectoResponde403() {
        String cliente = token(SECRETO, "1", "CLIENTE", Instant.now().plusSeconds(600));
        Resultado r = ejecutar(MockServerHttpRequest.put("/eventos/1/cancelacion")
                .header("Authorization", "Bearer " + cliente).build());
        assertEquals(HttpStatus.FORBIDDEN, r.status());
        assertNull(r.reenviado());
    }

    @Test
    void conTokenValidoReenviaLaIdentidadDelTokenYDescartaLaQueMandaElCliente() {
        String cliente = token(SECRETO, "7", "CLIENTE", Instant.now().plusSeconds(600));
        Resultado r = ejecutar(MockServerHttpRequest.get("/entradas")
                .header("Authorization", "Bearer " + cliente)
                .header("X-Usuario-Id", "99")      // intento de hacerse pasar por otro usuario
                .header("X-Rol", "ADMIN")          // intento de escalar privilegios
                .build());

        assertNotNull(r.reenviado());
        assertEquals("7", r.reenviado().getHeaders().getFirst("X-Usuario-Id"));
        assertEquals("CLIENTE", r.reenviado().getHeaders().getFirst("X-Rol"));
    }

    @Test
    void enUnEndpointPublicoSinTokenSeBorranLosHeadersDeIdentidad() {
        Resultado r = ejecutar(MockServerHttpRequest.get("/eventos").header("X-Rol", "ADMIN").build());
        assertNotNull(r.reenviado());
        assertNull(r.reenviado().getHeaders().getFirst("X-Rol"));
        assertNull(r.reenviado().getHeaders().getFirst("X-Usuario-Id"));
    }

    @Test
    void enUnEndpointPublicoUnTokenVencidoNoImpideElAcceso() {
        String vencido = token(SECRETO, "1", "CLIENTE", Instant.now().minusSeconds(60));
        Resultado r = ejecutar(MockServerHttpRequest.post("/auth/login").header("Authorization", "Bearer " + vencido).build());
        assertNotNull(r.reenviado());
    }

    // ---------- utilidades ----------

    private record Resultado(HttpStatus status, ServerHttpRequest reenviado) {
    }

    private Resultado ejecutar(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> capturado = new AtomicReference<>();
        GatewayFilterChain cadena = ex -> {
            capturado.set(ex);
            return Mono.empty();
        };
        filtro.filter(exchange, cadena).block();
        HttpStatus status = exchange.getResponse().getStatusCode() == null
                ? null : HttpStatus.valueOf(exchange.getResponse().getStatusCode().value());
        return new Resultado(status, capturado.get() == null ? null : capturado.get().getRequest());
    }

    private static String token(String secreto, String usuarioId, String rol, Instant vence) {
        return Jwts.builder()
                .issuer(JwtValidador.EMISOR)
                .subject(usuarioId)
                .claim("rol", rol)
                .issuedAt(Date.from(vence.minusSeconds(3600)))
                .expiration(Date.from(vence))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
