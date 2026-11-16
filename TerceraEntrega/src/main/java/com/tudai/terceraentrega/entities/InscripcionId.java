package com.tudai.terceraentrega.entities;
import jakarta.persistence.*; import java.io.Serializable; import java.util.Objects;
@Embeddable
public class InscripcionId implements Serializable {
 @Column(name="id_estudiante") private int idEstudiante; @Column(name="id_carrera") private int idCarrera;
 public InscripcionId(){} public InscripcionId(int e,int c){idEstudiante=e;idCarrera=c;}
 public int getIdEstudiante(){return idEstudiante;} public void setIdEstudiante(int v){idEstudiante=v;}
 public int getIdCarrera(){return idCarrera;} public void setIdCarrera(int v){idCarrera=v;}
 @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof InscripcionId x))return false;return idEstudiante==x.idEstudiante&&idCarrera==x.idCarrera;}
 @Override public int hashCode(){return Objects.hash(idEstudiante,idCarrera);}
}
