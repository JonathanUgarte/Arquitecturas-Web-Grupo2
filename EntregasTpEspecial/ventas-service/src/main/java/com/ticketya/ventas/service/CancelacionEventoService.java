package com.ticketya.ventas.service;

import com.ticketya.ventas.trazabilidad.Bitacora;
import com.ticketya.ventas.dto.CancelacionEventoDTO;
import com.ticketya.ventas.mensajeria.EventoCanceladoEvento;
import com.ticketya.ventas.model.EstadoEntrada;
import com.ticketya.ventas.model.EstadoReserva;
import com.ticketya.ventas.model.EventoCancelado;
import com.ticketya.ventas.repository.AsientoOcupadoRepository;
import com.ticketya.ventas.repository.EntradaRepository;
import com.ticketya.ventas.repository.EventoCanceladoRepository;
import com.ticketya.ventas.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// Lo llama el servicio de catalogo cuando se cancela un evento: todas las entradas vendidas pasan
// a A_REINTEGRAR y las reservas pendientes se liberan. Todo en una unica transaccion (atomico).
// Es idempotente: si catalogo reintenta el aviso, la segunda vez no hay nada que cambiar.
// Despues del commit publica "evento.cancelado" para que notificaciones avise a los compradores.
@Service
@RequiredArgsConstructor
public class CancelacionEventoService {

    private final EntradaRepository entradaRepo;
    private final ReservaRepository reservaRepo;
    private final AsientoOcupadoRepository asientoRepo;
    private final EventoCanceladoRepository eventoCanceladoRepo;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public CancelacionEventoDTO cancelarEvento(Long eventoId) {
        if (!eventoCanceladoRepo.existsById(eventoId)) {
            EventoCancelado marca = new EventoCancelado();
            marca.setEventoId(eventoId);
            marca.setCanceladoEn(LocalDateTime.now(clock));
            eventoCanceladoRepo.save(marca);
        }

        List<EstadoReserva> pendientes = List.of(EstadoReserva.PENDIENTE);
        asientoRepo.eliminarDeReservasDelEvento(eventoId, pendientes);
        int reservas = reservaRepo.cambiarEstadoDelEvento(eventoId, pendientes, EstadoReserva.CANCELADA);

        List<Long> compradores = entradaRepo.usuariosConEntradasEnEstado(eventoId, EstadoEntrada.VENDIDA);
        int entradas = entradaRepo.cambiarEstadoDelEvento(eventoId, EstadoEntrada.VENDIDA, EstadoEntrada.A_REINTEGRAR);

        // Aviso asincronico a los compradores (se envia despues del commit). Si catalogo reintenta y ya no
        // quedan entradas vendidas, no se vuelve a avisar.
        if (entradas > 0) {
            eventos.publishEvent(new EventoCanceladoEvento(eventoId, compradores, entradas, LocalDateTime.now(clock)));
        }
        Bitacora.registrar("CANCELACION_PROCESADA", "evento=" + eventoId + " entradasAReintegrar=" + entradas
                + " reservasCanceladas=" + reservas);
        return new CancelacionEventoDTO(eventoId, entradas, reservas);
    }
}
