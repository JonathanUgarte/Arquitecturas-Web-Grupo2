package com.ticketya.preferencias.service;

import com.ticketya.preferencias.client.CatalogoClient;
import com.ticketya.preferencias.client.EventoDetalle;
import com.ticketya.preferencias.dto.CompraHistorialDTO;
import com.ticketya.preferencias.dto.PerfilDTO;
import com.ticketya.preferencias.dto.PreferenciasRequest;
import com.ticketya.preferencias.exception.BadRequestException;
import com.ticketya.preferencias.mensajeria.CompraConfirmadaEvento;
import com.ticketya.preferencias.model.CompraRegistrada;
import com.ticketya.preferencias.model.PerfilCliente;
import com.ticketya.preferencias.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfilService {

    static final Set<String> TIPOS_VALIDOS = Set.of("RECITAL", "TEATRO", "DEPORTE", "CONFERENCIA", "FESTIVAL", "OTRO");

    private final PerfilRepository perfilRepo;
    private final CatalogoClient catalogoClient;

    public PerfilDTO obtener(Long usuarioId) {
        return toDTO(perfilRepo.findById(usuarioId).orElseGet(() -> new PerfilCliente(usuarioId)));
    }

    public PerfilDTO actualizar(Long usuarioId, PreferenciasRequest req) {
        PerfilCliente perfil = perfilRepo.findById(usuarioId).orElseGet(() -> new PerfilCliente(usuarioId));
        if (req.ciudad() != null) {
            String ciudad = req.ciudad().trim();
            perfil.setCiudad(ciudad.isEmpty() ? null : ciudad);
            perfil.setCiudadNormalizada(ciudad.isEmpty() ? null : normalizar(ciudad));
        }
        if (req.tiposFavoritos() != null) {
            Set<String> tipos = new LinkedHashSet<>();
            for (String t : req.tiposFavoritos()) {
                String tipo = t == null ? "" : t.trim().toUpperCase(Locale.ROOT);
                if (!TIPOS_VALIDOS.contains(tipo)) {
                    throw new BadRequestException("Tipo de evento invalido: " + t + ". Validos: " + TIPOS_VALIDOS);
                }
                tipos.add(tipo);
            }
            perfil.setTiposFavoritos(tipos);
        }
        if (req.artistasFavoritos() != null) {
            Set<String> artistas = req.artistasFavoritos().stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            perfil.setArtistasFavoritos(artistas);
            perfil.setArtistasNormalizados(artistas.stream().map(PerfilService::normalizar)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        perfil.setActualizado(LocalDateTime.now());
        return toDTO(perfilRepo.save(perfil));   // @Version: si otro proceso lo modifico, falla con 409
    }

    // Consumidor de "compra.confirmada". Idempotente: si el mensaje llega dos veces, la compra se registra una sola.
    public void registrarCompra(CompraConfirmadaEvento evento) {
        PerfilCliente perfil = perfilRepo.findById(evento.usuarioId())
                .orElseGet(() -> new PerfilCliente(evento.usuarioId()));
        boolean yaRegistrada = perfil.getCompras().stream()
                .anyMatch(c -> Objects.equals(c.getCompraId(), evento.compraId()));
        if (yaRegistrada) {
            log.info("Compra {} ya registrada para el usuario {}: se ignora el duplicado",
                    evento.compraId(), evento.usuarioId());
            return;
        }
        // Se enriquece con los datos del evento. Si catalogo no responde, se guarda igual (sin tipo ni ciudad):
        // el historial no es critico y la co-compra solo necesita el id del evento.
        Optional<EventoDetalle> detalle = catalogoClient.detalle(evento.eventoId());
        perfil.getCompras().add(new CompraRegistrada(
                evento.compraId(),
                evento.eventoId(),
                detalle.map(EventoDetalle::nombre).orElse(null),
                detalle.map(EventoDetalle::tipo).orElse(null),
                detalle.map(EventoDetalle::participantes).orElse(List.of()),
                detalle.map(EventoDetalle::ciudad).orElse(null),
                evento.fecha()));
        perfil.setActualizado(LocalDateTime.now());
        perfilRepo.save(perfil);   // si choca con otra escritura (@Version), el listener reintenta el mensaje
        log.info("Compra {} registrada en el historial del usuario {}", evento.compraId(), evento.usuarioId());
    }

    // Clientes de esa ciudad que tienen como favorito a alguno de esos artistas
    public List<Long> interesados(String ciudad, Collection<String> artistas) {
        if (ciudad == null || ciudad.isBlank() || artistas == null || artistas.isEmpty()) {
            return List.of();
        }
        List<String> normalizados = artistas.stream().filter(Objects::nonNull).map(PerfilService::normalizar).toList();
        return perfilRepo.findByCiudadNormalizadaAndArtistasNormalizadosIn(normalizar(ciudad), normalizados).stream()
                .map(PerfilCliente::getUsuarioId)
                .distinct()
                .sorted()
                .toList();
    }

    static String normalizar(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }

    private static PerfilDTO toDTO(PerfilCliente p) {
        List<CompraHistorialDTO> historial = p.getCompras().stream()
                .sorted(Comparator.comparing(CompraRegistrada::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> new CompraHistorialDTO(c.getCompraId(), c.getEventoId(), c.getNombreEvento(), c.getTipo(),
                        c.getParticipantes(), c.getCiudad(), c.getFecha()))
                .toList();
        return new PerfilDTO(p.getUsuarioId(), p.getCiudad(), p.getTiposFavoritos(), p.getArtistasFavoritos(), historial);
    }
}
