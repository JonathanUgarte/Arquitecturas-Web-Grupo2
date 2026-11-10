package com.ticketya.ventas.controller;

import com.ticketya.ventas.exception.ForbiddenException;

// Sin autenticacion en esta entrega: el rol viaja en el header X-Rol (JWT llega en la entrega 2)
public final class Seguridad {

    private Seguridad() {
    }

    public static void exigirAdministrador(String rol) {
        if (!"ADMIN".equalsIgnoreCase(rol)) {
            throw new ForbiddenException("Esta accion requiere el header X-Rol: ADMIN");
        }
    }
}
