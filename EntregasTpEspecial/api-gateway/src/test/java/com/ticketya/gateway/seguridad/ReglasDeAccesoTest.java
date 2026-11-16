package com.ticketya.gateway.seguridad;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.ticketya.gateway.seguridad.ReglasDeAcceso.Decision.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Verifica, endpoint por endpoint, que cada uno solo es accesible por los roles que corresponden.
// Cada fila: metodo, ruta, rol del usuario (ANONIMO = sin token) y decision esperada.
class ReglasDeAccesoTest {

    private final ReglasDeAcceso reglas = new ReglasDeAcceso();

    @ParameterizedTest(name = "{0} {1} como {2} -> {3}")
    @CsvSource({
            // ---- publicos ----
            "POST, /auth/login,                         ANONIMO,     PERMITIDO",
            "POST, /auth/registro,                      ANONIMO,     PERMITIDO",
            "GET,  /eventos,                            ANONIMO,     PERMITIDO",
            "GET,  /eventos/1,                          ANONIMO,     PERMITIDO",
            "GET,  /lugares,                            ANONIMO,     PERMITIDO",
            "GET,  /disponibilidad/eventos/1/sectores/2, ANONIMO,    PERMITIDO",

            // ---- gestion de usuarios: solo ADMIN ----
            "POST, /auth/usuarios,                      ANONIMO,     NO_AUTENTICADO",
            "POST, /auth/usuarios,                      CLIENTE,     PROHIBIDO",
            "POST, /auth/usuarios,                      ORGANIZADOR, PROHIBIDO",
            "POST, /auth/usuarios,                      ADMIN,       PERMITIDO",
            "GET,  /auth/usuarios,                      ACCESO,      PROHIBIDO",

            // ---- catalogo: organizador y admin ----
            "POST, /lugares,                            ANONIMO,     NO_AUTENTICADO",
            "POST, /lugares,                            CLIENTE,     PROHIBIDO",
            "POST, /lugares,                            ORGANIZADOR, PERMITIDO",
            "POST, /eventos,                            CLIENTE,     PROHIBIDO",
            "POST, /eventos,                            ORGANIZADOR, PERMITIDO",
            "POST, /eventos,                            ADMIN,       PERMITIDO",
            "PUT,  /eventos/7/precios,                  ACCESO,      PROHIBIDO",
            "PUT,  /eventos/7/precios,                  ORGANIZADOR, PERMITIDO",
            "POST, /eventos/7/publicacion,              CLIENTE,     PROHIBIDO",
            "POST, /eventos/7/publicacion,              ORGANIZADOR, PERMITIDO",

            // ---- catalogo: solo admin suspende o cancela ----
            "PUT,  /eventos/7/suspension,               ORGANIZADOR, PROHIBIDO",
            "PUT,  /eventos/7/suspension,               ADMIN,       PERMITIDO",
            "PUT,  /eventos/7/cancelacion,              CLIENTE,     PROHIBIDO",
            "PUT,  /eventos/7/cancelacion,              ORGANIZADOR, PROHIBIDO",
            "PUT,  /eventos/7/cancelacion,              ADMIN,       PERMITIDO",

            // ---- ventas: solo el cliente compra y ve sus entradas ----
            "POST,   /reservas,                         ANONIMO,     NO_AUTENTICADO",
            "POST,   /reservas,                         CLIENTE,     PERMITIDO",
            "POST,   /reservas,                         ADMIN,       PROHIBIDO",
            "POST,   /reservas,                         ORGANIZADOR, PROHIBIDO",
            "GET,    /reservas/5,                       CLIENTE,     PERMITIDO",
            "POST,   /reservas/5/pago,                  CLIENTE,     PERMITIDO",
            "POST,   /reservas/5/pago,                  ACCESO,      PROHIBIDO",
            "DELETE, /reservas/5,                       CLIENTE,     PERMITIDO",
            "GET,    /entradas,                         ANONIMO,     NO_AUTENTICADO",
            "GET,    /entradas,                         CLIENTE,     PERMITIDO",
            "GET,    /entradas/3,                       ORGANIZADOR, PROHIBIDO",

            // ---- devoluciones: el cliente solicita, el admin resuelve ----
            "POST, /devoluciones,                       CLIENTE,     PERMITIDO",
            "POST, /devoluciones,                       ADMIN,       PROHIBIDO",
            "GET,  /devoluciones,                       CLIENTE,     PROHIBIDO",
            "GET,  /devoluciones,                       ADMIN,       PERMITIDO",
            "PUT,  /devoluciones/9/aprobacion,          CLIENTE,     PROHIBIDO",
            "PUT,  /devoluciones/9/aprobacion,          ADMIN,       PERMITIDO",
            "PUT,  /devoluciones/9/rechazo,             ORGANIZADOR, PROHIBIDO",
            "PUT,  /devoluciones/9/rechazo,             ADMIN,       PERMITIDO",

            // ---- preferencias y recomendaciones: solo CLIENTE ----
            "GET,  /preferencias,                       ANONIMO,     NO_AUTENTICADO",
            "GET,  /preferencias,                       CLIENTE,     PERMITIDO",
            "PUT,  /preferencias,                       CLIENTE,     PERMITIDO",
            "PUT,  /preferencias,                       ORGANIZADOR, PROHIBIDO",
            "GET,  /recomendaciones,                    CLIENTE,     PERMITIDO",
            "GET,  /recomendaciones,                    ADMIN,       PROHIBIDO",

            // ---- notificaciones: cualquier usuario autenticado ----
            "GET,  /notificaciones,                     ANONIMO,     NO_AUTENTICADO",
            "GET,  /notificaciones,                     CLIENTE,     PERMITIDO",
            "GET,  /notificaciones,                     ORGANIZADOR, PERMITIDO",
    })
    void cadaEndpointSoloEsAccesiblePorSusRoles(String metodo, String ruta, String rol,
                                                ReglasDeAcceso.Decision esperada) {
        UsuarioAutenticado usuario = "ANONIMO".equals(rol) ? null : new UsuarioAutenticado(1L, rol);
        assertEquals(esperada, reglas.evaluar(metodo, ruta, usuario));
    }
}
