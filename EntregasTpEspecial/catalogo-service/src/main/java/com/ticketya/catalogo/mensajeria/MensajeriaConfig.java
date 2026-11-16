package com.ticketya.catalogo.mensajeria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Exchange de tipo "topic" compartido por todos los servicios: cada evento se publica con una clave
// (por ejemplo "compra.confirmada") y cada consumidor enlaza su propia cola a las claves que le interesan.
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE = "ticketya.eventos";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Los mensajes viajan en JSON, con el mismo ObjectMapper de Spring (fechas en formato ISO)
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
