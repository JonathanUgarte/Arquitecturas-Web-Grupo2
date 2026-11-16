package com.ticketya.preferencias.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Cola propia de este servicio, enlazada al exchange comun con la clave "compra.confirmada".
// Si un mensaje falla 3 veces, RabbitMQ lo manda a la cola "ticketya.fallidos" (dead letter) para revisarlo.
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE = "ticketya.eventos";
    public static final String EXCHANGE_FALLIDOS = "ticketya.fallidos";
    public static final String COLA_FALLIDOS = "ticketya.fallidos";
    public static final String COLA_COMPRAS = "preferencias.compras";

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
    public Binding bindingCompras() {
        return BindingBuilder.bind(colaCompras()).to(eventosExchange()).with("compra.confirmada");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
