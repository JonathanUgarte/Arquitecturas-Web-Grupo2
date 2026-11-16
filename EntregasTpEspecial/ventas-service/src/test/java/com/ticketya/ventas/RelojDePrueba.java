package com.ticketya.ventas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Reloj controlable para los tests: permite "adelantar el tiempo" (vencimiento de reservas, plazo de 72 hs)
public class RelojDePrueba extends Clock {

    private volatile Instant instante = Instant.now();
    private final ZoneId zona = ZoneId.systemDefault();

    public void fijarEn(Instant nuevo) {
        this.instante = nuevo;
    }

    public void avanzar(Duration duracion) {
        this.instante = this.instante.plus(duracion);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(this);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
