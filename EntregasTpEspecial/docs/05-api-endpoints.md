# API REST - Endpoints, parámetros, códigos y ejemplos

Todas las rutas se acceden por el gateway: `http://localhost:8080`.
Documentación interactiva de cada servicio: `http://localhost:8081/swagger-ui.html` (catálogo) y
`http://localhost:8082/swagger-ui.html` (ventas). Con el sistema levantado, el gateway publica el OpenAPI de cada
servicio en `GET http://localhost:8080/api-docs/{auth|catalogo|ventas|preferencias|notificaciones|fila}` (público), y
`scripts/exportar-openapi.ps1` los guarda en esta carpeta (`openapi-{servicio}.json`).

**Autenticación (entrega 2):** cada pedido protegido lleva `Authorization: Bearer <token>`, obtenido en
`POST /auth/login`. El gateway valida el token y reenvía la identidad al servicio. Documentación interactiva de
auth-service (ejecución local): `http://localhost:8083/swagger-ui.html`.

| Header | Uso |
|---|---|
| `Authorization: Bearer <token>` | Obligatorio en todo endpoint que no sea público. Sin token o con uno inválido: **401**. Con un rol no permitido: **403**. |
| `X-Usuario-Id`, `X-Rol` | Ya **no** los envía el cliente: el gateway los descarta y los completa a partir del token. Los servicios los leen. |
| `Idempotency-Key` | Obligatorio en el pago. Una clave por intento de pago. |
| `X-Turno-Fila` | Solo al reservar en un evento con fila virtual: el turno que da `GET /filas/{eventoId}/turno`. |
| `X-Correlation-Id` | Opcional: el gateway lo genera si no viene y lo devuelve en la respuesta. |

Endpoints públicos (sin token): `POST /auth/login`, `POST /auth/registro`, y todos los `GET` de `/eventos`, `/lugares`
y `/disponibilidad`. El rol requerido por cada uno de los demás está indicado en cada endpoint y en la tabla del README.

**Formato de los errores** (todos los servicios):
```json
{ "timestamp": "2026-11-11T10:15:30.123", "status": 409, "error": "Conflict",
  "mensaje": "Alguno de los asientos ya fue reservado o vendido, o el sistema esta ocupado" }
```

---

# Autenticación (auth-service)

## POST /auth/registro (público)
Registra un cliente. El rol es siempre `CLIENTE`.

| Código | Cuándo |
|---|---|
| 201 | Usuario creado |
| 400 | Email mal formado o contraseña de menos de 8 caracteres |
| 409 | Ya existe un usuario con ese email |

Request: `{ "nombre": "Ana Perez", "email": "ana@mail.com", "password": "secreta123" }`

Response 201: `{ "id": 6, "nombre": "Ana Perez", "email": "ana@mail.com", "rol": "CLIENTE", "fechaAlta": "2026-11-16T10:00:00" }`

## POST /auth/login (público)
| Código | Cuándo |
|---|---|
| 200 | Login correcto |
| 401 | Email o contraseña incorrectos (mismo mensaje en los dos casos) |

Request: `{ "email": "cliente@ticketya.com", "password": "ticketya123" }`

Response 200:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEnSegundos": 3600,
  "usuarioId": 1, "nombre": "Cliente Uno", "rol": "CLIENTE" }
