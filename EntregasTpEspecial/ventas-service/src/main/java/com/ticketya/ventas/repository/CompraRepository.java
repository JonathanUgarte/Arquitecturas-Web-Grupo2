package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByClaveIdempotencia(String claveIdempotencia);
}
