package com.ticketya.gateway.seguridad;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// Autenticacion y autorizacion centralizadas en el gateway (patron "gateway offloading"):
//  1. Borra los headers X-Usuario-Id y X-Rol que mande el cliente (nadie puede hacerse pasar por otro).
//  2. Si viene "Authorization: Bearer <token>", valida el JWT.
//  3. Aplica la tabla de ReglasDeAcceso: 401 sin token valido, 403 si el rol no corresponde.
//  4. Reenvia el pedido al servicio con X-Usuario-Id y X-Rol tomados del token.
// Los servicios no se publican fuera de la red de Docker, asi que solo reciben pedidos que pasaron por aca.
@Component
public class AutenticacionFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USUARIO = "X-Usuario-Id";
    public static final String HEADER_ROL = "X-Rol";
    public static final String ATRIBUTO_USUARIO = "ticketya.usuario";

    private final JwtValidador jwtValidador;
    private final ReglasDeAcceso reglas = new ReglasDeAcceso();

    public AutenticacionFilter(JwtValidador jwtValidador) {
        this.jwtValidador = jwtValidador;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String metodo = request.getMethod().name();
        String ruta = request.getURI().getPath();
        boolean publico = reglas.esPublico(metodo, ruta);

        UsuarioAutenticado usuario = null;
        String autorizacion = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (autorizacion != null && autorizacion.startsWith("Bearer ")) {
            try {
                usuario = jwtValidador.validar(autorizacion.substring(7).trim());
            } catch (JwtValidador.TokenInvalidoException e) {
                // En un endpoint publico un token vencido no molesta: se atiende como anonimo
                if (!publico) {
                    return rechazar(exchange, HttpStatus.UNAUTHORIZED, e.getMessage());
                }
            }
        }

        switch (reglas.evaluar(metodo, ruta, usuario)) {
            case NO_AUTENTICADO:
                return rechazar(exchange, HttpStatus.UNAUTHORIZED,
                        "Falta el token: enviar el header 'Authorization: Bearer <token>' (se obtiene en POST /auth/login)");
            case PROHIBIDO:
                return rechazar(exchange, HttpStatus.FORBIDDEN, "El rol " + usuario.rol() + " no puede usar este endpoint");
            default:
                break;
        }

        final UsuarioAutenticado autenticado = usuario;
        ServerHttpRequest reenviado = request.mutate().headers(h -> {
            h.remove(HEADER_USUARIO);
            h.remove(HEADER_ROL);
            if (autenticado != null) {
                h.set(HEADER_USUARIO, String.valueOf(autenticado.id()));
                h.set(HEADER_ROL, autenticado.rol());
            }
        }).build();
        if (autenticado != null) {
            exchange.getAttributes().put(ATRIBUTO_USUARIO, autenticado);
        }
        return chain.filter(exchange.mutate().request(reenviado).build());
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        // Mismo formato de error que los servicios (ErrorDTO)
        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"mensaje\":\"" + mensaje.replace("\"", "'") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    // Despues del filtro de registro (que debe medir y loguear tambien los pedidos rechazados)
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
