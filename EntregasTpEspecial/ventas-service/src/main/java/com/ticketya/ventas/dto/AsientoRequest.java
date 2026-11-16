package com.ticketya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AsientoRequest(
        @Schema(example = "3") @NotNull Integer fila,
        @Schema(example = "5") @NotNull Integer numero
) {
}
