package com.ticketya.catalogo;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import com.ticketya.catalogo.repository.EventoRepository;
import com.ticketya.catalogo.repository.LugarRepository;
import com.ticketya.catalogo.repository.PrecioSectorRepository;
import com.ticketya.catalogo.service.CancelacionService;
import com.ticketya.catalogo.service.EventoService;
import com.ticketya.catalogo.service.LugarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Consistencia eventual: si ventas no recibe el aviso de cancelacion, queda pendiente y se reenvia despues
@SpringBootTest
@ActiveProfiles("test")
class CancelacionPendienteTest {

    @MockitoBean VentasClient ventasClient;

    @Autowired LugarService lugarService;
    @Autowired EventoService eventoService;
    @Autowired CancelacionService cancelacionService;
    @Autowired EventoRepository eventoRepo;
    @Autowired PrecioSectorRepository precioRepo;
    @Autowired LugarRepository lugarRepo;

    private Long eventoId;

    @BeforeEach
    void preparar() {
        precioRepo.deleteAll();
        eventoRepo.deleteAll();
        lugarRepo.deleteAll();
        LugarDTO lugar = lugarService.crear(new LugarRequest("Estadio", "Calle 1", "Tandil", -37.3, -59.1,
                List.of(new SectorRequest("Campo", TipoSector.NO_NUMERADO, 100, null, null))));
        eventoId = eventoService.crear(new EventoRequest("Recital", "x", TipoEvento.RECITAL, "Prod", List.of("Banda"),
                LocalDateTime.now().plusDays(10), lugar.id(), null), 3L).id();
    }

    @Test
    void unAvisoNoRecibidoQuedaPendienteYSeReenviaHastaQueVentasLoConfirme() {
        when(ventasClient.notificarCancelacion(eventoId)).thenReturn(false);

        CancelacionDTO cancelacion = cancelacionService.cancelar(eventoId, "Lluvia");
        assertFalse(cancelacion.reintegroNotificado());
        assertEquals(List.of(eventoId), eventoService.eventosConAvisoCancelacionPendiente());

        // ventas sigue caido: sigue pendiente
        assertEquals(0, cancelacionService.reintentarAvisosPendientes());
        assertEquals(List.of(eventoId), eventoService.eventosConAvisoCancelacionPendiente());

        // ventas vuelve: se confirma y deja de estar pendiente
        when(ventasClient.notificarCancelacion(eventoId)).thenReturn(true);
        assertEquals(1, cancelacionService.reintentarAvisosPendientes());
        assertTrue(eventoService.eventosConAvisoCancelacionPendiente().isEmpty());
        verify(ventasClient, times(3)).notificarCancelacion(eventoId);
    }

    @Test
    void unAvisoRecibidoNoQuedaPendiente() {
        when(ventasClient.notificarCancelacion(eventoId)).thenReturn(true);

        cancelacionService.cancelar(eventoId, "Lluvia");

        assertTrue(eventoService.eventosConAvisoCancelacionPendiente().isEmpty());
    }
}
