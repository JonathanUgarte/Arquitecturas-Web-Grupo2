package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.EstadoReserva;
import com.ticketya.ventas.model.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Bloquea la fila: dos pagos de la misma reserva no pueden avanzar a la vez
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    // Entradas que el cliente ya "tiene tomadas" para un evento (reservas en curso no vencidas)
    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r " +
           "WHERE r.usuarioId = :usuarioId AND r.eventoId = :eventoId " +
           "AND (r.estado = :enPago OR (r.estado = :pendiente AND r.venceEn >= :ahora))")
    long cantidadActiva(@Param("usuarioId") Long usuarioId,
                        @Param("eventoId") Long eventoId,
                        @Param("enPago") EstadoReserva enPago,
                        @Param("pendiente") EstadoReserva pendiente,
                        @Param("ahora") LocalDateTime ahora);

    @Query("SELECT r FROM Reserva r " +
           "WHERE r.eventoId = :eventoId AND r.sectorId = :sectorId " +
           "AND r.estado = :estado AND r.venceEn < :limite")
    List<Reserva> findVencidas(@Param("eventoId") Long eventoId,
                               @Param("sectorId") Long sectorId,
                               @Param("estado") EstadoReserva estado,
                               @Param("limite") LocalDateTime limite);

    @Query("SELECT COALESCE(SUM(r.cantidad), 0) FROM Reserva r " +
           "WHERE r.eventoId = :eventoId AND r.sectorId = :sectorId " +
           "AND r.estado = :estado AND r.venceEn < :limite")
    long cantidadVencida(@Param("eventoId") Long eventoId,
                         @Param("sectorId") Long sectorId,
                         @Param("estado") EstadoReserva estado,
                         @Param("limite") LocalDateTime limite);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estado = :nuevo " +
           "WHERE r.eventoId = :eventoId AND r.sectorId = :sectorId " +
           "AND r.estado = :actual AND r.venceEn < :limite")
    int marcarVencidas(@Param("eventoId") Long eventoId,
                       @Param("sectorId") Long sectorId,
                       @Param("actual") EstadoReserva actual,
                       @Param("nuevo") EstadoReserva nuevo,
                       @Param("limite") LocalDateTime limite);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Reserva r SET r.estado = :nuevo " +
           "WHERE r.eventoId = :eventoId AND r.estado IN :estados")
    int cambiarEstadoDelEvento(@Param("eventoId") Long eventoId,
                               @Param("estados") Collection<EstadoReserva> estados,
                               @Param("nuevo") EstadoReserva nuevo);
}
