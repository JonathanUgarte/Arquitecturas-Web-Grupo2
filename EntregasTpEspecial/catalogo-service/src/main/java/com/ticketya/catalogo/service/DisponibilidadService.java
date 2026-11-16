package com.ticketya.catalogo.service;

import com.ticketya.catalogo.client.EstadisticaVentasDTO;
import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.trazabilidad.Bitacora;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Mantiene el estado AGOTADO de los eventos. Se dispara al consumir de RabbitMQ una compra confirmada o una
// devolucion aprobada: compara las entradas vendidas (las informa ventas) con la capacidad en venta (la conoce
// catalogo). Es consistencia eventual: el estado cambia unos milisegundos despues de la compra.
// Ventas no depende de este estado para impedir la sobreventa: esa garantia esta en su propia base.
@Service
@RequiredArgsConstructor
@Slf4j
public class DisponibilidadService {

    private final EventoService eventoService;
    private final VentasClient ventasClient;

    public void reevaluar(Long eventoId) {
        EventoService.Disponibilidad d = eventoService.disponibilidad(eventoId);
        if (d.estado() != EstadoEvento.PUBLICADO && d.estado() != EstadoEvento.AGOTADO) {
            return;   // cancelado, suspendido, finalizado o borrador: no corresponde
        }
        long vendidas = ventasClient.estadisticas(eventoId).stream()
                .mapToLong(EstadisticaVentasDTO::vendidas)
                .sum();
        boolean agotado = d.capacidad() > 0 && vendidas >= d.capacidad();
        if (eventoService.actualizarDisponibilidad(eventoId, agotado)) {
            Bitacora.registrar(agotado ? "EVENTO_AGOTADO" : "EVENTO_CON_LUGARES",
                    "evento=" + eventoId + " vendidas=" + vendidas + " capacidad=" + d.capacidad());
        }
    }
}
