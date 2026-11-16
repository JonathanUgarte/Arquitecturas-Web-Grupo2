package com.ticketya.auth.dto;

import com.ticketya.auth.model.Rol;

import java.time.LocalDateTime;

public record UsuarioDTO(Long id, String nombre, String email, Rol rol, LocalDateTime fechaAlta) {
}
