package com.ticketya.preferencias.client;

import com.ticketya.preferencias.exception.ServicioNoDisponibleException;
import com.ticketya.preferencias.trazabilidad.Correlacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

// Cliente HTTP hacia el catalogo (endpoints publicos de consulta). Llama directo al servicio por la red interna.
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(RestClient.Builder builder, @Value("${preferencias.catalogo-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory)
                .requestInterceptor(Correlacion.propagar())   // reenvia el X-Correlation-Id
                .build();
    }

    // Eventos a la venta: son los candidatos a recomendar
    public List<EventoCatalogo> eventosALaVenta() {
        try {
            List<EventoCatalogo> eventos = restClient.get()
                    .uri("/eventos")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EventoCatalogo>>() {
                    });
            return eventos == null ? List.of() : eventos;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("El servicio de catalogo no responde");
        }
    }

    // Detalle de un evento. Vacio si no se pudo obtener (se usa para enriquecer el historial: no es critico)
    public Optional<EventoDetalle> detalle(Long eventoId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/eventos/{id}", eventoId)
                    .retrieve()
                    .body(EventoDetalle.class));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }
}
