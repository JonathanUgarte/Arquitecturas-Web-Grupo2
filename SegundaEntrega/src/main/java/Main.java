import data.CsvDataLoader;
import dto.CarreraDTO;
import dto.EstudianteDTO;
import dto.ReporteCarreraDTO;
import entities.Genero;
import jakarta.persistence.EntityManager;
import repository.CarreraRepository;
import repository.EstudianteRepository;
import repository.JPAUtil;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        EntityManager em = JPAUtil.getEntityManager();
        EstudianteRepository estudianteRepo = new EstudianteRepository(em);
        CarreraRepository carreraRepo = new CarreraRepository(em);

        try {
            System.out.println("========== CARGA DE DATOS DESDE CSV ==========");
            new CsvDataLoader(em).cargarTodo();

            System.out.println("\n========== 2.C ESTUDIANTES ORDENADOS ==========");
            List<EstudianteDTO> estudiantes = estudianteRepo.obtenerTodosOrdenados();
            estudiantes.stream().limit(15).forEach(e ->
                    System.out.println("- " + e.getApellido() + ", " + e.getNombres()
                            + " | Edad: " + e.getEdad() + " | Genero: " + e.getGenero()));
            System.out.println("Total de estudiantes: " + estudiantes.size());

            System.out.println("\n========== 2.D BUSQUEDA POR LIBRETA 34978 ==========");
            EstudianteDTO porLibreta = estudianteRepo.obtenerPorLibreta("34978");
            System.out.println("- " + porLibreta.getNombres() + " " + porLibreta.getApellido()
                    + " | Edad: " + porLibreta.getEdad() + " | Genero: " + porLibreta.getGenero());

            System.out.println("\n========== 2.E ESTUDIANTES MASCULINOS ==========");
            List<EstudianteDTO> masculinos = estudianteRepo.obtenerPorGenero(Genero.MASCULINO);
            System.out.println("Cantidad: " + masculinos.size());
            masculinos.stream().limit(10).forEach(e ->
                    System.out.println("- " + e.getApellido() + ", " + e.getNombres()
                            + " | Edad: " + e.getEdad()));

            System.out.println("\n========== 2.F CARRERAS POR CANTIDAD DE INSCRIPTOS ==========");
            List<CarreraDTO> carreras = carreraRepo.obtenerCarrerasConInscriptosOrdenadas();
            carreras.forEach(c -> System.out.println(
                    "- " + c.getNombre() + " -> " + c.getCantidadInscriptos() + " inscriptos"));

            System.out.println("\n========== 2.G TUDAI + CARRERA ==========");
            List<EstudianteDTO> filtrados =
                    estudianteRepo.obtenerEstudiantesPorCarreraYCiudad("TUDAI", "Paquera");
            if (filtrados.isEmpty()) {
                System.out.println("- No hay resultados para ese filtro en los CSV.");
            } else {
                filtrados.forEach(e -> System.out.println(
                        "- " + e.getApellido() + ", " + e.getNombres()
                                + " | Edad: " + e.getEdad() + " | Genero: " + e.getGenero()));
            }

            System.out.println("\n========== 3. REPORTE DE CARRERAS POR ANIO ==========");
            List<ReporteCarreraDTO> reporte = carreraRepo.generarReporteCarreras();
            reporte.forEach(r -> System.out.println("- " + r));

            System.out.println("\n========== FIN DE LAS PRUEBAS ==========");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em.isOpen()) em.close();
            JPAUtil.close();
        }
    }
}
