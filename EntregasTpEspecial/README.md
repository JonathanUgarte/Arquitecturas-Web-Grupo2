# TicketYa

Plataforma de venta de entradas (ticketera virtual) basada en **microservicios con Spring Boot**.
Trabajo práctico de Arquitectura Web (TUDAI). **Entrega 1** (núcleo: publicación, búsqueda y compra sin doble venta) completa;
**Entrega 2** (sistema completo) en curso: ya incluye seguridad con JWT y roles, despliegue con Docker, MongoDB,
preferencias y recomendaciones, notificaciones asincrónicas con RabbitMQ, control de acceso con QR, reportes para
organizadores, fila virtual con Redis, resiliencia (reintentos y circuit breaker) y caché del catálogo.

> Integrantes: Agustin pagola, Agustin Rebord, Jonatan Ugarte

## Qué incluye la entrega 1

- Eventos, lugares y sectores (numerados y no numerados), con precio por sector y publicación.
- Compra en dos pasos: **reserva temporal** de lugares (10 minutos) y **pago** con una pasarela simulada, con emisión de entradas con **código QR único**.
- **Garantía de no doble venta**, también con varias instancias a la vez, probada con tests de concurrencia.
- Pagos **idempotentes** (se puede reintentar sin cobrar dos veces).
- **Devoluciones** (hasta 72 horas antes) y **cancelación de eventos** con paso de las entradas a reintegro.
- **Búsqueda** por tipo, organizador, participante, ciudad y rango de fechas, ordenada por fecha o precio.
- **API Gateway** como punto de entrada único.
- Documentación OpenAPI (Swagger), colección de Postman, tests unitarios y de concurrencia.

## Qué suma la entrega 2 (hasta ahora)

- **Autenticación y autorización con JWT y roles** (`CLIENTE`, `ORGANIZADOR`, `ADMIN`, `ACCESO`): nuevo `auth-service`
  que registra usuarios y emite los tokens; el gateway los valida y aplica una tabla de permisos por endpoint.
- **Docker**: cada microservicio y cada base de datos en su propio contenedor, y todo el sistema con un único
  `docker-compose.yml`. En Docker cada servicio usa su propia base **PostgreSQL**.
- Tests de seguridad: cada endpoint solo es accesible por los roles que corresponden.
- **Preferencias y recomendaciones (10)** en un nuevo `preferencias-service` con **MongoDB**: ciudad, tipos y artistas
  favoritos, historial de compras y sugerencias del tipo "quienes compraron este evento también compraron...".
- **Notificaciones (11)** en un nuevo `notificaciones-service`: avisos al confirmar una compra, al cancelarse un evento y
  cuando un artista favorito publica un evento en la ciudad del cliente.
- **Control de acceso con QR (8)**: el personal de acceso escanea la entrada; un QR no puede ingresar dos veces, ni
  siquiera si se escanea en dos puertas al mismo tiempo. Cada escaneo queda en una bitácora.
- **Reportes para organizadores (12)**: vendidas, devueltas y a reintegrar por sector, ocupación y recaudación. Cada
  organizador solo ve y modifica *sus* eventos.
- **Fila virtual (6)** en un nuevo `fila-service` con **Redis**: en eventos de alta demanda, los clientes hacen la fila,
  ven su posición y se los habilita por tandas, en orden de llegada, con un turno para comprar.
- **Resiliencia** con Resilience4j: timeouts, reintentos y circuit breaker hacia catálogo, ventas y la pasarela de pagos.
  Si la pasarela no responde, la reserva no se pierde; los reintegros y avisos de cancelación que fallan se completan
  solos cuando el otro servicio vuelve.
- **Caché del catálogo en Redis**: detalle de eventos, búsquedas y lugares, con invalidación al cambiar un evento. La
  disponibilidad de asientos **no** se cachea (ver `docs/02-arquitectura.md`, sección 17).
- **Comunicación asincrónica con RabbitMQ**: ventas y catálogo publican eventos (`compra.confirmada`, `evento.cancelado`,
  `evento.publicado`) que consumen preferencias y notificaciones, con reintentos y cola de mensajes fallidos.

## Estructura del repositorio

