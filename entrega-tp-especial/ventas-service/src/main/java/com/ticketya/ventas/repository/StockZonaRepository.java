package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.StockZona;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockZonaRepository extends JpaRepository<StockZona, Long> {

    Optional<StockZona> findByEventoIdAndSectorId(Long eventoId, Long sectorId);

    // Alternativa X (pesimista): la transaccion "toma" la zona y las demas esperan su turno
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockZona s WHERE s.eventoId = :eventoId AND s.sectorId = :sectorId")
    Optional<StockZona> findForUpdate(@Param("eventoId") Long eventoId, @Param("sectorId") Long sectorId);
}
