package com.ticketya.catalogo.repository;

import java.util.List;

import com.ticketya.catalogo.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    List<Evento> findByOrganizadorIdOrderByFechaHoraAsc(Long organizadorId);

    List<Evento> findAllByOrderByFechaHoraAsc();

    List<Evento> findByAvisoCancelacionPendienteTrue();
}
