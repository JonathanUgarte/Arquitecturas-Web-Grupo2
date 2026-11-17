package com.ticketya.ventas.controller;

import com.ticketya.ventas.model.EstadoEntrada;
import com.ticketya.ventas.repository.EntradaRepository;
import com.ticketya.ventas.dto.CancelacionEventoDTO;
import com.ticketya.ventas.dto.EstadisticaSectorDTO;
import com.ticketya.ventas.service.CancelacionEventoService;
import com.ticketya.ventas.service.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints que usan SOLO otros servicios (cancelaciones, reportes, compradores a avisar). El gateway no los expone.
@Tag(name = "Interno", description = "Uso exclusivo entre microservicios: no se publica en el gateway")
@RestController
@RequestMapping("/interno")
@RequiredArgsConstructor
public class InternoController {

    private final CancelacionEventoService cancelacionEventoService;
    private final EstadisticasService estadisticasService;
    private final EntradaRepository entradaRepo;

    @Operation(summary = "Avisar la cancelacion de un evento",
            description = "Las entradas vendidas pasan a A_REINTEGRAR y las reservas pendientes se liberan, en una sola transaccion. Es idempotente.")
    @ApiResponse(responseCode = "200", description = "Cantidad de entradas y reservas afectadas")
    @PostMapping("/eventos/{eventoId}/cancelacion")
    public ResponseEntity<CancelacionEventoDTO> cancelarEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(cancelacionEventoService.cancelarEvento(eventoId));
    }

    @Operation(summary = "Clientes con entradas vigentes de un evento",
            description = "Ids de los clientes con al menos una entrada VENDIDA (para avisarles un cambio del evento).")
    @ApiResponse(responseCode = "200", description = "Lista de ids de usuario, sin repetidos")
    @GetMapping("/eventos/{eventoId}/compradores")
    public ResponseEntity<List<Long>> compradores(@PathVariable Long eventoId) {
        return ResponseEntity.ok(entradaRepo.usuariosConEntradasEnEstado(eventoId, EstadoEntrada.VENDIDA));
    }

    @Operation(summary = "Ventas por sector de un evento (para los reportes del organizador)",
            description = "Entradas vigentes, utilizadas, devueltas y a reintegrar, y montos, agrupados por sector.")
    @ApiResponse(responseCode = "200", description = "Una fila por sector que tuvo ventas")
    @GetMapping("/eventos/{eventoId}/estadisticas")
    public ResponseEntity<List<EstadisticaSectorDTO>> estadisticas(@PathVariable Long eventoId) {
        return ResponseEntity.ok(estadisticasService.porSector(eventoId));
    }
}
