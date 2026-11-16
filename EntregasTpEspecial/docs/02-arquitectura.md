# TicketYa - Arquitectura de microservicios

## 1. Microservicios y responsabilidades

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| **api-gateway** | 8080 | Punto de entrada único. Valida el JWT y los roles (entrega 2), enruta a los servicios, asigna un `X-Correlation-Id` a cada pedido y registra método, ruta, usuario, código de respuesta y tiempo. No publica las rutas `/interno/**`. | - |
| **auth-service** | 8083 | Usuarios y roles: registro de clientes, alta de usuarios con rol (admin) y login, que emite el JWT. *(entrega 2)* | `auth` |
| **catalogo-service** | 8081 | Lugares, sectores y eventos: alta, precios por sector, publicación, suspensión, cancelación y búsqueda por múltiples criterios. Es el dueño de la verdad sobre qué se vende y a qué precio. | `catalogo` |
| **ventas-service** | 8082 | Reserva temporal de lugares, compra y pago (pasarela simulada), entradas con QR, devoluciones y cancelación de entradas. Garantiza que una entrada no se venda dos veces. | `ventas` |

**Por qué esta división:** catálogo se lee mucho y cambia poco (candidato a caché en la entrega 2), mientras que ventas
recibe mucha escritura concurrente sobre los mismos recursos. Separarlos permite escalar y proteger ventas de forma
independiente, y deja un lugar natural para sumar servicios en la entrega 2 (notificaciones, fila virtual,
recomendaciones y reportes).

Cada servicio tiene su propia base de datos y **no accede a la del otro**: lo que necesita lo pide por HTTP.
En Docker cada base es un PostgreSQL 16 en su propio contenedor; sin Docker (desarrollo y tests) se usa H2.

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
```

## 3. API pública (a través del gateway, `http://localhost:8080`)

| Servicio | Endpoints |
|---|---|
| auth | `POST /auth/registro`, `POST /auth/login`, `POST/GET /auth/usuarios` |
| catalogo | `POST/GET /lugares`, `GET /lugares/{id}`, `POST/GET /eventos`, `GET /eventos/{id}`, `PUT /eventos/{id}/precios`, `POST /eventos/{id}/publicacion`, `PUT /eventos/{id}/suspension`, `PUT /eventos/{id}/cancelacion` |
| ventas | `POST /reservas`, `GET/DELETE /reservas/{id}`, `POST /reservas/{id}/pago`, `GET /entradas`, `GET /entradas/{id}`, `POST/GET /devoluciones`, `PUT /devoluciones/{id}/aprobacion`, `PUT /devoluciones/{id}/rechazo`, `GET /disponibilidad/eventos/{e}/sectores/{s}` |

Detalle con ejemplos en [05-api-endpoints.md](05-api-endpoints.md) y en los OpenAPI de cada servicio.

**API interna** (solo entre servicios, el gateway no la publica):

| Quién llama | Endpoint | Para qué |
|---|---|---|
| ventas -> catalogo | `GET /interno/eventos/{eventoId}/sectores/{sectorId}` | Obtener estado del evento, fecha, capacidad y precio del sector. |
| catalogo -> ventas | `POST /interno/eventos/{eventoId}/cancelacion` | Avisar la cancelación para pasar las entradas a reintegro. |

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
| Bases de datos | Una base por servicio, esquemas separados: H2 en desarrollo, PostgreSQL en Docker (entrega 2). MongoDB pendiente. |
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
| Una base de datos por servicio | E1 |
| Concurrencia e idempotencia (doble venta) | E1 |
| Trazabilidad: registro de cada solicitud y `X-Correlation-Id` | E1 (parcial en el gateway), E2 (log centralizado) |
| Seguridad con JWT y roles | E2 (hecho) |
| Base NoSQL en al menos un servicio | E2 |
| Consistencia entre servicios y compensaciones | E2 |
| Resiliencia: reintentos, timeouts y fallback | E1 (timeouts), E2 (reintentos y fallback) |
| Caché de catálogo (y por qué no se cachea la disponibilidad) | E2 |
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

**Limitación conocida:** todavía no se controla que un organizador solo modifique *sus* eventos (el organizador del evento
es un texto). Se puede resolver guardando el id del organizador al crear el evento.

## 10. Despliegue con Docker (entrega 2)

`docker compose up --build` levanta 7 contenedores en una red interna:

| Contenedor | Imagen | Puerto publicado |
|---|---|---|
| `api-gateway` | build de `api-gateway/Dockerfile` | **8080** (único punto de entrada) |
| `auth-service`, `catalogo-service`, `ventas-service` | build de su `Dockerfile` | ninguno |
| `auth-db`, `catalogo-db`, `ventas-db` | `postgres:16-alpine` (con volumen propio) | ninguno |

- Cada `Dockerfile` compila con Maven en una primera etapa y copia solo el jar a una imagen con el JRE (más liviana).
- Los servicios se comunican por el nombre del contenedor (`http://catalogo-service:8081`), configurado con variables
  de entorno; no hace falta cambiar código entre la ejecución local y Docker.
- Cada servicio espera a que su base esté lista (`healthcheck` con `pg_isready`).
- El mecanismo contra la doble venta funciona igual en PostgreSQL: la restricción de unicidad y el `SELECT ... FOR UPDATE`
  son de la base, no de H2.
