package com.ticketya.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Valida firma, emisor y vencimiento de los JWT que emite auth-service (HS256 con secreto compartido)
@Component
public class JwtValidador {

    static final String EMISOR = "ticketya-auth";

    private final SecretKey clave;

    public JwtValidador(@Value("${jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    // Devuelve el usuario si el token es valido. Lanza TokenInvalidoException si esta vencido,
    // mal firmado, mal formado o le faltan datos.
    public UsuarioAutenticado validar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(EMISOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String rol = claims.get("rol", String.class);
            if (claims.getSubject() == null || rol == null) {
                throw new TokenInvalidoException("El token no tiene usuario o rol");
            }
            return new UsuarioAutenticado(Long.valueOf(claims.getSubject()), rol);
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("Token invalido o vencido");
        }
    }

    public static class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
