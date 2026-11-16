package com.ticketya.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PrecioRequest(
        @Schema(example = "1") @NotNull Long sectorId,
        @Schema(example = "15000") @NotNull @Positive BigDecimal precio
) {
}
