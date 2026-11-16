package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.CancelacionEventoDTO;
import com.ticketya.ventas.service.CancelacionEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints que usa SOLO el servicio de catalogo. El gateway no los expone hacia afuera.
@Tag(name = "Interno", description = "Uso exclusivo entre microservicios: no se publica en el gateway")
@RestController
@RequestMapping("/interno")
@RequiredArgsConstructor
public class InternoController {

    private final CancelacionEventoService cancelacionEventoService;

    @Operation(summary = "Avisar la cancelacion de un evento",
            description = "Las entradas vendidas pasan a A_REINTEGRAR y las reservas pendientes se liberan, en una sola transaccion. Es idempotente.")
    @ApiResponse(responseCode = "200", description = "Cantidad de entradas y reservas afectadas")
    @PostMapping("/eventos/{eventoId}/cancelacion")
    public ResponseEntity<CancelacionEventoDTO> cancelarEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(cancelacionEventoService.cancelarEvento(eventoId));
    }
}
