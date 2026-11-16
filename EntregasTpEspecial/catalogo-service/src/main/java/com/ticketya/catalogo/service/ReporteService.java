package com.ticketya.catalogo.service;

import com.ticketya.catalogo.client.EstadisticaVentasDTO;
import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.ReporteEventoDTO;
import com.ticketya.catalogo.dto.ReporteSectorDTO;
import com.ticketya.catalogo.exception.NotFoundException;
import com.ticketya.catalogo.model.Evento;
import com.ticketya.catalogo.model.PrecioSector;
import com.ticketya.catalogo.model.Sector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// (12) Reportes para organizadores. Composicion de APIs: catalogo aporta los sectores, capacidades y precios
// (sus datos) y ventas aporta las cantidades vendidas, devueltas y la recaudacion (los suyos).
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final EventoRepository eventoRepo;
    private final PrecioSectorRepository precioRepo;
    private final EventoService eventoService;
    private final VentasClient ventasClient;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public ReporteEventoDTO reporte(Long eventoId, Long usuarioId, String rol) {
        eventoService.verificarPermiso(eventoId, usuarioId, rol);
        Evento e = eventoRepo.findById(eventoId)
                .orElseThrow(() -> new NotFoundException("No existe el evento " + eventoId));

        Map<Long, EstadisticaVentasDTO> ventas = ventasClient.estadisticas(eventoId).stream()
                .collect(Collectors.toMap(EstadisticaVentasDTO::sectorId, Function.identity(), (a, b) -> a));

        List<ReporteSectorDTO> sectores = new ArrayList<>();
        List<PrecioSector> precios = new ArrayList<>(precioRepo.findByEventoId(eventoId));
        precios.sort(Comparator.comparing(p -> p.getSector().getId()));
        for (PrecioSector p : precios) {
            Sector s = p.getSector();
            EstadisticaVentasDTO v = ventas.get(s.getId());
            long vendidas = v == null ? 0 : v.vendidas();
            int capacidad = s.capacidadTotal();
            sectores.add(new ReporteSectorDTO(s.getId(), s.getNombre(), s.getTipo(), p.getPrecio(), capacidad,
                    vendidas, Math.max(0, capacidad - vendidas), porcentaje(vendidas, capacidad),
                    v == null ? 0 : v.utilizadas(), v == null ? 0 : v.devueltas(), v == null ? 0 : v.aReintegrar(),
                    monto(v == null ? null : v.recaudacion()), monto(v == null ? null : v.montoReintegrado())));
        }

        int capacidadTotal = sectores.stream().mapToInt(ReporteSectorDTO::capacidad).sum();
        long vendidas = sectores.stream().mapToLong(ReporteSectorDTO::vendidas).sum();
        return new ReporteEventoDTO(e.getId(), e.getNombre(), e.getEstado(), e.getFechaHora(),
                e.getLugar().getNombre(), e.getLugar().getCiudad(), LocalDateTime.now(reloj), sectores,
                capacidadTotal,
                vendidas,
                sectores.stream().mapToLong(ReporteSectorDTO::devueltas).sum(),
                sectores.stream().mapToLong(ReporteSectorDTO::aReintegrar).sum(),
                sectores.stream().mapToLong(ReporteSectorDTO::utilizadas).sum(),
                porcentaje(vendidas, capacidadTotal),
                sectores.stream().map(ReporteSectorDTO::recaudacion).reduce(BigDecimal.ZERO, BigDecimal::add),
                sectores.stream().map(ReporteSectorDTO::montoReintegrado).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    // Ocupacion = vendidas / capacidad, en porcentaje con un decimal
    static BigDecimal porcentaje(long vendidas, int capacidad) {
        if (capacidad <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(vendidas * 100L)
                .divide(BigDecimal.valueOf(capacidad), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal monto(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
