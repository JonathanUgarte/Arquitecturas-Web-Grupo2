package com.ticketya.fila;

import com.ticketya.fila.client.CatalogoClient;
import com.ticketya.fila.dto.EstadoEnFila;
import com.ticketya.fila.dto.FilaDTO;
import com.ticketya.fila.exception.ConflictException;
import com.ticketya.fila.service.FilaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// (6) Fila virtual contra un Redis REAL (Testcontainers). Tandas de 2 clientes (application-test.properties).
// Si la maquina no tiene Docker, estos tests se saltean (no fallan).
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FilaServiceTest {

    private static final String SECRETO = "secreto-de-turnos-para-los-tests-de-al-menos-32-caracteres";
    private static final Long EVENTO = 1L;

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @MockitoBean CatalogoClient catalogoClient;

    @Autowired FilaService filaService;
    @Autowired StringRedisTemplate redisTemplate;

    @BeforeEach
    void limpiar() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        when(catalogoClient.evento(anyLong())).thenAnswer(inv ->
                new CatalogoClient.EventoFila(inv.getArgument(0), "Recital masivo", "PUBLICADO", true));
    }

    @Test
    void laPosicionRespetaElOrdenDeLlegada() {
        assertEquals(1L, filaService.ingresar(EVENTO, 10L).posicion());
        assertEquals(2L, filaService.ingresar(EVENTO, 20L).posicion());
        FilaDTO tercero = filaService.ingresar(EVENTO, 30L);
        assertEquals(EstadoEnFila.ESPERANDO, tercero.estado());
        assertEquals(3L, tercero.posicion());
        assertEquals(2L, tercero.personasAdelante());
    }

    @Test
    void entrarDosVecesNoCambiaElLugar() {
        filaService.ingresar(EVENTO, 10L);
        filaService.ingresar(EVENTO, 20L);

        assertEquals(1L, filaService.ingresar(EVENTO, 10L).posicion());
        assertEquals(2L, filaService.consultar(EVENTO, 20L).posicion());
    }

    @Test
    void cadaTandaHabilitaALosPrimerosEnLlegar() {
        filaService.ingresar(EVENTO, 10L);
        filaService.ingresar(EVENTO, 20L);
        filaService.ingresar(EVENTO, 30L);

        assertEquals(2, filaService.habilitarSiguienteTanda(EVENTO));

        assertEquals(EstadoEnFila.HABILITADO, filaService.consultar(EVENTO, 10L).estado());
        assertEquals(EstadoEnFila.HABILITADO, filaService.consultar(EVENTO, 20L).estado());
        FilaDTO tercero = filaService.consultar(EVENTO, 30L);
        assertEquals(EstadoEnFila.ESPERANDO, tercero.estado());
        assertEquals(1L, tercero.posicion());   // avanzo al primer lugar
    }

    @Test
    void noSaleMasDeUnaTandaPorIntervaloAunqueSeDispareDosVeces() {
        for (long u = 1; u <= 5; u++) {
            filaService.ingresar(EVENTO, u);
        }
        assertEquals(2, filaService.habilitarSiguienteTanda(EVENTO));
        assertEquals(0, filaService.habilitarSiguienteTanda(EVENTO));   // otra instancia en el mismo intervalo
        assertEquals(3, filaService.estado(EVENTO).enEspera());
        assertEquals(2, filaService.estado(EVENTO).habilitadosEnTotal());
    }

    @Test
    void elTurnoEsUnTokenFirmadoConElUsuarioYElEvento() {
        filaService.ingresar(EVENTO, 10L);
        filaService.habilitarSiguienteTanda(EVENTO);

        FilaDTO habilitado = filaService.consultar(EVENTO, 10L);
        assertNotNull(habilitado.turnoVenceEn());
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(habilitado.turno())
                .getPayload();
        assertEquals("10", claims.getSubject());
        assertEquals(EVENTO, claims.get("evento", Long.class));
    }

    @Test
    void unClienteHabilitadoQueVuelveAEntrarConservaSuTurno() {
        filaService.ingresar(EVENTO, 10L);
        filaService.habilitarSiguienteTanda(EVENTO);

        assertEquals(EstadoEnFila.HABILITADO, filaService.ingresar(EVENTO, 10L).estado());
    }

    @Test
    void alSalirDeLaFilaQuedaFuera() {
        filaService.ingresar(EVENTO, 10L);
        filaService.salir(EVENTO, 10L);

        assertEquals(EstadoEnFila.FUERA_DE_FILA, filaService.consultar(EVENTO, 10L).estado());
    }

    @Test
    void unEventoSinFilaVirtualNoAdmiteIngresos() {
        when(catalogoClient.evento(2L)).thenReturn(new CatalogoClient.EventoFila(2L, "Teatro", "PUBLICADO", false));

        assertThrows(ConflictException.class, () -> filaService.ingresar(2L, 10L));
    }

    @Test
    void unEventoQueNoEstaALaVentaNoAdmiteIngresos() {
        when(catalogoClient.evento(3L)).thenReturn(new CatalogoClient.EventoFila(3L, "Show", "BORRADOR", true));

        assertThrows(ConflictException.class, () -> filaService.ingresar(3L, 10L));
    }
}
