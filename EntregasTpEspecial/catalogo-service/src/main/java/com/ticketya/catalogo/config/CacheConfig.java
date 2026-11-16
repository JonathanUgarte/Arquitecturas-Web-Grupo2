package com.ticketya.catalogo.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.catalogo.dto.EventoDTO;
import com.ticketya.catalogo.dto.EventoResumenDTO;
import com.ticketya.catalogo.dto.LugarDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

// Cache del catalogo en Redis (compartida por todas las instancias del servicio).
//
// Que se cachea y por cuanto tiempo:
//   evento     detalle de un evento (GET /eventos/{id})            10 min  - se invalida al cambiarlo
//   busquedas  resultados de GET /eventos con sus filtros           60 s   - se invalida al publicar/suspender/cancelar/cambiar precios
//   lugares    listado de lugares                                  10 min  - se invalida al crear un lugar
// Son datos de LECTURA MASIVA (en una apertura de venta miles de clientes miran el mismo evento) que cambian poco.
//
// Que NO se cachea: la disponibilidad de asientos y el cupo (ventas), ni los datos que ventas pide para reservar
// (estado y precio del sector). Cambian a cada segundo durante una venta masiva: un dato viejo haria creer que hay
// lugar cuando no lo hay. La fuente de verdad para vender es siempre la base de ventas.
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String EVENTO = "evento";
    public static final String BUSQUEDAS = "busquedas";
    public static final String LUGARES = "lugares";

    // Solo se usa cuando spring.cache.type=redis (en los tests la cache es en memoria o esta apagada)
    @Bean
    public RedisCacheManagerBuilderCustomizer cachesDelCatalogo(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("catalogo:")
                .disableCachingNullValues();
        return builder -> builder
                .withCacheConfiguration(EVENTO, base.entryTtl(Duration.ofMinutes(10))
                        .serializeValuesWith(SerializationPair.fromSerializer(serializadorEvento(mapper))))
                .withCacheConfiguration(BUSQUEDAS, base.entryTtl(Duration.ofSeconds(60))
                        .serializeValuesWith(SerializationPair.fromSerializer(serializadorBusquedas(mapper))))
                .withCacheConfiguration(LUGARES, base.entryTtl(Duration.ofMinutes(10))
                        .serializeValuesWith(SerializationPair.fromSerializer(serializadorLugares(mapper))));
    }

    // Un serializador JSON por tipo de valor (sin informacion de clases en el JSON: mas liviano y seguro)
    public static RedisSerializer<EventoDTO> serializadorEvento(ObjectMapper mapper) {
        return new Jackson2JsonRedisSerializer<>(mapper, EventoDTO.class);
    }

    public static RedisSerializer<Object> serializadorBusquedas(ObjectMapper mapper) {
        JavaType tipo = mapper.getTypeFactory().constructCollectionType(List.class, EventoResumenDTO.class);
        return new Jackson2JsonRedisSerializer<>(mapper, tipo);
    }

    public static RedisSerializer<Object> serializadorLugares(ObjectMapper mapper) {
        JavaType tipo = mapper.getTypeFactory().constructCollectionType(List.class, LugarDTO.class);
        return new Jackson2JsonRedisSerializer<>(mapper, tipo);
    }

    // Si Redis no responde, la cache se saltea y se lee de la base: el catalogo sigue funcionando (mas lento)
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache '{}' no disponible al leer ({}): se consulta la base", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache '{}' no disponible al guardar: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache '{}' no disponible al invalidar: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache '{}' no disponible al limpiar: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
