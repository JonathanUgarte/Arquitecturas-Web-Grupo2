package com.ticketya.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreciosRequest(@NotEmpty @Valid List<PrecioRequest> precios) {
}
