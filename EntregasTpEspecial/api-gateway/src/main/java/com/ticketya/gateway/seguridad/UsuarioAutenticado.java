package com.ticketya.gateway.seguridad;

// Lo que el gateway sabe del usuario despues de validar su JWT
public record UsuarioAutenticado(Long id, String rol) {
}
