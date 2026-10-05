package com.tudai.terceraentrega.service;

import com.tudai.terceraentrega.dto.AltaEstudianteRequest;
import com.tudai.terceraentrega.dto.CarreraDTO;
import com.tudai.terceraentrega.dto.EstudianteDTO;
import com.tudai.terceraentrega.dto.MatriculaRequest;
import com.tudai.terceraentrega.dto.ReporteCarreraDTO;
import com.tudai.terceraentrega.entities.Carrera;
import com.tudai.terceraentrega.entities.Estudiante;
import com.tudai.terceraentrega.entities.Genero;
import com.tudai.terceraentrega.entities.Inscripcion;
import com.tudai.terceraentrega.entities.InscripcionId;
import com.tudai.terceraentrega.repository.CarreraRepository;
import com.tudai.terceraentrega.repository.EstudianteRepository;
import com.tudai.terceraentrega.repository.InscripcionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@Service
public class IntegradorService {

    private final EstudianteRepository estudiantes;
    private final CarreraRepository carreras;
    private final InscripcionRepository inscripciones;

    public IntegradorService(EstudianteRepository estudiantes,
                             CarreraRepository carreras,
                             InscripcionRepository inscripciones) {
        this.estudiantes = estudiantes;
        this.carreras = carreras;
        this.inscripciones = inscripciones;
    }

    private EstudianteDTO convertirADTO(Estudiante estudiante) {
        return new EstudianteDTO(
                estudiante.getNombres(),
                estudiante.getApellido(),
                estudiante.getEdad(),
                estudiante.getGenero().name()
        );
    }

    private Genero convertirGenero(String genero) {
        String valor = genero.trim().toUpperCase();

        return switch (valor) {
            case "MALE", "MASCULINO" -> Genero.MASCULINO;
            case "FEMALE", "FEMENINO" -> Genero.FEMENINO;
            default -> Genero.OTRO;
        };
    }

    // a) Dar de alta un estudiante.
    @Transactional
    public EstudianteDTO alta(AltaEstudianteRequest request) {
        if (estudiantes.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese documento");
        }

        if (estudiantes.existsByNumeroLibretaUniversitaria(request.getNumeroLibretaUniversitaria())) {
            throw new IllegalArgumentException("Ya existe un estudiante con esa libreta");
        }

        Estudiante estudiante = new Estudiante();
        estudiante.setIdEstudiante(Integer.parseInt(request.getNumeroDocumento()));
        estudiante.setNombres(request.getNombres());
        estudiante.setApellido(request.getApellido());
        estudiante.setEdad(request.getEdad());
        estudiante.setGenero(convertirGenero(request.getGenero()));
        estudiante.setNumeroDocumento(request.getNumeroDocumento());
        estudiante.setCiudadResidencia(request.getCiudadResidencia());
        estudiante.setNumeroLibretaUniversitaria(request.getNumeroLibretaUniversitaria());

        return convertirADTO(estudiantes.save(estudiante));
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

        InscripcionId id = new InscripcionId(
                estudiante.getIdEstudiante(),
                carrera.getIdCarrera()
        );

        if (inscripciones.existsById(id)) {
            throw new IllegalArgumentException("El estudiante ya está matriculado en esa carrera");
        }

        int anio = request.getAnioInscripcion() == null
                ? Year.now().getValue()
                : request.getAnioInscripcion();

        inscripciones.save(new Inscripcion(estudiante, carrera, anio, null));
    }

    // c) Recuperar todos. Pageable define pagina, cantidad y criterio de ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> todos(Pageable pageable) {
        return estudiantes.findAll(pageable)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // d) Recuperar por libreta universitaria.
    @Transactional(readOnly = true)
    public EstudianteDTO porLibreta(String libreta) {
        Estudiante estudiante = estudiantes
                .findByNumeroLibretaUniversitaria(libreta)
                .orElseThrow(() -> new NoSuchElementException("Estudiante no encontrado"));

        return convertirADTO(estudiante);
    }

    // e) Filtrar por genero; Pageable agrega paginacion y ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> porGenero(String genero, Pageable pageable) {
        return estudiantes.findByGenero(convertirGenero(genero), pageable)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // f) El orden requerido es por cantidad de inscriptos y lo resuelve la consulta JPQL.
    @Transactional(readOnly = true)
    public List<CarreraDTO> carrerasConInscriptos() {
        return carreras.carrerasConInscriptos();
    }

    // g) ?1 y ?2 filtran carrera/ciudad; Pageable agrega paginacion y ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> porCarreraYCiudad(String carrera,
                                                  String ciudad,
                                                  Pageable pageable) {
        return estudiantes.buscarPorCarreraYCiudad(carrera, ciudad, pageable)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // h) TreeMap mantiene carreras alfabeticamente y anios cronologicamente.
    @Transactional(readOnly = true)
    public List<ReporteCarreraDTO> reporte() {
        List<Inscripcion> lista = inscripciones.findAllByOrderByCarreraNombreAscAnioInscripcionAsc();
        Map<String, TreeMap<Integer, long[]>> datos = new TreeMap<>();

        for (Inscripcion inscripcion : lista) {
            String carrera = inscripcion.getCarrera().getNombre();
            datos.computeIfAbsent(carrera, key -> new TreeMap<>());

            datos.get(carrera)
                    .computeIfAbsent(inscripcion.getAnioInscripcion(), key -> new long[2])[0]++;

            if (inscripcion.getAnioEgreso() != null) {
                datos.get(carrera)
                        .computeIfAbsent(inscripcion.getAnioEgreso(), key -> new long[2])[1]++;
            }
        }

        List<ReporteCarreraDTO> reporte = new ArrayList<>();
        datos.forEach((carrera, anios) ->
                anios.forEach((anio, cantidades) ->
                        reporte.add(new ReporteCarreraDTO(
                                carrera,
                                anio,
                                cantidades[0],
                                cantidades[1]
                        ))
                )
        );

        return reporte;
    }
}
