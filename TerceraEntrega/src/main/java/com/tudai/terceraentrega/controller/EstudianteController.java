package com.tudai.terceraentrega.controller;

import com.tudai.terceraentrega.dto.AltaEstudianteRequest;
import com.tudai.terceraentrega.dto.EstudianteDTO;
import com.tudai.terceraentrega.service.EstudianteService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {

    private final EstudianteService service;

    public EstudianteController(EstudianteService service) {
        this.service = service;
    }

    // a) POST /api/estudiantes
    @PostMapping
    public ResponseEntity<EstudianteDTO> alta(@RequestBody AltaEstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.alta(request));
    }

    // c) GET /api/estudiantes?page=0&size=10&sort=apellido,asc
    @GetMapping
    public List<EstudianteDTO> todos(Pageable pageable) {
        return service.todos(pageable);
    }

    // d) GET /api/estudiantes/libreta/999999
    @GetMapping("/libreta/{libreta}")
    public EstudianteDTO porLibreta(@PathVariable String libreta) {
        return service.porLibreta(libreta);
    }

    // e) GET /api/estudiantes/genero/MASCULINO?page=0&size=10&sort=apellido,asc
    @GetMapping("/genero/{genero}")
    public List<EstudianteDTO> porGenero(@PathVariable String genero, Pageable pageable) {
        return service.porGenero(genero, pageable);
    }

    // g) GET /api/estudiantes/carrera/TUARI/ciudad/Tandil?page=0&size=10&sort=apellido,asc
    @GetMapping("/carrera/{carrera}/ciudad/{ciudad}")
    public List<EstudianteDTO> porCarreraYCiudad(@PathVariable String carrera,
                                                 @PathVariable String ciudad,
                                                 Pageable pageable) {
        return service.porCarreraYCiudad(carrera, ciudad, pageable);
    }
}
