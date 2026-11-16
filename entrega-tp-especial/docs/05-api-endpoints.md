# API REST - Endpoints, parámetros, códigos y ejemplos

Todas las rutas se acceden por el gateway: `http://localhost:8080`.
Documentación interactiva de cada servicio: `http://localhost:8081/swagger-ui.html` (catálogo) y
`http://localhost:8082/swagger-ui.html` (ventas). Los OpenAPI exportados están en esta carpeta
(`openapi-catalogo.json` y `openapi-ventas.json`).

**Identificación sin autenticación (entrega 1):**

| Header | Uso |
|---|---|
| `X-Usuario-Id` | Id del cliente. Obligatorio en reservas, pagos, entradas y solicitud de devoluciones. |
| `X-Rol: ADMIN` | Obligatorio en las acciones de administrador (suspender, cancelar, resolver devoluciones). |
| `Idempotency-Key` | Obligatorio en el pago. Una clave por intento de pago. |
| `X-Correlation-Id` | Opcional: el gateway lo genera si no viene y lo devuelve en la respuesta. |

**Formato de los errores** (todos los servicios):
```json
{ "timestamp": "2026-11-11T10:15:30.123", "status": 409, "error": "Conflict",
  "mensaje": "Alguno de los asientos ya fue reservado o vendido, o el sistema esta ocupado" }
```

---

# Catálogo

## POST /lugares
Crea un lugar con sus sectores.

| Código | Cuándo |
|---|---|
| 201 | Lugar creado |
| 400 | Faltan datos o un sector es inválido (numerado sin filas/asientos, no numerado sin capacidad) |

Request:
```json
{
  "nombre": "Estadio Municipal", "direccion": "Av. Rivadavia 1000", "ciudad": "Tandil",
  "latitud": -37.32, "longitud": -59.13,
  "sectores": [
    { "nombre": "Campo",  "tipo": "NO_NUMERADO", "capacidad": 5000 },
    { "nombre": "Platea", "tipo": "NUMERADO", "filas": 5, "asientosPorFila": 10 }
  ]
}
```
Response 201:
```json
{
  "id": 1, "nombre": "Estadio Municipal", "direccion": "Av. Rivadavia 1000", "ciudad": "Tandil",
  "latitud": -37.32, "longitud": -59.13,
  "sectores": [
    { "id": 1, "nombre": "Campo",  "tipo": "NO_NUMERADO", "capacidad": 5000, "filas": null, "asientosPorFila": null, "capacidadTotal": 5000 },
    { "id": 2, "nombre": "Platea", "tipo": "NUMERADO", "capacidad": null, "filas": 5, "asientosPorFila": 10, "capacidadTotal": 50 }
  ]
}
```

## GET /lugares  ·  GET /lugares/{id}
Lista los lugares / obtiene uno. Códigos: 200, y 404 si no existe el lugar (en el segundo).

## POST /eventos
Crea un evento en estado BORRADOR.

| Código | Cuándo |
|---|---|
| 201 | Evento creado en BORRADOR |
| 400 | Faltan datos o la fecha no es futura |
| 404 | No existe el lugar |

Request:
```json
{
  "nombre": "Recital de Verano", "descripcion": "Show de rock en el estadio", "tipo": "RECITAL",
  "organizador": "Productora Sur", "participantes": ["Banda Uno", "Banda Dos"],
  "fechaHora": "2027-01-15T21:00:00", "lugarId": 1
}
```
Tipos: `RECITAL`, `FESTIVAL`, `TEATRO`, `DEPORTE`, `CONFERENCIA`, `OTRO`.

## PUT /eventos/{id}/precios
Define el precio de cada sector. Solo en BORRADOR; reemplaza los precios anteriores.

| Código | Cuándo |
|---|---|
| 200 | Precios configurados |
| 400 | Un sector no pertenece al lugar del evento, está repetido o el precio no es positivo |
| 404 | No existe el evento |
| 409 | El evento no está en BORRADOR |

Request:
```json
{ "precios": [ { "sectorId": 1, "precio": 15000 }, { "sectorId": 2, "precio": 30000 } ] }
```

## POST /eventos/{id}/publicacion
Publica el evento (BORRADOR -> PUBLICADO). Sin body.

| Código | Cuándo |
|---|---|
| 200 | Publicado |
| 404 | No existe el evento |
| 409 | No está en BORRADOR, o faltan precios (el mensaje indica los sectores sin precio) |

## GET /eventos/{id}
Detalle del evento con su lugar, sectores y precios. Códigos: 200, 404.

