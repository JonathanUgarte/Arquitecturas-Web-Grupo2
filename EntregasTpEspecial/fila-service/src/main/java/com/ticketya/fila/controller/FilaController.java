package com.ticketya.fila.controller;

import com.ticketya.fila.dto.EstadoFilaDTO;
import com.ticketya.fila.dto.FilaDTO;
import com.ticketya.fila.exception.ForbiddenException;
import com.ticketya.fila.service.FilaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "Fila virtual", description = "Ingreso a la fila, posicion y turno para comprar en eventos de alta demanda")
@RestController
@RequestMapping("/filas")
@RequiredArgsConstructor
public class FilaController {

    private final FilaService filaService;

    @Operation(summary = "Entrar a la fila de un evento (cliente)",
            description = "Solo para eventos con fila virtual que esten a la venta. Entrar dos veces no cambia el lugar.")
    @ApiResponse(responseCode = "200", description = "Posicion en la fila (o el turno, si ya esta habilitado)")
    @ApiResponse(responseCode = "403", description = "No es cliente")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @ApiResponse(responseCode = "409", description = "El evento no usa fila virtual o no esta a la venta")
    @PostMapping("/{eventoId}/ingreso")
    public ResponseEntity<FilaDTO> ingresar(
            @PathVariable Long eventoId,
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirRol(rol, "CLIENTE");
        return ResponseEntity.ok(filaService.ingresar(eventoId, usuarioId));
    }

    @Operation(summary = "Consultar mi posicion o mi turno (cliente)",
            description = "ESPERANDO: posicion y tiempo estimado. HABILITADO: el turno (token) para enviar en el header "
                    + "X-Turno-Fila al reservar. FUERA_DE_FILA: no esta en la fila o se le vencio el turno.")
    @ApiResponse(responseCode = "200", description = "Situacion del cliente en la fila")
    @GetMapping("/{eventoId}/turno")
    public ResponseEntity<FilaDTO> consultar(
            @PathVariable Long eventoId,
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirRol(rol, "CLIENTE");
        return ResponseEntity.ok(filaService.consultar(eventoId, usuarioId));
    }

    @Operation(summary = "Salir de la fila (cliente)")
    @ApiResponse(responseCode = "204", description = "Salio de la fila (y perdio el turno, si lo tenia)")
    @DeleteMapping("/{eventoId}/ingreso")
    public ResponseEntity<Void> salir(
            @PathVariable Long eventoId,
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirRol(rol, "CLIENTE");
        filaService.salir(eventoId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Estado de la fila (organizador o administrador)")
    @ApiResponse(responseCode = "200", description = "Cuantos esperan, cuantos se habilitaron y la configuracion de las tandas")
    @GetMapping("/{eventoId}/estado")
    public ResponseEntity<EstadoFilaDTO> estado(
            @PathVariable Long eventoId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "ORGANIZADOR")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirRol(rol, "ORGANIZADOR", "ADMIN");
        return ResponseEntity.ok(filaService.estado(eventoId));
    }

    // Segunda barrera de autorizacion (el gateway valida el JWT y reenvia el rol en X-Rol)
    private static void exigirRol(String rol, String... permitidos) {
        if (rol == null || !Set.of(permitidos).contains(rol)) {
            throw new ForbiddenException("Esta accion requiere el rol " + String.join(" o ", permitidos));
        }
    }
}
