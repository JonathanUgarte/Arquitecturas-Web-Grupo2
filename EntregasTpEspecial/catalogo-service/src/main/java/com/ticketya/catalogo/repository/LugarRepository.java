package com.ticketya.catalogo.repository;

import com.ticketya.catalogo.model.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LugarRepository extends JpaRepository<Lugar, Long> {
}
