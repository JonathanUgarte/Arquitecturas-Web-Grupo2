package com.ticketya.fila.service;

import com.ticketya.fila.client.CatalogoClient;
import com.ticketya.fila.config.FilaProperties;
import com.ticketya.fila.dto.EstadoEnFila;
import com.ticketya.fila.dto.EstadoFilaDTO;
import com.ticketya.fila.dto.FilaDTO;
import com.ticketya.fila.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

// (6) Fila virtual por tandas, sobre Redis (version simplificada).
//
// Claves de Redis por evento {e}:
//   fila:{e}:espera          SORTED SET  usuario -> numero de llegada (orden justo: el primero que llego, primero sale)
//   fila:{e}:contador        contador atomico (INCR) que da el numero de llegada, igual en todas las instancias
//   fila:{e}:turno:{u}       turno vigente del usuario (el token), con vencimiento automatico (TTL)
//   fila:{e}:habilitados     cuantos clientes se habilitaron en total
//   fila:{e}:lock-tanda      candado (SET NX con TTL) para que con varias instancias salga UNA tanda por intervalo
//   fila:{e}:con-fila        cache de "este evento usa fila y esta a la venta" (consulta a catalogo cada 30 s)
//   filas:activas            SET con los eventos que tienen gente esperando (los recorre el planificador)
//
// Por que Redis: todas las operaciones son atomicas y en memoria (soporta miles de ingresos por segundo), el
// sorted set da la posicion en O(log n), y los turnos vencen solos con el TTL. No hace falta persistir la fila.
@Service
@RequiredArgsConstructor
@Slf4j
public class FilaService {

    private final StringRedisTemplate redis;
    private final CatalogoClient catalogoClient;
    private final EmisorDeTurnos emisor;
    private final FilaProperties props;
    private final Clock clock;

    // ---------- cliente ----------

    public FilaDTO ingresar(Long eventoId, Long usuarioId) {
        verificarEventoConFila(eventoId);
        String usuario = String.valueOf(usuarioId);
        if (Boolean.TRUE.equals(redis.hasKey(claveTurno(eventoId, usuario)))) {
            return consultar(eventoId, usuarioId);   // ya esta habilitado: no pierde su turno
        }
        // ZADD NX: si ya estaba en la fila conserva su lugar original (entrar dos veces no lo adelanta ni lo atrasa)
        if (redis.opsForZSet().score(claveEspera(eventoId), usuario) == null) {
            Long numero = redis.opsForValue().increment(clave(eventoId, "contador"));
            redis.opsForZSet().addIfAbsent(claveEspera(eventoId), usuario, numero == null ? 0 : numero);
        }
        redis.opsForSet().add(CLAVE_ACTIVAS, String.valueOf(eventoId));
        return consultar(eventoId, usuarioId);
    }

    public FilaDTO consultar(Long eventoId, Long usuarioId) {
        String usuario = String.valueOf(usuarioId);
        String claveTurno = claveTurno(eventoId, usuario);
        String turno = redis.opsForValue().get(claveTurno);
        if (turno != null) {
            Long segundos = redis.getExpire(claveTurno);
            LocalDateTime vence = segundos == null || segundos < 0 ? null
                    : LocalDateTime.ofInstant(clock.instant().plusSeconds(segundos), ZoneId.systemDefault());
            return new FilaDTO(eventoId, EstadoEnFila.HABILITADO, null, 0L, 0L, turno, vence,
                    "Es tu turno: tenes " + props.getMinutosTurno() + " minutos para reservar. Envia el turno en el header X-Turno-Fila.");
        }
        Long rank = redis.opsForZSet().rank(claveEspera(eventoId), usuario);
        if (rank == null) {
            return new FilaDTO(eventoId, EstadoEnFila.FUERA_DE_FILA, null, null, null, null, null,
                    "No estas en la fila (o se vencio tu turno): ingresa de nuevo");
        }
        // Con 'rank' personas adelante, le toca en la tanda numero rank / tamanio (empezando en 0)
        long tandasAntes = rank / props.getTamanioTanda();
        long segundosEstimados = (tandasAntes + 1) * Math.max(1, props.getIntervaloMs() / 1000);
        return new FilaDTO(eventoId, EstadoEnFila.ESPERANDO, rank + 1, rank, segundosEstimados, null, null,
                "Estas en la fila: posicion " + (rank + 1) + ". Te habilitamos por orden de llegada.");
    }

