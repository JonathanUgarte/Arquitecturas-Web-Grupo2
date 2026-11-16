package com.ticketya.ventas.client;

import com.ticketya.ventas.config.VentasProperties;
import com.ticketya.ventas.dto.SectorVentaDTO;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.exception.ServicioNoDisponibleException;
import com.ticketya.ventas.trazabilidad.Correlacion;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Cliente HTTP hacia el servicio de catalogo: ventas necesita el dato, entonces ventas tiene el Client
@Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(VentasProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);   // timeout de conexion
        factory.setReadTimeout(3000);      // timeout de respuesta
        this.restClient = RestClient.builder()
                .baseUrl(props.getCatalogoUrl())
                .requestFactory(factory)
                .requestInterceptor(Correlacion.propagar())   // reenvia el X-Correlation-Id
                .build();
    }

    // Resiliencia (configurada en application.properties):
    //  - timeouts de conexion (2 s) y de respuesta (3 s): nunca se espera indefinidamente
    //  - Retry: hasta 3 intentos ante caidas o errores 5xx (ServicioNoDisponibleException); un 404 no se reintenta
    //  - CircuitBreaker: si catalogo falla seguido, las reservas fallan al instante (503) hasta que se recupere
    // No hay fallback con datos viejos: para vender hacen falta el estado y el precio ACTUALES del evento.
    @Retry(name = "catalogo")
    @CircuitBreaker(name = "catalogo")
    public SectorVentaDTO obtenerSector(Long eventoId, Long sectorId) {
        try {
            return restClient.get()
                    .uri("/interno/eventos/{eventoId}/sectores/{sectorId}", eventoId, sectorId)
                    .retrieve()
                    .body(SectorVentaDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("El evento o el sector no existe, o el sector no tiene precio en ese evento");
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException("El servicio de catalogo no responde");
        } catch (RestClientResponseException e) {
            throw new ServicioNoDisponibleException(
                    "El servicio de catalogo respondio con error " + e.getStatusCode().value());
        }
    }
}
