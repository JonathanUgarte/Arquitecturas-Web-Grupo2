package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.Entrada;
import com.ticketya.ventas.model.EstadoEntrada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Entrada e WHERE e.id = :id")
    Optional<Entrada> findByIdForUpdate(@Param("id") Long id);

    List<Entrada> findByUsuarioIdOrderByFechaCompraDesc(Long usuarioId);

    List<Entrada> findByCompraId(Long compraId);

    @Query("SELECT COUNT(e) FROM Entrada e " +
           "WHERE e.usuarioId = :usuarioId AND e.eventoId = :eventoId AND e.estado = :estado")
    long contarPorUsuarioEventoEstado(@Param("usuarioId") Long usuarioId,
                                      @Param("eventoId") Long eventoId,
                                      @Param("estado") EstadoEntrada estado);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Entrada e SET e.estado = :nuevo WHERE e.eventoId = :eventoId AND e.estado = :actual")
    int cambiarEstadoDelEvento(@Param("eventoId") Long eventoId,
                               @Param("actual") EstadoEntrada actual,
                               @Param("nuevo") EstadoEntrada nuevo);
}
