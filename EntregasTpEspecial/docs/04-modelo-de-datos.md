# Modelo de datos por microservicio

Cada servicio tiene su propia base de datos. Las relaciones **dentro** de un servicio son claves foráneas; las relaciones
**entre** servicios se guardan solo como el id del otro servicio, sin clave foránea (no se puede cruzar de una base a otra).

## catalogo-service

```mermaid
erDiagram
    LUGAR ||--o{ SECTOR : "tiene"
    LUGAR ||--o{ EVENTO : "aloja"
    EVENTO ||--o{ PRECIO_SECTOR : "define"
    SECTOR ||--o{ PRECIO_SECTOR : "se vende a"
    EVENTO ||--o{ EVENTO_PARTICIPANTES : "incluye"

    LUGAR {
        Long id PK
        String nombre
        String direccion
        String ciudad
        Double latitud
        Double longitud
    }
    SECTOR {
        Long id PK
        Long lugar_id FK
        String nombre
        String tipo "NUMERADO o NO_NUMERADO"
        Integer capacidad "solo NO_NUMERADO"
        Integer filas "solo NUMERADO"
        Integer asientosPorFila "solo NUMERADO"
    }
    EVENTO {
        Long id PK
        Long lugar_id FK
        String nombre
        String descripcion
        String tipo
        String organizador
        LocalDateTime fechaHora
        String estado "BORRADOR PUBLICADO AGOTADO SUSPENDIDO CANCELADO FINALIZADO"
        String motivoCancelacion
    }
    PRECIO_SECTOR {
        Long id PK
        Long evento_id FK
        Long sector_id FK
        BigDecimal precio
    }
    EVENTO_PARTICIPANTES {
        Long evento_id FK
        String participante
    }
```

Restricción de unicidad: `PRECIO_SECTOR (evento_id, sector_id)` (un solo precio por sector en cada evento).

## ventas-service

```mermaid
erDiagram
    RESERVA ||--o{ ASIENTO_OCUPADO : "reserva"
    RESERVA ||--o{ COMPRA : "se paga con"
    COMPRA ||--o{ ENTRADA : "emite"
    ENTRADA ||--o{ DEVOLUCION : "puede tener"

    RESERVA {
        Long id PK
        Long usuarioId
        Long eventoId "id de catalogo"
        Long sectorId "id de catalogo"
        String tipoSector
        int cantidad
        BigDecimal precioUnitario
        BigDecimal total
        LocalDateTime fechaEvento "copia"
        String estado "PENDIENTE EN_PAGO CONFIRMADA CANCELADA VENCIDA"
        LocalDateTime creadaEn
        LocalDateTime venceEn
    }
    ASIENTO_OCUPADO {
        Long id PK
        Long reserva_id FK
        Long evento_id "id de catalogo"
        Long sector_id "id de catalogo"
        int fila
        int numero
    }
    STOCK_ZONA {
        Long id PK
        Long evento_id "id de catalogo"
        Long sector_id "id de catalogo"
        int disponibles
    }
    COMPRA {
        Long id PK
        Long reserva_id FK
        String claveIdempotencia UK
        String medioPago
        BigDecimal monto
        String estado "PROCESANDO APROBADA RECHAZADA"
        String transaccionPago
        String motivoRechazo
        LocalDateTime creadaEn
    }
    ENTRADA {
        Long id PK
        Long compra_id FK
        String codigoQr UK
        Long eventoId "id de catalogo"
        Long sectorId "id de catalogo"
        Integer fila
        Integer numero
        Long usuarioId
        BigDecimal precio
        String estado "VENDIDA DEVUELTA A_REINTEGRAR USADA"
        LocalDateTime fechaEvento "copia"
        LocalDateTime fechaCompra
    }
    DEVOLUCION {
        Long id PK
        Long entrada_id FK
        Long usuarioId
        String motivo
        BigDecimal monto
        String estado "PENDIENTE APROBADA RECHAZADA"
        LocalDateTime solicitadaEn
        LocalDateTime resueltaEn
        boolean reintegroSolicitado
    }
    EVENTO_CANCELADO {
        Long eventoId PK
        LocalDateTime canceladoEn
    }
```

Restricciones de unicidad que sostienen la garantía de no doble venta:

- `ASIENTO_OCUPADO (evento_id, sector_id, fila, numero)`: un asiento solo puede estar ocupado una vez.
- `STOCK_ZONA (evento_id, sector_id)`: una sola fila de cupo por zona (se modifica con bloqueo pesimista).
- `COMPRA (clave_idempotencia)`: un intento de pago por clave.
- `ENTRADA (codigo_qr)`: cada código QR es único.

## auth-service (entrega 2) · PostgreSQL

```mermaid
erDiagram
    USUARIO {
        Long id PK
        String nombre
        String email UK "en minúsculas"
        String password_hash "BCrypt, nunca la contraseña"
        String rol "CLIENTE, ORGANIZADOR, ADMIN o ACCESO"
        LocalDateTime fecha_alta
    }
```

El `id` de `USUARIO` es el que viaja en el JWT y el que los demás servicios guardan como `usuario_id` / `usuarioId`.

