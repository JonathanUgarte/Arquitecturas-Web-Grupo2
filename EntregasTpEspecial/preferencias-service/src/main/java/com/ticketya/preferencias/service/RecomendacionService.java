package com.ticketya.preferencias.service;

import com.ticketya.preferencias.client.CatalogoClient;
import com.ticketya.preferencias.dto.RecomendacionDTO;
import com.ticketya.preferencias.model.CompraRegistrada;
import com.ticketya.preferencias.model.PerfilCliente;
import com.ticketya.preferencias.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// Junta los datos que necesita el Recomendador: el perfil del cliente, los eventos a la venta (catalogo)
// y la co-compra ("clientes que compraron lo mismo que vos, que otros eventos compraron"), calculada en MongoDB.
@Service
@RequiredArgsConstructor
public class RecomendacionService {

    private final PerfilRepository perfilRepo;
    private final CatalogoClient catalogoClient;
    private final Recomendador recomendador = new Recomendador();

    public List<RecomendacionDTO> recomendar(Long usuarioId, int limite) {
        PerfilCliente perfil = perfilRepo.findById(usuarioId).orElseGet(() -> new PerfilCliente(usuarioId));

        Set<Long> misEventos = perfil.getCompras().stream()
                .map(CompraRegistrada::getEventoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> misTipos = perfil.getCompras().stream()
                .map(CompraRegistrada::getTipo)
                .filter(Objects::nonNull)
                .map(t -> t.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Recomendador.Perfil datos = new Recomendador.Perfil(
                perfil.getTiposFavoritos(),
                perfil.getArtistasNormalizados(),
                perfil.getCiudadNormalizada(),
                misEventos,
                misTipos);

        return recomendador.recomendar(datos, catalogoClient.eventosALaVenta(), coCompras(usuarioId, misEventos), limite);
    }

    // eventoId -> cantidad de OTROS clientes que, habiendo comprado alguno de mis eventos, compraron ese evento
    private Map<Long, Integer> coCompras(Long usuarioId, Set<Long> misEventos) {
        Map<Long, Integer> conteo = new HashMap<>();
        if (misEventos.isEmpty()) {
            return conteo;
        }
        for (PerfilCliente otro : perfilRepo.findByComprasEventoIdIn(misEventos)) {
            if (otro.getUsuarioId().equals(usuarioId)) {
                continue;
            }
            otro.getCompras().stream()
                    .map(CompraRegistrada::getEventoId)
                    .filter(id -> id != null && !misEventos.contains(id))
                    .distinct()
                    .forEach(id -> conteo.merge(id, 1, Integer::sum));
        }
        return conteo;
    }
}
