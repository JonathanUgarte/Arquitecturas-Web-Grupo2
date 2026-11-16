# TicketYa - Arquitectura de microservicios

## 1. Microservicios y responsabilidades

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| **api-gateway** | 8080 | Punto de entrada único. Valida el JWT y los roles (entrega 2), enruta a los servicios, asigna un `X-Correlation-Id` a cada pedido y registra método, ruta, usuario, código de respuesta y tiempo. No publica las rutas `/interno/**`. | - |
| **auth-service** | 8083 | Usuarios y roles: registro de clientes, alta de usuarios con rol (admin) y login, que emite el JWT. *(entrega 2)* | `auth` |
| **preferencias-service** | 8084 | Preferencias de cada cliente (ciudad, tipos y artistas favoritos), historial de compras y recomendaciones de eventos. *(entrega 2)* | MongoDB `preferencias` |
| **fila-service** | 8086 | Fila virtual para eventos de alta demanda: posición por orden de llegada, habilitación por tandas y turno firmado para reservar. *(entrega 2)* | Redis |
| **notificaciones-service** | 8085 | Avisos a los clientes (email simulado): compra confirmada, evento cancelado y artista favorito en su ciudad. Se activa por mensajes de RabbitMQ. *(entrega 2)* | MongoDB `notificaciones` |
| **catalogo-service** | 8081 | Lugares, sectores y eventos: alta, precios por sector, publicación, suspensión, cancelación y búsqueda por múltiples criterios. Es el dueño de la verdad sobre qué se vende y a qué precio. | `catalogo` |
| **ventas-service** | 8082 | Reserva temporal de lugares, compra y pago (pasarela simulada), entradas con QR, devoluciones y cancelación de entradas. Garantiza que una entrada no se venda dos veces. | `ventas` |

**Por qué esta división:** catálogo se lee mucho y cambia poco (candidato a caché en la entrega 2), mientras que ventas
recibe mucha escritura concurrente sobre los mismos recursos. Separarlos permite escalar y proteger ventas de forma
independiente, y deja un lugar natural para sumar servicios en la entrega 2 (notificaciones, fila virtual,
recomendaciones y reportes).

Cada servicio tiene su propia base de datos y **no accede a la del otro**: lo que necesita lo pide por HTTP.
En Docker: auth, catálogo y ventas usan un PostgreSQL 16 cada uno (sin Docker, H2); preferencias y notificaciones
usan MongoDB, cada uno con su propia base dentro de la misma instancia. Los servicios se avisan cosas en forma
asincrónica a través de **RabbitMQ** (sección 11).

## 2. Diagrama de módulos

```mermaid
flowchart LR
    subgraph Clientes["Sistemas externos"]
        APP["Apps de clientes y organizadores"]
        BO["Backoffice administrativo"]
        LEC["Lectores de acceso (QR)"]
    end

    GW["api-gateway :8080<br/>valida JWT y roles"]

    subgraph AUT["auth-service :8083"]
        AC["Controllers"] --> AS["Services + JwtService"] --> AR["Repositories"]
    end

    subgraph CAT["catalogo-service :8081"]
        CC["Controllers"] --> CS["Services"] --> CR["Repositories"]
    end
    subgraph VEN["ventas-service :8082"]
        VC["Controllers"] --> VS["Services"] --> VR["Repositories"]
        VCL["CatalogoClient"]
        PAS["PasarelaPagoMock"]
    end

    subgraph PRE["preferencias-service :8084"]
        PC["Controllers"] --> PS["Services + Recomendador"] --> PR["Repositories"]
    end
    subgraph NOT["notificaciones-service :8085"]
        NC["Controller + Consumidores"] --> NS["NotificacionService"] --> NR["Repositories"]
        NS --> MAIL["EmailSimulado"]
    end

    subgraph FIL["fila-service :8086"]
        FC["Controller"] --> FS["FilaService + Planificador de tandas"]
    end
    RD[("Redis")]

    MQ{{"RabbitMQ<br/>exchange ticketya.eventos"}}
    MGP[("MongoDB preferencias")]
    MGN[("MongoDB notificaciones")]
    DBA[("PostgreSQL auth")]
    DBC[("PostgreSQL catalogo")]
    DBV[("PostgreSQL ventas")]

    APP --> GW
    BO --> GW
    LEC -.->|"entrega 2"| GW
    GW -->|"/auth"| AC
    GW -->|"/lugares, /eventos"| CC
    GW -->|"/reservas, /entradas, /devoluciones, /disponibilidad"| VC
    AR --> DBA
    CR --> DBC
    VR --> DBV
    VS --> VCL
    VCL -->|"GET /interno/eventos/{e}/sectores/{s}"| CC
    VS --> PAS
    CS -->|"POST /interno/eventos/{e}/cancelacion"| VC
    GW -->|"/preferencias, /recomendaciones"| PC
    GW -->|"/notificaciones"| NC
    GW -->|"/filas"| FC
    FS --> RD
    FS -->|"GET /eventos/{e}"| CC
    PR --> MGP
    NR --> MGN
    VS -.->|"compra.confirmada, evento.cancelado"| MQ
    CS -.->|"evento.publicado"| MQ
    MQ -.-> PC
    MQ -.-> NC
    NS -->|"GET /interno/interesados"| PC
```