Response 200 (resumido):
```json
{
  "id": 1, "nombre": "Recital de Verano", "tipo": "RECITAL", "organizador": "Productora Sur",
  "participantes": ["Banda Uno", "Banda Dos"], "fechaHora": "2027-01-15T21:00:00", "estado": "PUBLICADO",
  "lugar": { "id": 1, "nombre": "Estadio Municipal", "ciudad": "Tandil", "sectores": [ "..." ] },
  "precios": [
    { "sectorId": 1, "sector": "Campo",  "tipo": "NO_NUMERADO", "precio": 15000.00 },
    { "sectorId": 2, "sector": "Platea", "tipo": "NUMERADO",    "precio": 30000.00 }
  ]
}
```

## GET /eventos
Busca eventos **a la venta** (PUBLICADO o AGOTADO). Todos los parámetros son opcionales y se combinan.

| Parámetro | Descripción | Ejemplo |
|---|---|---|
| `tipo` | Tipo de evento | `RECITAL` |
| `organizador` | Nombre o parte del nombre (sin distinguir mayúsculas) | `productora` |
| `participante` | Nombre o parte del nombre de un artista, equipo o disertante | `banda` |
| `ciudad` | Ciudad del lugar | `Tandil` |
| `desde`, `hasta` | Rango de fechas (ISO) | `2026-12-01T00:00:00` |
| `orden` | `fecha` (por defecto) o `precio` (por el precio más bajo del evento) | `precio` |

| Código | Cuándo |
|---|---|
| 200 | Lista de eventos |
| 400 | Orden inválido o un filtro con formato incorrecto |

Ejemplo: `GET /eventos?tipo=RECITAL&ciudad=Tandil&orden=precio`

Response 200:
```json
[
  { "id": 1, "nombre": "Recital de Verano", "tipo": "RECITAL", "organizador": "Productora Sur",
    "participantes": ["Banda Uno", "Banda Dos"], "fechaHora": "2027-01-15T21:00:00",
    "estado": "PUBLICADO", "lugar": "Estadio Municipal", "ciudad": "Tandil", "precioDesde": 15000.00 }
]
```

## PUT /eventos/{id}/suspension  ·  header `X-Rol: ADMIN`
Suspende un evento PUBLICADO o AGOTADO.

| Código | Cuándo |
|---|---|
| 200 | Suspendido |
| 403 | No es administrador |
| 404 | No existe el evento |
| 409 | El evento no está PUBLICADO ni AGOTADO |

## PUT /eventos/{id}/cancelacion  ·  header `X-Rol: ADMIN`
Cancela el evento y avisa a ventas para que sus entradas vendidas pasen a reintegro.

| Código | Cuándo |
|---|---|
| 200 | Cancelado |
| 403 | No es administrador |
| 404 | No existe el evento |
| 409 | Ya estaba CANCELADO o FINALIZADO |

Request: `{ "motivo": "Mal tiempo" }`

Response 200:
```json
{ "eventoId": 1, "estado": "CANCELADO", "motivo": "Mal tiempo", "reintegroNotificado": true }
```
`reintegroNotificado: false` indica que el evento se canceló pero ventas no respondió: el aviso queda pendiente.

---

# Ventas

## POST /reservas  ·  header `X-Usuario-Id`
Reserva lugares por 10 minutos. Sector NUMERADO: se indican los asientos. Sector NO_NUMERADO: se indica la cantidad.

| Código | Cuándo |
|---|---|
| 201 | Reserva creada (PENDIENTE) |
| 400 | Faltan datos, falta indicar asientos o cantidad, o el asiento no existe en el sector |
| 404 | No existe el evento o el sector |
| 409 | Asiento ya tomado, sin cupo, evento no a la venta o ya ocurrido, o límite de 4 entradas por cliente |
| 503 | El servicio de catálogo no responde |

Request (sector numerado):
```json
{ "eventoId": 1, "sectorId": 2, "asientos": [ { "fila": 3, "numero": 5 }, { "fila": 3, "numero": 6 } ] }
```
Request (zona sin asiento): `{ "eventoId": 1, "sectorId": 1, "cantidad": 2 }`

Response 201:
```json
{
  "id": 1, "usuarioId": 1, "eventoId": 1, "sectorId": 2, "estado": "PENDIENTE", "cantidad": 2,
  "asientos": [ { "fila": 3, "numero": 5 }, { "fila": 3, "numero": 6 } ],
  "precioUnitario": 30000.00, "total": 60000.00, "venceEn": "2026-11-11T10:10:00"
}
```

## GET /reservas/{id}  ·  header `X-Usuario-Id`
Consulta una reserva propia (si ya venció, se informa `VENCIDA`). Códigos: 200, 403, 404.

## POST /reservas/{id}/pago  ·  headers `X-Usuario-Id` e `Idempotency-Key`
Paga la reserva. **Es idempotente**: repetir el pago con la misma clave devuelve el mismo resultado sin cobrar de nuevo.

| Código | Cuándo |
|---|---|
| 200 | Pago aprobado: entradas emitidas con código QR |
| 402 | Pago rechazado: la reserva se cancela y los lugares quedan libres |
| 400 | Falta el medio de pago o la clave de idempotencia está vacía |
| 403 | La reserva no es del usuario |
| 404 | No existe la reserva |
| 409 | La reserva venció o ya no está pendiente, la clave se usó en otra reserva, o hay otro pago en proceso |

