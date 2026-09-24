package entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Year;
import java.util.Objects;

/**
 * Clase de asociacion que resuelve la relacion muchos-a-muchos entre
 * {@link Estudiante} y {@link Carrera}, portando los atributos propios de
 * la inscripcion (anio de ingreso, anio de egreso y antiguedad derivada).
 */
@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    @EmbeddedId
    private InscripcionId id;

    @ManyToOne
    @MapsId("idEstudiante")
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "anio_inscripcion", nullable = false)
    private int anioInscripcion;

    @Column(name = "anio_egreso")
    private Integer anioEgreso;

    public Inscripcion() {
    }

    public Inscripcion(Estudiante estudiante, Carrera carrera, int anioInscripcion, Integer anioEgreso) {
        this.estudiante = estudiante;
        this.carrera = carrera;
        this.anioInscripcion = anioInscripcion;
        this.anioEgreso = anioEgreso;
        this.id = new InscripcionId(estudiante.getIdEstudiante(), carrera.getIdCarrera());
    }

    /**
     * Antiguedad en anios dentro de esta carrera (derivada del anio de ingreso).
     */
    public int getAntiguedad() {
        return Year.now().getValue() - anioInscripcion;
    }

    public boolean isGraduado() {
        return anioEgreso != null;
    }

    public InscripcionId getId() {
        return id;
    }

    public void setId(InscripcionId id) {
        this.id = id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }

    public int getAnioInscripcion() {
        return anioInscripcion;
    }

    public void setAnioInscripcion(int anioInscripcion) {
        this.anioInscripcion = anioInscripcion;
    }

    public Integer getAnioEgreso() {
        return anioEgreso;
    }

    public void setAnioEgreso(Integer anioEgreso) {
        this.anioEgreso = anioEgreso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Inscripcion that = (Inscripcion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}