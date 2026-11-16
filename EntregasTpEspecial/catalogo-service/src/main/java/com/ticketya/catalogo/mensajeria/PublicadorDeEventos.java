package com.ticketya.catalogo.mensajeria;

import com.ticketya.catalogo.trazabilidad.Correlacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// Envia a RabbitMQ los eventos del catalogo, solo despues de confirmar la transaccion (AFTER_COMMIT).
// Fire-and-forget: si RabbitMQ no esta disponible, la publicacion del evento igual se completa.
@Component
@ConditionalOnProperty(name = "ticketya.mensajeria.habilitada", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PublicadorDeEventos {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(fallbackExecution = true)
    public void eventoPublicado(EventoPublicadoEvento evento) {
        try {
            rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, EventoPublicadoEvento.CLAVE, evento,
                    Correlacion.enMensaje());
            log.info("Evento publicado [{}]: {}", EventoPublicadoEvento.CLAVE, evento);
        } catch (AmqpException e) {
            log.warn("No se pudo publicar [{}] (RabbitMQ no disponible): {}", EventoPublicadoEvento.CLAVE, e.getMessage());
        }
    }
}
