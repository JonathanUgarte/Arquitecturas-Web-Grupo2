package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SectorRequest(
        @Schema(example = "Campo") @NotBlank String nombre,
        @Schema(description = "NUMERADO (filas y asientos) o NO_NUMERADO (zona con cupo)", example = "NO_NUMERADO")
        @NotNull TipoSector tipo,
        @Schema(description = "Cupo maximo; solo para sectores NO_NUMERADO", example = "5000") Integer capacidad,
        @Schema(description = "Cantidad de filas; solo para sectores NUMERADO", example = "5") Integer filas,
        @Schema(description = "Asientos por fila; solo para sectores NUMERADO", example = "10") Integer asientosPorFila
) {
}
