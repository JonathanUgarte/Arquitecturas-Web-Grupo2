package com.ticketya.notificaciones.client;

import com.ticketya.notificaciones.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;

// Pregunta a preferencias que clientes tienen a estos artistas como favoritos en esa ciudad
@Component
public class PreferenciasClient {

    private final RestClient restClient;

    public PreferenciasClient(RestClient.Builder builder, @Value("${notificaciones.preferencias-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    // Si preferencias no responde se lanza la excepcion: el mensaje se reintenta y, si sigue fallando,
    // queda en la cola de fallidos (no se pierde el aviso en silencio)
    public List<Long> interesados(String ciudad, Collection<String> artistas) {
        try {
            List<Long> ids = restClient.get()
                    .uri(uri -> uri.path("/interno/interesados")
                            .queryParam("ciudad", ciudad)
                            .queryParam("artistas", artistas.toArray())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Long>>() {
                    });
            return ids == null ? List.of() : ids;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("El servicio de preferencias no responde: " + e.getMessage());
        }
    }
}