Request: `{ "medioPago": "TARJETA" }`. La pasarela simulada rechaza los medios de pago cuyo nombre contiene "RECHAZ" (por ejemplo `TARJETA_RECHAZADA`).

Response 200:
```json
{
  "id": 1, "reservaId": 1, "estado": "APROBADA", "monto": 60000.00, "medioPago": "TARJETA",
  "transaccionPago": "TX-4f95d4fd-c1f7-4a8a-9c03-403a7be90e9c", "motivoRechazo": null,
  "entradas": [
    { "id": 1, "codigoQr": "7b1d0b6e-2f2a-4f3a-9d54-2c5f0e8a1c11", "eventoId": 1, "sectorId": 2,
      "fila": 3, "numero": 5, "precio": 30000.00, "estado": "VENDIDA", "fechaEvento": "2027-01-15T21:00:00" },
    { "id": 2, "codigoQr": "0d4b9a58-91c8-4e7b-8d3e-6a2c7f19b3aa", "eventoId": 1, "sectorId": 2,
      "fila": 3, "numero": 6, "precio": 30000.00, "estado": "VENDIDA", "fechaEvento": "2027-01-15T21:00:00" }
  ]
}
```
Response 402 (pago rechazado): igual estructura con `"estado": "RECHAZADA"`, `"motivoRechazo": "Pago rechazado por el medio de pago"` y `"entradas": []`.

## DELETE /reservas/{id}  ·  header `X-Usuario-Id`
Cancela una reserva PENDIENTE y libera sus lugares. Códigos: 204, 403, 404, 409 (no está PENDIENTE).

## GET /entradas  ·  header `X-Usuario-Id`
Lista las entradas del cliente, de la más reciente a la más antigua. Código: 200.

## GET /entradas/{id}  ·  header `X-Usuario-Id`
Obtiene una entrada propia. Códigos: 200, 403, 404.

## GET /disponibilidad/eventos/{eventoId}/sectores/{sectorId}
Lugares libres de un sector. **No se cachea nunca**: cambia con cada compra. Códigos: 200, 404, 503.

Response 200 (sector numerado):
```json
{ "eventoId": 1, "sectorId": 2, "tipo": "NUMERADO", "capacidadTotal": 50, "disponibles": 48,
  "asientosOcupados": [ { "fila": 3, "numero": 5 }, { "fila": 3, "numero": 6 } ] }
```
Para una zona: `"disponibles"` es el cupo restante y `"asientosOcupados"` es `[]`.

## POST /devoluciones  ·  header `X-Usuario-Id`
Solicita la devolución de una entrada (hasta 72 horas antes del evento).

| Código | Cuándo |
|---|---|
| 201 | Solicitud registrada (PENDIENTE) |
| 403 | La entrada no es del usuario |
| 404 | No existe la entrada |
| 409 | Faltan menos de 72 horas, la entrada no está vendida, o ya hay una solicitud pendiente |

Request: `{ "entradaId": 1, "motivo": "No puedo asistir" }`

Response 201:
```json
{ "id": 1, "entradaId": 1, "estado": "PENDIENTE", "motivo": "No puedo asistir", "monto": 30000.00,
  "solicitadaEn": "2026-11-11T10:20:00", "resueltaEn": null, "reintegroSolicitado": false }
```

## GET /devoluciones?estado=PENDIENTE  ·  header `X-Rol: ADMIN`
Lista las solicitudes por estado (`PENDIENTE` por defecto). Códigos: 200, 403.

## PUT /devoluciones/{id}/aprobacion  ·  header `X-Rol: ADMIN`
Aprueba: la entrada pasa a DEVUELTA, se libera el asiento o lugar de la zona y se pide el reintegro a la pasarela.
Códigos: 200, 403, 404, 409 (ya resuelta o la entrada ya no está vendida).

## PUT /devoluciones/{id}/rechazo  ·  header `X-Rol: ADMIN`
Rechaza la solicitud: la entrada sigue vendida. Códigos: 200, 403, 404, 409.

---

# Interno (solo entre servicios, no se publica en el gateway)

| Método y ruta | Servicio | Descripción |
|---|---|---|
| `GET /interno/eventos/{eventoId}/sectores/{sectorId}` | catalogo (8081) | Estado del evento, fecha, capacidad, filas, asientos por fila y precio de un sector. 404 si el sector no tiene precio en ese evento. |
| `POST /interno/eventos/{eventoId}/cancelacion` | ventas (8082) | Pasa las entradas VENDIDA a A_REINTEGRAR y libera las reservas pendientes. Idempotente. |

Si se intenta acceder a `/interno/**` por el gateway, responde 404.
