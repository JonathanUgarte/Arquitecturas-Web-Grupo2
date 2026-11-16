package com.ticketya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DevolucionRequest(
        @Schema(example = "1") @NotNull Long entradaId,
        @Schema(example = "No puedo asistir") @NotBlank String motivo
) {
}
