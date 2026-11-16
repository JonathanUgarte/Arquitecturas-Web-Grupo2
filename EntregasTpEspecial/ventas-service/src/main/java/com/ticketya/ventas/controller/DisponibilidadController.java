package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.DisponibilidadDTO;
import com.ticketya.ventas.service.DisponibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Disponibilidad", description = "Lugares libres de un sector. Cambia con cada compra, por eso nunca se cachea")
@RestController
@RequestMapping("/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @Operation(summary = "Consultar la disponibilidad de un sector de un evento",
            description = "Sector NUMERADO: lista los asientos ocupados. Sector NO_NUMERADO: informa el cupo disponible.")
    @ApiResponse(responseCode = "200", description = "Disponibilidad del sector")
    @ApiResponse(responseCode = "404", description = "No existe el evento o el sector")
    @ApiResponse(responseCode = "503", description = "El servicio de catalogo no responde")
    @GetMapping("/eventos/{eventoId}/sectores/{sectorId}")
    public ResponseEntity<DisponibilidadDTO> consultar(@PathVariable Long eventoId,
                                                       @PathVariable Long sectorId) {
        return ResponseEntity.ok(disponibilidadService.consultar(eventoId, sectorId));
    }
}
