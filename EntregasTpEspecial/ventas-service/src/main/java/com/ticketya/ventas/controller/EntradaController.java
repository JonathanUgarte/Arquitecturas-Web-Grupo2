package com.ticketya.ventas.controller;

import com.ticketya.ventas.dto.EntradaDTO;
import com.ticketya.ventas.service.EntradaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Entradas", description = "Entradas emitidas, cada una con su codigo QR unico")
@RestController
@RequestMapping("/entradas")
@RequiredArgsConstructor
public class EntradaController {

    private final EntradaService entradaService;

    @Operation(summary = "Listar las entradas del cliente")
    @ApiResponse(responseCode = "200", description = "Entradas del cliente, de la mas reciente a la mas antigua")
    @ApiResponse(responseCode = "403", description = "No es cliente")
    @GetMapping
    public ResponseEntity<List<EntradaDTO>> misEntradas(
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol del usuario (lo completa el gateway a partir del JWT)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        Seguridad.exigirRol(rol, Seguridad.CLIENTE);
        return ResponseEntity.ok(entradaService.listar(usuarioId));
    }

    @Operation(summary = "Obtener una entrada propia")
    @ApiResponse(responseCode = "200", description = "Entrada encontrada")
    @ApiResponse(responseCode = "403", description = "No es cliente o la entrada no es del usuario")
    @ApiResponse(responseCode = "404", description = "No existe la entrada")
    @GetMapping("/{id}")
    public ResponseEntity<EntradaDTO> obtener(
            @PathVariable Long id,
            @Parameter(description = "Id del cliente", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol del usuario (lo completa el gateway a partir del JWT)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        Seguridad.exigirRol(rol, Seguridad.CLIENTE);
        return ResponseEntity.ok(entradaService.obtener(usuarioId, id));
    }
}