## 3. API pública (a través del gateway, `http://localhost:8080`)

| Servicio | Endpoints |
|---|---|
| auth | `POST /auth/registro`, `POST /auth/login`, `POST/GET /auth/usuarios` |
| catalogo | `POST/GET /lugares`, `GET /lugares/{id}`, `POST/GET /eventos`, `GET /eventos/{id}`, `PUT /eventos/{id}/precios`, `POST /eventos/{id}/publicacion`, `PUT /eventos/{id}/suspension`, `PUT /eventos/{id}/cancelacion`, `GET /eventos/mis-eventos`, `GET /eventos/{id}/reporte` |
| preferencias | `GET/PUT /preferencias`, `GET /recomendaciones` |
| notificaciones | `GET /notificaciones` |
| fila | `POST/DELETE /filas/{e}/ingreso`, `GET /filas/{e}/turno`, `GET /filas/{e}/estado` |
| ventas | `POST /reservas`, `GET/DELETE /reservas/{id}`, `POST /reservas/{id}/pago`, `GET /entradas`, `GET /entradas/{id}`, `POST/GET /devoluciones`, `PUT /devoluciones/{id}/aprobacion`, `PUT /devoluciones/{id}/rechazo`, `GET /disponibilidad/eventos/{e}/sectores/{s}`, `POST /accesos` |

Detalle con ejemplos en [05-api-endpoints.md](05-api-endpoints.md) y en los OpenAPI de cada servicio.

**API interna** (solo entre servicios, el gateway no la publica):

| Quién llama | Endpoint | Para qué |
|---|---|---|
| ventas -> catalogo | `GET /interno/eventos/{eventoId}/sectores/{sectorId}` | Obtener estado del evento, fecha, capacidad y precio del sector. |
| catalogo -> ventas | `POST /interno/eventos/{eventoId}/cancelacion` | Avisar la cancelación para pasar las entradas a reintegro. |
| notificaciones -> preferencias | `GET /interno/interesados?ciudad=&artistas=` | Clientes de esa ciudad con alguno de esos artistas como favorito. |
| preferencias, notificaciones, fila -> catalogo | `GET /eventos`, `GET /eventos/{id}` (públicos) | Eventos a la venta (candidatos a recomendar), nombre/tipo/ciudad de un evento y si usa fila virtual. |
| catalogo -> ventas | `GET /interno/eventos/{eventoId}/estadisticas` | Ventas por sector para el reporte del organizador. |

## 4. Cómo se garantiza que una entrada no se venda dos veces

El control está **en la base de datos**, no en la memoria de una instancia. Por eso funciona aunque haya varias
instancias de ventas ejecutándose al mismo tiempo.

