package com.ticketya.catalogo.controller;

import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.exception.ForbiddenException;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.service.CancelacionService;
import com.ticketya.catalogo.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Eventos", description = "Alta, precios por sector, publicacion, suspension, cancelacion y busqueda de eventos")
@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;
    private final CancelacionService cancelacionService;

    @Operation(summary = "Crear un evento (queda en estado BORRADOR)")
    @ApiResponse(responseCode = "201", description = "Evento creado en BORRADOR")
    @ApiResponse(responseCode = "400", description = "Faltan datos o la fecha no es futura")
    @ApiResponse(responseCode = "404", description = "No existe el lugar")
    @PostMapping
    public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crear(req));
    }

    @Operation(summary = "Buscar eventos por multiples criterios",
            description = "Devuelve solo eventos a la venta (PUBLICADO o AGOTADO). Todos los filtros son opcionales y se combinan. "
                    + "Orden: 'fecha' (por defecto) o 'precio' (por el precio mas bajo).")
    @ApiResponse(responseCode = "200", description = "Lista de eventos que cumplen los criterios")
    @ApiResponse(responseCode = "400", description = "Orden o filtro con formato invalido")
    @GetMapping
    public ResponseEntity<List<EventoResumenDTO>> buscar(
            @Parameter(description = "Tipo de evento", example = "RECITAL")
            @RequestParam(required = false) TipoEvento tipo,
            @Parameter(description = "Nombre (o parte) del organizador", example = "Productora")
            @RequestParam(required = false) String organizador,
            @Parameter(description = "Nombre (o parte) de un artista, equipo o disertante", example = "Banda")
            @RequestParam(required = false) String participante,
            @Parameter(description = "Ciudad del lugar", example = "Tandil")
            @RequestParam(required = false) String ciudad,
            @Parameter(description = "Fecha desde (ISO)", example = "2026-12-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha hasta (ISO)", example = "2027-03-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "fecha o precio", example = "precio")
            @RequestParam(defaultValue = "fecha") String orden) {
        return ResponseEntity.ok(eventoService.buscar(tipo, organizador, participante, ciudad, desde, hasta, orden));
    }

    @Operation(summary = "Obtener el detalle de un evento (lugar, sectores y precios)")
    @ApiResponse(responseCode = "200", description = "Evento encontrado")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtener(id));
    }

    @Operation(summary = "Definir el precio de cada sector del evento",
            description = "Solo mientras el evento esta en BORRADOR. Reemplaza los precios anteriores.")
    @ApiResponse(responseCode = "200", description = "Precios configurados")
    @ApiResponse(responseCode = "400", description = "Un sector no pertenece al lugar del evento o esta repetido")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @ApiResponse(responseCode = "409", description = "El evento no esta en BORRADOR")
    @PutMapping("/{id}/precios")
    public ResponseEntity<EventoDTO> configurarPrecios(@PathVariable Long id,
                                                        @Valid @RequestBody PreciosRequest req) {
        return ResponseEntity.ok(eventoService.configurarPrecios(id, req));
    }

    @Operation(summary = "Publicar un evento (BORRADOR -> PUBLICADO)",
            description = "Requiere que todos los sectores del lugar tengan precio.")
    @ApiResponse(responseCode = "200", description = "Evento publicado")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @ApiResponse(responseCode = "409", description = "No esta en BORRADOR o faltan precios")
    @PostMapping("/{id}/publicacion")
    public ResponseEntity<EventoDTO> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.publicar(id));
    }

    @Operation(summary = "Suspender un evento (administrador)")
    @ApiResponse(responseCode = "200", description = "Evento suspendido")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @ApiResponse(responseCode = "409", description = "El evento no esta PUBLICADO ni AGOTADO")
    @PutMapping("/{id}/suspension")
    public ResponseEntity<EventoDTO> suspender(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario (sin autenticacion en la entrega 1)", example = "ADMIN")
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        exigirAdministrador(rol);
        return ResponseEntity.ok(eventoService.suspender(id));
    }

    @Operation(summary = "Cancelar un evento (administrador)",
            description = "El evento pasa a CANCELADO y se avisa a ventas para que sus entradas vendidas pasen a reintegro. "
                    + "Si ventas no responde, el evento igual queda cancelado y reintegroNotificado vale false.")
    @ApiResponse(responseCode = "200", description = "Evento cancelado")
    @ApiResponse(responseCode = "403", description = "No es administrador")
    @ApiResponse(responseCode = "404", description = "No existe el evento")
    @ApiResponse(responseCode = "409", description = "El evento ya estaba cancelado o finalizado")
    @PutMapping("/{id}/cancelacion")
    public ResponseEntity<CancelacionDTO> cancelar(
            @PathVariable Long id,
            @Parameter(description = "Rol del usuario (sin autenticacion en la entrega 1)", example = "ADMIN")
            @RequestHeader(value = "X-Rol", required = false) String rol,
            @Valid @RequestBody CancelacionRequest req) {
        exigirAdministrador(rol);
        return ResponseEntity.ok(cancelacionService.cancelar(id, req.motivo()));
    }

    // Sin autenticacion en esta entrega: el rol viaja en el header X-Rol (JWT llega en la entrega 2)
    private void exigirAdministrador(String rol) {
        if (!"ADMIN".equalsIgnoreCase(rol)) {
            throw new ForbiddenException("Esta accion requiere el header X-Rol: ADMIN");
        }
    }
}
