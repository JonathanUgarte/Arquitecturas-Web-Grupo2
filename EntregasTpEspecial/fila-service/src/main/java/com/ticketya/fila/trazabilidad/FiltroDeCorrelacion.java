package com.ticketya.fila.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Toma el X-Correlation-Id que manda el gateway (o genera uno si se llamo directo al servicio), lo deja en el
// MDC para que aparezca en cada linea de log mientras se atiende el pedido, lo devuelve en la respuesta y
// registra el pedido en la bitacora (metodo, ruta, usuario, codigo y tiempo).
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class FiltroDeCorrelacion extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(Correlacion.HEADER);
        if (id == null || id.isBlank()) {
            id = Correlacion.nuevoId();
        }
        long inicio = System.currentTimeMillis();
        MDC.put(Correlacion.MDC_CLAVE, id);
        response.setHeader(Correlacion.HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            if (!request.getRequestURI().startsWith("/actuator")) {
                String usuario = request.getHeader("X-Usuario-Id");
                log.info("{} {} usuario={} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                        usuario == null ? "-" : usuario, response.getStatus(), System.currentTimeMillis() - inicio);
            }
            MDC.remove(Correlacion.MDC_CLAVE);
        }
    }
}