```
docker-compose.yml    Levanta todo el sistema (bases, servicios y gateway)
api-gateway/          Punto de entrada único (puerto 8080): valida el JWT y los roles
auth-service/         Usuarios, roles, registro y login con JWT (puerto 8083)
preferencias-service/ Preferencias, historial y recomendaciones, con MongoDB (puerto 8084)
notificaciones-service/ Avisos a los clientes a partir de mensajes de RabbitMQ, con MongoDB (puerto 8085)
fila-service/         Fila virtual por tandas para eventos de alta demanda, con Redis (puerto 8086)
catalogo-service/     Lugares, eventos, precios, publicación y búsqueda (puerto 8081)
ventas-service/       Reservas, pagos, entradas, devoluciones y disponibilidad (puerto 8082)
docs/                 Diseño: user stories, arquitectura, diagramas, modelo de datos, endpoints y OpenAPI
postman/              Colección de requests para probar todos los endpoints
```

Cada servicio es un proyecto Spring Boot **independiente**, con su propio `pom.xml`, y se construye y ejecuta por separado.

## Cómo ejecutarlo con Docker (recomendado)

Requisito: Docker Desktop (o Docker Engine con el plugin compose). Desde esta carpeta:

```bash
docker compose up --build
```

La primera vez tarda unos minutos (descarga imágenes y dependencias de Maven). Levanta 13 contenedores:

| Tipo | Contenedores |
|---|---|
| Bases relacionales | `auth-db`, `catalogo-db`, `ventas-db` (PostgreSQL 16) |
| Base NoSQL | `mongo` (MongoDB 7, configuración provista por la cátedra), con las bases `preferencias` y `notificaciones` |
| Mensajería | `rabbitmq` (RabbitMQ 3.13 con consola web) |
| Fila virtual | `redis` (Redis 7) |
| Microservicios | `auth-service`, `catalogo-service`, `ventas-service`, `preferencias-service`, `notificaciones-service`, `fila-service` |
| Punto de entrada | `api-gateway` |

**Solo el gateway publica un puerto: `http://localhost:8080`.** Los servicios y las bases quedan en la red interna de
Docker, así nadie puede saltearse la validación del JWT llamando directo a un servicio.

- Detener: `docker compose down` · Detener y borrar los datos: `docker compose down -v`
- **Consola de RabbitMQ:** http://localhost:15672 (usuario `ticketya`, clave `ticketya`): colas, mensajes y la cola de fallidos.
- **MongoDB** (por ejemplo con MongoDB Compass): `mongodb://root:password@localhost:27017/?authSource=admin`.
- MongoDB, RabbitMQ y Redis se publican solo en `127.0.0.1` (no quedan expuestos a la red), para estas herramientas y para
  correr servicios desde IntelliJ.
- El secreto del JWT se puede cambiar con la variable `JWT_SECRET` (por ejemplo en un archivo `.env` junto al compose).

## Cómo ejecutarlo sin Docker (desarrollo)

### Requisitos

- **JDK 21**
- **Maven 3.9+**, o IntelliJ IDEA (que trae Maven incluido)
- Acceso a internet la primera vez (para descargar las dependencias)
- No hace falta instalar ninguna base de datos: sin Docker cada servicio usa H2 (en archivo)

### Ejecución

`preferencias-service` y `notificaciones-service` necesitan MongoDB y RabbitMQ, `fila-service` necesita Redis, catálogo y
ventas usan RabbitMQ para publicar eventos, y catálogo usa Redis como caché. Sin Docker para los servicios, se pueden levantar solo esos dos contenedores:

```bash
docker compose up -d mongo rabbitmq redis
```

(Si RabbitMQ no está levantado, catálogo y ventas funcionan igual: solo no se envían los avisos. Si Redis no está, el
catálogo funciona sin caché.)

Con IntelliJ: abrir el repositorio, agregar cada `pom.xml` como proyecto Maven y ejecutar las clases `main`
**en este orden**: `AuthServiceApplication`, `CatalogoServiceApplication`, `VentasServiceApplication`,
`PreferenciasServiceApplication`, `NotificacionesServiceApplication`, `FilaServiceApplication` y `ApiGatewayApplication`.

Con la línea de comandos (una terminal por servicio):

```bash
cd auth-service     && mvn spring-boot:run
cd catalogo-service && mvn spring-boot:run
cd ventas-service   && mvn spring-boot:run
cd preferencias-service   && mvn spring-boot:run
cd notificaciones-service && mvn spring-boot:run
cd fila-service           && mvn spring-boot:run
cd api-gateway      && mvn spring-boot:run
```

| Servicio | Puerto | Documentación interactiva (Swagger) | Consola de la base (H2) |
|---|---|---|---|
| api-gateway | 8080 | - | - |
| catalogo-service | 8081 | http://localhost:8081/swagger-ui.html | http://localhost:8081/h2-console |
| ventas-service | 8082 | http://localhost:8082/swagger-ui.html | http://localhost:8082/h2-console |
| auth-service | 8083 | http://localhost:8083/swagger-ui.html | http://localhost:8083/h2-console |
| preferencias-service | 8084 | http://localhost:8084/swagger-ui.html | MongoDB (`preferencias`) |
| notificaciones-service | 8085 | http://localhost:8085/swagger-ui.html | MongoDB (`notificaciones`) |
| fila-service | 8086 | http://localhost:8086/swagger-ui.html | Redis |

