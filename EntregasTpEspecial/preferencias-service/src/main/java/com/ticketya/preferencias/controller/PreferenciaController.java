package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.PerfilDTO;
import com.ticketya.preferencias.dto.PreferenciasRequest;
import com.ticketya.preferencias.dto.RecomendacionDTO;
import com.ticketya.preferencias.exception.BadRequestException;
import com.ticketya.preferencias.service.PerfilService;
import com.ticketya.preferencias.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Preferencias y recomendaciones", description = "Preferencias del cliente (ciudad, tipos de evento y artistas favoritos) y eventos sugeridos")
@RestController
@RequiredArgsConstructor
public class PreferenciaController {

    private final PerfilService perfilService;
    private final RecomendacionService recomendacionService;

    @Operation(summary = "Ver mis preferencias y mi historial de compras (cliente)")
    @ApiResponse(responseCode = "200", description = "Perfil del cliente (vacio si todavia no cargo nada)")
    @ApiResponse(responseCode = "403", description = "No es cliente")
    @GetMapping("/preferencias")
    public ResponseEntity<PerfilDTO> obtener(
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        Seguridad.exigirCliente(rol);
        return ResponseEntity.ok(perfilService.obtener(usuarioId));
    }

    @Operation(summary = "Actualizar mis preferencias (cliente)",
            description = "Los campos que no se envian (null) quedan como estaban. Una lista vacia borra esa preferencia.")
    @ApiResponse(responseCode = "200", description = "Preferencias guardadas")
    @ApiResponse(responseCode = "400", description = "Tipo de evento invalido")
    @ApiResponse(responseCode = "403", description = "No es cliente")
    @ApiResponse(responseCode = "409", description = "El perfil se modifico al mismo tiempo, reintentar")
    @PutMapping("/preferencias")
    public ResponseEntity<PerfilDTO> actualizar(
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @RequestBody PreferenciasRequest req) {
        Seguridad.exigirCliente(rol);
        return ResponseEntity.ok(perfilService.actualizar(usuarioId, req));
    }

    @Operation(summary = "Eventos recomendados para mi (cliente)",
            description = "Combina 'quienes compraron lo mismo que vos tambien compraron...', artistas y tipos favoritos, "
                    + "tipos comprados antes y ciudad. Cada recomendacion explica sus motivos. Sin datos, sugiere los "
                    + "proximos eventos a la venta.")
    @ApiResponse(responseCode = "200", description = "Eventos recomendados, del mas al menos relevante")
    @ApiResponse(responseCode = "403", description = "No es cliente")
    @ApiResponse(responseCode = "503", description = "El catalogo no responde")
    @GetMapping("/recomendaciones")
    public ResponseEntity<List<RecomendacionDTO>> recomendar(
            @Parameter(description = "Id del cliente (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(description = "Rol (lo completa el gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Parameter(description = "Cantidad maxima de recomendaciones (1 a 50)", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        Seguridad.exigirCliente(rol);
        if (limite < 1 || limite > 50) {
            throw new BadRequestException("El limite debe estar entre 1 y 50");
        }
        return ResponseEntity.ok(recomendacionService.recomendar(usuarioId, limite));
    }
}