```

## POST /auth/usuarios (ADMIN)
Alta de un usuario con cualquier rol (`CLIENTE`, `ORGANIZADOR`, `ADMIN`, `ACCESO`).

| Código | Cuándo |
|---|---|
| 201 | Usuario creado |
| 401 | Sin token o token inválido |
| 403 | No es administrador |
| 409 | Ya existe un usuario con ese email |

Request: `{ "nombre": "Productora Norte", "email": "norte@mail.com", "password": "secreta123", "rol": "ORGANIZADOR" }`

## GET /auth/usuarios (ADMIN)
Lista los usuarios registrados (sin contraseñas).

---

# Catálogo

## POST /lugares  ·  ORGANIZADOR o ADMIN
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

## GET /lugares  ·  GET /lugares/{id}  ·  público
Lista los lugares / obtiene uno. Códigos: 200, y 404 si no existe el lugar (en el segundo).

## POST /eventos  ·  ORGANIZADOR o ADMIN
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

## PUT /eventos/{id}/precios  ·  ORGANIZADOR o ADMIN
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

## POST /eventos/{id}/publicacion  ·  ORGANIZADOR o ADMIN
Publica el evento (BORRADOR -> PUBLICADO). Sin body.

| Código | Cuándo |
|---|---|
| 200 | Publicado |
| 404 | No existe el evento |
| 409 | No está en BORRADOR, o faltan precios (el mensaje indica los sectores sin precio) |

## GET /eventos/{id}  ·  público
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

## GET /eventos  ·  público
Busca eventos **a la venta** (PUBLICADO o AGOTADO). Todos los parámetros son opcionales y se combinan.

| Parámetro | Descripción | Ejemplo |
|---|---|---|
| `tipo` | Tipo de evento | `RECITAL` |
| `organizador` | Nombre o parte del nombre (sin distinguir mayúsculas) | `productora` |
| `participante` | Nombre o parte del nombre de un artista, equipo o disertante | `banda` |
| `ciudad` | Ciudad del lugar | `Tandil` |
| `desde`, `hasta` | Rango de fechas (ISO) | `2026-12-01T00:00:00` |
| `orden` | `fecha` (por defecto), `precio` (por el precio más bajo del evento) o `relevancia` (primero los que coinciden exacto con el artista u organizador buscado, después los que empiezan con el texto y por último los que lo contienen; a igual relevancia, por fecha) | `relevancia` |

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

## PUT /eventos/{id}/suspension  ·  ADMIN
Suspende un evento PUBLICADO o AGOTADO.

| Código | Cuándo |
|---|---|
| 200 | Suspendido |
| 403 | No es administrador |
| 404 | No existe el evento |
| 409 | El evento no está PUBLICADO ni AGOTADO |

## PUT /eventos/{id}/cancelacion  ·  ADMIN
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

## POST /reservas  ·  CLIENTE
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

## GET /reservas/{id}  ·  CLIENTE
Consulta una reserva propia (si ya venció, se informa `VENCIDA`). Códigos: 200, 403, 404.

## POST /reservas/{id}/pago  ·  CLIENTE  ·  header `Idempotency-Key`
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

## DELETE /reservas/{id}  ·  CLIENTE
Cancela una reserva PENDIENTE y libera sus lugares. Códigos: 204, 403, 404, 409 (no está PENDIENTE).

## GET /entradas  ·  CLIENTE
Lista las entradas del cliente, de la más reciente a la más antigua. Código: 200.

## GET /entradas/{id}  ·  CLIENTE
Obtiene una entrada propia. Códigos: 200, 403, 404.

## GET /disponibilidad/eventos/{eventoId}/sectores/{sectorId}  ·  público
Lugares libres de un sector. **No se cachea nunca**: cambia con cada compra. Códigos: 200, 404, 503.

Response 200 (sector numerado):
```json
{ "eventoId": 1, "sectorId": 2, "tipo": "NUMERADO", "capacidadTotal": 50, "disponibles": 48,
  "asientosOcupados": [ { "fila": 3, "numero": 5 }, { "fila": 3, "numero": 6 } ] }