Datos de la consola H2: JDBC URL `jdbc:h2:file:./data/catalogo` (o `./data/ventas`, `./data/auth`), usuario `sa`, sin contraseña.

> Sin Docker los servicios quedan accesibles en sus puertos y confían en los headers `X-Usuario-Id` y `X-Rol`:
> sirve para desarrollar y probar con Swagger, pero los pedidos "reales" deben entrar siempre por el gateway.
Las bases se crean en la carpeta `data/` (no se sube al repositorio).

**Todos los pedidos de los clientes entran por el gateway:** `http://localhost:8080`.
Las rutas `/interno/**` (comunicación entre servicios) no se publican en el gateway.

## Datos de prueba

La primera vez que arranca, el catálogo carga datos de ejemplo:

| Qué | Detalle |
|---|---|
| Lugar 1 | "Estadio Municipal" (Tandil), con el sector 1 "Campo" (zona sin asiento, cupo 5000) y el sector 2 "Platea" (numerada, 5 filas x 10 asientos) |
| Evento 1 | "Recital de Prueba", **publicado**, con precios: Campo 15000 y Platea 30000 |
| Evento 2 | "Obra de Teatro de Prueba", en **borrador** (sin precios), para probar precios y publicación |

Usuarios de prueba (los crea `auth-service` al arrancar por primera vez). La contraseña de todos es **`ticketya123`**:

| Email | Rol | Id |
|---|---|---|
| `cliente@ticketya.com` | CLIENTE | 1 |
| `cliente2@ticketya.com` | CLIENTE | 2 |
| `organizador@ticketya.com` | ORGANIZADOR | 3 |
| `admin@ticketya.com` | ADMIN | 4 |
| `acceso@ticketya.com` | ACCESO (personal de acceso) | 5 |

Para empezar de cero: sin Docker, detener los servicios y borrar la carpeta `data/`; con Docker, `docker compose down -v`.

## Cómo probarlo

1. Importar en Postman la colección `postman/TicketYa.postman_collection.json` (usa la variable `baseUrl` = `http://localhost:8080`).
2. Ejecutar primero la carpeta **0. Autenticación**: hace login con un usuario de cada rol y guarda los tokens.
3. Ejecutar en orden: **1. Catálogo**, **2. Compra**, **3. Devoluciones y cancelación** y **4. Preferencias, recomendaciones
   y notificaciones**. Cada request ya usa el token del rol que corresponde. La carpeta 4 necesita MongoDB y RabbitMQ:
   muestra el historial armado a partir de la compra, la recomendación del evento de un artista favorito y los tres
   tipos de aviso (compra, cancelación y artista favorito).
   La carpeta **5. Acceso con QR, reportes y fila virtual** compra una entrada y la valida en la puerta (y prueba que no
   entra dos veces), muestra los reportes del organizador, y hace la fila de un evento de alta demanda hasta obtener
   el turno y reservar con él (espera unos segundos a que salga la tanda).
   La carpeta **6. Resiliencia y caché** paga con `TARJETA_INESTABLE` (la pasarela falla una vez y el reintento automático
   la aprueba) y con `PASARELA_CAIDA` (503, pero la reserva sigue vigente y el pago se reintenta).

### Autenticación (entrega 2)

1. `POST /auth/login` con `{"email": "...", "password": "..."}` devuelve un JWT (válido 60 minutos).
2. Enviarlo en cada pedido: `Authorization: Bearer <token>`.
3. El gateway valida el token y reenvía al servicio la identidad en `X-Usuario-Id` y el rol en `X-Rol`. Si el cliente
   manda esos headers, el gateway los descarta: nadie puede hacerse pasar por otro usuario.

