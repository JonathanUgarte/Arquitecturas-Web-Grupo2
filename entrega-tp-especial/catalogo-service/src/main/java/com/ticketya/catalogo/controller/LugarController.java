package com.ticketya.catalogo.controller;

import com.ticketya.catalogo.dto.LugarDTO;
import com.ticketya.catalogo.dto.LugarRequest;
import com.ticketya.catalogo.service.LugarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Lugares", description = "Estadios, teatros y salas, con sus sectores numerados o no numerados")
@RestController
@RequestMapping("/lugares")
@RequiredArgsConstructor
public class LugarController {

    private final LugarService lugarService;

    @Operation(summary = "Crear un lugar con sus sectores",
            description = "Un sector NUMERADO necesita filas y asientosPorFila; uno NO_NUMERADO, una capacidad.")
    @ApiResponse(responseCode = "201", description = "Lugar creado")
    @ApiResponse(responseCode = "400", description = "Faltan datos o un sector es invalido")
    @PostMapping
    public ResponseEntity<LugarDTO> crear(@Valid @RequestBody LugarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lugarService.crear(req));
    }

    @Operation(summary = "Obtener un lugar por id")
    @ApiResponse(responseCode = "200", description = "Lugar encontrado")
    @ApiResponse(responseCode = "404", description = "No existe el lugar")
    @GetMapping("/{id}")
    public ResponseEntity<LugarDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(lugarService.obtener(id));
    }

    @Operation(summary = "Listar todos los lugares")
    @ApiResponse(responseCode = "200", description = "Lista de lugares")
    @GetMapping
    public ResponseEntity<List<LugarDTO>> listar() {
        return ResponseEntity.ok(lugarService.listar());
    }
}
