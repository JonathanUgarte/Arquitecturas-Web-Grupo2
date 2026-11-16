package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para el inciso 2.f: carrera y cantidad de estudiantes inscriptos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarreraDTO {
    private String nombre;
    private Long cantidadInscriptos;
}