| Endpoint | Quién puede |
|---|---|
| `POST /auth/login`, `POST /auth/registro`, `GET /eventos/**`, `GET /lugares/**`, `GET /disponibilidad/**` | Público (sin token) |
| `POST /lugares`, `POST /eventos`, `PUT /eventos/{id}/precios`, `POST /eventos/{id}/publicacion` | ORGANIZADOR, ADMIN |
| `PUT /eventos/{id}/suspension`, `PUT /eventos/{id}/cancelacion` | ADMIN |
| `/reservas/**`, `/entradas/**`, `POST /devoluciones` | CLIENTE |
| `GET /devoluciones`, `PUT /devoluciones/{id}/aprobacion`, `PUT /devoluciones/{id}/rechazo`, `/auth/usuarios` | ADMIN |
| `/preferencias`, `/recomendaciones`, `/filas/**` (ingreso, turno, salir) | CLIENTE |
| `POST /accesos` (escanear QR) | ACCESO, ADMIN |
| `GET /eventos/mis-eventos`, `GET /eventos/{id}/reporte`, `GET /filas/{id}/estado` | ORGANIZADOR (solo sus eventos), ADMIN |
| `GET /notificaciones` | Cualquier usuario autenticado (ve las suyas) |

Sin token (o con un token vencido o mal firmado) la respuesta es **401**; con un rol que no corresponde, **403**.
Medios de pago de la pasarela simulada: `TARJETA_RECHAZADA` (rechazo definitivo, 402), `TARJETA_INESTABLE` (falla el primer
intento y el reintento automático la aprueba) y `PASARELA_CAIDA` (no responde: 503 y la reserva se conserva).

## Tests

Desde cada carpeta de servicio: `mvn test` (o, en IntelliJ, clic derecho sobre `src/test/java` y **Run Tests**).

| Servicio | Tests | Qué cubren |
|---|---|---|
| ventas-service | `CompraTest` (13) | Reserva temporal, pago aprobado y rechazado, QR único, reintento idempotente, reserva vencida, límite por cliente, cupo de zonas |
| ventas-service | `DevolucionTest` (7) | Plazo de 72 horas, aprobar y rechazar devoluciones, cancelación de eventos y paso a reintegro |
| ventas-service | `DobleVentaTest` (3) | **20 clientes simultáneos** sobre el mismo asiento y sobre el último lugar de una zona: solo uno tiene éxito |
| catalogo-service | `EventoServiceTest` (23) | Búsqueda por múltiples criterios y orden, publicación, precios, suspensión y cancelación |
| catalogo-service | `SeguridadEndpointsTest` | Cada endpoint protegido rechaza con 403 los roles que no corresponden; las consultas son públicas |
| ventas-service | `SeguridadEndpointsTest` | Ídem para reservas, pagos, entradas y devoluciones |
| auth-service | `AuthControllerTest` | Registro (siempre CLIENTE), email repetido, login con JWT válido, contraseña incorrecta y alta solo por ADMIN |
| api-gateway | `ReglasDeAccesoTest` | Tabla de permisos: cada endpoint, con cada rol (y sin token), da el resultado esperado |
| api-gateway | `AutenticacionFilterTest` | 401 sin token, vencido o mal firmado; 403 con rol incorrecto; reenvío de la identidad del token y descarte de headers falsificados |
| ventas-service | `EventosPublicadosTest` | Se publica una sola `compra.confirmada` aunque se reintente el pago, ninguna si se rechaza, y una sola `evento.cancelado` con los compradores |
| preferencias-service | `RecomendadorTest` | Puntaje de recomendaciones: co-compra, artista y tipo favorito, ciudad, arranque en frío, límite y desempate |
| preferencias-service | `PreferenciasIntegracionTest` * | Contra MongoDB real: preferencias, historial idempotente, recomendación por co-compra y búsqueda de interesados |
| notificaciones-service | `NotificacionServiceTest` | Avisos de compra, cancelación (uno por comprador) y artista favorito; duplicados; fallas del canal y de preferencias |
| notificaciones-service | `NotificacionesIntegracionTest` * | De punta a punta con RabbitMQ y MongoDB reales: mensaje publicado → aviso registrado, sin duplicados |
| ventas-service | `AccesoTest` | Ingreso permitido, QR repetido, de otro evento, inexistente o no vigente, bitácora, no se devuelve una entrada usada, y **8 puertas escaneando el mismo QR a la vez: entra una sola** |
| ventas-service | `EstadisticasTest` | Vendidas, utilizadas, devueltas y a reintegrar por sector, con sus montos |
| ventas-service | `FilaVirtualTest` | En un evento con fila: sin turno, turno de otro cliente, de otro evento, vencido o falsificado → 403; con turno válido reserva |
| catalogo-service | `ReporteYDuenioTest` | Ocupación y recaudación por sector y total, sectores sin ventas, ventas caído (503), otro organizador (403), admin, mis eventos |
| ventas-service | `PagoResilienteTest` | Reintento ante una falla momentánea sin cobrar dos veces, pasarela caída (503 y reserva conservada), circuito abierto (no se llama más a la pasarela) y reintento programado de reintegros |
| ventas-service | `ResilienciaCatalogoTest` | Contra un catálogo simulado (servidor HTTP): reintentos, un 404 no se reintenta, se rinde a los 3 intentos y el circuito se abre |
| catalogo-service | `ResilienciaVentasTest` | Aviso de cancelación reintentado, fallback si ventas no responde, reportes con reintento y 503 |
| catalogo-service | `CancelacionPendienteTest` | Un aviso de cancelación no recibido queda pendiente y se reenvía hasta que ventas lo confirma |
| catalogo-service | `CacheTest` | El detalle y las búsquedas salen de la caché y se invalidan al cambiar un evento; lugares |
| catalogo-service | `SerializacionCacheTest` | Lo que se guarda en Redis se lee igual (fechas, listas, records) |
| fila-service | `FilaServiceTest` * | Contra Redis real: orden de llegada, reingreso sin perder el lugar, tandas en orden, una sola tanda por intervalo, turno firmado, salir, eventos sin fila |

