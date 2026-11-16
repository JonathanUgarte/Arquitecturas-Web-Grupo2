package com.ticketya.notificaciones.mensajeria;

import com.ticketya.notificaciones.service.NotificacionService;
import com.ticketya.notificaciones.trazabilidad.Correlacion;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

// Punto de entrada asincronico: cada cola dispara un tipo de aviso.
// El X-Correlation-Id viaja en un header del mensaje: el log del aviso queda unido al pedido que lo origino.
@Component
@RequiredArgsConstructor
public class ConsumidorDeEventos {

    private final NotificacionService notificacionService;

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRAS)
    public void compraConfirmada(CompraConfirmadaEvento evento,
                                 @Header(name = Correlacion.HEADER, required = false) String correlacion) {
        Correlacion.conId(correlacion, () -> notificacionService.compraConfirmada(evento));
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_CANCELACIONES)
    public void eventoCancelado(EventoCanceladoEvento evento,
                                @Header(name = Correlacion.HEADER, required = false) String correlacion) {
        Correlacion.conId(correlacion, () -> notificacionService.eventoCancelado(evento));
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_PUBLICACIONES)
    public void eventoPublicado(EventoPublicadoEvento evento,
                                @Header(name = Correlacion.HEADER, required = false) String correlacion) {
        Correlacion.conId(correlacion, () -> notificacionService.eventoPublicado(evento));
    }
}
