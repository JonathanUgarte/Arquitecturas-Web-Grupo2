package com.ticketya.notificaciones.mensajeria;

import com.ticketya.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Punto de entrada asincronico: cada cola dispara un tipo de aviso
@Component
@RequiredArgsConstructor
public class ConsumidorDeEventos {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRAS)
    public void compraConfirmada(CompraConfirmadaEvento evento) {
        notificacionService.compraConfirmada(evento);
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_CANCELACIONES)
    public void eventoCancelado(EventoCanceladoEvento evento) {
        notificacionService.eventoCancelado(evento);
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_PUBLICACIONES)
    public void eventoPublicado(EventoPublicadoEvento evento) {
        notificacionService.eventoPublicado(evento);
    }
}
