package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.Devolucion;
import com.ticketya.ventas.model.EstadoDevolucion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Devolucion d WHERE d.id = :id")
    Optional<Devolucion> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEntradaIdAndEstado(Long entradaId, EstadoDevolucion estado);

    List<Devolucion> findByEstadoOrderBySolicitadaEnAsc(EstadoDevolucion estado);
}
