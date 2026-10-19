package com.tudai.terceraentrega.service;

import com.tudai.terceraentrega.dto.CarreraDTO;
import com.tudai.terceraentrega.dto.ReporteCarreraDTO;
import com.tudai.terceraentrega.repository.CarreraRepository;
import com.tudai.terceraentrega.repository.ConteoAnualProjection;
import com.tudai.terceraentrega.repository.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CarreraService {

    private final CarreraRepository carreras;
    private final InscripcionRepository inscripciones;

    public CarreraService(CarreraRepository carreras, InscripcionRepository inscripciones) {
        this.carreras = carreras;
        this.inscripciones = inscripciones;
    }

    // f) El orden requerido es por cantidad de inscriptos y lo resuelve la consulta JPQL.
    @Transactional(readOnly = true)
    public List<CarreraDTO> carrerasConInscriptos() {
        return carreras.carrerasConInscriptos();
    }

    // h) Los conteos se calculan en la base con GROUP BY (JPQL); aca solo se combinan.
    // TreeMap mantiene carreras alfabeticamente y anios cronologicamente.
    @Transactional(readOnly = true)
    public List<ReporteCarreraDTO> reporte() {
        Map<String, TreeMap<Integer, long[]>> datos = new TreeMap<>();

        for (ConteoAnualProjection p : inscripciones.contarInscriptosPorCarreraYAnio()) {
            datos.computeIfAbsent(p.getCarrera(), k -> new TreeMap<>())
                    .computeIfAbsent(p.getAnio(), k -> new long[2])[0] = p.getCantidad();
        }

        for (ConteoAnualProjection p : inscripciones.contarEgresadosPorCarreraYAnio()) {
            datos.computeIfAbsent(p.getCarrera(), k -> new TreeMap<>())
                    .computeIfAbsent(p.getAnio(), k -> new long[2])[1] = p.getCantidad();
        }

        List<ReporteCarreraDTO> reporte = new ArrayList<>();
        datos.forEach((carrera, anios) ->
                anios.forEach((anio, c) ->
                        reporte.add(new ReporteCarreraDTO(carrera, anio, c[0], c[1]))
                )
        );
        return reporte;
    }
}
