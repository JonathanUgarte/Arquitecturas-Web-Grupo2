package com.ticketya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReservaRequest(
        @Schema(example = "1") @NotNull Long eventoId,
        @Schema(example = "2") @NotNull Long sectorId,
        @Schema(description = "Asientos a reservar; solo para sectores NUMERADO") @Valid List<AsientoRequest> asientos,
        @Schema(description = "Cantidad de entradas; solo para sectores NO_NUMERADO", example = "2") Integer cantidad
) {
}
