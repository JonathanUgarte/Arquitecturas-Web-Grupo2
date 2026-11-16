package com.ticketya.auth.dto;

import com.ticketya.auth.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Alta hecha por un administrador: puede elegir cualquier rol (organizador, personal de acceso, otro admin)
public record AltaUsuarioRequest(
        @Schema(example = "Productora Sur") @NotBlank String nombre,
        @Schema(example = "productora@sur.com") @NotBlank @Email String email,
        @Schema(example = "secreta123") @NotBlank @Size(min = 8, message = "debe tener al menos 8 caracteres") String password,
        @Schema(example = "ORGANIZADOR") @NotNull Rol rol) {
}
