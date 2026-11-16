package com.ticketya.catalogo.service;

import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.exception.BadRequestException;
import com.ticketya.catalogo.exception.ConflictException;
import com.ticketya.catalogo.exception.NotFoundException;
import com.ticketya.catalogo.model.*;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepo;
    private final LugarRepository lugarRepo;
    private final PrecioSectorRepository precioRepo;

    // ---------- Crear (siempre en BORRADOR) ----------
    @Transactional
    public EventoDTO crear(EventoRequest req) {
        Lugar lugar = lugarRepo.findById(req.lugarId())
                .orElseThrow(() -> new NotFoundException("No existe el lugar " + req.lugarId()));

        Evento e = new Evento();
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setTipo(req.tipo());
        e.setOrganizador(req.organizador());
        e.setParticipantes(req.participantes() == null ? new ArrayList<>() : new ArrayList<>(req.participantes()));
        e.setFechaHora(req.fechaHora());
        e.setLugar(lugar);
        e.setEstado(EstadoEvento.BORRADOR);
        return toDTO(eventoRepo.save(e));
    }

    @Transactional(readOnly = true)
    public EventoDTO obtener(Long id) {
        return toDTO(buscar(id));
    }

    // ---------- Precios por sector (solo en BORRADOR) ----------
    @Transactional
    public EventoDTO configurarPrecios(Long eventoId, PreciosRequest req) {
        Evento e = buscar(eventoId);
        if (e.getEstado() != EstadoEvento.BORRADOR) {
            throw new ConflictException("Solo se pueden configurar precios de eventos en BORRADOR");
        }

        Map<Long, Sector> sectoresDelLugar = e.getLugar().getSectores().stream()
                .collect(Collectors.toMap(Sector::getId, s -> s));
        Set<Long> vistos = new HashSet<>();
        for (PrecioRequest p : req.precios()) {
            if (!sectoresDelLugar.containsKey(p.sectorId())) {
                throw new BadRequestException("El sector " + p.sectorId() + " no pertenece al lugar del evento");
            }
            if (!vistos.add(p.sectorId())) {
                throw new BadRequestException("Sector repetido en la lista de precios: " + p.sectorId());
            }
        }

        precioRepo.deleteByEventoId(eventoId);
        precioRepo.flush();   // que el borrado se ejecute antes de los nuevos inserts (clave unica evento+sector)
        for (PrecioRequest p : req.precios()) {
            PrecioSector ps = new PrecioSector();
            ps.setEvento(e);
            ps.setSector(sectoresDelLugar.get(p.sectorId()));
            ps.setPrecio(p.precio());
            precioRepo.save(ps);
        }
        return toDTO(e);
    }

    // ---------- Publicar: BORRADOR -> PUBLICADO (requiere precio en todos los sectores) ----------
    @Transactional
    public EventoDTO publicar(Long id) {
        Evento e = buscar(id);
        if (e.getEstado() != EstadoEvento.BORRADOR) {
            throw new ConflictException("Solo se puede publicar un evento en BORRADOR");
        }
        Set<Long> conPrecio = precioRepo.findByEventoId(id).stream()
                .map(p -> p.getSector().getId())
                .collect(Collectors.toSet());
        List<String> sinPrecio = e.getLugar().getSectores().stream()
                .filter(s -> !conPrecio.contains(s.getId()))
                .map(Sector::getNombre)
                .toList();
        if (!sinPrecio.isEmpty()) {
            throw new ConflictException("Faltan precios para los sectores: " + String.join(", ", sinPrecio));
        }
        e.setEstado(EstadoEvento.PUBLICADO);
        return toDTO(e);
    }

    // ---------- Suspender (administrador) ----------
    @Transactional
    public EventoDTO suspender(Long id) {
        Evento e = buscar(id);
        if (e.getEstado() != EstadoEvento.PUBLICADO && e.getEstado() != EstadoEvento.AGOTADO) {
            throw new ConflictException("Solo se puede suspender un evento PUBLICADO o AGOTADO");
        }
        e.setEstado(EstadoEvento.SUSPENDIDO);
        return toDTO(e);
    }

    // ---------- Cancelar (administrador). El aviso a ventas lo hace CancelacionService, fuera de la transaccion ----------
    @Transactional
    public EventoDTO cancelar(Long id, String motivo) {
        Evento e = buscar(id);
        if (e.getEstado() == EstadoEvento.CANCELADO || e.getEstado() == EstadoEvento.FINALIZADO) {
            throw new ConflictException("El evento ya esta " + e.getEstado());
        }
        e.setEstado(EstadoEvento.CANCELADO);
        e.setMotivoCancelacion(motivo);
        return toDTO(e);
    }

    // ---------- Busqueda por multiples criterios ----------
    @Transactional(readOnly = true)
    public List<EventoResumenDTO> buscar(TipoEvento tipo, String organizador, String participante, String ciudad,
                                         LocalDateTime desde, LocalDateTime hasta, String orden) {
        if (orden != null && !orden.equalsIgnoreCase("fecha") && !orden.equalsIgnoreCase("precio")) {
            throw new BadRequestException("El orden debe ser 'fecha' o 'precio'");
        }

        Specification<Evento> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> ps = new ArrayList<>();
            // solo se buscan eventos a la venta
            ps.add(root.get("estado").in(EstadoEvento.PUBLICADO, EstadoEvento.AGOTADO));
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (organizador != null && !organizador.isBlank()) {
                ps.add(cb.like(cb.lower(root.<String>get("organizador")), "%" + organizador.toLowerCase() + "%"));
            }
            if (participante != null && !participante.isBlank()) {
                Join<Evento, String> p = root.join("participantes", JoinType.LEFT);
                ps.add(cb.like(cb.lower(p), "%" + participante.toLowerCase() + "%"));
            }
            if (ciudad != null && !ciudad.isBlank()) {
                ps.add(cb.equal(cb.lower(root.join("lugar").<String>get("ciudad")), ciudad.toLowerCase()));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaHora"), desde));
            }
            if (hasta != null) {
                ps.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("fechaHora"), hasta));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        List<EventoResumenDTO> resultado = eventoRepo.findAll(spec, Sort.by("fechaHora")).stream()
                .map(this::toResumen)
                .collect(Collectors.toCollection(ArrayList::new));

        if ("precio".equalsIgnoreCase(orden)) {
            resultado.sort(Comparator.comparing(EventoResumenDTO::precioDesde,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return resultado;
    }

    // ---------- Para el servicio de ventas: datos de un sector de un evento ----------
    @Transactional(readOnly = true)
    public SectorVentaDTO obtenerSectorVenta(Long eventoId, Long sectorId) {
        PrecioSector p = precioRepo.findByEventoIdAndSectorId(eventoId, sectorId)
                .orElseThrow(() -> new NotFoundException(
                        "El sector " + sectorId + " no tiene precio en el evento " + eventoId));
        Evento e = p.getEvento();
        Sector s = p.getSector();
        return new SectorVentaDTO(e.getId(), e.getNombre(), e.getEstado(), e.getFechaHora(),
                s.getId(), s.getNombre(), s.getTipo(), s.capacidadTotal(),
                s.getFilas(), s.getAsientosPorFila(), p.getPrecio());
    }

    // ---------- helpers ----------
    private Evento buscar(Long id) {
        return eventoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el evento " + id));
    }

    private EventoDTO toDTO(Evento e) {
        List<PrecioSectorDTO> precios = precioRepo.findByEventoId(e.getId()).stream()
                .map(p -> new PrecioSectorDTO(p.getSector().getId(), p.getSector().getNombre(),
                        p.getSector().getTipo(), p.getPrecio()))
                .toList();
        return new EventoDTO(e.getId(), e.getNombre(), e.getDescripcion(), e.getTipo(), e.getOrganizador(),
                List.copyOf(e.getParticipantes()), e.getFechaHora(), e.getEstado(),
                LugarService.toDTO(e.getLugar()), precios);
    }

    private EventoResumenDTO toResumen(Evento e) {
        return new EventoResumenDTO(e.getId(), e.getNombre(), e.getTipo(), e.getOrganizador(),
                List.copyOf(e.getParticipantes()), e.getFechaHora(), e.getEstado(),
                e.getLugar().getNombre(), e.getLugar().getCiudad(), precioRepo.precioMinimo(e.getId()));
    }
}
