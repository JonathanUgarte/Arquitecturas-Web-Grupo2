package com.ticketya.catalogo.client;

import com.ticketya.catalogo.exception.ServicioNoDisponibleException;
import com.ticketya.catalogo.trazabilidad.Correlacion;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

// Cliente HTTP hacia el servicio de ventas (el que necesita el dato/accion tiene el Client)
@Component
@Slf4j
public class VentasClient {

    private final RestClient restClient;

    public VentasClient(@Value("${ventas.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);   // timeout de conexion
        factory.setReadTimeout(3000);      // timeout de respuesta
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor(Correlacion.propagar())   // reenvia el X-Correlation-Id
                .build();
    }

    // Avisa a ventas que el evento se cancelo, para que pase sus entradas a reintegro.
    // Resiliencia: hasta 3 intentos (ventas lo procesa en forma idempotente, asi que repetirlo es seguro) y
    // circuit breaker. Fallback: devuelve false; el evento igual queda cancelado y el aviso queda PENDIENTE,
    // que se reintenta periodicamente (CancelacionService.reintentarAvisosPendientes).
    @Retry(name = "ventas", fallbackMethod = "cancelacionNoNotificada")
    @CircuitBreaker(name = "ventas")
    public boolean notificarCancelacion(Long eventoId) {
        try {
            restClient.post()
                    .uri("/interno/eventos/{id}/cancelacion", eventoId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("Ventas no recibio el aviso de cancelacion: " + e.getMessage());
        }
    }

    private boolean cancelacionNoNotificada(Long eventoId, Throwable causa) {
        log.warn("No se pudo notificar a ventas la cancelacion del evento {} (queda pendiente): {}",
                eventoId, causa.getMessage());
        return false;
    }

    // Ventas por sector de un evento, para el reporte del organizador. Sin ventas no hay reporte posible:
    // si el servicio no responde se informa 503 (no se inventan ceros).
    @Retry(name = "ventas")
    @CircuitBreaker(name = "ventas")
    public List<EstadisticaVentasDTO> estadisticas(Long eventoId) {
        try {
            List<EstadisticaVentasDTO> datos = restClient.get()
                    .uri("/interno/eventos/{id}/estadisticas", eventoId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EstadisticaVentasDTO>>() {
                    });
            return datos == null ? List.of() : datos;
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener las ventas del evento {}: {}", eventoId, e.getMessage());
            throw new ServicioNoDisponibleException("El servicio de ventas no responde: no se puede armar el reporte");
        }
    }
}
