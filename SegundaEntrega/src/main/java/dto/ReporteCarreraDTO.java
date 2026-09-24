package dto;

public class ReporteCarreraDTO {
    private String nombreCarrera;
    private int anio;
    private int inscriptos;
    private int egresados;

    public ReporteCarreraDTO(String nombreCarrera, int anio) {
        this.nombreCarrera = nombreCarrera;
        this.anio = anio;
        this.inscriptos = 0;
        this.egresados = 0;
    }

    public void sumarInscripto() {
        this.inscriptos++;
    }

    public void sumarEgresado() {
        this.egresados++;
    }

    public String getNombreCarrera() { return nombreCarrera; }
    public int getAnio() { return anio; }
    public int getInscriptos() { return inscriptos; }
    public int getEgresados() { return egresados; }

    @Override
    public String toString() {
        return "Carrera: " + nombreCarrera + " | Año: " + anio +
               " | Inscriptos: " + inscriptos + " | Egresados: " + egresados;
    }
}
