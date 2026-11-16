package com.ticketya.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

// Primer paso de trazabilidad: cada solicitud queda registrada (metodo, ruta, usuario, codigo de
// respuesta y tiempo) y lleva un identificador de correlacion (X-Correlation-Id) que se reenvia a los
// servicios y vuelve en la respuesta. La entrega 2 lo completa con el log centralizado.
@Component
@Slf4j
public class RegistroDeSolicitudesFilter implements GlobalFilter, Ordered {

    private static final String HEADER_CORRELACION = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();

        String correlacion = exchange.getRequest().getHeaders().getFirst(HEADER_CORRELACION);
        if (correlacion == null || correlacion.isBlank()) {
            correlacion = UUID.randomUUID().toString();
        }
        final String idCorrelacion = correlacion;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HEADER_CORRELACION, idCorrelacion)
                .build();
        ServerWebExchange nuevo = exchange.mutate().request(request).build();
        nuevo.getResponse().getHeaders().set(HEADER_CORRELACION, idCorrelacion);

        return chain.filter(nuevo).doFinally(senal -> {
            String usuario = request.getHeaders().getFirst("X-Usuario-Id");
            Integer codigo = nuevo.getResponse().getStatusCode() == null
                    ? null
                    : nuevo.getResponse().getStatusCode().value();
            log.info("[{}] {} {} usuario={} -> {} ({} ms)", idCorrelacion, request.getMethod(),
                    request.getURI().getPath(), usuario, codigo, System.currentTimeMillis() - inicio);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
