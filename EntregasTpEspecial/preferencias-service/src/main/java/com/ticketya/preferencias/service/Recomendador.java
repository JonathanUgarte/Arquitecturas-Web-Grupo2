package com.ticketya.preferencias.service;

import com.ticketya.preferencias.client.EventoCatalogo;
import com.ticketya.preferencias.dto.RecomendacionDTO;

import java.util.*;

// Algoritmo de recomendacion (version simplificada, sin dependencias de Spring para probarlo con tests unitarios).
//
// Cada evento a la venta que el cliente todavia no compro suma puntos por:
//   +3 por cada otro cliente que compro algo en comun con el y tambien compro este evento
//      ("quienes compraron entradas para este evento tambien compraron para...")
//   +5 si participa uno de sus artistas favoritos
//   +2 si es de uno de sus tipos de evento favoritos
//   +1 si es de un tipo que ya compro antes (aunque no lo haya marcado como favorito)
//   +1 si es en su ciudad
// Se ordena por puntaje y, a igual puntaje, por fecha. Si no hay ningun dato para puntuar (cliente nuevo,
// "arranque en frio"), se devuelven los proximos eventos a la venta.
public class Recomendador {

    static final int PUNTOS_CO_COMPRA = 3;
    static final int PUNTOS_ARTISTA = 5;
    static final int PUNTOS_TIPO_FAVORITO = 2;
    static final int PUNTOS_TIPO_COMPRADO = 1;
    static final int PUNTOS_CIUDAD = 1;

    // Datos del cliente ya normalizados (minusculas / mayusculas segun corresponda)
    public record Perfil(Set<String> tiposFavoritos, Set<String> artistasFavoritos, String ciudad,
                         Set<Long> eventosComprados, Set<String> tiposComprados) {
    }

    public List<RecomendacionDTO> recomendar(Perfil perfil, List<EventoCatalogo> candidatos,
                                             Map<Long, Integer> coCompras, int limite) {
        List<RecomendacionDTO> resultado = new ArrayList<>();
        for (EventoCatalogo evento : candidatos) {
            if (perfil.eventosComprados().contains(evento.id())) {
                continue;
            }
            int puntaje = 0;
            List<String> motivos = new ArrayList<>();

            int otros = coCompras.getOrDefault(evento.id(), 0);
            if (otros > 0) {
                puntaje += PUNTOS_CO_COMPRA * otros;
                motivos.add(otros + (otros == 1 ? " cliente que compro" : " clientes que compraron")
                        + " lo mismo que vos tambien compro entradas para este evento");
            }
            Optional<String> artista = artistaFavorito(evento, perfil.artistasFavoritos());
            if (artista.isPresent()) {
                puntaje += PUNTOS_ARTISTA;
                motivos.add("Participa " + artista.get() + ", uno de tus artistas favoritos");
            }
            String tipo = evento.tipo() == null ? null : evento.tipo().toUpperCase(Locale.ROOT);
            if (tipo != null && perfil.tiposFavoritos().contains(tipo)) {
                puntaje += PUNTOS_TIPO_FAVORITO;
                motivos.add("Es de tipo " + tipo + ", uno de tus favoritos");
            } else if (tipo != null && perfil.tiposComprados().contains(tipo)) {
                puntaje += PUNTOS_TIPO_COMPRADO;
                motivos.add("Ya compraste entradas para eventos de tipo " + tipo);
            }
            if (perfil.ciudad() != null && evento.ciudad() != null
                    && perfil.ciudad().equals(evento.ciudad().toLowerCase(Locale.ROOT))) {
                puntaje += PUNTOS_CIUDAD;
                motivos.add("Es en tu ciudad (" + evento.ciudad() + ")");
            }
            resultado.add(new RecomendacionDTO(evento.id(), evento.nombre(), evento.tipo(), evento.ciudad(),
                    evento.fechaHora(), evento.precioDesde(), puntaje, motivos));
        }

        boolean hayPuntuados = resultado.stream().anyMatch(r -> r.puntaje() > 0);
        Comparator<RecomendacionDTO> porFecha = Comparator.comparing(RecomendacionDTO::fechaHora,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (!hayPuntuados) {
            // Arranque en frio: proximos eventos a la venta
            return resultado.stream()
                    .sorted(porFecha)
                    .limit(limite)
                    .map(r -> new RecomendacionDTO(r.eventoId(), r.nombre(), r.tipo(), r.ciudad(), r.fechaHora(),
                            r.precioDesde(), 0, List.of("Proximo evento a la venta")))
                    .toList();
        }
        return resultado.stream()
                .filter(r -> r.puntaje() > 0)
                .sorted(Comparator.comparingInt(RecomendacionDTO::puntaje).reversed().thenComparing(porFecha))
                .limit(limite)
                .toList();
    }

    private static Optional<String> artistaFavorito(EventoCatalogo evento, Set<String> favoritos) {
        if (evento.participantes() == null) {
            return Optional.empty();
        }
        return evento.participantes().stream()
                .filter(p -> p != null && favoritos.contains(p.trim().toLowerCase(Locale.ROOT)))
                .findFirst();
    }
}
