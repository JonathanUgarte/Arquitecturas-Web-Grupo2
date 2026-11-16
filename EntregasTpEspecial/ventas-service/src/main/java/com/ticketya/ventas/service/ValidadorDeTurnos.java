package com.ticketya.ventas.service;

import com.ticketya.ventas.exception.ForbiddenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// (6) Verifica el turno de la fila virtual que emite fila-service: un token firmado con el usuario, el evento y
// el vencimiento. Se verifica localmente (firma + datos), sin llamar a fila-service en cada reserva.
@Component
public class ValidadorDeTurnos {

    static final String EMISOR = "ticketya-fila";

    private final SecretKey clave;

    public ValidadorDeTurnos(@Value("${ticketya.turno.secreto}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ticketya.turno.secreto debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public void validar(String turno, Long usuarioId, Long eventoId) {
        if (turno == null || turno.isBlank()) {
            throw new ForbiddenException("Este evento usa fila virtual: ingresa a la fila (POST /filas/" + eventoId
                    + "/ingreso) y, cuando sea tu turno, envia el turno en el header X-Turno-Fila");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clave).requireIssuer(EMISOR).build()
                    .parseSignedClaims(turno.trim()).getPayload();
        } catch (ExpiredJwtException e) {
            throw new ForbiddenException("Tu turno de la fila virtual vencio: ingresa a la fila de nuevo");
        } catch (JwtException | IllegalArgumentException e) {
            throw new ForbiddenException("El turno de la fila virtual no es valido");
        }
        if (!String.valueOf(usuarioId).equals(claims.getSubject())) {
            throw new ForbiddenException("El turno de la fila virtual es de otro usuario");
        }
        Long eventoDelTurno = claims.get("evento", Long.class);
        if (!eventoId.equals(eventoDelTurno)) {
            throw new ForbiddenException("El turno de la fila virtual es de otro evento");
        }
    }
}
