package com.ticketya.ventas.config;

import com.ticketya.ventas.service.DevolucionService;
import com.ticketya.ventas.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// Tareas periodicas de recuperacion (consistencia eventual): lo que no se pudo hacer porque un servicio externo
// estaba caido se reintenta mas tarde, sin intervencion manual.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ventas.reintentos.habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReintentosProgramados {

    private final DevolucionService devolucionService;
    private final ReservaService reservaService;

    // Reintegros de devoluciones aprobadas que la pasarela no acepto en su momento
    @Scheduled(fixedDelayString = "${ventas.reintentos.intervalo-ms}", initialDelayString = "${ventas.reintentos.intervalo-ms}")
    public void reintegrosPendientes() {
        devolucionService.reintentarReintegrosPendientes();
    }

    // Pagos que quedaron a medias porque ventas se cayo mientras cobraba (reservas trabadas EN_PAGO)
    @Scheduled(fixedDelayString = "${ventas.reintentos.intervalo-ms}", initialDelayString = "${ventas.reintentos.intervalo-ms}")
    public void pagosColgados() {
        reservaService.recuperarPagosColgados();
    }
}
