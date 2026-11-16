package com.ticketya.catalogo.controller;

import com.ticketya.catalogo.exception.ForbiddenException;

import java.util.Arrays;

// Autorizacion dentro del servicio (segunda barrera, ademas de la del gateway).
// El gateway valida el JWT y reenvia el rol en el header X-Rol, borrando cualquier X-Rol que mande el cliente.
public final class Seguridad {

    public static final String CLIENTE = "CLIENTE";
    public static final String ORGANIZADOR = "ORGANIZADOR";
    public static final String ADMIN = "ADMIN";
    public static final String ACCESO = "ACCESO";

    private Seguridad() {
    }

    public static void exigirRol(String rol, String... permitidos) {
        if (rol == null || Arrays.stream(permitidos).noneMatch(rol::equals)) {
            throw new ForbiddenException("Esta accion requiere el rol " + String.join(" o ", permitidos));
        }
    }

    public static void exigirAdministrador(String rol) {
        exigirRol(rol, ADMIN);
    }
}
