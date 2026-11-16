package com.ticketya.fila.client;

import com.ticketya.fila.exception.NotFoundException;
import com.ticketya.fila.exception.ServicioNoDisponibleException;
import com.ticketya.fila.trazabilidad.Correlacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Consulta en catalogo si el evento existe, esta a la venta y usa fila virtual
@Component
public class CatalogoClient {

    public record EventoFila(Long id, String nombre, String estado, boolean filaVirtual) {
    }

    private final RestClient restClient;

    public CatalogoClient(RestClient.Builder builder, @Value("${fila.catalogo-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory)
                .requestInterceptor(Correlacion.propagar())   // reenvia el X-Correlation-Id
                .build();
    }

    public EventoFila evento(Long eventoId) {
        try {
            EventoFila e = restClient.get().uri("/eventos/{id}", eventoId).retrieve().body(EventoFila.class);
            if (e == null) {
                throw new NotFoundException("No existe el evento " + eventoId);
            }
            return e;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("No existe el evento " + eventoId);
        } catch (RestClientException ex) {
            throw new ServicioNoDisponibleException("El servicio de catalogo no responde");
        }
    }
}
