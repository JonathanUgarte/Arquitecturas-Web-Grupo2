package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record EventoRequest(
        @Schema(example = "Recital de Verano") @NotBlank String nombre,
        @Schema(example = "Show de rock en el estadio") String descripcion,
        @Schema(example = "RECITAL") @NotNull TipoEvento tipo,
        @Schema(example = "Productora Sur") @NotBlank String organizador,
        @Schema(description = "Artistas, equipos o disertantes", example = "[\"Banda Uno\", \"Banda Dos\"]")
        List<String> participantes,
        @Schema(example = "2027-01-15T21:00:00") @NotNull @Future LocalDateTime fechaHora,
        @Schema(description = "Id del lugar donde se realiza", example = "1") @NotNull Long lugarId
) {
}
