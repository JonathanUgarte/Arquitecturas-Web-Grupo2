package com.ticketya.ventas.mensajeria;

import com.ticketya.ventas.trazabilidad.Correlacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// Envia a RabbitMQ los eventos que publican los servicios de negocio (con ApplicationEventPublisher).
//  - AFTER_COMMIT: solo se avisa lo que realmente quedo guardado. Si la transaccion se deshace, no sale nada.
//  - fallbackExecution: si se publica fuera de una transaccion (el pago), se envia en el momento.
//  - Fire-and-forget: si RabbitMQ no esta disponible, la compra NO falla; solo se pierde el aviso y queda en el log.
@Component
@ConditionalOnProperty(name = "ticketya.mensajeria.habilitada", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PublicadorDeEventos {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(fallbackExecution = true)
    public void compraConfirmada(CompraConfirmadaEvento evento) {
        enviar(CompraConfirmadaEvento.CLAVE, evento);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void eventoCancelado(EventoCanceladoEvento evento) {
        enviar(EventoCanceladoEvento.CLAVE, evento);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void devolucionAprobada(DevolucionAprobadaEvento evento) {
        enviar(DevolucionAprobadaEvento.CLAVE, evento);
    }

    private void enviar(String clave, Object evento) {
        try {
            rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, clave, evento, Correlacion.enMensaje());
            log.info("Evento publicado [{}]: {}", clave, evento);
        } catch (AmqpException e) {
            log.warn("No se pudo publicar el evento [{}] (RabbitMQ no disponible): {}", clave, e.getMessage());
        }
    }
}
