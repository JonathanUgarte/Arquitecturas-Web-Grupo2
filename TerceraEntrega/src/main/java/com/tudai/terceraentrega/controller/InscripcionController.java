package com.tudai.terceraentrega.controller;

import com.tudai.terceraentrega.dto.MatriculaRequest;
import com.tudai.terceraentrega.service.InscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService service;

    public InscripcionController(InscripcionService service) {
        this.service = service;
    }

    // b) POST /api/inscripciones
    @PostMapping
    public ResponseEntity<Map<String, String>> matricular(@RequestBody MatriculaRequest request) {
        service.matricular(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Estudiante matriculado correctamente"));
    }
}
