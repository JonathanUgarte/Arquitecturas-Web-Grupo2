package com.ticketya.notificaciones.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

// Consulta el nombre del evento para armar mensajes legibles. No es critico: si falla, se usa "el evento #id".
@Component
public class CatalogoClient {

    record EventoNombre(Long id, String nombre) {
    }

    private final RestClient restClient;

    public CatalogoClient(RestClient.Builder builder, @Value("${notificaciones.catalogo-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Optional<String> nombreEvento(Long eventoId) {
        try {
            EventoNombre evento = restClient.get().uri("/eventos/{id}", eventoId).retrieve().body(EventoNombre.class);
            return Optional.ofNullable(evento).map(EventoNombre::nombre);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
