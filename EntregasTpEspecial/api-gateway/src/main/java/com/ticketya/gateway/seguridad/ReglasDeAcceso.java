package com.ticketya.gateway.seguridad;

import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

// Tabla de autorizacion: que rol puede llamar a cada endpoint publicado en el gateway.
// Se evalua en orden y gana la primera regla que coincide. Si ninguna coincide, alcanza con estar autenticado.
// Es una clase sin dependencias de Spring para poder probarla con tests unitarios simples.
public class ReglasDeAcceso {

    public static final String CLIENTE = "CLIENTE";
    public static final String ORGANIZADOR = "ORGANIZADOR";
    public static final String ADMIN = "ADMIN";
    public static final String ACCESO = "ACCESO";

    public enum Decision { PERMITIDO, NO_AUTENTICADO, PROHIBIDO }

    // metodo null = cualquier metodo; roles vacio = endpoint publico (no requiere token)
    private record Regla(String metodo, String patron, Set<String> roles) {
    }

    private static final Set<String> PUBLICO = Set.of();

    private static final List<Regla> REGLAS = List.of(
            // ---- reportes y eventos propios del organizador (antes de la regla publica GET /eventos/**) ----
            new Regla("GET", "/eventos/mis-eventos", Set.of(ORGANIZADOR, ADMIN)),
            new Regla("GET", "/eventos/*/reporte", Set.of(ORGANIZADOR, ADMIN)),

            // ---- publicos: login, registro y navegacion del catalogo ----
            new Regla("POST", "/auth/login", PUBLICO),
            new Regla("POST", "/auth/registro", PUBLICO),
            new Regla("GET", "/eventos/**", PUBLICO),
            new Regla("GET", "/lugares/**", PUBLICO),
            new Regla("GET", "/disponibilidad/**", PUBLICO),
            new Regla("GET", "/api-docs/**", PUBLICO),   // documentacion OpenAPI de los servicios

            // ---- auth: gestion de usuarios ----
            new Regla(null, "/auth/usuarios/**", Set.of(ADMIN)),

            // ---- catalogo: organizadores (y admin) crean lugares y eventos; solo admin suspende o cancela ----
            new Regla("POST", "/lugares", Set.of(ORGANIZADOR, ADMIN)),
            new Regla("POST", "/eventos", Set.of(ORGANIZADOR, ADMIN)),
            new Regla("PUT", "/eventos/*/precios", Set.of(ORGANIZADOR, ADMIN)),
            new Regla("POST", "/eventos/*/publicacion", Set.of(ORGANIZADOR, ADMIN)),
            new Regla("PUT", "/eventos/*/suspension", Set.of(ADMIN)),
            new Regla("PUT", "/eventos/*/cancelacion", Set.of(ADMIN)),

            // ---- ventas: el cliente compra y ve sus entradas; el admin resuelve devoluciones ----
            new Regla(null, "/reservas/**", Set.of(CLIENTE)),
            new Regla(null, "/entradas/**", Set.of(CLIENTE)),
            new Regla("POST", "/devoluciones", Set.of(CLIENTE)),
            new Regla("GET", "/devoluciones", Set.of(ADMIN)),
            new Regla("PUT", "/devoluciones/*/aprobacion", Set.of(ADMIN)),
            new Regla("PUT", "/devoluciones/*/rechazo", Set.of(ADMIN)),

            // ---- preferencias y recomendaciones: solo el cliente ----
            new Regla(null, "/preferencias/**", Set.of(CLIENTE)),
            new Regla(null, "/recomendaciones/**", Set.of(CLIENTE)),

            // ---- control de acceso: el personal que escanea los QR en la puerta (y el admin) ----
            new Regla("POST", "/accesos", Set.of(ACCESO, ADMIN)),

            // ---- fila virtual: el cliente hace la fila; organizador y admin ven su estado ----
            new Regla("GET", "/filas/*/estado", Set.of(ORGANIZADOR, ADMIN)),
            new Regla(null, "/filas/**", Set.of(CLIENTE))

            // /notificaciones: no tiene regla, alcanza con estar autenticado (cada uno ve las suyas)
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    public boolean esPublico(String metodo, String ruta) {
        Regla regla = buscar(metodo, ruta);
        return regla != null && regla.roles().isEmpty();
    }

    public Decision evaluar(String metodo, String ruta, UsuarioAutenticado usuario) {
        Regla regla = buscar(metodo, ruta);
        if (regla != null && regla.roles().isEmpty()) {
            return Decision.PERMITIDO;
        }
        if (usuario == null) {
            return Decision.NO_AUTENTICADO;
        }
        if (regla == null || regla.roles().contains(usuario.rol())) {
            return Decision.PERMITIDO;
        }
        return Decision.PROHIBIDO;
    }

    private Regla buscar(String metodo, String ruta) {
        for (Regla regla : REGLAS) {
            boolean metodoOk = regla.metodo() == null || regla.metodo().equalsIgnoreCase(metodo);
            if (metodoOk && matcher.match(regla.patron(), ruta)) {
                return regla;
            }
        }
        return null;
    }
}
