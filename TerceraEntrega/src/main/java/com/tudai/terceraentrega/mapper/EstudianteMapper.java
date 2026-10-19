package com.tudai.terceraentrega.mapper;

import com.tudai.terceraentrega.dto.EstudianteDTO;
import com.tudai.terceraentrega.entities.Estudiante;
import com.tudai.terceraentrega.entities.Genero;
import org.springframework.stereotype.Component;

@Component
public class EstudianteMapper {

    public EstudianteDTO toDTO(Estudiante estudiante) {
        return new EstudianteDTO(
                estudiante.getNombres(),
                estudiante.getApellido(),
                estudiante.getEdad(),
                estudiante.getGenero().name()
        );
    }

    public Genero toGenero(String genero) {
        String valor = genero.trim().toUpperCase();

        return switch (valor) {
            case "MALE", "MASCULINO" -> Genero.MASCULINO;
            case "FEMALE", "FEMENINO" -> Genero.FEMENINO;
            default -> Genero.OTRO;
        };
    }
}
