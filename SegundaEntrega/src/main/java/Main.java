import dto.ReporteCarreraDTO;
import entities.Carrera;
import entities.Estudiante;
import entities.Genero;
import repository.CarreraRepository;
import repository.EstudianteRepository;
import repository.InscripcionRepository;
import repository.JPAUtil;

import jakarta.persistence.EntityManager;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // 1. Inicializamos el EntityManager y los repositorios
        EntityManager em = JPAUtil.getEntityManager();
        EstudianteRepository estudianteRepo = new EstudianteRepository(em);
        CarreraRepository carreraRepo = new CarreraRepository(em);
        InscripcionRepository inscripcionRepo = new InscripcionRepository(em);

        try {
            System.out.println("--- INICIANDO PRUEBAS DEL TRABAJO PRÁCTICO ---");

            // Preparación: Creamos una carrera de prueba (usamos em.persist directo por simplicidad)
            Carrera carrera = new Carrera();
            carrera.setNombre("TUARI"); // Tecnicatura en Desarrollo de Aplicaciones
            em.getTransaction().begin();
            em.persist(carrera);
            em.getTransaction().commit();

            // A) Dar de alta un estudiante
            System.out.println("\na) Dando de alta un estudiante...");
            Estudiante estudiante = new Estudiante();
            estudiante.setNombres("Agustin");
            estudiante.setApellido("Pagola");
            estudiante.setEdad(25);
            estudiante.setGenero(Genero.MASCULINO); // Asegurate de tener esto en tu Enum Genero
            estudiante.setNumeroDocumento("12345678");
            estudiante.setCiudadResidencia("Tandil");
            estudiante.setNumeroLibretaUniversitaria("12345");

            estudianteRepo.guardarEstudiante(estudiante);
            System.out.println("Estudiante guardado con éxito: " + estudiante.getNombres() + " " + estudiante.getApellido());

            // B) Matricular un estudiante en una carrera
            System.out.println("\nb) Matriculando al estudiante en la carrera...");
            inscripcionRepo.matricularEstudiante(estudiante, carrera);
            System.out.println("Estudiante matriculado con éxito.");

            // C) Recuperar todos los estudiantes y aplicar algún criterio de ordenamiento simple (por apellido y nombre)
            System.out.println("\nc) Recuperando todos los estudiantes ordenados:");
            List<Estudiante> todos = estudianteRepo.obtenerTodosOrdenados();
            for (Estudiante e : todos) {
                System.out.println(" - " + e.getApellido() + ", " + e.getNombres());
            }

            // D) Recuperar un estudiante en base a su número de libreta universitaria
            System.out.println("\nd) Recuperando estudiante por número de libreta (12345):");
            Estudiante porLibreta = estudianteRepo.obtenerPorLibreta(12345);
            System.out.println(" - Encontrado: " + porLibreta.getNombres() + " " + porLibreta.getApellido());

            // E) Recuperar todos los estudiantes en base a su género
            System.out.println("\ne) Recuperando estudiantes por género (MASCULINO):");
            List<Estudiante> porGenero = estudianteRepo.obtenerPorGenero(Genero.MASCULINO);
            for (Estudiante e : porGenero) {
                System.out.println(" - " + e.getNombres() + " (" + e.getGenero() + ")");
            }

            // F) Recuperar las carreras con estudiantes inscriptos y ordenarlas por cantidad de inscriptos
            System.out.println("\nf) Recuperando carreras ordenadas por cantidad de inscriptos:");
            List<Carrera> carrerasOrdenadas = carreraRepo.obtenerCarrerasConInscriptosOrdenadas();
            for (Carrera c : carrerasOrdenadas) {
                System.out.println(" - Carrera: " + c.getNombre());
            }

            // G) Recuperar los estudiantes de una determinada carrera, filtrados por ciudad de residencia
            System.out.println("\ng) Recuperando estudiantes de 'TUARI' que viven en 'Tandil':");
            List<Estudiante> filtrados = estudianteRepo.obtenerEstudiantesPorCarreraYCiudad("TUARI", "Tandil");
            for (Estudiante e : filtrados) {
                System.out.println(" - " + e.getNombres() + " " + e.getApellido() + " (Ciudad: " + e.getCiudadResidencia() + ")");
            }
            System.out.println("\n3) Generando reporte de carreras (Inscriptos/Egresados por año):");
            List<ReporteCarreraDTO> reporte = carreraRepo.generarReporteCarreras();
            for (ReporteCarreraDTO item : reporte) {
                System.out.println(" - " + item.toString());
            }

        } catch (Exception e) {
            System.err.println("Ocurrió un error durante las pruebas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Siempre cerramos la conexión al finalizar
            em.close();
            JPAUtil.close();
            System.out.println("\n--- FIN DE LAS PRUEBAS (Conexión cerrada) ---");
        }
    }
}