```
Para una zona: `"disponibles"` es el cupo restante y `"asientosOcupados"` es `[]`.

## POST /devoluciones  ·  CLIENTE
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

## GET /devoluciones?estado=PENDIENTE  ·  ADMIN
Lista las solicitudes por estado (`PENDIENTE` por defecto). Códigos: 200, 403.

## PUT /devoluciones/{id}/aprobacion  ·  ADMIN
Aprueba: la entrada pasa a DEVUELTA, se libera el asiento o lugar de la zona y se pide el reintegro a la pasarela.
Códigos: 200, 403, 404, 409 (ya resuelta o la entrada ya no está vendida).

## PUT /devoluciones/{id}/rechazo  ·  ADMIN
Rechaza la solicitud: la entrada sigue vendida. Códigos: 200, 403, 404, 409.

---

# Interno (solo entre servicios, no se publica en el gateway)

| Método y ruta | Servicio | Descripción |
|---|---|---|
| `GET /interno/eventos/{eventoId}/sectores/{sectorId}` | catalogo (8081) | Estado del evento, fecha, capacidad, filas, asientos por fila y precio de un sector. 404 si el sector no tiene precio en ese evento. |
| `POST /interno/eventos/{eventoId}/cancelacion` | ventas (8082) | Pasa las entradas VENDIDA a A_REINTEGRAR y libera las reservas pendientes. Idempotente. |
| `GET /interno/eventos/{eventoId}/estadisticas` | ventas (8082) | Vendidas, utilizadas, devueltas y a reintegrar por sector (reportes y estado AGOTADO). |
| `GET /interno/eventos/{eventoId}/compradores` | ventas (8082) | Ids de los clientes con entradas vigentes (aviso de suspensión). |
| `GET /interno/interesados?ciudad=&artistas=` | preferencias (8084) | Clientes de esa ciudad con alguno de esos artistas como favorito. |

Si se intenta acceder a `/interno/**` por el gateway, responde 404.

---

# Preferencias y recomendaciones (preferencias-service)

## GET /preferencias  ·  CLIENTE
Preferencias del cliente y su historial de compras (el historial se arma solo, a partir de los mensajes `compra.confirmada`).

Response 200:
```json
{ "usuarioId": 1, "ciudad": "Tandil", "tiposFavoritos": ["RECITAL"], "artistasFavoritos": ["Los Nuevos"],
  "historial": [ { "compraId": 12, "eventoId": 1, "nombreEvento": "Recital de Prueba", "tipo": "RECITAL",
                   "participantes": ["Banda Uno", "Banda Dos"], "ciudad": "Tandil", "fecha": "2026-11-16T11:30:00" } ] }
```

## PUT /preferencias  ·  CLIENTE
Los campos en `null` (o que no se envían) quedan como estaban.

| Código | Cuándo |
|---|---|
| 200 | Preferencias guardadas |
| 400 | Tipo de evento inexistente |
| 409 | El perfil se modificó al mismo tiempo desde otro lado (reintentar) |

Request: `{ "ciudad": "Tandil", "tiposFavoritos": ["RECITAL"], "artistasFavoritos": ["Los Nuevos"] }`

## GET /recomendaciones?limite=10  ·  CLIENTE
Eventos a la venta sugeridos, del más al menos relevante, con el puntaje y los motivos.

| Código | Cuándo |
|---|---|
| 200 | Lista de recomendaciones (sin datos del cliente: los próximos eventos a la venta) |
| 400 | `limite` fuera de 1 a 50 |
| 503 | El catálogo no responde |

Response 200:
```json
[ { "eventoId": 5, "nombre": "Los Nuevos en vivo", "tipo": "RECITAL", "ciudad": "Tandil",
    "fechaHora": "2027-05-10T21:00:00", "precioDesde": 20000, "puntaje": 8,
    "motivos": ["Participa Los Nuevos, uno de tus artistas favoritos", "Es de tipo RECITAL, uno de tus favoritos",
                "Es en tu ciudad (Tandil)"] } ]
```

---

# Notificaciones (notificaciones-service)

## GET /notificaciones  ·  cualquier usuario autenticado
Avisos recibidos por el usuario, del más reciente al más antiguo.

Response 200:
```json
[ { "id": "6738f2a1...", "tipo": "ARTISTA_FAVORITO", "canal": "EMAIL",
    "titulo": "Tu artista favorito viene a tu ciudad",
    "mensaje": "Los Nuevos se presenta en Tandil el 10/05/2027 21:00: Los Nuevos en vivo. Ya podes comprar tus entradas.",
    "referencia": "evento:5", "estado": "ENVIADA", "fecha": "2026-11-16T11:40:00" } ]
```

Los avisos **no se crean por HTTP**: se generan al consumir los mensajes de RabbitMQ (ver `02-arquitectura.md`, sección 11).

---

# Control de acceso (ventas-service)

## POST /accesos  ·  ACCESO o ADMIN
El personal escanea el QR en la puerta del evento. La entrada queda marcada como `USADA`.

| Código | Cuándo |
|---|---|
| 200 | Ingreso permitido |
| 403 | No es personal de acceso ni administrador |
| 404 | No existe una entrada con ese código |
| 409 | Ya fue utilizada (indica cuándo), es de otro evento, o no está vigente (devuelta o evento cancelado) |

Request: `{ "eventoId": 5, "codigoQr": "3f6c2a1e-6b0a-4e7e-9a59-2f5b1c7d8e90" }`

Response 200: `{ "resultado": "PERMITIDO", "entradaId": 31, "eventoId": 5, "sectorId": 1, "fila": null, "numero": null, "fechaUso": "2027-05-10T20:15:03" }`

---

# Reportes y eventos del organizador (catalogo-service)

Al crear un evento, quien lo crea queda como su organizador (`organizadorId`). `PUT /eventos/{id}/precios` y
`POST /eventos/{id}/publicacion` responden **403** si lo intenta otro organizador. `POST /eventos` acepta además
`"filaVirtual": true` para eventos de alta demanda.

## GET /eventos/mis-eventos  ·  ORGANIZADOR o ADMIN
Eventos creados por el organizador, en cualquier estado. Un administrador ve todos.

## GET /eventos/{id}/reporte  ·  organizador dueño del evento o ADMIN

| Código | Cuándo |
|---|---|
| 200 | Reporte |
| 403 | No es el organizador del evento ni administrador |
| 404 | No existe el evento |
| 503 | El servicio de ventas no responde |

Response 200 (resumido):
```json
{ "eventoId": 5, "evento": "Los Nuevos en vivo", "estado": "PUBLICADO", "ciudad": "Tandil",
  "sectores": [
    { "sector": "Campo", "tipo": "NO_NUMERADO", "precio": 20000, "capacidad": 5000, "vendidas": 1250,
      "disponibles": 3750, "ocupacionPorcentaje": 25.0, "utilizadas": 0, "devueltas": 12, "aReintegrar": 0,
      "recaudacion": 25000000, "montoReintegrado": 240000 },
    { "sector": "Platea", "tipo": "NUMERADO", "capacidad": 50, "vendidas": 50, "ocupacionPorcentaje": 100.0, "...": "..." } ],
  "capacidadTotal": 5050, "vendidas": 1300, "devueltas": 12, "aReintegrar": 0, "utilizadas": 0,
  "ocupacionPorcentaje": 25.7, "recaudacion": 27000000, "montoReintegrado": 240000 }
```

---

# Fila virtual (fila-service)

## POST /filas/{eventoId}/ingreso  ·  CLIENTE
| Código | Cuándo |
|---|---|
| 200 | En la fila (`ESPERANDO`, con posición y tiempo estimado) o ya `HABILITADO` |
| 404 | No existe el evento |
| 409 | El evento no usa fila virtual o no está a la venta |

## GET /filas/{eventoId}/turno  ·  CLIENTE
```json
{ "eventoId": 7, "estado": "ESPERANDO", "posicion": 1234, "personasAdelante": 1233, "tiempoEstimadoSegundos": 125,
  "turno": null, "turnoVenceEn": null, "mensaje": "Estas en la fila: posicion 1234..." }
```
Cuando le toca: `"estado": "HABILITADO"`, `"turno": "eyJhbGciOiJIUzI1NiJ9..."` y `"turnoVenceEn"`. Ese turno se envía en el
header `X-Turno-Fila` de `POST /reservas`. Sin turno, o con uno vencido, de otro usuario o de otro evento, la reserva
responde **403**. `FUERA_DE_FILA`: no está en la fila o se le venció el turno.

## DELETE /filas/{eventoId}/ingreso  ·  CLIENTE
Sale de la fila (y pierde el turno). Responde 204.

## GET /filas/{eventoId}/estado  ·  ORGANIZADOR o ADMIN
`{ "eventoId": 7, "enEspera": 830, "habilitadosEnTotal": 400, "tamanioTanda": 50, "intervaloSegundos": 5, "minutosTurno": 10 }`
