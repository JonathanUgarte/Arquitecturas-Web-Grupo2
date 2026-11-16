package com.ticketya.notificaciones.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// Registro de cada aviso enviado (bitacora de notificaciones del cliente)
@Document("notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    private String id;

    @Indexed
    private Long usuarioId;

    private TipoNotificacion tipo;
    private String canal;
    private String titulo;
    private String mensaje;

    // Que la origino: "compra:12", "evento:3"
    private String referencia;

    // tipo + referencia + usuario. Unica: si RabbitMQ entrega el mismo mensaje dos veces, no se avisa dos veces.
    @Indexed(unique = true)
    private String claveIdempotencia;

    private EstadoNotificacion estado;
    private String detalleError;

    // Intentos de envio realizados (el primero + los reenvios automaticos de los FALLIDA)
    private int intentos;
    private LocalDateTime fecha;
}
