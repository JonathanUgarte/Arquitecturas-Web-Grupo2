package com.ticketya.catalogo.controller;

import com.ticketya.catalogo.dto.SectorVentaDTO;
import com.ticketya.catalogo.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints que usa SOLO el servicio de ventas. El gateway no los expone hacia afuera.
@Tag(name = "Interno", description = "Uso exclusivo entre microservicios: no se publica en el gateway")
@RestController
@RequestMapping("/interno")
@RequiredArgsConstructor
public class InternoController {

    private final EventoService eventoService;

    @Operation(summary = "Datos de un sector de un evento para vender (estado del evento, fecha, capacidad y precio)")
    @ApiResponse(responseCode = "200", description = "Datos del sector")
    @ApiResponse(responseCode = "404", description = "El sector no tiene precio en ese evento")
    @GetMapping("/eventos/{eventoId}/sectores/{sectorId}")
    public ResponseEntity<SectorVentaDTO> sectorParaVenta(@PathVariable Long eventoId,
                                                          @PathVariable Long sectorId) {
        return ResponseEntity.ok(eventoService.obtenerSectorVenta(eventoId, sectorId));
    }
}
