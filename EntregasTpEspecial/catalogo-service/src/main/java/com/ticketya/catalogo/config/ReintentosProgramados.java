package com.ticketya.catalogo.config;

import com.ticketya.catalogo.service.CancelacionService;
import com.ticketya.catalogo.service.EventoService;
import com.ticketya.catalogo.trazabilidad.Bitacora;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// Tareas periodicas: reenvio de los avisos de cancelacion que ventas no recibio, y finalizacion de eventos pasados
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "catalogo.reintentos.habilitado", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ReintentosProgramados {

    private final CancelacionService cancelacionService;
    private final EventoService eventoService;

    @Scheduled(fixedDelayString = "${catalogo.reintentos.intervalo-ms}", initialDelayString = "${catalogo.reintentos.intervalo-ms}")
    public void avisosDeCancelacionPendientes() {
        cancelacionService.reintentarAvisosPendientes();
    }

    // Los eventos que ya ocurrieron pasan a FINALIZADO (dejan de aparecer a la venta)
    @Scheduled(fixedDelayString = "${catalogo.finalizacion.intervalo-ms}", initialDelayString = "${catalogo.finalizacion.intervalo-ms}")
    public void finalizarEventosPasados() {
        int finalizados = eventoService.finalizarEventosPasados();
        if (finalizados > 0) {
            Bitacora.registrar("EVENTOS_FINALIZADOS", "cantidad=" + finalizados);
        }
    }
}