| Caso | Mecanismo |
|---|---|
| **Asiento numerado** | Restricción de unicidad sobre `(evento, sector, fila, número)` en `asiento_ocupado`. Si dos clientes intentan tomar el mismo asiento, la base rechaza al segundo y su transacción se deshace por completo. |
| **Zona sin asiento (cupo)** | Una fila de cupo por zona (`stock_zona`) que se modifica con bloqueo pesimista (`SELECT ... FOR UPDATE`): las compras esperan su turno y el cupo no puede quedar negativo. Es la alternativa "pesimista" indicada para mucha competencia por el mismo cupo. |
| **Reintento del cliente** | Cada pago lleva un `Idempotency-Key` único guardado con restricción de unicidad. Repetir el pago devuelve el resultado anterior sin cobrar otra vez. |
| **Pago en curso** | Al comenzar el cobro la reserva pasa a `EN_PAGO`: mientras tanto nadie puede liberar sus lugares aunque su plazo esté por vencer. |
| **Reservas vencidas** | Se liberan al reservar (los lugares de reservas vencidas más de 5 segundos). No hace falta un proceso aparte. |

Esto se prueba con tests que lanzan **20 clientes simultáneos** sobre el mismo asiento y sobre el último lugar de una zona
(`DobleVentaTest`).

## 5. Consistencia entre servicios (entrega 1)

Una cancelación de evento involucra a los dos servicios, y no existe una transacción de base de datos que los abarque:

1. Catálogo cambia el estado del evento a CANCELADO y **confirma su transacción**.
2. Recién después avisa a ventas por HTTP (**fuera de la transacción**, con timeouts de conexión y de lectura).
3. Ventas pasa las entradas a `A_REINTEGRAR` en **una única transacción**, de forma idempotente.
4. Si ventas no responde, el evento igual queda cancelado y la respuesta devuelve `reintegroNotificado: false`.

Lo mismo se aplica al pago: la llamada a la pasarela se hace **fuera** de toda transacción de base de datos, para no
mantener conexiones ni bloqueos tomados mientras se espera a un sistema externo. Las compensaciones automáticas (Saga),
los reintentos programados y el circuit breaker se completan en la entrega 2.

## 6. Decisiones y simplificaciones de la entrega 1

| Tema | Decisión |
|---|---|
| Autenticación | Entrega 1: sin JWT (`X-Usuario-Id` y `X-Rol`). Entrega 2: JWT con roles, ver sección 9. |
| Pasarela de pagos | Simulada (`PasarelaPagoMock`): rechaza los medios de pago cuyo nombre contiene "RECHAZ" y aprueba el resto. |
| Bases de datos | Una base por servicio, esquemas separados: PostgreSQL (H2 en desarrollo) para auth, catálogo y ventas; MongoDB para preferencias y notificaciones. Justificación en `04-modelo-de-datos.md`. |
| Reserva temporal | Vence a los 10 minutos (`ventas.reserva-minutos`). Máximo 4 entradas por cliente y evento (`ventas.max-entradas-por-evento`). |
| Orden por relevancia | No implementado (se sumará con las recomendaciones). Se puede ordenar por fecha o por precio. |
| Despliegue | Entrega 2: un contenedor por servicio y por base, todo con `docker-compose.yml` (ver sección 10). |

## 7. Limitaciones conocidas

- Si el servicio de ventas se cae justo mientras una reserva está `EN_PAGO`, la reserva queda en ese estado hasta que se
  resuelva manualmente. La entrega 2 lo resuelve con compensaciones.
- Si el pago ya se aprobó cuando se cancela el evento, la entrada se emite directamente en `A_REINTEGRAR` (hay una marca de
  evento cancelado para cubrir ese caso). Queda una ventana de milisegundos que también cubre la entrega 2.
- Si catálogo no logra avisar la cancelación a ventas, el aviso no se reintenta automáticamente todavía.
- Las llamadas entre servicios usan direcciones fijas (`localhost`); service discovery queda para la entrega 2.

## 8. Requisitos no funcionales y su entrega

