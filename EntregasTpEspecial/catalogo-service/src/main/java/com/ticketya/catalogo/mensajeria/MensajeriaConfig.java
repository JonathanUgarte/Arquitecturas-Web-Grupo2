package com.ticketya.catalogo.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Exchange de tipo "topic" compartido por todos los servicios: cada evento se publica con una clave
// (por ejemplo "compra.confirmada") y cada consumidor enlaza su propia cola a las claves que le interesan.
// Catalogo publica "evento.publicado" y consume "compra.confirmada" y "devolucion.aprobada" (estado AGOTADO).
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE = "ticketya.eventos";
    public static final String EXCHANGE_FALLIDOS = "ticketya.fallidos";
    public static final String COLA_FALLIDOS = "ticketya.fallidos";
    public static final String COLA_DISPONIBILIDAD = "catalogo.disponibilidad";

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
    public Queue colaDisponibilidad() {
        return QueueBuilder.durable(COLA_DISPONIBILIDAD).deadLetterExchange(EXCHANGE_FALLIDOS).build();
    }

    @Bean
    public Binding bindingCompras() {
        return BindingBuilder.bind(colaDisponibilidad()).to(eventosExchange()).with("compra.confirmada");
    }

    @Bean
    public Binding bindingDevoluciones() {
        return BindingBuilder.bind(colaDisponibilidad()).to(eventosExchange()).with("devolucion.aprobada");
    }

    // Los mensajes viajan en JSON, con el mismo ObjectMapper de Spring (fechas en formato ISO)
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
