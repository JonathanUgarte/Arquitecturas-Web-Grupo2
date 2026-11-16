package com.ticketya.ventas.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Acceso RESILIENTE a la pasarela de pagos (servicio externo).
//  - Retry: ante una falla temporal (PasarelaNoDisponibleException) reintenta hasta 3 veces con espera creciente.
//    Es seguro porque cada cobro lleva una referencia unica: la pasarela no cobra dos veces la misma compra.
//  - CircuitBreaker: si la pasarela falla seguido, deja de llamarla un tiempo y falla al instante
//    (CallNotPermittedException), en lugar de dejar a cada cliente esperando timeouts.
// Configuracion en application.properties (resilience4j.*.instances.pasarela).
@Component
@RequiredArgsConstructor
public class ClientePasarela {

    private final PasarelaPagoMock pasarela;

    @Retry(name = "pasarela")
    @CircuitBreaker(name = "pasarela")
    public ResultadoPago cobrar(String referencia, BigDecimal monto, String medioPago) {
        return pasarela.cobrar(referencia, monto, medioPago);
    }

    @Retry(name = "pasarela")
    @CircuitBreaker(name = "pasarela")
    public void reintegrar(BigDecimal monto, String transaccionPago) {
        pasarela.reintegrar(monto, transaccionPago);
    }
}