| Requisito | Entrega |
|---|---|
| Arquitectura de microservicios Spring Boot independientes | E1 |
| API Gateway como punto de entrada único | E1 |
| API REST síncrona | E1 |
| Comunicación asíncrona donde se justifique | E2 (hecho: RabbitMQ para avisos e historial, sección 11) |
| Una base de datos por servicio | E1 |
| Concurrencia e idempotencia (doble venta) | E1 |
| Trazabilidad: registro de cada solicitud y `X-Correlation-Id` | E1 (parcial en el gateway), E2 (log centralizado) |
| Seguridad con JWT y roles | E2 (hecho) |
| Base NoSQL en al menos un servicio | E2 (hecho: MongoDB en preferencias y notificaciones) |
| Consistencia entre servicios y compensaciones | E2 |
| Resiliencia: reintentos, timeouts y fallback | E1 (timeouts), E2 (hecho: Resilience4j, sección 16) |
| Caché de catálogo (y por qué no se cachea la disponibilidad) | E2 (hecho: Redis, sección 17) |
| Docker y docker-compose | E2 (hecho) |
| Prueba de carga | E2 |

## 9. Seguridad: JWT y roles (entrega 2)

**Roles:** `CLIENTE` (compra), `ORGANIZADOR` (crea lugares y eventos), `ADMIN` (gestión completa) y `ACCESO`
(personal que escanea los QR en el ingreso; se usará con el control de acceso).

```mermaid
sequenceDiagram
    actor U as Usuario
    participant GW as api-gateway
    participant AU as auth-service
    participant SV as catalogo / ventas

    U->>GW: POST /auth/login {email, password}
    GW->>AU: (ruta pública)
    AU->>AU: verifica la contraseña (BCrypt)
    AU-->>U: 200 {token, rol, usuarioId}
    U->>GW: POST /reservas + Authorization: Bearer token
    GW->>GW: valida firma, emisor y vencimiento del JWT
    GW->>GW: tabla de permisos: ¿el rol puede usar este endpoint?
    alt sin token o token inválido
        GW-->>U: 401
    else rol no permitido
        GW-->>U: 403
    else permitido
        GW->>SV: pedido + X-Usuario-Id y X-Rol (tomados del token)
        SV->>SV: segunda verificación del rol
        SV-->>U: respuesta
    end
```

| Decisión | Por qué |
|---|---|
| **Un servicio propio para usuarios** (`auth-service`) | Es el único que conoce contraseñas. Los demás servicios no guardan credenciales ni dependen de él en cada pedido. |
| **JWT firmado con HS256 y secreto compartido** con el gateway | El token se valida sin consultar a `auth-service` (no es un cuello de botella ni un punto único de falla en la apertura de una venta masiva). Con más servicios que validen tokens convendría RS256 (clave pública/privada). |
| **Validación centralizada en el gateway** (*gateway offloading*) | La autenticación se resuelve en un solo lugar. El gateway descarta los `X-Usuario-Id`/`X-Rol` que mande el cliente y los reemplaza por los del token, así nadie puede hacerse pasar por otro. |
| **Segunda verificación en cada servicio** (defensa en profundidad) | Si por error un pedido llegara sin pasar por el gateway, el servicio igual exige el rol correcto. Además, en Docker los servicios no publican puertos: solo el gateway es accesible desde afuera. |
| **Contraseñas con BCrypt** | Hash lento y con sal propia por usuario: un robo de la base no expone las contraseñas. |
| **Registro público siempre como CLIENTE** | Nadie puede auto-asignarse un rol: organizadores, personal de acceso y administradores los da de alta un ADMIN. |
| **Token de 60 minutos, sin refresh token** | Simplificación: alcanza para una compra completa. Revocar tokens antes de su vencimiento queda fuera del alcance. |

La tabla completa de permisos por endpoint está en el README y se verifica en `ReglasDeAccesoTest` (gateway) y
`SeguridadEndpointsTest` (catálogo y ventas).

**Dueño del evento:** al crear un evento se guarda el id del organizador (`organizadorId`, tomado del JWT). Solo ese
organizador (o un administrador) puede configurar precios, publicarlo y ver sus reportes; otro organizador recibe 403.

## 10. Despliegue con Docker (entrega 2)

`docker compose up --build` levanta 13 contenedores en una red interna:

