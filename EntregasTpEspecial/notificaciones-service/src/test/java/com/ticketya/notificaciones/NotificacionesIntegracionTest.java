package com.ticketya.notificaciones;

import com.ticketya.notificaciones.client.CatalogoClient;
import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.mensajeria.CompraConfirmadaEvento;
import com.ticketya.notificaciones.mensajeria.EventoPublicadoEvento;
import com.ticketya.notificaciones.mensajeria.MensajeriaConfig;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

// Prueba de integracion de punta a punta del flujo asincronico, con RabbitMQ y MongoDB reales (Testcontainers):
// se publica un mensaje en el exchange, como lo haria ventas, y se verifica que el aviso quede registrado.
// Si la maquina no tiene Docker, estos tests se saltean (no fallan).
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class NotificacionesIntegracionTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.11");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @MockitoBean CatalogoClient catalogoClient;
    @MockitoBean PreferenciasClient preferenciasClient;

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired NotificacionRepository repo;

    @BeforeEach
    void limpiar() {
        repo.deleteAll();
        when(catalogoClient.nombreEvento(anyLong())).thenReturn(Optional.of("Recital de Prueba"));
    }

    @Test
    void unaCompraPublicadaEnRabbitTerminaEnUnAvisoAlCliente() throws InterruptedException {
        rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, "compra.confirmada",
                new CompraConfirmadaEvento(50L, 1L, 1L, 2, new BigDecimal("30000"), LocalDateTime.now()));

        List<Notificacion> avisos = esperar(() -> repo.findByUsuarioIdOrderByFechaDesc(1L), 1);
        assertEquals(TipoNotificacion.COMPRA_CONFIRMADA, avisos.get(0).getTipo());
        assertTrue(avisos.get(0).getMensaje().contains("Recital de Prueba"));
    }

    @Test
    void elMismoMensajeEntregadoDosVecesGeneraUnSoloAviso() throws InterruptedException {
        CompraConfirmadaEvento compra = new CompraConfirmadaEvento(51L, 2L, 1L, 1, BigDecimal.TEN, LocalDateTime.now());
        rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, "compra.confirmada", compra);
        rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, "compra.confirmada", compra);

        esperar(() -> repo.findByUsuarioIdOrderByFechaDesc(2L), 1);
        Thread.sleep(1000);   // margen para que se procese el duplicado
        assertEquals(1, repo.findByUsuarioIdOrderByFechaDesc(2L).size());
    }

    @Test
    void unEventoPublicadoAvisaALosFansDeLaCiudad() throws InterruptedException {
        when(preferenciasClient.interesados(anyString(), anyList())).thenReturn(List.of(7L, 8L));

        rabbitTemplate.convertAndSend(MensajeriaConfig.EXCHANGE, "evento.publicado",
                new EventoPublicadoEvento(3L, "Gira 2027", "RECITAL", List.of("Banda Uno"), "Tandil",
                        LocalDateTime.of(2027, 2, 1, 21, 0)));

        esperar(() -> repo.findByUsuarioIdOrderByFechaDesc(7L), 1);
        esperar(() -> repo.findByUsuarioIdOrderByFechaDesc(8L), 1);
    }

    // El consumo es asincronico: se espera (hasta 10 segundos) a que aparezcan los avisos
    private static <T> List<T> esperar(Supplier<List<T>> consulta, int cantidad) throws InterruptedException {
        long limite = System.currentTimeMillis() + 10_000;
        List<T> resultado = consulta.get();
        while (resultado.size() < cantidad && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
            resultado = consulta.get();
        }
        assertEquals(cantidad, resultado.size(), "no llegaron los avisos esperados a tiempo");
        return resultado;
    }
}
