package com.ticketya.preferencias.service;

import com.ticketya.preferencias.client.EventoCatalogo;
import com.ticketya.preferencias.dto.RecomendacionDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Tests unitarios del algoritmo de recomendacion (no necesitan base ni Spring)
class RecomendadorTest {

    private final Recomendador recomendador = new Recomendador();
    private final LocalDateTime base = LocalDateTime.of(2027, 1, 1, 21, 0);

    private final EventoCatalogo rockTandil = evento(1L, "RECITAL", List.of("Banda Uno"), "Tandil", 10);
    private final EventoCatalogo teatroAzul = evento(2L, "TEATRO", List.of("Elenco Estable"), "Azul", 20);
    private final EventoCatalogo futbolTandil = evento(3L, "DEPORTE", List.of("Club Local"), "Tandil", 30);
    private final EventoCatalogo rockAzul = evento(4L, "RECITAL", List.of("Banda Dos"), "Azul", 5);

    @Test
    void unArtistaFavoritoPesaMasQueElTipoYLaCiudad() {
        var perfil = perfil(Set.of("DEPORTE"), Set.of("banda dos"), "tandil", Set.of(), Set.of());

        List<RecomendacionDTO> r = recomendador.recomendar(perfil, List.of(rockTandil, teatroAzul, futbolTandil, rockAzul),
                Map.of(), 10);

        assertEquals(List.of(4L, 3L, 1L), ids(r));   // artista (5) > tipo + ciudad (3) > ciudad (1)
        assertEquals(5, r.get(0).puntaje());
        assertTrue(r.get(0).motivos().get(0).contains("Banda Dos"));
    }

    @Test
    void laCoCompraSumaPorCadaClienteYNoSeRecomiendaLoYaComprado() {
        var perfil = perfil(Set.of(), Set.of(), null, Set.of(1L), Set.of("RECITAL"));

        // 2 clientes que compraron lo mismo compraron el teatro; 1 compro el futbol
        List<RecomendacionDTO> r = recomendador.recomendar(perfil, List.of(rockTandil, teatroAzul, futbolTandil, rockAzul),
                Map.of(2L, 2, 3L, 1), 10);

        assertFalse(ids(r).contains(1L), "no se recomienda un evento ya comprado");
        assertEquals(2L, r.get(0).eventoId());
        assertEquals(6, r.get(0).puntaje());
        assertTrue(r.get(0).motivos().get(0).startsWith("2 clientes"));
        // el recital de Azul suma 1 por ser de un tipo que ya compro
        assertTrue(ids(r).contains(4L));
    }

    @Test
    void sinDatosDelClienteDevuelveLosProximosEventos() {
        var perfil = perfil(Set.of(), Set.of(), null, Set.of(), Set.of());

        List<RecomendacionDTO> r = recomendador.recomendar(perfil, List.of(teatroAzul, rockTandil, futbolTandil),
                Map.of(), 2);

        assertEquals(List.of(1L, 2L), ids(r));   // por fecha, respetando el limite
        assertEquals(0, r.get(0).puntaje());
        assertEquals(List.of("Proximo evento a la venta"), r.get(0).motivos());
    }

    @Test
    void respetaElLimiteYDesempataPorFecha() {
        var perfil = perfil(Set.of(), Set.of(), "tandil", Set.of(), Set.of());

        List<RecomendacionDTO> r = recomendador.recomendar(perfil, List.of(futbolTandil, rockTandil), Map.of(), 1);

        assertEquals(List.of(1L), ids(r));   // los dos suman 1 por la ciudad: gana el mas proximo
    }

    @Test
    void lasComparacionesNoDependenDeMayusculas() {
        var perfil = perfil(Set.of(), Set.of("banda uno"), "tandil", Set.of(), Set.of());
        EventoCatalogo conMayusculas = evento(9L, "RECITAL", List.of("BANDA UNO "), "TANDIL", 1);

        List<RecomendacionDTO> r = recomendador.recomendar(perfil, List.of(conMayusculas), Map.of(), 5);

        assertEquals(6, r.get(0).puntaje());   // artista (5) + ciudad (1)
    }

    // ---------- utilidades ----------

    private EventoCatalogo evento(Long id, String tipo, List<String> participantes, String ciudad, int diasDesdeBase) {
        return new EventoCatalogo(id, "Evento " + id, tipo, participantes, base.plusDays(diasDesdeBase), ciudad,
                new BigDecimal("10000"));
    }

    private static Recomendador.Perfil perfil(Set<String> tipos, Set<String> artistas, String ciudad,
                                              Set<Long> comprados, Set<String> tiposComprados) {
        return new Recomendador.Perfil(tipos, artistas, ciudad, comprados, tiposComprados);
    }

    private static List<Long> ids(List<RecomendacionDTO> r) {
        return r.stream().map(RecomendacionDTO::eventoId).toList();
    }
}
