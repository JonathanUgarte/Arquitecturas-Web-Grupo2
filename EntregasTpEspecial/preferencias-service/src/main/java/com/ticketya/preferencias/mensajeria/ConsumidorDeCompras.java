package com.ticketya.preferencias.mensajeria;

import com.ticketya.preferencias.service.PerfilService;
import com.ticketya.preferencias.trazabilidad.Correlacion;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

// Cada compra confirmada se agrega al historial del cliente, que alimenta las recomendaciones.
// El X-Correlation-Id viaja en un header del mensaje: el log del consumo queda unido al pedido que lo origino.
@Component
@RequiredArgsConstructor
public class ConsumidorDeCompras {

    private final PerfilService perfilService;

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRAS)
    public void compraConfirmada(CompraConfirmadaEvento evento,
                                 @Header(name = Correlacion.HEADER, required = false) String correlacion) {
        Correlacion.conId(correlacion, () -> perfilService.registrarCompra(evento));
    }
}
