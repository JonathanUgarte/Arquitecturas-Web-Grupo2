package com.ticketya.auth.dto;

import com.ticketya.auth.model.Rol;

// Respuesta del login: el token se envia luego en el header "Authorization: Bearer <token>"
public record TokenDTO(String token, String tipo, long expiraEnSegundos, Long usuarioId, String nombre, Rol rol) {
}