| Contenedor | Imagen | Puerto publicado |
|---|---|---|
| `api-gateway` | build de `api-gateway/Dockerfile` | **8080** (único punto de entrada) |
| `auth-service`, `catalogo-service`, `ventas-service` | build de su `Dockerfile` | ninguno |
| `auth-db`, `catalogo-db`, `ventas-db` | `postgres:16-alpine` (con volumen propio) | ninguno |
| `preferencias-service`, `notificaciones-service` | build de su `Dockerfile` | ninguno |
| `mongo` | `mongo:7.0.11` con `--auth` (configuración de la cátedra) | `127.0.0.1:27017` (solo local, para Compass) |
| `rabbitmq` | `rabbitmq:3.13-management` | `127.0.0.1:5672` y consola web `127.0.0.1:15672` (solo local) |
| `fila-service` | build de su `Dockerfile` | ninguno |
| `redis` | `redis:7-alpine` | `127.0.0.1:6379` (solo local) |

- Cada `Dockerfile` compila con Maven en una primera etapa y copia solo el jar a una imagen con el JRE (más liviana).
- Los servicios se comunican por el nombre del contenedor (`http://catalogo-service:8081`), configurado con variables
  de entorno; no hace falta cambiar código entre la ejecución local y Docker.
- Cada servicio espera a que su base esté lista (`healthcheck` con `pg_isready`).
- El mecanismo contra la doble venta funciona igual en PostgreSQL: la restricción de unicidad y el `SELECT ... FOR UPDATE`
  son de la base, no de H2.

## 11. Comunicación asincrónica con RabbitMQ (entrega 2)

Los avisos entre servicios que **no necesitan respuesta inmediata** viajan como mensajes por RabbitMQ, en lugar de
llamadas HTTP. Hay un único exchange de tipo *topic*, `ticketya.eventos`; cada mensaje lleva una clave y cada servicio
consumidor tiene **su propia cola** enlazada a las claves que le interesan.

```mermaid
flowchart LR
    V["ventas-service"] -->|"compra.confirmada"| X{{"exchange<br/>ticketya.eventos"}}
    V -->|"evento.cancelado"| X
    C["catalogo-service"] -->|"evento.publicado"| X
    X -->|"compra.confirmada"| Q1[["preferencias.compras"]] --> P["preferencias-service<br/>(historial)"]
    X -->|"compra.confirmada"| Q2[["notificaciones.compras"]] --> N["notificaciones-service"]
    X -->|"evento.cancelado"| Q3[["notificaciones.cancelaciones"]] --> N
    X -->|"evento.publicado"| Q4[["notificaciones.publicaciones"]] --> N
    Q1 & Q2 & Q3 & Q4 -.->|"falla 3 veces"| DLQ[["ticketya.fallidos"]]
```

| Mensaje | Lo publica | Cuándo | Lo consumen |
|---|---|---|---|
| `compra.confirmada` | ventas | Pago aprobado y entradas emitidas | preferencias (historial) y notificaciones (aviso al cliente) |
| `evento.cancelado` | ventas | Después de pasar a reintegro las entradas de un evento cancelado; lleva la lista de compradores | notificaciones (un aviso por comprador) |
| `evento.publicado` | catálogo | Un evento pasa a PUBLICADO | notificaciones (avisa a los fans de esos artistas en esa ciudad, consultando a preferencias) |

| Decisión | Por qué |
|---|---|
| **Asincrónico para avisos e historial** | La compra no espera a que se mande un email ni a que se actualice el historial. Si notificaciones o preferencias están caídos, los mensajes quedan en su cola y se procesan cuando vuelven: no se pierden ni frenan la venta. |
| **Sincrónico (HTTP) donde hace falta la respuesta** | Reservar necesita el precio y el estado del evento *ya*; la cancelación necesita confirmar el paso a reintegro. Esas llamadas siguen siendo REST. |
| **Publicar después del commit** (`@TransactionalEventListener(AFTER_COMMIT)`) | Solo se anuncia lo que realmente quedó guardado: si la transacción se deshace, no sale ningún mensaje. |
| **Fire-and-forget en el productor** | Si RabbitMQ no está disponible al publicar, la compra igual se completa y queda registrado en el log. *Limitación:* ese aviso se pierde; la solución completa es el patrón *Transactional Outbox* (guardar el mensaje en la misma transacción y enviarlo después). |
| **Consumidores idempotentes** | RabbitMQ garantiza entrega "al menos una vez": un mensaje puede llegar repetido. Preferencias ignora una compra ya registrada (`compraId`) y notificaciones tiene un índice único por aviso (`tipo:referencia:usuario`). |
| **Reintentos y cola de fallidos** | Si procesar un mensaje falla (por ejemplo, preferencias no responde), se reintenta 3 veces (1 s, 2 s) y luego va a `ticketya.fallidos` (*dead letter queue*) para revisarlo, en vez de reencolarse para siempre. |
| **Un contrato JSON, sin clases compartidas** | Cada servicio tiene su propio `record` del mensaje: solo comparten el formato, no una librería. Los microservicios se pueden desplegar por separado. |