\* Usan **Testcontainers**: levantan MongoDB, RabbitMQ o Redis reales en contenedores, así que necesitan **Docker Desktop abierto**.
Si Docker no está disponible, esos tests se saltean (no fallan) y el resto corre igual.

Los tests usan bases en memoria y simulan los servicios externos, así que no necesitan Docker ni que los otros servicios estén levantados.

## Diseño

| Documento | Contenido |
|---|---|
| [docs/01-user-stories.md](docs/01-user-stories.md) | Features y user stories de todo el sistema, con criterios de aceptación |
| [docs/02-arquitectura.md](docs/02-arquitectura.md) | Microservicios, responsabilidades, API, diagrama de módulos, mecanismo contra la doble venta, decisiones y limitaciones |
| [docs/03-secuencia-compra.md](docs/03-secuencia-compra.md) | Diagrama de secuencia de la compra de una entrada numerada (pago rechazado y reserva vencida) |
| [docs/04-modelo-de-datos.md](docs/04-modelo-de-datos.md) | Diagrama entidad-relación por microservicio y entidades solapadas |
| [docs/05-api-endpoints.md](docs/05-api-endpoints.md) | Endpoints con parámetros, códigos de respuesta y ejemplos JSON |
| `docs/openapi-catalogo.json`, `docs/openapi-ventas.json` | Documentación OpenAPI exportada de cada servicio |

Los diagramas están escritos con Mermaid: se visualizan directamente en GitHub.

### Cómo se evita vender dos veces la misma entrada

El control está en la base de datos, por eso funciona con varias instancias de ventas a la vez:
una restricción de unicidad sobre el asiento (asientos numerados), un bloqueo pesimista sobre el cupo (zonas sin asiento)
y una clave de idempotencia por pago (reintentos del cliente). El detalle está en `docs/02-arquitectura.md`.

## Simplificaciones de la entrega 1

- ~~Sin autenticación~~ (resuelto en la entrega 2 con JWT).
- Pasarela de pagos simulada: rechaza los medios de pago que contienen "RECHAZ" y aprueba el resto.
- Una base H2 por servicio (en la entrega 2: PostgreSQL en Docker y MongoDB para preferencias y notificaciones).
- Orden por relevancia no implementado (se suma con las recomendaciones).
- ~~Ejecución local, sin contenedores~~ (resuelto en la entrega 2 con Docker y docker-compose).

Las limitaciones conocidas están detalladas en `docs/02-arquitectura.md`.

## Entrega 2: estado

| Tema | Estado |
|---|---|
| JWT con roles (auth-service + gateway) | Hecho |
| Docker por servicio y docker-compose | Hecho |
| Pruebas de acceso por rol | Hecho |
| Base NoSQL (MongoDB), preferencias y recomendaciones, notificaciones | Hecho |
| Comunicación asincrónica (RabbitMQ) | Hecho |
| Control de acceso con QR, reportes para organizadores, fila virtual (Redis) | Hecho |
| Resiliencia (timeouts, reintentos, circuit breaker, fallback) y caché del catálogo | Hecho |
| Compensaciones (Saga) documentadas, log centralizado | Pendiente |
| Pruebas de integración entre servicios y prueba de carga | Pendiente |

## Uso de herramientas de IA

Este trabajo se desarrolló con asistencia de Claude  como herramienta de programación y documentación,
a partir de la consigna. El grupo revisó el diseño y el código propuestos, adoptó las decisiones de diseño
documentadas en docs ejecutó los tres servicios y corrió todos los tests, que pasan.

No se utilizó OpenSpec: en su lugar, el diseño, las user stories con criterios de aceptación, los diagramas y el
detalle de la API están escritos en docs.