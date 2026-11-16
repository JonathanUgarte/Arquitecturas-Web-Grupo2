package com.ticketya.fila.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

// Emite el turno de un cliente habilitado: un JWT firmado (HS256) con el usuario, el evento y el vencimiento.
// Ventas lo verifica con el mismo secreto, sin consultar a este servicio en cada reserva: en la apertura de una
// venta masiva la fila no se convierte en un cuello de botella para las compras.
@Component
public class EmisorDeTurnos {

    public static final String EMISOR = "ticketya-fila";

    private final SecretKey clave;

    public EmisorDeTurnos(@Value("${ticketya.turno.secreto}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ticketya.turno.secreto debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String emitir(Long eventoId, Long usuarioId, Instant ahora, Instant vence) {
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuarioId))
                .claim("evento", eventoId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vence))
                .signWith(clave)
                .compact();
    }
}
