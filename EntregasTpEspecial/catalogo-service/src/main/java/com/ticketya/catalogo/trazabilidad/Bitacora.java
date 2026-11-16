package com.ticketya.catalogo.trazabilidad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Bitacora de operaciones criticas: una linea por operacion, con un logger propio llamado "BITACORA".
// Cada linea lleva el X-Correlation-Id del pedido (MDC), asi que en Grafana se puede filtrar por "BITACORA"
// para ver todas las operaciones, o por un id para seguir una operacion por todos los servicios.
public final class Bitacora {

    private static final Logger LOG = LoggerFactory.getLogger("BITACORA");

    private Bitacora() {
    }

    public static void registrar(String operacion, String detalle) {
        LOG.info("{} | {}", operacion, detalle);
    }
}
