package com.ticketya.catalogo.repository;

import com.ticketya.catalogo.model.PrecioSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PrecioSectorRepository extends JpaRepository<PrecioSector, Long> {

    List<PrecioSector> findByEventoId(Long eventoId);

    Optional<PrecioSector> findByEventoIdAndSectorId(Long eventoId, Long sectorId);

    void deleteByEventoId(Long eventoId);

    @Query("SELECT MIN(p.precio) FROM PrecioSector p WHERE p.evento.id = :eventoId")
    BigDecimal precioMinimo(@Param("eventoId") Long eventoId);
}