    public void salir(Long eventoId, Long usuarioId) {
        String usuario = String.valueOf(usuarioId);
        redis.opsForZSet().remove(claveEspera(eventoId), usuario);
        redis.delete(claveTurno(eventoId, usuario));
    }

    // ---------- tandas ----------

    // Habilita a los proximos 'tamanioTanda' clientes en orden de llegada. Devuelve cuantos habilito.
    public int habilitarSiguienteTanda(Long eventoId) {
        // Candado: aunque haya varias instancias del servicio, sale una sola tanda por intervalo
        Boolean tomado = redis.opsForValue().setIfAbsent(clave(eventoId, "lock-tanda"), "1",
                Duration.ofMillis(Math.max(100, props.getIntervaloMs() - 100)));
        if (!Boolean.TRUE.equals(tomado)) {
            return 0;
        }
        // ZPOPMIN es atomico: cada cliente sale de la fila una sola vez, aunque dos procesos lo intenten a la vez
        Set<ZSetOperations.TypedTuple<String>> siguientes =
                redis.opsForZSet().popMin(claveEspera(eventoId), props.getTamanioTanda());
        int habilitados = 0;
        if (siguientes != null) {
            Instant ahora = clock.instant();
            Instant vence = ahora.plus(Duration.ofMinutes(props.getMinutosTurno()));
            for (ZSetOperations.TypedTuple<String> t : siguientes) {
                String usuario = t.getValue();
                String token = emisor.emitir(eventoId, Long.valueOf(usuario), ahora, vence);
                redis.opsForValue().set(claveTurno(eventoId, usuario), token, Duration.ofMinutes(props.getMinutosTurno()));
                habilitados++;
            }
        }
        if (habilitados > 0) {
            redis.opsForValue().increment(clave(eventoId, "habilitados"), habilitados);
            log.info("Fila del evento {}: tanda de {} cliente(s) habilitada", eventoId, habilitados);
        }
        Long quedan = redis.opsForZSet().zCard(claveEspera(eventoId));
        if (quedan == null || quedan == 0) {
            redis.opsForSet().remove(CLAVE_ACTIVAS, String.valueOf(eventoId));
        }
        return habilitados;
    }

    public Set<String> eventosConFilaActiva() {
        Set<String> activas = redis.opsForSet().members(CLAVE_ACTIVAS);
        return activas == null ? Set.of() : activas;
    }

    // ---------- organizador / administrador ----------

    public EstadoFilaDTO estado(Long eventoId) {
        Long enEspera = redis.opsForZSet().zCard(claveEspera(eventoId));
        String habilitados = redis.opsForValue().get(clave(eventoId, "habilitados"));
        return new EstadoFilaDTO(eventoId, enEspera == null ? 0 : enEspera,
                habilitados == null ? 0 : Long.parseLong(habilitados), props.getTamanioTanda(),
                props.getIntervaloMs() / 1000, props.getMinutosTurno());
    }

    // ---------- helpers ----------

    private void verificarEventoConFila(Long eventoId) {
        String cache = clave(eventoId, "con-fila");
        if (Boolean.TRUE.equals(redis.hasKey(cache))) {
            return;   // ya se verifico hace menos de 30 s: no se consulta a catalogo en cada ingreso
        }
        CatalogoClient.EventoFila e = catalogoClient.evento(eventoId);
        if (!e.filaVirtual()) {
            throw new ConflictException("El evento " + eventoId + " no usa fila virtual: se puede reservar directamente");
        }
        if (!"PUBLICADO".equals(e.estado())) {
            throw new ConflictException("El evento no esta a la venta (estado " + e.estado() + ")");
        }
        redis.opsForValue().set(cache, "1", Duration.ofSeconds(30));
    }

    static final String CLAVE_ACTIVAS = "filas:activas";

    private static String clave(Long eventoId, String sufijo) {
        return "fila:" + eventoId + ":" + sufijo;
    }

    private static String claveEspera(Long eventoId) {
        return clave(eventoId, "espera");
    }

    private static String claveTurno(Long eventoId, String usuario) {
        return clave(eventoId, "turno:" + usuario);
    }
}
