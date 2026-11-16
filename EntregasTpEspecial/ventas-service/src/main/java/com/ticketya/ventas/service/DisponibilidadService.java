package com.ticketya.ventas.service;

import com.ticketya.ventas.client.CatalogoClient;
import com.ticketya.ventas.dto.AsientoDTO;
import com.ticketya.ventas.dto.DisponibilidadDTO;
import com.ticketya.ventas.dto.SectorVentaDTO;
import com.ticketya.ventas.model.EstadoReserva;
import com.ticketya.ventas.model.StockZona;
import com.ticketya.ventas.model.TipoSector;
import com.ticketya.ventas.repository.AsientoOcupadoRepository;
import com.ticketya.ventas.repository.ReservaRepository;
import com.ticketya.ventas.repository.StockZonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// Disponibilidad de un sector. Es un dato que cambia en cada compra: NUNCA se cachea.
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final CatalogoClient catalogoClient;
    private final AsientoOcupadoRepository asientoRepo;
    private final StockZonaRepository stockRepo;
    private final ReservaRepository reservaRepo;
    private final Clock clock;

    public DisponibilidadDTO consultar(Long eventoId, Long sectorId) {
        SectorVentaDTO sector = catalogoClient.obtenerSector(eventoId, sectorId);
        LocalDateTime limite = LocalDateTime.now(clock).minusSeconds(LiberacionService.GRACIA_SEGUNDOS);

        if (sector.tipo() == TipoSector.NUMERADO) {
            List<AsientoDTO> ocupados = asientoRepo.ocupados(eventoId, sectorId, EstadoReserva.PENDIENTE, limite)
                    .stream()
                    .map(a -> new AsientoDTO(a.getFila(), a.getNumero()))
                    .toList();
            return new DisponibilidadDTO(eventoId, sectorId, sector.tipo(), sector.capacidadTotal(),
                    sector.capacidadTotal() - ocupados.size(), ocupados);
        }

        int base = stockRepo.findByEventoIdAndSectorId(eventoId, sectorId)
                .map(StockZona::getDisponibles)
                .orElse(sector.capacidadTotal());
        long vencidas = reservaRepo.cantidadVencida(eventoId, sectorId, EstadoReserva.PENDIENTE, limite);
        int disponibles = (int) Math.min(sector.capacidadTotal(), base + vencidas);
        return new DisponibilidadDTO(eventoId, sectorId, sector.tipo(), sector.capacidadTotal(), disponibles, List.of());
    }
}
