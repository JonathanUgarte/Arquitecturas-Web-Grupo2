package com.ticketya.preferencias.mensajeria;

import com.ticketya.preferencias.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Cada compra confirmada se agrega al historial del cliente, que alimenta las recomendaciones
@Component
@RequiredArgsConstructor
public class ConsumidorDeCompras {

    private final PerfilService perfilService;

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRAS)
    public void compraConfirmada(CompraConfirmadaEvento evento) {
        perfilService.registrarCompra(evento);
    }
}
