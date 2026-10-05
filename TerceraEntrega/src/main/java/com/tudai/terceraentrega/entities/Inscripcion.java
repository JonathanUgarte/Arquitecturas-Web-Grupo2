package com.tudai.terceraentrega.entities;
import jakarta.persistence.*; import java.time.Year;
@Entity @Table(name="inscripcion")
public class Inscripcion {
 @EmbeddedId private InscripcionId id;
 @ManyToOne @MapsId("idEstudiante") @JoinColumn(name="id_estudiante") private Estudiante estudiante;
 @ManyToOne @MapsId("idCarrera") @JoinColumn(name="id_carrera") private Carrera carrera;
 @Column(name="anio_inscripcion",nullable=false) private int anioInscripcion;
 @Column(name="anio_egreso") private Integer anioEgreso;
 public Inscripcion(){}
 public Inscripcion(Estudiante e,Carrera c,int ai,Integer ae){estudiante=e;carrera=c;anioInscripcion=ai;anioEgreso=ae;id=new InscripcionId(e.getIdEstudiante(),c.getIdCarrera());}
 public int getAntiguedad(){return Year.now().getValue()-anioInscripcion;} public boolean isGraduado(){return anioEgreso!=null;}
 public InscripcionId getId(){return id;} public void setId(InscripcionId v){id=v;} public Estudiante getEstudiante(){return estudiante;} public void setEstudiante(Estudiante v){estudiante=v;} public Carrera getCarrera(){return carrera;} public void setCarrera(Carrera v){carrera=v;} public int getAnioInscripcion(){return anioInscripcion;} public void setAnioInscripcion(int v){anioInscripcion=v;} public Integer getAnioEgreso(){return anioEgreso;} public void setAnioEgreso(Integer v){anioEgreso=v;}
}
