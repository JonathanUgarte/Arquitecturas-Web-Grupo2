package com.ticketya.catalogo.client;

import com.ticketya.catalogo.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
                .build();
    }

    // Avisa a ventas que el evento se cancelo, para que pase sus entradas a reintegro.
    // Devuelve false si no se pudo (ventas caido o con error): el evento igual queda cancelado.
    public boolean notificarCancelacion(Long eventoId) {
        try {
            restClient.post()
                    .uri("/interno/eventos/{id}/cancelacion", eventoId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("No se pudo notificar a ventas la cancelacion del evento {}: {}", eventoId, e.getMessage());
            return false;
        }
    }

    // Ventas por sector de un evento, para el reporte del organizador. Sin ventas no hay reporte posible:
    // si el servicio no responde se informa 503 (no se inventan ceros).
    public List<EstadisticaVentasDTO> estadisticas(Long eventoId) {
        try {
            List<EstadisticaVentasDTO> datos = restClient.get()
                    .uri("/interno/eventos/{id}/estadisticas", eventoId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EstadisticaVentasDTO>>() {
                    });
            return datos == null ? List.of() : datos;
        } catch (Exception e) {
            log.warn("No se pudieron obtener las ventas del evento {}: {}", eventoId, e.getMessage());
            throw new ServicioNoDisponibleException("El servicio de ventas no responde: no se puede armar el reporte");
        }
    }
}
