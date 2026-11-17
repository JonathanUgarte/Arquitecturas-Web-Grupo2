package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketya.ventas.model.EstadoCompra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByClaveIdempotencia(String claveIdempotencia);

    List<Compra> findByEstadoAndCreadaEnBefore(EstadoCompra estado, LocalDateTime limite);
}
