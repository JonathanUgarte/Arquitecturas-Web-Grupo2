package com.ticketya.ventas.service;

import com.ticketya.ventas.dto.EstadisticaSectorDTO;
import com.ticketya.ventas.model.EstadoEntrada;
import com.ticketya.ventas.repository.EntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

// (12) Datos de ventas por sector para los reportes de los organizadores (una consulta agrupada en la base)
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private final EntradaRepository entradaRepo;

    private static final class Acumulador {
        long vendidas, utilizadas, devueltas, aReintegrar;
        BigDecimal recaudacion = BigDecimal.ZERO;
        BigDecimal reintegrado = BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<EstadisticaSectorDTO> porSector(Long eventoId) {
        Map<Long, Acumulador> porSector = new TreeMap<>();
        for (Object[] fila : entradaRepo.resumenPorSectorYEstado(eventoId)) {
            Long sectorId = (Long) fila[0];
            EstadoEntrada estado = (EstadoEntrada) fila[1];
            long cantidad = ((Number) fila[2]).longValue();
            BigDecimal monto = fila[3] == null ? BigDecimal.ZERO : (BigDecimal) fila[3];
            Acumulador a = porSector.computeIfAbsent(sectorId, id -> new Acumulador());
            switch (estado) {
                case VENDIDA -> {
                    a.vendidas += cantidad;
                    a.recaudacion = a.recaudacion.add(monto);
                }
                case USADA -> {
                    a.vendidas += cantidad;
                    a.utilizadas += cantidad;
                    a.recaudacion = a.recaudacion.add(monto);
                }
                case DEVUELTA -> {
                    a.devueltas += cantidad;
                    a.reintegrado = a.reintegrado.add(monto);
                }
                case A_REINTEGRAR -> {
                    a.aReintegrar += cantidad;
                    a.reintegrado = a.reintegrado.add(monto);
                }
            }
        }
        List<EstadisticaSectorDTO> resultado = new ArrayList<>();
        porSector.forEach((sectorId, a) -> resultado.add(new EstadisticaSectorDTO(sectorId, a.vendidas, a.utilizadas,
                a.devueltas, a.aReintegrar, a.recaudacion, a.reintegrado)));
        return resultado;
    }
}