**Consistencia:** el historial de preferencias y los avisos son **eventualmente consistentes**: aparecen unos
milisegundos después de la compra. La disponibilidad de asientos, en cambio, sigue siendo **fuertemente consistente**
(transacción única en ventas).

## 12. Recomendaciones (versión simplificada)

El `Recomendador` puntúa cada evento a la venta que el cliente no compró todavía:

| Criterio | Puntos |
|---|---|
| Cada otro cliente que compró algo en común con él y también compró este evento ("quienes compraron... también compraron...") | +3 |
| Participa uno de sus artistas favoritos | +5 |
| Es de uno de sus tipos de evento favoritos | +2 |
| Es de un tipo que ya compró antes | +1 |
| Es en su ciudad | +1 |

Se ordena por puntaje (y por fecha a igual puntaje), y cada recomendación devuelve los **motivos** en texto. Sin datos
del cliente (arranque en frío) se sugieren los próximos eventos a la venta.

**Simplificaciones justificadas:** la co-compra se calcula en el momento consultando MongoDB (índice en
`compras.eventoId`), lo que alcanza para el volumen del TP. A gran escala se precalcularía periódicamente una matriz de
similitud entre eventos, o se usaría un motor de recomendación dedicado.

## 13. Control de acceso con QR (entrega 2)

El personal de acceso (rol `ACCESO`) escanea el QR con `POST /accesos {eventoId, codigoQr}`. Todo se resuelve en
**una sola sentencia atómica**:

```sql
UPDATE entrada SET estado = 'USADA', fecha_uso = now()
 WHERE codigo_qr = ? AND evento_id = ? AND estado = 'VENDIDA'
```

Si actualizó 1 fila, el ingreso está permitido. Si actualizó 0, se busca el motivo para informarlo: no existe (404),
es de otro evento, ya fue utilizada (con la hora del primer ingreso) o no está vigente porque fue devuelta o el evento se
canceló (409). **Si dos puertas escanean el mismo QR al mismo tiempo**, la base deja actualizar a una sola: la otra
encuentra la fila ya en `USADA` y es rechazada. Esto vale también con varias instancias de ventas, porque la decisión
la toma la base. Se verifica en `AccesoTest` con 8 escaneos simultáneos.

Cada escaneo, permitido o rechazado, queda en la tabla `REGISTRO_ACCESO` (bitácora: qué QR, en qué puerta, quién y
cuándo). Una entrada ya utilizada no se puede devolver.

## 14. Reportes para organizadores (entrega 2)

`GET /eventos/{id}/reporte` (organizador dueño del evento o administrador) se arma por **composición de APIs** en catálogo:

```mermaid
sequenceDiagram
    actor O as Organizador
    participant GW as api-gateway
    participant C as catalogo-service
    participant V as ventas-service
    O->>GW: GET /eventos/5/reporte (JWT ORGANIZADOR)
    GW->>C: + X-Usuario-Id, X-Rol
    C->>C: ¿es el dueño del evento? (si no, 403)
    C->>C: sectores, capacidades y precios (sus datos)
    C->>V: GET /interno/eventos/5/estadisticas
    V->>V: SELECT sector, estado, COUNT, SUM(precio) GROUP BY sector, estado
    V-->>C: vendidas, utilizadas, devueltas, a reintegrar y montos por sector
    C-->>O: por sector y total: vendidas, disponibles, % de ocupación, devueltas, recaudación
```

