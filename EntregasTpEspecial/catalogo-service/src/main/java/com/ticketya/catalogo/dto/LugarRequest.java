package com.ticketya.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LugarRequest(
        @Schema(example = "Estadio Municipal") @NotBlank String nombre,
        @Schema(example = "Av. Rivadavia 1000") @NotBlank String direccion,
        @Schema(example = "Tandil") @NotBlank String ciudad,
        @Schema(example = "-37.32") Double latitud,
        @Schema(example = "-59.13") Double longitud,
        @NotEmpty @Valid List<SectorRequest> sectores
) {
}
