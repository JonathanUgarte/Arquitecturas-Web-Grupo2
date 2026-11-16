package com.ticketya.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Se carga desde application.properties (prefijo "jwt")
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    // Secreto HMAC compartido con el gateway. HS256 exige al menos 32 bytes.
    private String secret;
    private long expiracionMinutos = 60;
}
