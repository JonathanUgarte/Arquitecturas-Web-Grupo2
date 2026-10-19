package com.tudai.terceraentrega.controller;

import com.tudai.terceraentrega.dto.CarreraDTO;
import com.tudai.terceraentrega.dto.ReporteCarreraDTO;
import com.tudai.terceraentrega.service.CarreraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/carreras")
public class CarreraController {

    private final CarreraService service;

    public CarreraController(CarreraService service) {
        this.service = service;
    }

    // f) GET /api/carreras/con-inscriptos (ordenadas por cantidad de inscriptos)
    @GetMapping("/con-inscriptos")
    public List<CarreraDTO> carrerasConInscriptos() {
        return service.carrerasConInscriptos();
    }

    // h) GET /api/carreras/reporte
    @GetMapping("/reporte")
    public List<ReporteCarreraDTO> reporte() {
        return service.reporte();
    }
}
