package com.ticketya.ventas.repository;

import com.ticketya.ventas.model.RegistroAcceso;
import com.ticketya.ventas.model.ResultadoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    List<RegistroAcceso> findByEventoIdOrderByFechaDesc(Long eventoId);

    long countByCodigoQrAndResultado(String codigoQr, ResultadoAcceso resultado);
}
