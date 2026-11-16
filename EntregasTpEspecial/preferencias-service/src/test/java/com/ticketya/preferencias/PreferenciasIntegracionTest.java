package com.ticketya.preferencias;

import com.ticketya.preferencias.client.CatalogoClient;
import com.ticketya.preferencias.client.EventoCatalogo;
import com.ticketya.preferencias.client.EventoDetalle;
import com.ticketya.preferencias.dto.PerfilDTO;
import com.ticketya.preferencias.dto.PreferenciasRequest;
import com.ticketya.preferencias.dto.RecomendacionDTO;
import com.ticketya.preferencias.exception.BadRequestException;
import com.ticketya.preferencias.mensajeria.CompraConfirmadaEvento;
import com.ticketya.preferencias.repository.PerfilRepository;
import com.ticketya.preferencias.service.PerfilService;
import com.ticketya.preferencias.service.RecomendacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// Prueba de integracion contra un MongoDB REAL (la misma imagen que usa docker-compose), levantado con
// Testcontainers. Si la maquina no tiene Docker, estos tests se saltean (no fallan).
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PreferenciasIntegracionTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.11");

    @MockitoBean
    CatalogoClient catalogoClient;

    @Autowired PerfilService perfilService;
    @Autowired RecomendacionService recomendacionService;
    @Autowired PerfilRepository perfilRepo;

    private final LocalDateTime fecha = LocalDateTime.of(2027, 3, 1, 21, 0);

    @BeforeEach
    void limpiar() {
        perfilRepo.deleteAll();
        when(catalogoClient.detalle(anyLong())).thenAnswer(inv -> Optional.of(new EventoDetalle(
                inv.getArgument(0), "Evento " + inv.getArgument(0), "RECITAL", List.of("Banda Uno"), fecha,
                new EventoDetalle.Lugar("Tandil"))));
    }

    @Test
    void guardaYDevuelveLasPreferencias() {
        perfilService.actualizar(1L, new PreferenciasRequest(" Tandil ", Set.of("recital"), Set.of("Banda Uno")));

        PerfilDTO perfil = perfilService.obtener(1L);
        assertEquals("Tandil", perfil.ciudad());
        assertEquals(Set.of("RECITAL"), perfil.tiposFavoritos());
        assertEquals(Set.of("Banda Uno"), perfil.artistasFavoritos());
    }

    @Test
    void rechazaUnTipoDeEventoInexistente() {
        assertThrows(BadRequestException.class,
                () -> perfilService.actualizar(1L, new PreferenciasRequest(null, Set.of("CIRCO"), null)));
    }

    @Test
    void unMensajeDeCompraRepetidoSeRegistraUnaSolaVez() {
        CompraConfirmadaEvento compra = compra(100L, 1L, 10L);

        perfilService.registrarCompra(compra);
        perfilService.registrarCompra(compra);   // RabbitMQ puede entregar el mismo mensaje dos veces

        PerfilDTO perfil = perfilService.obtener(1L);
        assertEquals(1, perfil.historial().size());
        assertEquals("RECITAL", perfil.historial().get(0).tipo());
        assertEquals("Tandil", perfil.historial().get(0).ciudad());
    }

    @Test
    void siElCatalogoNoRespondeLaCompraSeRegistraIgual() {
        when(catalogoClient.detalle(anyLong())).thenReturn(Optional.empty());

        perfilService.registrarCompra(compra(100L, 1L, 10L));

        PerfilDTO perfil = perfilService.obtener(1L);
        assertEquals(1, perfil.historial().size());
        assertNull(perfil.historial().get(0).tipo());
    }

    @Test
    void recomiendaLoQueCompraronOtrosClientesConGustosEnComun() {
        // el cliente 1 compro el evento 10; los clientes 2 y 3 tambien, y ademas compraron el 20; el 3 compro el 30
        perfilService.registrarCompra(compra(1L, 1L, 10L));
        perfilService.registrarCompra(compra(2L, 2L, 10L));
        perfilService.registrarCompra(compra(3L, 2L, 20L));
        perfilService.registrarCompra(compra(4L, 3L, 10L));
        perfilService.registrarCompra(compra(5L, 3L, 20L));
        perfilService.registrarCompra(compra(6L, 3L, 30L));
        perfilService.registrarCompra(compra(7L, 4L, 40L));   // otro cliente sin nada en comun
        when(catalogoClient.eventosALaVenta()).thenReturn(List.of(
                enVenta(10L), enVenta(20L), enVenta(30L), enVenta(40L)));

        List<RecomendacionDTO> r = recomendacionService.recomendar(1L, 10);

        assertEquals(20L, r.get(0).eventoId());   // lo compraron 2 clientes con gustos en comun
        assertEquals(30L, r.get(1).eventoId());   // lo compro 1
        assertTrue(r.stream().noneMatch(x -> x.eventoId().equals(10L)), "no recomienda lo ya comprado");
    }

    @Test
    void encuentraALosInteresadosPorArtistaFavoritoYCiudad() {
        perfilService.actualizar(1L, new PreferenciasRequest("Tandil", null, Set.of("Banda Uno")));
        perfilService.actualizar(2L, new PreferenciasRequest("Azul", null, Set.of("Banda Uno")));      // otra ciudad
        perfilService.actualizar(3L, new PreferenciasRequest("tandil", null, Set.of("Otra Banda")));   // otro artista
        perfilService.actualizar(4L, new PreferenciasRequest("TANDIL", null, Set.of("banda uno", "Banda Dos")));

        assertEquals(List.of(1L, 4L), perfilService.interesados("Tandil", List.of("BANDA UNO", "Banda Tres")));
    }

    private CompraConfirmadaEvento compra(Long compraId, Long usuarioId, Long eventoId) {
        return new CompraConfirmadaEvento(compraId, usuarioId, eventoId, 1, new BigDecimal("15000"), fecha);
    }

    private EventoCatalogo enVenta(Long id) {
        return new EventoCatalogo(id, "Evento " + id, "TEATRO", List.of(), fecha.plusDays(id), "Azul",
                new BigDecimal("10000"));
    }
}
