package com.ticketya.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PagoRequest(
        @Schema(description = "Medio de pago. La pasarela simulada rechaza los que contienen 'RECHAZ' "
                + "(por ejemplo TARJETA_RECHAZADA) y aprueba el resto", example = "TARJETA")
        @NotBlank String medioPago
) {
}
