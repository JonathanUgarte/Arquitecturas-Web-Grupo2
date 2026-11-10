# TicketYa

Plataforma de venta de entradas (ticketera virtual) basada en **microservicios con Spring Boot**.
Trabajo práctico de Arquitectura Web (TUDAI) - **Entrega 1: núcleo del sistema** (publicación, búsqueda y compra sin doble venta).

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

## Estructura del repositorio

```
api-gateway/          Punto de entrada único (puerto 8080)
catalogo-service/     Lugares, eventos, precios, publicación y búsqueda (puerto 8081)
ventas-service/       Reservas, pagos, entradas, devoluciones y disponibilidad (puerto 8082)
docs/                 Diseño: user stories, arquitectura, diagramas, modelo de datos, endpoints y OpenAPI
postman/              Colección de requests para probar todos los endpoints
```

Cada servicio es un proyecto Spring Boot **independiente**, con su propio `pom.xml`, y se construye y ejecuta por separado.

## Requisitos

- **JDK 21**
- **Maven 3.9+**, o IntelliJ IDEA (que trae Maven incluido)
- Acceso a internet la primera vez (para descargar las dependencias)
- No hace falta instalar ninguna base de datos: cada servicio usa H2 (en archivo)

## Cómo ejecutarlo

Con IntelliJ: abrir el repositorio, agregar cada `pom.xml` como proyecto Maven y ejecutar las tres clases `main`
**en este orden**: `CatalogoServiceApplication`, `VentasServiceApplication` y `ApiGatewayApplication`.

Con la línea de comandos (una terminal por servicio):

```bash
cd catalogo-service && mvn spring-boot:run
cd ventas-service   && mvn spring-boot:run
cd api-gateway      && mvn spring-boot:run
```

| Servicio | Puerto | Documentación interactiva (Swagger) | Consola de la base (H2) |
|---|---|---|---|
| api-gateway | 8080 | - | - |
| catalogo-service | 8081 | http://localhost:8081/swagger-ui.html | http://localhost:8081/h2-console |
| ventas-service | 8082 | http://localhost:8082/swagger-ui.html | http://localhost:8082/h2-console |

Datos de la consola H2: JDBC URL `jdbc:h2:file:./data/catalogo` (o `./data/ventas`), usuario `sa`, sin contraseña.
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

Para empezar de cero, detener los servicios y borrar la carpeta `data/`.

## Cómo probarlo

1. Importar en Postman la colección `postman/TicketYa.postman_collection.json` (usa la variable `baseUrl` = `http://localhost:8080`).
2. Ejecutar las carpetas en orden: **1. Catálogo**, **2. Compra** y **3. Devoluciones y cancelación**.

Identificación sin autenticación (entrega 1): `X-Usuario-Id: 1` para clientes y `X-Rol: ADMIN` para acciones de administrador.
Para ver un pago rechazado se usa el medio de pago `TARJETA_RECHAZADA`.

## Tests

Desde cada carpeta de servicio: `mvn test` (o, en IntelliJ, clic derecho sobre `src/test/java` y **Run Tests**).

| Servicio | Tests | Qué cubren |
|---|---|---|
| ventas-service | `CompraTest` (13) | Reserva temporal, pago aprobado y rechazado, QR único, reintento idempotente, reserva vencida, límite por cliente, cupo de zonas |
| ventas-service | `DevolucionTest` (7) | Plazo de 72 horas, aprobar y rechazar devoluciones, cancelación de eventos y paso a reintegro |
| ventas-service | `DobleVentaTest` (3) | **20 clientes simultáneos** sobre el mismo asiento y sobre el último lugar de una zona: solo uno tiene éxito |
| catalogo-service | `EventoServiceTest` (23) | Búsqueda por múltiples criterios y orden, publicación, precios, suspensión y cancelación |

Los tests usan bases en memoria y simulan el servicio de catálogo, así que no necesitan que los otros servicios estén levantados.

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

- Sin autenticación: el cliente va en `X-Usuario-Id` y el administrador en `X-Rol: ADMIN` (JWT en la entrega 2).
- Pasarela de pagos simulada: rechaza los medios de pago que contienen "RECHAZ" y aprueba el resto.
- Una base H2 por servicio (MongoDB y otras tecnologías en la entrega 2).
- Orden por relevancia no implementado (se suma con las recomendaciones).
- Ejecución local, sin contenedores (Docker y docker-compose en la entrega 2).

Las limitaciones conocidas están detalladas en `docs/02-arquitectura.md`.

## Entrega 2 (pendiente)

Fila virtual, control de acceso con QR, preferencias y recomendaciones, notificaciones, reportes para organizadores,
JWT con roles, base NoSQL, caché, resiliencia (reintentos y fallback), log centralizado, Docker, pruebas de integración
y prueba de carga.

## Uso de herramientas de IA

Este trabajo se desarrolló con asistencia de Claude  como herramienta de programación y documentación,
a partir de la consigna. El grupo revisó el diseño y el código propuestos, adoptó las decisiones de diseño
documentadas en docs ejecutó los tres servicios y corrió todos los tests, que pasan.

No se utilizó OpenSpec: en su lugar, el diseño, las user stories con criterios de aceptación, los diagramas y el
detalle de la API están escritos en docs.