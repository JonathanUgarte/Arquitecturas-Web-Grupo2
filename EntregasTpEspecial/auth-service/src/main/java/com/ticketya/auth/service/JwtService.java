package com.ticketya.auth.service;

import com.ticketya.auth.config.JwtProperties;
import com.ticketya.auth.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

// Emite los JWT. El token lleva el id del usuario (subject) y su rol; el gateway lo valida con el mismo secreto.
@Service
public class JwtService {

    public static final String EMISOR = "ticketya-auth";

    private final SecretKey clave;
    private final long expiracionSegundos;
    private final Clock reloj;

    public JwtService(JwtProperties props, Clock reloj) {
        if (props.getSecret() == null || props.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expiracionSegundos = props.getExpiracionMinutos() * 60;
        this.reloj = reloj;
    }

    public String generar(Usuario usuario) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionSegundos)))
                .signWith(clave)
                .compact();
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }
}
