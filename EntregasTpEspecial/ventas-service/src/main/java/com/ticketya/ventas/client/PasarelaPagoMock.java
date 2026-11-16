package com.ticketya.ventas.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Pasarela de pagos SIMULADA (mock), con los comportamientos de una pasarela real para probar la resiliencia.
// Segun el medio de pago:
//   contiene "RECHAZ"   -> pago rechazado (respuesta definitiva). Ej.: TARJETA_RECHAZADA
//   contiene "INESTABLE"-> el primer intento falla (timeout) y el reintento se aprueba. Ej.: TARJETA_INESTABLE
//   contiene "CAIDA"    -> la pasarela no responde nunca. Ej.: PASARELA_CAIDA
//   cualquier otro      -> aprobado
// Es idempotente por referencia (como las pasarelas reales con "Idempotency-Key"): si se reintenta un cobro que
// ya se aprobo, devuelve la misma transaccion y no cobra dos veces.
@Component
@Slf4j
public class PasarelaPagoMock {

    private final Map<String, ResultadoPago> cobrosProcesados = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> intentos = new ConcurrentHashMap<>();
    private final AtomicInteger cobrosReales = new AtomicInteger();
    private volatile boolean reintegrosCaidos = false;

    public ResultadoPago cobrar(String referencia, BigDecimal monto, String medioPago) {
        ResultadoPago previo = cobrosProcesados.get(referencia);
        if (previo != null) {
            log.info("Pasarela (mock): la referencia {} ya fue procesada, no se cobra de nuevo", referencia);
            return previo;
        }
        String medio = medioPago == null ? "" : medioPago.toUpperCase();
        int intento = intentos.computeIfAbsent(referencia, r -> new AtomicInteger()).incrementAndGet();
        if (medio.contains("CAIDA") || (medio.contains("INESTABLE") && intento == 1)) {
            log.warn("Pasarela (mock): sin respuesta para {} (intento {})", referencia, intento);
            throw new PasarelaNoDisponibleException("La pasarela de pagos no respondio (timeout)");
        }
        ResultadoPago resultado;
        if (medio.contains("RECHAZ")) {
            log.info("Pasarela (mock): pago de {} RECHAZADO con {}", monto, medioPago);
            resultado = ResultadoPago.rechazado("Pago rechazado por el medio de pago");
        } else {
            String transaccion = "TX-" + UUID.randomUUID();
            cobrosReales.incrementAndGet();
            log.info("Pasarela (mock): pago de {} APROBADO con {} ({})", monto, medioPago, transaccion);
            resultado = ResultadoPago.aprobado(transaccion);
        }
        cobrosProcesados.put(referencia, resultado);
        return resultado;
    }

    public void reintegrar(BigDecimal monto, String transaccionPago) {
        if (reintegrosCaidos) {
            throw new PasarelaNoDisponibleException("La pasarela de pagos no respondio al pedido de reintegro");
        }
        log.info("Pasarela (mock): reintegro de {} solicitado para la transaccion {}", monto, transaccionPago);
    }

    // ---- solo para pruebas ----
    public void simularReintegrosCaidos(boolean caidos) {
        this.reintegrosCaidos = caidos;
    }

    public int cobrosReales() {
        return cobrosReales.get();
    }
}
