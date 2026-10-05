package com.tudai.terceraentrega.controller;

import com.tudai.terceraentrega.dto.AltaEstudianteRequest;
import com.tudai.terceraentrega.dto.CarreraDTO;
import com.tudai.terceraentrega.dto.EstudianteDTO;
import com.tudai.terceraentrega.dto.MatriculaRequest;
import com.tudai.terceraentrega.dto.ReporteCarreraDTO;
import com.tudai.terceraentrega.service.IntegradorService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IntegradorController {

    private final IntegradorService service;

    public IntegradorController(IntegradorService service) {
        this.service = service;
    }

    // a) Dar de alta un estudiante.
    @PostMapping("/estudiantes")
    public ResponseEntity<EstudianteDTO> alta(@RequestBody AltaEstudianteRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.alta(request));
    }

    // b) Matricular un estudiante en una carrera.
    @PostMapping("/inscripciones")
    public ResponseEntity<Map<String, String>> matricular(@RequestBody MatriculaRequest request) {
        service.matricular(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Estudiante matriculado correctamente"));
    }

    // c) Ejemplo: GET /api/estudiantes?page=0&size=10&sort=apellido,asc
    @GetMapping("/estudiantes")
    public List<EstudianteDTO> todos(Pageable pageable) {
        return service.todos(pageable);
    }

    // d) Ejemplo: GET /api/estudiantes/libreta/999999
    @GetMapping("/estudiantes/libreta/{libreta}")
    public EstudianteDTO porLibreta(@PathVariable String libreta) {
        return service.porLibreta(libreta);
    }

    // e) Ejemplo: GET /api/estudiantes/genero/MASCULINO?page=0&size=10&sort=apellido,asc
    @GetMapping("/estudiantes/genero/{genero}")
    public List<EstudianteDTO> porGenero(@PathVariable String genero, Pageable pageable) {
        return service.porGenero(genero, pageable);
    }

    // f) El orden por cantidad de inscriptos esta definido por la consulta.
    @GetMapping("/carreras/con-inscriptos")
    public List<CarreraDTO> carrerasConInscriptos() {
        return service.carrerasConInscriptos();
    }

    // g) Ejemplo: GET /api/estudiantes/carrera/TUARI/ciudad/Tandil?page=0&size=10&sort=apellido,asc
    @GetMapping("/estudiantes/carrera/{carrera}/ciudad/{ciudad}")
    public List<EstudianteDTO> porCarreraYCiudad(@PathVariable String carrera,
                                                  @PathVariable String ciudad,
                                                  Pageable pageable) {
        return service.porCarreraYCiudad(carrera, ciudad, pageable);
    }

    // h) Reporte ordenado por carrera alfabeticamente y por anio cronologicamente.
    @GetMapping("/carreras/reporte")
    public List<ReporteCarreraDTO> reporte() {
        return service.reporte();
    }
}
