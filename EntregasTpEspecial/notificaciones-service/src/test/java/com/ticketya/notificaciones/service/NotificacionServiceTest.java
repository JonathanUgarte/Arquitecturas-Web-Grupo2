package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.canal.CanalDeEnvio;
import com.ticketya.notificaciones.client.CatalogoClient;
import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.client.VentasClient;
import com.ticketya.notificaciones.exception.ServicioNoDisponibleException;
import com.ticketya.notificaciones.mensajeria.CompraConfirmadaEvento;
import com.ticketya.notificaciones.mensajeria.EventoCanceladoEvento;
import com.ticketya.notificaciones.mensajeria.EventoPublicadoEvento;
import com.ticketya.notificaciones.mensajeria.EventoSuspendidoEvento;
import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tests unitarios de la logica de avisos (repositorio, canal y otros servicios simulados con Mockito)
class NotificacionServiceTest {

    private NotificacionRepository repo;
    private CanalDeEnvio canal;
    private CatalogoClient catalogo;
    private PreferenciasClient preferencias;
    private VentasClient ventas;
    private NotificacionService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(NotificacionRepository.class);
        canal = mock(CanalDeEnvio.class);
        catalogo = mock(CatalogoClient.class);
        preferencias = mock(PreferenciasClient.class);
        ventas = mock(VentasClient.class);
        when(canal.nombre()).thenReturn("EMAIL");
        when(catalogo.nombreEvento(anyLong())).thenReturn(Optional.of("Recital de Prueba"));
        Clock reloj = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        servicio = new NotificacionService(repo, canal, catalogo, preferencias, ventas, reloj);
    }

    @Test
    void unaCompraConfirmadaEnviaYRegistraUnAviso() {
        servicio.compraConfirmada(new CompraConfirmadaEvento(12L, 1L, 5L, 2, new BigDecimal("60000"), LocalDateTime.now()));

        verify(canal).enviar(eq(1L), eq("Compra confirmada"), contains("\"Recital de Prueba\""));
        Notificacion guardada = guardada();
        assertEquals(TipoNotificacion.COMPRA_CONFIRMADA, guardada.getTipo());
        assertEquals(EstadoNotificacion.ENVIADA, guardada.getEstado());
        assertEquals("COMPRA_CONFIRMADA:compra:12:1", guardada.getClaveIdempotencia());
        assertTrue(guardada.getMensaje().contains("2 entradas"));
    }

    @Test
    void unMensajeRepetidoNoGeneraUnSegundoAviso() {
        when(repo.existsByClaveIdempotencia("COMPRA_CONFIRMADA:compra:12:1")).thenReturn(true);

        servicio.compraConfirmada(new CompraConfirmadaEvento(12L, 1L, 5L, 1, BigDecimal.TEN, LocalDateTime.now()));

        verify(canal, never()).enviar(anyLong(), anyString(), anyString());
        verify(repo, never()).save(any());
    }

    @Test
    void siElCatalogoNoRespondeElMensajeUsaElIdDelEvento() {
        when(catalogo.nombreEvento(5L)).thenReturn(Optional.empty());

        servicio.compraConfirmada(new CompraConfirmadaEvento(12L, 1L, 5L, 1, BigDecimal.TEN, LocalDateTime.now()));

        assertTrue(guardada().getMensaje().contains("#5"));
    }

    @Test
    void laCancelacionAvisaACadaComprador() {
        servicio.eventoCancelado(new EventoCanceladoEvento(5L, List.of(1L, 2L, 3L), 4, LocalDateTime.now()));

        verify(canal, times(3)).enviar(anyLong(), eq("Evento cancelado"), contains("reintegro"));
        verify(repo, times(3)).save(any());
    }

    @Test
    void laSuspensionAvisaACadaCompradorQueInformaVentas() {
        when(ventas.compradores(5L)).thenReturn(List.of(1L, 2L));

        servicio.eventoSuspendido(new EventoSuspendidoEvento(5L, "Recital de Prueba",
                LocalDateTime.of(2027, 1, 15, 21, 0), LocalDateTime.now()));

        verify(canal).enviar(eq(1L), eq("Evento suspendido"), contains("\"Recital de Prueba\" (previsto para el 15/01/2027 21:00)"));
        verify(canal).enviar(eq(2L), eq("Evento suspendido"), contains("siguen siendo validas"));
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo, times(2)).save(captor.capture());
        assertEquals(TipoNotificacion.EVENTO_SUSPENDIDO, captor.getValue().getTipo());
        assertEquals("EVENTO_SUSPENDIDO:evento:5:2", captor.getValue().getClaveIdempotencia());
    }

    @Test
    void siVentasNoRespondeLaSuspensionSeReintentaPorRabbit() {
        when(ventas.compradores(5L)).thenThrow(new ServicioNoDisponibleException("ventas caido"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.eventoSuspendido(
                new EventoSuspendidoEvento(5L, "Recital de Prueba", null, LocalDateTime.now())));
        verify(canal, never()).enviar(anyLong(), anyString(), anyString());
    }

    @Test
    void unaSuspensionRepetidaNoDuplicaAvisos() {
        when(ventas.compradores(5L)).thenReturn(List.of(1L));
        when(repo.existsByClaveIdempotencia("EVENTO_SUSPENDIDO:evento:5:1")).thenReturn(true);

        servicio.eventoSuspendido(new EventoSuspendidoEvento(5L, "Recital de Prueba", null, LocalDateTime.now()));

        verify(canal, never()).enviar(anyLong(), anyString(), anyString());
    }

    @Test
    void unEventoPublicadoAvisaSoloALosInteresadosQueDevuelvePreferencias() {
        when(preferencias.interesados("Tandil", List.of("Banda Uno"))).thenReturn(List.of(4L, 9L));

        servicio.eventoPublicado(new EventoPublicadoEvento(7L, "Gira 2027", "RECITAL", List.of("Banda Uno"), "Tandil",
                LocalDateTime.of(2027, 2, 1, 21, 0)));

        verify(canal).enviar(eq(4L), anyString(), contains("Banda Uno se presenta en Tandil el 01/02/2027 21:00"));
        verify(canal).enviar(eq(9L), anyString(), anyString());
        verify(repo, times(2)).save(any());
    }

    @Test
    void siPreferenciasNoRespondeSePropagaElErrorParaQueRabbitReintente() {
        when(preferencias.interesados(anyString(), anyList()))
                .thenThrow(new ServicioNoDisponibleException("caido"));

        assertThrows(ServicioNoDisponibleException.class, () -> servicio.eventoPublicado(
                new EventoPublicadoEvento(7L, "Gira", "RECITAL", List.of("Banda Uno"), "Tandil", null)));
        verify(repo, never()).save(any());
    }

    @Test
    void siElCanalFallaElAvisoQuedaRegistradoComoFallido() {
        doThrow(new RuntimeException("SMTP caido")).when(canal).enviar(anyLong(), anyString(), anyString());

        servicio.compraConfirmada(new CompraConfirmadaEvento(12L, 1L, 5L, 1, BigDecimal.TEN, LocalDateTime.now()));

        Notificacion guardada = guardada();
        assertEquals(EstadoNotificacion.FALLIDA, guardada.getEstado());
        assertEquals("SMTP caido", guardada.getDetalleError());
    }

    private Notificacion guardada() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    // ---- reenvio de avisos FALLIDA ----

    private Notificacion fallida(int intentos) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(1L);
        n.setTitulo("Compra confirmada");
        n.setMensaje("Tu compra #12 fue confirmada");
        n.setClaveIdempotencia("COMPRA_CONFIRMADA:compra:12:1");
        n.setEstado(EstadoNotificacion.FALLIDA);
        n.setDetalleError("Canal caido");
        n.setIntentos(intentos);
        return n;
    }

    @Test
    void unAvisoFallidoSeReenviaCuandoElCanalVuelve() {
        Notificacion n = fallida(1);
        when(repo.findByEstadoAndIntentosLessThan(EstadoNotificacion.FALLIDA, NotificacionService.MAX_INTENTOS))
                .thenReturn(List.of(n));

        assertEquals(1, servicio.reintentarFallidas());

        verify(canal).enviar(1L, "Compra confirmada", "Tu compra #12 fue confirmada");
        verify(repo).save(n);
        assertEquals(EstadoNotificacion.ENVIADA, n.getEstado());
        assertEquals(2, n.getIntentos());
        assertNull(n.getDetalleError());
    }

    @Test
    void siElCanalSigueCaidoQuedaFallidaYSumaElIntento() {
        Notificacion n = fallida(MAX_MENOS_UNO);
        when(repo.findByEstadoAndIntentosLessThan(EstadoNotificacion.FALLIDA, NotificacionService.MAX_INTENTOS))
                .thenReturn(List.of(n));
        doThrow(new RuntimeException("Canal caido")).when(canal).enviar(anyLong(), anyString(), anyString());

        assertEquals(0, servicio.reintentarFallidas());

        assertEquals(EstadoNotificacion.FALLIDA, n.getEstado());
        assertEquals(NotificacionService.MAX_INTENTOS, n.getIntentos());   // ya no se vuelve a buscar
        verify(repo).save(n);
    }

    @Test
    void elPrimerEnvioCuentaComoIntento() {
        servicio.compraConfirmada(new CompraConfirmadaEvento(12L, 1L, 5L, 1, BigDecimal.TEN, LocalDateTime.now()));
        assertEquals(1, guardada().getIntentos());
    }

    private static final int MAX_MENOS_UNO = NotificacionService.MAX_INTENTOS - 1;
}
