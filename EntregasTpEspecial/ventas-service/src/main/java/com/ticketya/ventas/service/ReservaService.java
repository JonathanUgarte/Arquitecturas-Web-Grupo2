package com.ticketya.ventas.service;

import com.ticketya.ventas.client.CatalogoClient;
import com.ticketya.ventas.client.PasarelaPagoMock;
import com.ticketya.ventas.client.ResultadoPago;
import com.ticketya.ventas.config.VentasProperties;
import com.ticketya.ventas.dto.*;
import com.ticketya.ventas.exception.*;
import com.ticketya.ventas.model.*;
import com.ticketya.ventas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

// Compra en dos pasos: 1) RESERVAR lugares por unos minutos, 2) PAGAR dentro de ese plazo.
//
// Las llamadas HTTP (catalogo, pasarela de pagos) quedan FUERA de las transacciones. Por eso se usa
// TransactionTemplate (= @Transactional "programatico"): permite abrir y cerrar cada transaccion
// donde hace falta, sin mantener conexiones ni bloqueos tomados mientras se espera a otro servicio.
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {

    private static final String ESTADO_PUBLICADO = "PUBLICADO";

    private final CatalogoClient catalogoClient;
    private final PasarelaPagoMock pasarela;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final VentasProperties props;
    private final LiberacionService liberacion;
    private final StockZonaInicializador stockInicializador;
    private final ReservaRepository reservaRepo;
    private final AsientoOcupadoRepository asientoRepo;
    private final StockZonaRepository stockRepo;
    private final CompraRepository compraRepo;
    private final EntradaRepository entradaRepo;
    private final EventoCanceladoRepository eventoCanceladoRepo;

    private record InicioPago(Long compraId, boolean nueva, boolean vencida, BigDecimal monto) {
    }

    // =====================================================================
    // 1) RESERVAR
    // =====================================================================
    public ReservaDTO reservar(Long usuarioId, ReservaRequest req) {
        SectorVentaDTO sector = catalogoClient.obtenerSector(req.eventoId(), req.sectorId());   // HTTP, fuera de la tx

        if (!ESTADO_PUBLICADO.equals(sector.estadoEvento())) {
            throw new ConflictException("El evento no esta a la venta (estado " + sector.estadoEvento() + ")");
        }
        if (!sector.fechaHora().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("El evento ya comenzo o finalizo");
        }
        int cantidad = validarSolicitud(req, sector);

        if (sector.tipo() == TipoSector.NO_NUMERADO) {
            try {
                stockInicializador.asegurar(sector.eventoId(), sector.sectorId(), sector.capacidadTotal());
            } catch (DataIntegrityViolationException e) {
                // otro hilo creo la fila primero: no pasa nada, ya existe
            }
        }

        try {
            return tx.execute(status -> crearReserva(usuarioId, req, sector, cantidad));
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // La base rechazo por unicidad (asiento ya tomado) o por bloqueo: otro cliente llego antes
            throw new ConflictException("Alguno de los asientos ya fue reservado o vendido, o el sistema esta ocupado");
        }
    }

    private int validarSolicitud(ReservaRequest req, SectorVentaDTO sector) {
        if (sector.tipo() == TipoSector.NUMERADO) {
            if (req.asientos() == null || req.asientos().isEmpty()) {
                throw new BadRequestException("Debe indicar los asientos a reservar");
            }
            Set<String> vistos = new HashSet<>();
            for (AsientoRequest a : req.asientos()) {
                boolean existe = a.fila() >= 1 && a.fila() <= sector.filas()
                        && a.numero() >= 1 && a.numero() <= sector.asientosPorFila();
                if (!existe) {
                    throw new BadRequestException("El asiento fila " + a.fila() + " numero " + a.numero()
                            + " no existe en el sector");
                }
                if (!vistos.add(a.fila() + "-" + a.numero())) {
                    throw new BadRequestException("Asiento repetido: fila " + a.fila() + " numero " + a.numero());
                }
            }
            return req.asientos().size();
        }
        if (req.cantidad() == null || req.cantidad() < 1) {
            throw new BadRequestException("Debe indicar la cantidad de entradas");
        }
        return req.cantidad();
    }

    // Dentro de la transaccion
    private ReservaDTO crearReserva(Long usuarioId, ReservaRequest req, SectorVentaDTO sector, int cantidad) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        StockZona stock = null;

        // 1. liberar lo vencido (y, en zonas, tomar el cupo con bloqueo pesimista)
        if (sector.tipo() == TipoSector.NO_NUMERADO) {
            stock = stockRepo.findForUpdate(sector.eventoId(), sector.sectorId())
                    .orElseThrow(() -> new IllegalStateException("Falta el stock de la zona"));
            liberacion.liberarVencidasZona(stock, ahora);
        } else {
            liberacion.liberarVencidasNumerado(sector.eventoId(), sector.sectorId(), ahora);
        }

        // 2. limite de entradas por cliente para el mismo evento
        long vendidas = entradaRepo.contarPorUsuarioEventoEstado(usuarioId, sector.eventoId(), EstadoEntrada.VENDIDA);
        long reservadas = reservaRepo.cantidadActiva(usuarioId, sector.eventoId(),
                EstadoReserva.EN_PAGO, EstadoReserva.PENDIENTE, ahora);
        if (vendidas + reservadas + cantidad > props.getMaxEntradasPorEvento()) {
            throw new ConflictException("Maximo " + props.getMaxEntradasPorEvento()
                    + " entradas por cliente para este evento");
        }

        // 3. zona: validar y descontar el cupo
        if (stock != null) {
            if (stock.getDisponibles() < cantidad) {
                throw new ConflictException("No hay cupo suficiente en la zona (quedan " + stock.getDisponibles() + ")");
            }
            stock.setDisponibles(stock.getDisponibles() - cantidad);
        }

        // 4. crear la reserva temporal
        Reserva r = new Reserva();
        r.setUsuarioId(usuarioId);
        r.setEventoId(sector.eventoId());
        r.setSectorId(sector.sectorId());
        r.setTipoSector(sector.tipo());
        r.setCantidad(cantidad);
        r.setPrecioUnitario(sector.precio());
        r.setTotal(sector.precio().multiply(BigDecimal.valueOf(cantidad)));
        r.setFechaEvento(sector.fechaHora());
        r.setEstado(EstadoReserva.PENDIENTE);
        r.setCreadaEn(ahora);
        r.setVenceEn(ahora.plusMinutes(props.getReservaMinutos()));
        reservaRepo.save(r);

        // 5. asientos numerados: la restriccion de unicidad hace que solo uno pueda tomar cada asiento
        List<AsientoDTO> asientos = new ArrayList<>();
        if (sector.tipo() == TipoSector.NUMERADO) {
            for (AsientoRequest a : req.asientos()) {
                AsientoOcupado ocupado = new AsientoOcupado();
                ocupado.setEventoId(sector.eventoId());
                ocupado.setSectorId(sector.sectorId());
                ocupado.setFila(a.fila());
                ocupado.setNumero(a.numero());
                ocupado.setReserva(r);
                asientoRepo.saveAndFlush(ocupado);
                asientos.add(new AsientoDTO(a.fila(), a.numero()));
            }
        }
        return Mapeos.toDTO(r, asientos, ahora);
    }

    // =====================================================================
    // 2) PAGAR (idempotente: la misma clave devuelve el mismo resultado sin cobrar de nuevo)
    // =====================================================================
    public CompraDTO pagar(Long usuarioId, Long reservaId, String claveIdempotencia, PagoRequest req) {
        // Fase 1 (transaccion corta): validar la reserva y dejarla EN_PAGO
        final InicioPago inicio = iniciarPagoSeguro(usuarioId, reservaId, claveIdempotencia, req.medioPago());

        if (inicio.vencida()) {
            throw new ConflictException("La reserva vencio: los lugares fueron liberados");
        }
        if (!inicio.nueva()) {
            // reintento con la misma clave: se devuelve lo que ya se resolvio
            return tx.execute(status -> compraDTO(compraRepo.findById(inicio.compraId()).orElseThrow()));
        }

        // Fase 2 (SIN transaccion): cobrar en la pasarela
        ResultadoPago cobro;
        try {
            cobro = pasarela.cobrar(inicio.monto(), req.medioPago());
        } catch (RuntimeException e) {
            log.warn("Error al cobrar en la pasarela: {}", e.getMessage());
            cobro = new ResultadoPago(false, null, "La pasarela de pagos no esta disponible");
        }
        final ResultadoPago resultado = cobro;

        // Fase 3 (transaccion corta): emitir las entradas o liberar los lugares
        return tx.execute(status -> confirmarPago(inicio.compraId(), resultado));
    }

    private InicioPago iniciarPagoSeguro(Long usuarioId, Long reservaId, String clave, String medioPago) {
        try {
            return tx.execute(status -> iniciarPago(usuarioId, reservaId, clave, medioPago));
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            throw new ConflictException("Hay otro pago en proceso para esta reserva, reintente en unos segundos");
        }
    }

    private InicioPago iniciarPago(Long usuarioId, Long reservaId, String clave, String medioPago) {
        Reserva r = reservaRepo.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new NotFoundException("No existe la reserva " + reservaId));
        if (!r.getUsuarioId().equals(usuarioId)) {
            throw new ForbiddenException("La reserva no pertenece al usuario");
        }

        // Idempotencia: si la clave ya se uso, se devuelve el resultado anterior
        Optional<Compra> existente = compraRepo.findByClaveIdempotencia(clave);
        if (existente.isPresent()) {
            Compra c = existente.get();
            if (!c.getReserva().getId().equals(reservaId)) {
                throw new ConflictException("La clave de idempotencia ya se uso en otra operacion");
            }
            if (c.getEstado() == EstadoCompra.PROCESANDO) {
                throw new ConflictException("El pago de esta reserva esta en proceso");
            }
            return new InicioPago(c.getId(), false, false, c.getMonto());
        }

        if (r.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ConflictException("La reserva esta " + r.getEstado() + " y no se puede pagar");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (r.getVenceEn().isBefore(ahora)) {
            // Reserva vencida: se liberan los lugares y se informa al cliente (despues de confirmar la tx)
            liberacion.liberarLugares(r);
            r.setEstado(EstadoReserva.VENCIDA);
            return new InicioPago(null, false, true, null);
        }

        // EN_PAGO: mientras se cobra, nadie puede liberar estos lugares
        r.setEstado(EstadoReserva.EN_PAGO);
        Compra c = new Compra();
        c.setReserva(r);
        c.setClaveIdempotencia(clave);
        c.setMedioPago(medioPago);
        c.setMonto(r.getTotal());
        c.setEstado(EstadoCompra.PROCESANDO);
        c.setCreadaEn(ahora);
        compraRepo.saveAndFlush(c);
        return new InicioPago(c.getId(), true, false, r.getTotal());
    }

    private CompraDTO confirmarPago(Long compraId, ResultadoPago resultado) {
        Compra c = compraRepo.findById(compraId).orElseThrow();
        Reserva r = reservaRepo.findByIdForUpdate(c.getReserva().getId()).orElseThrow();
        c.setTransaccionPago(resultado.transaccionId());

        if (resultado.aprobado()) {
            c.setEstado(EstadoCompra.APROBADA);
            r.setEstado(EstadoReserva.CONFIRMADA);
            return compraDTO(c, emitirEntradas(c, r));
        }

        // Pago rechazado: la reserva se cancela y los lugares vuelven a estar disponibles
        c.setEstado(EstadoCompra.RECHAZADA);
        c.setMotivoRechazo(resultado.motivo());
        r.setEstado(EstadoReserva.CANCELADA);
        liberacion.liberarLugares(r);
        return compraDTO(c, List.of());
    }

    private List<Entrada> emitirEntradas(Compra c, Reserva r) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        // si el evento se cancelo mientras se pagaba, la entrada nace lista para reintegro
        EstadoEntrada estado = eventoCanceladoRepo.existsById(r.getEventoId())
                ? EstadoEntrada.A_REINTEGRAR
                : EstadoEntrada.VENDIDA;

        List<Entrada> entradas = new ArrayList<>();
        if (r.getTipoSector() == TipoSector.NUMERADO) {
            for (AsientoOcupado a : asientoRepo.findByReservaId(r.getId())) {
                entradas.add(nuevaEntrada(c, r, estado, a.getFila(), a.getNumero(), ahora));
            }
        } else {
            for (int i = 0; i < r.getCantidad(); i++) {
                entradas.add(nuevaEntrada(c, r, estado, null, null, ahora));
            }
        }
        return entradas;
    }

    private Entrada nuevaEntrada(Compra c, Reserva r, EstadoEntrada estado, Integer fila, Integer numero,
                                 LocalDateTime ahora) {
        Entrada e = new Entrada();
        e.setCodigoQr(UUID.randomUUID().toString());   // codigo QR unico
        e.setEventoId(r.getEventoId());
        e.setSectorId(r.getSectorId());
        e.setFila(fila);
        e.setNumero(numero);
        e.setUsuarioId(r.getUsuarioId());
        e.setPrecio(r.getPrecioUnitario());
        e.setEstado(estado);
        e.setCompra(c);
        e.setFechaEvento(r.getFechaEvento());
        e.setFechaCompra(ahora);
        return entradaRepo.save(e);
    }

    private CompraDTO compraDTO(Compra c) {
        return compraDTO(c, entradaRepo.findByCompraId(c.getId()));
    }

    private CompraDTO compraDTO(Compra c, List<Entrada> entradas) {
        return new CompraDTO(c.getId(), c.getReserva().getId(), c.getEstado(), c.getMonto(), c.getMedioPago(),
                c.getTransaccionPago(), c.getMotivoRechazo(), entradas.stream().map(Mapeos::toDTO).toList());
    }

    // =====================================================================
    // Consultar y cancelar una reserva
    // =====================================================================
    public ReservaDTO obtener(Long usuarioId, Long reservaId) {
        return tx.execute(status -> {
            Reserva r = reservaRepo.findById(reservaId)
                    .orElseThrow(() -> new NotFoundException("No existe la reserva " + reservaId));
            if (!r.getUsuarioId().equals(usuarioId)) {
                throw new ForbiddenException("La reserva no pertenece al usuario");
            }
            List<AsientoDTO> asientos = asientoRepo.findByReservaId(r.getId()).stream()
                    .map(a -> new AsientoDTO(a.getFila(), a.getNumero()))
                    .toList();
            return Mapeos.toDTO(r, asientos, LocalDateTime.now(clock));
        });
    }

    public void cancelar(Long usuarioId, Long reservaId) {
        tx.executeWithoutResult(status -> {
            Reserva r = reservaRepo.findByIdForUpdate(reservaId)
                    .orElseThrow(() -> new NotFoundException("No existe la reserva " + reservaId));
            if (!r.getUsuarioId().equals(usuarioId)) {
                throw new ForbiddenException("La reserva no pertenece al usuario");
            }
            if (r.getEstado() != EstadoReserva.PENDIENTE) {
                throw new ConflictException("Solo se puede cancelar una reserva PENDIENTE (esta " + r.getEstado() + ")");
            }
            liberacion.liberarLugares(r);
            r.setEstado(EstadoReserva.CANCELADA);
        });
    }
}
