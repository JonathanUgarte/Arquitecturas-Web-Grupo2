package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.AsientoOcupado;
import com.ticketya.ventas.model.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AsientoOcupadoRepository extends JpaRepository<AsientoOcupado, Long> {

    List<AsientoOcupado> findByReservaId(Long reservaId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AsientoOcupado a WHERE a.reserva.id = :reservaId")
    int eliminarDeReserva(@Param("reservaId") Long reservaId);

    // Libera los asientos de las reservas que ya figuran como vencidas
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AsientoOcupado a WHERE a.eventoId = :eventoId AND a.sectorId = :sectorId " +
           "AND a.reserva.id IN (SELECT r.id FROM Reserva r WHERE r.estado = :estado)")
    int eliminarDeReservasEnEstado(@Param("eventoId") Long eventoId,
                                   @Param("sectorId") Long sectorId,
                                   @Param("estado") EstadoReserva estado);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AsientoOcupado a WHERE a.reserva.id IN " +
           "(SELECT r.id FROM Reserva r WHERE r.eventoId = :eventoId AND r.estado IN :estados)")
    int eliminarDeReservasDelEvento(@Param("eventoId") Long eventoId,
                                    @Param("estados") Collection<EstadoReserva> estados);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AsientoOcupado a WHERE a.eventoId = :eventoId AND a.sectorId = :sectorId " +
           "AND a.fila = :fila AND a.numero = :numero")
    int eliminarAsiento(@Param("eventoId") Long eventoId,
                        @Param("sectorId") Long sectorId,
                        @Param("fila") int fila,
                        @Param("numero") int numero);

    // Asientos ocupados que se muestran como tomados (se excluyen reservas vencidas sin liberar)
    @Query("SELECT a FROM AsientoOcupado a WHERE a.eventoId = :eventoId AND a.sectorId = :sectorId " +
           "AND NOT (a.reserva.estado = :pendiente AND a.reserva.venceEn < :limite) " +
           "ORDER BY a.fila, a.numero")
    List<AsientoOcupado> ocupados(@Param("eventoId") Long eventoId,
                                  @Param("sectorId") Long sectorId,
                                  @Param("pendiente") EstadoReserva pendiente,
                                  @Param("limite") LocalDateTime limite);
}
