package data;

import entities.Carrera;
import entities.Estudiante;
import entities.Genero;
import entities.Inscripcion;
import entities.InscripcionId;
import jakarta.persistence.EntityManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.List;

/** Carga los CSV ubicados en src/main/resources/data. */
public class CsvDataLoader {

    private final EntityManager em;

    public CsvDataLoader(EntityManager em) {
        this.em = em;
    }

    public void cargarTodo() {
        em.getTransaction().begin();
        try {
            int carreras = cargarCarreras();
            int estudiantes = cargarEstudiantes();
            int inscripciones = cargarInscripciones();
            em.getTransaction().commit();

            System.out.printf("CSV cargados: %d carreras, %d estudiantes, %d inscripciones.%n",
                    carreras, estudiantes, inscripciones);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }

    private int cargarCarreras() {
        List<String> lineas = leerLineas("data/carreras.csv");
        int cantidad = 0;
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) continue;
            String[] c = lineas.get(i).split(",", -1);

            Carrera carrera = new Carrera();
            carrera.setIdCarrera(Integer.parseInt(c[0].trim()));
            carrera.setNombre(c[1].trim());
            carrera.setDuracion(Integer.parseInt(c[2].trim()));
            em.persist(carrera);
            cantidad++;
        }
        return cantidad;
    }

    private int cargarEstudiantes() {
        List<String> lineas = leerLineas("data/estudiantes.csv");
        int cantidad = 0;
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) continue;
            String[] c = lineas.get(i).split(",", -1);

            int dni = Integer.parseInt(c[0].trim());
            Estudiante estudiante = new Estudiante();
            // El DNI se usa como id porque estudianteCarrera.csv referencia al estudiante por DNI.
            estudiante.setIdEstudiante(dni);
            estudiante.setNumeroDocumento(c[0].trim());
            estudiante.setNombres(c[1].trim());
            estudiante.setApellido(c[2].trim());
            estudiante.setEdad(Integer.parseInt(c[3].trim()));
            estudiante.setGenero(convertirGenero(c[4].trim()));
            estudiante.setCiudadResidencia(c[5].trim());
            estudiante.setNumeroLibretaUniversitaria(c[6].trim());
            em.persist(estudiante);
            cantidad++;
        }
        return cantidad;
    }

    private int cargarInscripciones() {
        List<String> lineas = leerLineas("data/estudianteCarrera.csv");
        int cantidad = 0;
        int duplicadas = 0;

        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) continue;
            String[] c = lineas.get(i).split(",", -1);

            int idEstudiante = Integer.parseInt(c[1].trim());
            int idCarrera = Integer.parseInt(c[2].trim());
            int anioInscripcion = Integer.parseInt(c[3].trim());
            int graduacionCsv = Integer.parseInt(c[4].trim());
            Integer anioEgreso = graduacionCsv == 0 ? null : graduacionCsv;

            // La PK del modelo es (estudiante,carrera). El CSV contiene una fila duplicada
            // para el par 64472668/7; se conserva la primera para evitar violar la PK.
            InscripcionId id = new InscripcionId(idEstudiante, idCarrera);
            if (em.find(Inscripcion.class, id) != null) {
                duplicadas++;
                continue;
            }

            Estudiante estudiante = em.find(Estudiante.class, idEstudiante);
            Carrera carrera = em.find(Carrera.class, idCarrera);
            if (estudiante == null || carrera == null) {
                System.out.printf("Fila %d omitida: estudiante %d o carrera %d inexistente.%n",
                        i + 1, idEstudiante, idCarrera);
                continue;
            }

            // 'antiguedad' del CSV no se persiste porque en la entidad se deriva del año de ingreso.
            Inscripcion inscripcion = new Inscripcion(estudiante, carrera, anioInscripcion, anioEgreso);
            em.persist(inscripcion);
            cantidad++;
        }

        if (duplicadas > 0) {
            System.out.println("Aviso: se omitieron " + duplicadas
                    + " inscripciones duplicadas según la clave (estudiante, carrera).");
        }
        return cantidad;
    }

    private Genero convertirGenero(String valor) {
        String normalizado = valor.trim().toUpperCase();
        return switch (normalizado) {
            case "MALE", "MASCULINO" -> Genero.MASCULINO;
            case "FEMALE", "FEMENINO" -> Genero.FEMENINO;
            default -> Genero.OTRO;
        };
    }

    /**
     * Lee cada línea intentando UTF-8 y, si esa línea contiene bytes no válidos,
     * usa Windows-1252. El CSV de estudiantes contiene datos con codificación mixta.
     */
    private List<String> leerLineas(String recurso) {
        InputStream in = CsvDataLoader.class.getClassLoader().getResourceAsStream(recurso);
        if (in == null) throw new IllegalStateException("No se encontró el recurso: " + recurso);

        try (BufferedInputStream bin = new BufferedInputStream(in)) {
            List<String> resultado = new ArrayList<>();
            ByteArrayOutputStream linea = new ByteArrayOutputStream();
            int b;
            while ((b = bin.read()) != -1) {
                if (b == '\n') {
                    resultado.add(decodificarLinea(linea.toByteArray()));
                    linea.reset();
                } else if (b != '\r') {
                    linea.write(b);
                }
            }
            if (linea.size() > 0) resultado.add(decodificarLinea(linea.toByteArray()));
            return resultado;
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo " + recurso, e);
        }
    }

    private String decodificarLinea(byte[] bytes) {
        CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return utf8.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }
}
