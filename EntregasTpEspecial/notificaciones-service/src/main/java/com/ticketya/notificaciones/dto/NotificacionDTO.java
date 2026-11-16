package com.ticketya.notificaciones.dto;

import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionDTO(String id, TipoNotificacion tipo, String canal, String titulo, String mensaje,
                              String referencia, EstadoNotificacion estado, LocalDateTime fecha) {
}
