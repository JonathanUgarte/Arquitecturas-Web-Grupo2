package com.ticketya.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CancelacionRequest(@Schema(example = "Mal tiempo") @NotBlank String motivo) {
}
