package com.ticketya.notificaciones.config;

import com.ticketya.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// Recuperacion automatica: los avisos que quedaron FALLIDA (el canal de envio no respondio) se reenvian
// periodicamente, hasta NotificacionService.MAX_INTENTOS. En los tests esta apagado.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "notificaciones.reintentos.habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReintentosProgramados {

    private final NotificacionService notificacionService;

    @Scheduled(fixedDelayString = "${notificaciones.reintentos.intervalo-ms}",
            initialDelayString = "${notificaciones.reintentos.intervalo-ms}")
    public void avisosFallidos() {
        notificacionService.reintentarFallidas();
    }
}
