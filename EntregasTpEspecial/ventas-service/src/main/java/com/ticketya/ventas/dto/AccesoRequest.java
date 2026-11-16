package com.ticketya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Lo que envia el lector en la puerta: el evento donde esta trabajando y el codigo QR escaneado
public record AccesoRequest(
        @Schema(example = "1") @NotNull Long eventoId,
        @Schema(example = "3f6c2a1e-6b0a-4e7e-9a59-2f5b1c7d8e90") @NotBlank String codigoQr) {
}
