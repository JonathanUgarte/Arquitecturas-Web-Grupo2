package com.ticketya.catalogo.config;

import com.ticketya.catalogo.service.CancelacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// Recuperacion automatica: los avisos de cancelacion que ventas no recibio se reenvian periodicamente
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "catalogo.reintentos.habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReintentosProgramados {

    private final CancelacionService cancelacionService;

    @Scheduled(fixedDelayString = "${catalogo.reintentos.intervalo-ms}", initialDelayString = "${catalogo.reintentos.intervalo-ms}")
    public void avisosDeCancelacionPendientes() {
        cancelacionService.reintentarAvisosPendientes();
    }
}
