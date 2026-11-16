package com.ticketya.ventas.service;

import com.ticketya.ventas.dto.AsientoDTO;
import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.dto.ReservaDTO;
import com.ticketya.ventas.model.Devolucion;
import com.ticketya.ventas.model.Entrada;
import com.ticketya.ventas.model.EstadoReserva;
import com.ticketya.ventas.model.Reserva;

import java.time.LocalDateTime;
import java.util.List;

// Conversion de entidades a DTOs (el Service nunca devuelve entidades)
public final class Mapeos {

    private Mapeos() {
    }

    public static ReservaDTO toDTO(Reserva r, List<AsientoDTO> asientos, LocalDateTime ahora) {
        // una reserva PENDIENTE que ya paso su vencimiento se informa como VENCIDA
        EstadoReserva estado = r.getEstado();
        if (estado == EstadoReserva.PENDIENTE && r.getVenceEn().isBefore(ahora)) {
            estado = EstadoReserva.VENCIDA;
        }
        return new ReservaDTO(r.getId(), r.getUsuarioId(), r.getEventoId(), r.getSectorId(), estado,
                r.getCantidad(), asientos, r.getPrecioUnitario(), r.getTotal(), r.getVenceEn());
    }

    public static EntradaDTO toDTO(Entrada e) {
        return new EntradaDTO(e.getId(), e.getCodigoQr(), e.getEventoId(), e.getSectorId(), e.getFila(),
                e.getNumero(), e.getPrecio(), e.getEstado(), e.getFechaEvento());
    }

    public static DevolucionDTO toDTO(Devolucion d) {
        return new DevolucionDTO(d.getId(), d.getEntrada().getId(), d.getEstado(), d.getMotivo(), d.getMonto(),
                d.getSolicitadaEn(), d.getResueltaEn(), d.isReintegroSolicitado());
    }
}