## preferencias-service (entrega 2) · MongoDB, base `preferencias`

Un **documento por cliente** en la colección `perfiles`, con el historial de compras **embebido** (no hay JOINs):

```json
{
  "_id": 1,                                   // = id del usuario
  "version": 3,                               // control de concurrencia optimista (@Version)
  "ciudad": "Tandil",
  "ciudadNormalizada": "tandil",              // índice: búsqueda sin importar mayúsculas
  "tiposFavoritos": ["RECITAL"],
  "artistasFavoritos": ["Los Nuevos"],
  "artistasNormalizados": ["los nuevos"],     // índice multivalor
  "compras": [                                // historial, se agrega al consumir "compra.confirmada"
    { "compraId": 12, "eventoId": 1,          // índice en compras.eventoId (co-compra)
      "nombreEvento": "Recital de Prueba", "tipo": "RECITAL",
      "participantes": ["Banda Uno"], "ciudad": "Tandil", "fecha": "2026-11-16T11:30:00" }
  ],
  "actualizado": "2026-11-16T11:31:00"
}
```

| Índice | Para qué |
|---|---|
| `compras.eventoId` | "Quienes compraron este evento": base de la recomendación por co-compra. |
| `ciudadNormalizada`, `artistasNormalizados` | Encontrar a los fans de un artista en una ciudad cuando se publica un evento. |

## notificaciones-service (entrega 2) · MongoDB, base `notificaciones`

Un documento por aviso enviado, en la colección `notificaciones`:

```json
{
  "_id": "6738f2...",
  "usuarioId": 1,                                    // índice
  "tipo": "COMPRA_CONFIRMADA",                       // o EVENTO_CANCELADO, ARTISTA_FAVORITO
  "canal": "EMAIL",
  "titulo": "Compra confirmada",
  "mensaje": "Tu compra #12 de 2 entradas para \"Recital de Prueba\" fue confirmada...",
  "referencia": "compra:12",
  "claveIdempotencia": "COMPRA_CONFIRMADA:compra:12:1",   // índice ÚNICO: un mismo aviso sale una sola vez
  "estado": "ENVIADA",                               // o FALLIDA
  "fecha": "2026-11-16T11:30:01"
}
```

### ¿Por qué MongoDB para estos dos servicios?

| Criterio | Preferencias y notificaciones | Catálogo, ventas y auth |
|---|---|---|
| Forma de los datos | Perfil con listas de largo variable (favoritos, historial); avisos independientes entre sí | Entidades muy relacionadas (lugar, sector, evento, precio, reserva, entrada) |
| Transacciones | No hacen falta: cada escritura toca un solo documento (que en Mongo es atómica) | Imprescindibles: la compra y la no doble venta dependen de transacciones, bloqueos y restricciones de unicidad |
| Lectura típica | "Todo el perfil de este cliente" (un solo documento, sin JOINs) | Consultas con filtros y joins (búsqueda por ciudad, fechas, participante) |
| Evolución | Se agregan preferencias nuevas sin migrar un esquema | Esquema estable |
| Volumen | Una notificación por aviso: crece rápido y casi no se modifica (bitácora) | Moderado |

Se usa **una sola instancia de MongoDB** (la configuración provista por la cátedra) con **una base por servicio**
(`preferencias` y `notificaciones`): misma tecnología, esquemas separados, y ningún servicio lee la base del otro.

## Relaciones entre servicios y entidades solapadas

| Dato en ventas | Referencia a catalogo | Cómo se resuelve |
|---|---|---|
| `evento_id`, `sector_id` (en reserva, asiento, stock y entrada) | `EVENTO.id`, `SECTOR.id` | Solo el id, sin clave foránea. Los datos del evento y del sector se piden por HTTP al reservar. |
| `usuario_id` (ventas), `_id` del perfil (preferencias), `usuarioId` (notificaciones) | `USUARIO.id` (auth) | Solo el id: viaja en el JWT. |
| `compras.eventoId` (preferencias), `referencia` (notificaciones) | `EVENTO.id` (catálogo) | Solo el id; los datos se piden a catálogo al consumir el mensaje. |

**Entidades solapadas entre bases:** ninguna entidad está duplicada. `Evento`, `Lugar` y `Sector` existen **únicamente**
en catálogo. Ventas guarda además **copias de valores puntuales** que necesita después, para no depender de catálogo:

| Copia en ventas | Origen | Motivo |
|---|---|---|
| `fechaEvento` (reserva y entrada) | `EVENTO.fechaHora` | Aplicar la regla de 72 horas de las devoluciones sin consultar a catálogo. |
| `precioUnitario` y `precio` | `PRECIO_SECTOR.precio` | Congelar el precio al momento de la compra: lo pagado no cambia si el organizador cambia la tarifa. |
| `tipoSector` | `SECTOR.tipo` | Saber cómo liberar los lugares (asientos o cupo) sin consultar a catálogo. |
| `compras[].tipo`, `participantes`, `ciudad`, `nombreEvento` (preferencias) | `EVENTO` y `LUGAR` (catálogo) | Recomendar sin consultar a catálogo por cada compra del historial. Es una copia del momento de la compra (consistencia eventual). |
