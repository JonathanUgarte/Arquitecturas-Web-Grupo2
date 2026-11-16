package com.ticketya.ventas.service;

import com.ticketya.ventas.model.EstadoReserva;
import com.ticketya.ventas.model.Reserva;
import com.ticketya.ventas.model.StockZona;
import com.ticketya.ventas.model.TipoSector;
import com.ticketya.ventas.repository.AsientoOcupadoRepository;
import com.ticketya.ventas.repository.ReservaRepository;
import com.ticketya.ventas.repository.StockZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// Libera los lugares de reservas vencidas o canceladas. Todos los metodos se llaman
// DENTRO de una transaccion abierta por el Service que los usa.
@Component
@RequiredArgsConstructor
public class LiberacionService {

    // Una reserva vencida se libera recien pasados estos segundos: asi nunca se libera
    // una reserva cuyo pago esta justo comenzando.
    public static final long GRACIA_SEGUNDOS = 5;

    private final ReservaRepository reservaRepo;
    private final AsientoOcupadoRepository asientoRepo;
    private final StockZonaRepository stockRepo;

    public void liberarVencidasNumerado(Long eventoId, Long sectorId, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minusSeconds(GRACIA_SEGUNDOS);
        // primero se cambia el estado (condicional) y despues se liberan los asientos de las VENCIDAS
        reservaRepo.marcarVencidas(eventoId, sectorId, EstadoReserva.PENDIENTE, EstadoReserva.VENCIDA, limite);
        asientoRepo.eliminarDeReservasEnEstado(eventoId, sectorId, EstadoReserva.VENCIDA);
    }

    // El stock ya viene bloqueado (FOR UPDATE) por quien llama
    public void liberarVencidasZona(StockZona stock, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minusSeconds(GRACIA_SEGUNDOS);
        List<Reserva> vencidas = reservaRepo.findVencidas(stock.getEventoId(), stock.getSectorId(),
                EstadoReserva.PENDIENTE, limite);
        for (Reserva r : vencidas) {
            stock.setDisponibles(stock.getDisponibles() + r.getCantidad());
            r.setEstado(EstadoReserva.VENCIDA);
        }
    }

    // Devuelve los lugares de UNA reserva (pago rechazado, cancelacion del cliente, reserva vencida)
    public void liberarLugares(Reserva r) {
        if (r.getTipoSector() == TipoSector.NUMERADO) {
            asientoRepo.eliminarDeReserva(r.getId());
        } else {
            StockZona stock = stockRepo.findForUpdate(r.getEventoId(), r.getSectorId())
                    .orElseThrow(() -> new IllegalStateException("Falta el stock de la zona"));
            stock.setDisponibles(stock.getDisponibles() + r.getCantidad());
        }
    }
}
