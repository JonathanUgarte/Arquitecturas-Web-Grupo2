package com.ticketya.notificaciones.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Una cola por tipo de evento, todas de este servicio, enlazadas al exchange comun "ticketya.eventos".
// Los mensajes que fallan 3 veces van a "ticketya.fallidos" (dead letter queue).
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE = "ticketya.eventos";
    public static final String EXCHANGE_FALLIDOS = "ticketya.fallidos";
    public static final String COLA_FALLIDOS = "ticketya.fallidos";

    public static final String COLA_COMPRAS = "notificaciones.compras";
    public static final String COLA_CANCELACIONES = "notificaciones.cancelaciones";
    public static final String COLA_PUBLICACIONES = "notificaciones.publicaciones";
    public static final String COLA_SUSPENSIONES = "notificaciones.suspensiones";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange fallidosExchange() {
        return new FanoutExchange(EXCHANGE_FALLIDOS, true, false);
    }

    @Bean
    public Queue colaFallidos() {
        return QueueBuilder.durable(COLA_FALLIDOS).build();
    }

    @Bean
    public Binding bindingFallidos() {
        return BindingBuilder.bind(colaFallidos()).to(fallidosExchange());
    }

    @Bean
    public Queue colaCompras() {
        return QueueBuilder.durable(COLA_COMPRAS).deadLetterExchange(EXCHANGE_FALLIDOS).build();
    }

    @Bean
    public Queue colaCancelaciones() {
        return QueueBuilder.durable(COLA_CANCELACIONES).deadLetterExchange(EXCHANGE_FALLIDOS).build();
    }

    @Bean
    public Queue colaPublicaciones() {
        return QueueBuilder.durable(COLA_PUBLICACIONES).deadLetterExchange(EXCHANGE_FALLIDOS).build();
    }

    @Bean
    public Queue colaSuspensiones() {
        return QueueBuilder.durable(COLA_SUSPENSIONES).deadLetterExchange(EXCHANGE_FALLIDOS).build();
    }

    @Bean
    public Binding bindingSuspensiones() {
        return BindingBuilder.bind(colaSuspensiones()).to(eventosExchange()).with("evento.suspendido");
    }

    @Bean
    public Binding bindingCompras() {
        return BindingBuilder.bind(colaCompras()).to(eventosExchange()).with("compra.confirmada");
    }

    @Bean
    public Binding bindingCancelaciones() {
        return BindingBuilder.bind(colaCancelaciones()).to(eventosExchange()).with("evento.cancelado");
    }

    @Bean
    public Binding bindingPublicaciones() {
        return BindingBuilder.bind(colaPublicaciones()).to(eventosExchange()).with("evento.publicado");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
