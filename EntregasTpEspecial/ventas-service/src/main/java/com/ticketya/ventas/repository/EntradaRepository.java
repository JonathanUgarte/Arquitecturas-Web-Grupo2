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

    @Query("SELECT DISTINCT e.usuarioId FROM Entrada e WHERE e.eventoId = :eventoId AND e.estado = :estado")
    List<Long> usuariosConEntradasEnEstado(@Param("eventoId") Long eventoId,
                                           @Param("estado") EstadoEntrada estado);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    // Control de acceso: marca la entrada como usada SOLO si es de ese evento y sigue vendida.
    // Es una unica sentencia atomica: si dos puertas escanean el mismo QR a la vez, la base deja
    // actualizar a una sola (la otra encuentra 0 filas porque el estado ya no es VENDIDA).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Entrada e SET e.estado = :usada, e.fechaUso = :ahora " +
           "WHERE e.codigoQr = :codigoQr AND e.eventoId = :eventoId AND e.estado = :vendida")
    int marcarComoUsada(@Param("codigoQr") String codigoQr,
                        @Param("eventoId") Long eventoId,
                        @Param("vendida") EstadoEntrada vendida,
                        @Param("usada") EstadoEntrada usada,
                        @Param("ahora") java.time.LocalDateTime ahora);

    // Reportes: cantidad y recaudacion por sector y estado de un evento
    @Query("SELECT e.sectorId, e.estado, COUNT(e), SUM(e.precio) FROM Entrada e " +
           "WHERE e.eventoId = :eventoId GROUP BY e.sectorId, e.estado")
    List<Object[]> resumenPorSectorYEstado(@Param("eventoId") Long eventoId);
}
