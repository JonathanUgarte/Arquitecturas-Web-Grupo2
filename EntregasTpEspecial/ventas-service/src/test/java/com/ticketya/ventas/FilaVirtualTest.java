package com.ticketya.ventas;

import com.ticketya.ventas.dto.AsientoRequest;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.dto.ReservaRequest;
import com.ticketya.ventas.exception.ForbiddenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// (6) En un evento con fila virtual, ventas exige el turno firmado por fila-service (header X-Turno-Fila)
class FilaVirtualTest extends VentasTestBase {

    private static final String SECRETO = "secreto-de-turnos-para-los-tests-de-al-menos-32-caracteres";
    private static final ReservaRequest PEDIDO =
            new ReservaRequest(EVENTO, PLATEA, List.of(new AsientoRequest(1, 1)), null);

    @BeforeEach
    void eventoConFila() {
        stubPlateaConFila(reloj.ahora().plusDays(30));
    }

    @Test
    void sinTurnoNoSePuedeReservar() {
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> reservaService.reservar(1L, PEDIDO, null));
        assertTrue(ex.getMessage().contains("fila virtual"));
    }

    @Test
    void conUnTurnoValidoSeReservaNormalmente() {
        ReservaDTO reserva = reservaService.reservar(1L, PEDIDO, turno(SECRETO, "1", EVENTO, 600));
        assertNotNull(reserva.id());
    }

    @Test
    void elTurnoDeOtroClienteNoSirve() {
        assertThrows(ForbiddenException.class,
                () -> reservaService.reservar(2L, PEDIDO, turno(SECRETO, "1", EVENTO, 600)));
    }

    @Test
    void elTurnoDeOtroEventoNoSirve() {
        assertThrows(ForbiddenException.class,
                () -> reservaService.reservar(1L, PEDIDO, turno(SECRETO, "1", 99L, 600)));
    }

    @Test
    void unTurnoVencidoNoSirve() {
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> reservaService.reservar(1L, PEDIDO, turno(SECRETO, "1", EVENTO, -60)));
        assertTrue(ex.getMessage().contains("vencio"));
    }

    @Test
    void unTurnoFalsificadoNoSirve() {
        String falso = turno("otro-secreto-que-no-es-el-de-fila-service-123456", "1", EVENTO, 600);
        assertThrows(ForbiddenException.class, () -> reservaService.reservar(1L, PEDIDO, falso));
    }

    @Test
    void unEventoSinFilaNoPideTurno() {
        stubPlatea(reloj.ahora().plusDays(30));
        assertNotNull(reservaService.reservar(1L, PEDIDO, null).id());
    }

    // Igual al que emite fila-service
    private static String turno(String secreto, String usuario, Long evento, long segundosDeVida) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer("ticketya-fila")
                .subject(usuario)
                .claim("evento", evento)
                .issuedAt(Date.from(ahora.minusSeconds(3600)))
                .expiration(Date.from(ahora.plusSeconds(segundosDeVida)))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
