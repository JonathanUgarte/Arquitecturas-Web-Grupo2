package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.exception.ForbiddenException;

// Segunda barrera de autorizacion: el gateway valida el JWT y reenvia el rol en X-Rol
public final class Seguridad {

    private Seguridad() {
    }

    public static void exigirCliente(String rol) {
        if (!"CLIENTE".equals(rol)) {
            throw new ForbiddenException("Esta accion requiere el rol CLIENTE");
        }
    }
}
