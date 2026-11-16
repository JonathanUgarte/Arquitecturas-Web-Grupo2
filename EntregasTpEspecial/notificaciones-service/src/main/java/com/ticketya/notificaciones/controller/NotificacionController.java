package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.NotificacionDTO;
import com.ticketya.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notificaciones", description = "Avisos recibidos por el usuario")
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Mis notificaciones (cualquier usuario autenticado)",
            description = "Avisos enviados al usuario, del mas reciente al mas antiguo.")
    @ApiResponse(responseCode = "200", description = "Notificaciones del usuario")
    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> misNotificaciones(
            @Parameter(description = "Id del usuario (lo completa el gateway a partir del JWT)", example = "1")
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listar(usuarioId));
    }
}
