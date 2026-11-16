package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.model.EstadoDevolucion;
import com.ticketya.ventas.service.DevolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Devoluciones", description = "El cliente solicita la devolucion hasta 72 horas antes del evento y un administrador la resuelve")
@RestController
@RequestMapping("/devoluciones")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    @Operation(summary = "Solicitar la devolucion de una entrada")
    @ApiResponse(responseCode = "201", description = "Solicitud registrada (estado PENDIENTE)")
    @ApiResponse(responseCode = "403", description = "La entrada no es del usuario")
    @ApiResponse(responseCode = "404", description = "No existe la entrada")
    @ApiResponse(responseCode = "409", description = "Faltan menos de 72 horas, la entrada no esta vendida o ya hay una solicitud pendiente")
    @PostMapping
    public ResponseEntity<DevolucionDTO> solicitar(
            @Parameter(description = "Id del cliente (sin autenticacion en la entrega 1)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Valid @RequestBody DevolucionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucionService.solicitar(usuarioId, req));
    }

    @Operation(summary = "Listar solicitudes de devolucion por estado (administrador)")
    @ApiResponse(responseCode = "200", description = "Solicitudes en ese estado (por defecto PENDIENTE)")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @GetMapping
    public ResponseEntity<List<DevolucionDTO>> listar(
            @Parameter(description = "Rol del usuario", example = "ADMIN")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestParam(defaultValue = "PENDIENTE") EstadoDevolucion estado) {
        Seguridad.exigirAdministrador(rol);
        return ResponseEntity.ok(devolucionService.listar(estado));
    }

    @Operation(summary = "Aprobar una devolucion (administrador)",
            description = "Se libera el asiento o lugar de la zona y se solicita el reintegro a la pasarela de pagos.")
    @ApiResponse(responseCode = "200", description = "Devolucion aprobada")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @ApiResponse(responseCode = "404", description = "No existe la devolucion")
    @ApiResponse(responseCode = "409", description = "La devolucion ya fue resuelta o la entrada ya no esta vendida")
    @PutMapping("/{id}/aprobacion")
    public ResponseEntity<DevolucionDTO> aprobar(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario", example = "ADMIN")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        Seguridad.exigirAdministrador(rol);
        return ResponseEntity.ok(devolucionService.aprobar(id));
    }

    @Operation(summary = "Rechazar una devolucion (administrador)")
    @ApiResponse(responseCode = "200", description = "Devolucion rechazada")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @ApiResponse(responseCode = "404", description = "No existe la devolucion")
    @ApiResponse(responseCode = "409", description = "La devolucion ya fue resuelta")
    @PutMapping("/{id}/rechazo")
    public ResponseEntity<DevolucionDTO> rechazar(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario", example = "ADMIN")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        Seguridad.exigirAdministrador(rol);
        return ResponseEntity.ok(devolucionService.rechazar(id));
    }
}