Cada servicio aporta lo que es suyo: catálogo la capacidad y los precios, ventas las cantidades y montos. No se copian
datos de ventas en catálogo, así que el reporte siempre está al día. Si ventas no responde, el reporte devuelve 503 en
lugar de mostrar ceros falsos. `GET /eventos/mis-eventos` lista los eventos del organizador en cualquier estado.

## 15. Fila virtual con Redis (entrega 2, versión simplificada)

Un evento creado con `filaVirtual: true` exige un **turno** para reservar.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant F as fila-service
    participant R as Redis
    participant V as ventas-service
    C->>F: POST /filas/7/ingreso
    F->>R: INCR contador · ZADD NX espera (usuario, número de llegada)
    F-->>C: ESPERANDO, posición 1.234, ~2 min
    loop cada 5 segundos (planificador)
        F->>R: SET NX lock-tanda (una tanda por intervalo)
        F->>R: ZPOPMIN espera 50 (los primeros 50)
        F->>R: SET turno:{u} = token firmado, TTL 10 min
    end
    C->>F: GET /filas/7/turno
    F-->>C: HABILITADO + turno
    C->>V: POST /reservas + header X-Turno-Fila
    V->>V: verifica firma, usuario, evento y vencimiento (sin llamar a fila)
    V-->>C: 201 reserva creada
