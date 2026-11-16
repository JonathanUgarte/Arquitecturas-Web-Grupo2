package com.ticketya.ventas.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

// Pasarela de pagos SIMULADA (mock). Rechaza cuando el medio de pago contiene "RECHAZ"
// (por ejemplo "TARJETA_RECHAZADA"); cualquier otro medio de pago se aprueba.
@Component
@Slf4j
public class PasarelaPagoMock {

    public ResultadoPago cobrar(BigDecimal monto, String medioPago) {
        if (medioPago != null && medioPago.toUpperCase().contains("RECHAZ")) {
            log.info("Pasarela (mock): pago de {} RECHAZADO con {}", monto, medioPago);
            return new ResultadoPago(false, null, "Pago rechazado por el medio de pago");
        }
        String transaccion = "TX-" + UUID.randomUUID();
        log.info("Pasarela (mock): pago de {} APROBADO con {} ({})", monto, medioPago, transaccion);
        return new ResultadoPago(true, transaccion, null);
    }

    public boolean reintegrar(BigDecimal monto, String transaccionPago) {
        log.info("Pasarela (mock): reintegro de {} solicitado para la transaccion {}", monto, transaccionPago);
        return true;
    }
}
