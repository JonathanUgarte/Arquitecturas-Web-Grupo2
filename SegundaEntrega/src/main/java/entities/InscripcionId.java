package entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de la inscripcion (estudiante, carrera).
 */
@Embeddable
public class InscripcionId implements Serializable {

    @Column(name = "id_estudiante")
    private int idEstudiante;

    @Column(name = "id_carrera")
    private int idCarrera;

    public InscripcionId() {
    }

    public InscripcionId(int idEstudiante, int idCarrera) {
        this.idEstudiante = idEstudiante;
        this.idCarrera = idCarrera;
    }

    public int getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(int idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public int getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(int idCarrera) {
        this.idCarrera = idCarrera;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InscripcionId that = (InscripcionId) o;
        return idEstudiante == that.idEstudiante && idCarrera == that.idCarrera;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEstudiante, idCarrera);
    }
}