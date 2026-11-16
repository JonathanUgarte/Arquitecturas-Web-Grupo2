package com.ticketya.notificaciones.canal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Proveedor de email simulado: escribe el aviso en el log en lugar de enviarlo (simplificacion del TP)
@Component
@Slf4j
public class EmailSimulado implements CanalDeEnvio {

    @Override
    public String nombre() {
        return "EMAIL";
    }

    @Override
    public void enviar(Long usuarioId, String titulo, String mensaje) {
        log.info("[EMAIL] usuario={} | {} | {}", usuarioId, titulo, mensaje);
    }
}
