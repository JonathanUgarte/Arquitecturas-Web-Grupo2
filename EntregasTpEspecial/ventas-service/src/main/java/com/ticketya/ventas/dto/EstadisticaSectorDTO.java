package com.ticketya.ventas.dto;

import java.math.BigDecimal;

// Ventas de un sector de un evento, para el reporte del organizador (lo arma catalogo)
//  vendidas: entradas vigentes (VENDIDA + USADA) | utilizadas: ya ingresaron (USADA)
//  devueltas: devoluciones aprobadas | aReintegrar: por cancelacion del evento
//  recaudacion: lo cobrado por las entradas vigentes | montoReintegrado: devueltas + a reintegrar
public record EstadisticaSectorDTO(Long sectorId, long vendidas, long utilizadas, long devueltas, long aReintegrar,
                                   BigDecimal recaudacion, BigDecimal montoReintegrado) {
}