```

| Decisión | Por qué |
|---|---|
| **Redis** (sorted set + claves con TTL) | Miles de ingresos por segundo, operaciones atómicas en memoria, posición en O(log n) y turnos que vencen solos. |
| **Orden por número de llegada** (`INCR`), no por hora | Justo con quien llegó primero, e igual en todas las instancias (no depende del reloj de cada máquina). |
| **Tandas por intervalo** con candado `SET NX` | Regula cuántos clientes compran a la vez (protege ventas y la base). Con varias instancias del servicio sale una sola tanda por intervalo, y `ZPOPMIN` garantiza que nadie se habilite dos veces. |
| **Turno = token firmado** (HS256, secreto compartido con ventas) | Ventas lo verifica localmente: la fila no queda en el camino de cada reserva ni es un punto único de falla para comprar. El token lleva usuario y evento, así que no sirve para otra persona ni para otro evento, y vence a los 10 minutos. |
| **La fila es efímera** | Si Redis se reinicia, la fila se pierde y los clientes vuelven a entrar; las compras siguen en ventas (fuente de verdad). Se acepta como simplificación. |

**Simplificaciones justificadas:** tamaño de tanda e intervalo fijos (configurables por variable de entorno, no por
evento); el cliente consulta su turno (*polling*) en lugar de recibir un aviso *push*; un turno no se "consume" al
reservar (vale 10 minutos, y el límite de 4 entradas por cliente y evento sigue aplicando).

## 16. Resiliencia ante fallas de otros servicios (entrega 2)

Toda llamada a otro servicio puede fallar o tardar. Las llamadas importantes usan **Resilience4j** (anotaciones
`@Retry` y `@CircuitBreaker`, configuradas en `application.properties`):

| Llamada | Timeout | Reintentos | Circuit breaker | Si sigue fallando (fallback) |
|---|---|---|---|---|
| ventas → catálogo (datos del sector al reservar) | 2 s conexión, 3 s respuesta | 3 intentos (200 ms, 400 ms), solo ante caídas o 5xx; un 404 no se reintenta | se abre con 50% de fallas en las últimas 20 llamadas (mínimo 10), 15 s | **503** al instante. No se usan datos viejos: para vender hacen falta el estado y el precio actuales. |
| ventas → pasarela de pagos (cobro) | (simulada) | 3 intentos (300 ms, 600 ms) | igual, 30 s | **503 y la reserva se conserva** (vuelve a PENDIENTE con sus lugares hasta que vence): el cliente reintenta el pago con otra clave. |
| ventas → pasarela (reintegro de una devolución) | (simulada) | 3 intentos | el de la pasarela | La devolución queda aprobada con el reintegro **pendiente**, y una tarea lo reintenta cada minuto. |
| catálogo → ventas (aviso de cancelación) | 2 s / 3 s | 3 intentos (ventas lo procesa en forma idempotente) | se abre con 50% de fallas, 15 s | El evento queda cancelado con el aviso **pendiente**, y una tarea lo reenvía cada 30 s hasta que ventas lo confirme. |
| catálogo → ventas (datos para reportes) | 2 s / 3 s | 3 intentos | el de ventas | **503**: el reporte no se arma con ceros falsos. |
| notificaciones, preferencias, fila → catálogo/preferencias | 2 s / 3 s | — (los mensajes de RabbitMQ se reintentan 3 veces y van a la cola de fallidos) | — | Nombre del evento: se usa el id. Historial: se guarda sin datos del evento. |

**Por qué cada mecanismo:**
- **Timeout:** ninguna llamada espera indefinidamente; un hilo bloqueado es un cliente menos atendido en la venta masiva.
- **Reintento con espera creciente:** muchas fallas son momentáneas (un reinicio, un pico). Solo se reintenta lo que es
  seguro repetir: el cobro lleva una **referencia única por compra** (la pasarela no cobra dos veces la misma compra),
  y los avisos de cancelación son idempotentes en ventas. Un error de negocio (404, pago rechazado) no se reintenta.
- **Circuit breaker:** si un servicio viene fallando, seguir llamándolo solo suma esperas y carga. El circuito "abierto"
  responde 503 al instante, y pasado un tiempo deja pasar unas pocas llamadas de prueba ("semiabierto") para ver si
  el servicio se recuperó.
- **Tareas de recuperación** (consistencia eventual): lo que no se pudo completar porque otro servicio estaba caído
  (reintegros, avisos de cancelación) queda registrado como pendiente y se completa solo cuando el servicio vuelve.

La pasarela simulada permite probarlo: `TARJETA_INESTABLE` falla el primer intento (el reintento la aprueba, cobrando
una sola vez) y `PASARELA_CAIDA` no responde nunca (`PagoResilienteTest`, carpeta 6 de Postman). Contra catálogo se
prueba con un servidor HTTP simulado que devuelve errores (`ResilienciaCatalogoTest`, `ResilienciaVentasTest`).

## 17. Caché (entrega 2)

El catálogo cachea en **Redis** (compartido por todas sus instancias) lo que se lee masivamente y cambia poco:

| Caché | Qué guarda | Duración | Se invalida cuando |
|---|---|---|---|
| `evento` | Detalle de un evento (`GET /eventos/{id}`) | 10 min | se cambian sus precios, se publica, suspende o cancela |
| `busquedas` | Resultado de `GET /eventos` para cada combinación de filtros | 60 s | cualquier evento se publica, suspende, cancela o cambia precios |
| `lugares` | Listado de lugares | 10 min | se crea un lugar |

- Se usa *cache-aside* con las anotaciones de Spring (`@Cacheable` / `@CacheEvict`): la primera lectura va a la base y
  guarda el resultado; las siguientes salen de Redis hasta que vence o se invalida.
- La duración corta de las búsquedas acota cuánto puede verse un dato desactualizado (por ejemplo, el "precio desde").
- **Si Redis se cae, el catálogo sigue funcionando**: un `CacheErrorHandler` registra el problema y se lee de la base.
- En una apertura de venta, miles de clientes consultan el mismo evento: con la caché, esas lecturas no llegan a
  PostgreSQL y la base queda libre para lo que sí la necesita.

**Qué NO se cachea, y por qué:**
- **La disponibilidad de asientos y el cupo** (ventas): durante una venta masiva cambian a cada segundo. Un dato
  cacheado mostraría lugares libres que ya se vendieron, y el cliente fallaría recién al reservar. Además, la garantía de
  no doble venta depende de leer el estado real dentro de la transacción.
- **El estado y el precio que ventas pide para reservar** (`/interno/eventos/{e}/sectores/{s}`): se vende con el dato
  actual. Si el administrador suspende o cancela un evento, no se puede seguir vendiendo por un valor cacheado.
- **Reportes y "mis eventos"**: los usa un organizador por vez y tienen que estar al día.
