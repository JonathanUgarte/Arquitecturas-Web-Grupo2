package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Lo usa SOLO notificaciones-service. El gateway no publica /interno/**.
@Tag(name = "Interno", description = "Uso exclusivo entre microservicios: no se publica en el gateway")
@RestController
@RequestMapping("/interno")
@RequiredArgsConstructor
public class InternoController {

    private final PerfilService perfilService;

    @Operation(summary = "Clientes de una ciudad que tienen como favorito a alguno de estos artistas")
    @GetMapping("/interesados")
    public ResponseEntity<List<Long>> interesados(@RequestParam String ciudad,
                                                  @RequestParam List<String> artistas) {
        return ResponseEntity.ok(perfilService.interesados(ciudad, artistas));
    }
}
