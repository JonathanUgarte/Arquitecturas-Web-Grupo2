package com.ticketya.ventas.client;

import com.ticketya.ventas.config.VentasProperties;
import com.ticketya.ventas.dto.SectorVentaDTO;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.exception.ServicioNoDisponibleException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
                .build();
    }

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
