package com.tudai.terceraentrega.service;

import com.tudai.terceraentrega.dto.MatriculaRequest;
import com.tudai.terceraentrega.entities.Carrera;
import com.tudai.terceraentrega.entities.Estudiante;
import com.tudai.terceraentrega.entities.Inscripcion;
import com.tudai.terceraentrega.entities.InscripcionId;
import com.tudai.terceraentrega.repository.CarreraRepository;
import com.tudai.terceraentrega.repository.EstudianteRepository;
import com.tudai.terceraentrega.repository.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.NoSuchElementException;

@Service
public class InscripcionService {

    private final EstudianteRepository estudiantes;
    private final CarreraRepository carreras;
    private final InscripcionRepository inscripciones;

    public InscripcionService(EstudianteRepository estudiantes,
                              CarreraRepository carreras,
                              InscripcionRepository inscripciones) {
        this.estudiantes = estudiantes;
        this.carreras = carreras;
        this.inscripciones = inscripciones;
    }

    // b) Matricular un estudiante en una carrera.
    @Transactional
    public void matricular(MatriculaRequest request) {
        Estudiante estudiante = estudiantes
                .findByNumeroLibretaUniversitaria(request.getLibreta())
                .orElseThrow(() -> new NoSuchElementException("Estudiante no encontrado"));

        Carrera carrera = carreras
                .findById(request.getIdCarrera())
                .orElseThrow(() -> new NoSuchElementException("Carrera no encontrada"));

        InscripcionId id = new InscripcionId(estudiante.getIdEstudiante(), carrera.getIdCarrera());

        if (inscripciones.existsById(id)) {
            throw new IllegalArgumentException("El estudiante ya está matriculado en esa carrera");
        }

        int anio = request.getAnioInscripcion() == null
                ? Year.now().getValue()
                : request.getAnioInscripcion();

        inscripciones.save(new Inscripcion(estudiante, carrera, anio, null));
    }
}
