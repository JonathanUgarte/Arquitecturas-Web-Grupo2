package com.ticketya.catalogo.mensajeria;

import com.ticketya.catalogo.service.DisponibilidadService;
import com.ticketya.catalogo.trazabilidad.Correlacion;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

// Cada compra confirmada o devolucion aprobada puede cambiar si el evento esta AGOTADO
@Component
@RequiredArgsConstructor
public class ConsumidorDeVentas {

    private final DisponibilidadService disponibilidadService;

    @RabbitListener(queues = MensajeriaConfig.COLA_DISPONIBILIDAD)
    public void ventasCambiaron(CambioDeVentasEvento evento,
                                @Header(name = Correlacion.HEADER, required = false) String correlacion) {
        if (evento.eventoId() != null) {
            Correlacion.conId(correlacion, () -> disponibilidadService.reevaluar(evento.eventoId()));
        }
    }
}
