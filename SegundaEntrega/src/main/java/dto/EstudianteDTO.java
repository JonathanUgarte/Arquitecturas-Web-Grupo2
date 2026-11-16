package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO usado para devolver datos de estudiantes sin exponer la entidad JPA.
 * Se mantiene simple, siguiendo la estructura iniciada en clase.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteDTO {
    private String nombres;
    private String apellido;
    private int edad;
    private String genero;
}
