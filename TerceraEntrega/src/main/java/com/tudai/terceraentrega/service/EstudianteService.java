package com.tudai.terceraentrega.service;

import com.tudai.terceraentrega.dto.AltaEstudianteRequest;
import com.tudai.terceraentrega.dto.EstudianteDTO;
import com.tudai.terceraentrega.entities.Estudiante;
import com.tudai.terceraentrega.mapper.EstudianteMapper;
import com.tudai.terceraentrega.repository.EstudianteRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EstudianteService {

    private final EstudianteRepository estudiantes;
    private final EstudianteMapper mapper;

    public EstudianteService(EstudianteRepository estudiantes, EstudianteMapper mapper) {
        this.estudiantes = estudiantes;
        this.mapper = mapper;
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
        estudiante.setGenero(mapper.toGenero(request.getGenero()));
        estudiante.setNumeroDocumento(request.getNumeroDocumento());
        estudiante.setCiudadResidencia(request.getCiudadResidencia());
        estudiante.setNumeroLibretaUniversitaria(request.getNumeroLibretaUniversitaria());

        return mapper.toDTO(estudiantes.save(estudiante));
    }

    // c) Recuperar todos. Pageable define pagina, cantidad y criterio de ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> todos(Pageable pageable) {
        return estudiantes.findAll(pageable)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    // d) Recuperar por libreta universitaria.
    @Transactional(readOnly = true)
    public EstudianteDTO porLibreta(String libreta) {
        return estudiantes.findByNumeroLibretaUniversitaria(libreta)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Estudiante no encontrado"));
    }

    // e) Filtrar por genero; Pageable agrega paginacion y ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> porGenero(String genero, Pageable pageable) {
        return estudiantes.findByGenero(mapper.toGenero(genero), pageable)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    // g) ?1 y ?2 filtran carrera/ciudad; Pageable agrega paginacion y ordenamiento.
    @Transactional(readOnly = true)
    public List<EstudianteDTO> porCarreraYCiudad(String carrera, String ciudad, Pageable pageable) {
        return estudiantes.buscarPorCarreraYCiudad(carrera, ciudad, pageable)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
}
