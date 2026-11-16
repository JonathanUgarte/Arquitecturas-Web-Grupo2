package com.ticketya.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Registro publico: siempre crea un usuario con rol CLIENTE
public record RegistroRequest(
        @Schema(example = "Ana Perez") @NotBlank String nombre,
        @Schema(example = "ana@mail.com") @NotBlank @Email String email,
        @Schema(example = "secreta123") @NotBlank @Size(min = 8, message = "debe tener al menos 8 caracteres") String password) {
}
