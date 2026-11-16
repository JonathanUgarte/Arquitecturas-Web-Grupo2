package com.ticketya.catalogo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketya.catalogo.config.CacheConfig;
import com.ticketya.catalogo.dto.*;
import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.TipoEvento;
import com.ticketya.catalogo.model.TipoSector;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Lo que se guarda en Redis se tiene que poder leer igual (fechas, listas, records anidados).
// Test unitario: no necesita Redis, prueba los serializadores que usa la cache.
class SerializacionCacheTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void elDetalleDeUnEventoIdaYVuelta() {
        LugarDTO lugar = new LugarDTO(1L, "Estadio", "Calle 1", "Tandil", -37.3, -59.1,
                List.of(new SectorDTO(1L, "Campo", TipoSector.NO_NUMERADO, 100, null, null, 100)));
        EventoDTO evento = new EventoDTO(5L, "Recital", "desc", TipoEvento.RECITAL, "Prod", List.of("Banda"),
                LocalDateTime.of(2027, 5, 10, 21, 0), EstadoEvento.PUBLICADO, lugar,
                List.of(new PrecioSectorDTO(1L, "Campo", TipoSector.NO_NUMERADO, new BigDecimal("20000"))), 3L, true);

        RedisSerializer<EventoDTO> s = CacheConfig.serializadorEvento(mapper);

        assertEquals(evento, s.deserialize(s.serialize(evento)));
    }

    @Test
    void unaListaInmutableDeResultadosIdaYVuelta() {
        // Stream.toList() devuelve una lista inmutable: tiene que poder guardarse y leerse igual
        List<EventoResumenDTO> resultados = Stream.of(
                new EventoResumenDTO(1L, "A", TipoEvento.RECITAL, "Prod", List.of("X"), LocalDateTime.of(2027, 1, 1, 21, 0),
                        EstadoEvento.PUBLICADO, "Estadio", "Tandil", new BigDecimal("1000")),
                new EventoResumenDTO(2L, "B", TipoEvento.TEATRO, "Prod", List.of(), LocalDateTime.of(2027, 2, 1, 20, 0),
                        EstadoEvento.PUBLICADO, "Teatro", "Azul", null)).toList();

        RedisSerializer<Object> s = CacheConfig.serializadorBusquedas(mapper);

        assertEquals(resultados, s.deserialize(s.serialize(resultados)));
    }
}
