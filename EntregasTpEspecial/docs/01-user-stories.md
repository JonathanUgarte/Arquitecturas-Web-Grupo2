# TicketYa - Features y User Stories

Cada feature agrupa historias de usuario con sus criterios de aceptación (formato Dado / Cuando / Entonces).
La columna **Entrega** indica en cuál se implementa: **E1** (core) o **E2** (sistema completo).
Los números entre paréntesis (p. ej. "req. 4") remiten a la sección FUNCIONALIDAD de la consigna.

Roles: **Cliente**, **Organizador**, **Administrador**, **Personal de acceso**.

| Feature | Requisitos | Entrega |
|---|---|---|
| F1. Lugares y eventos | 1, 2 | E1 |
| F2. Publicación y moderación | 3 | E1 |
| F3. Compra con reserva temporal y entradas con QR | 4 | E1 |
| F4. Garantía de no doble venta | 5 | E1 |
| F5. Devoluciones y cancelación de eventos | 7 | E1 |
| F6. Búsqueda de eventos | 9 | E1 |
| F7. Fila virtual para eventos de alta demanda | 6 | E2 |
| F8. Control de acceso con QR | 8 | E2 |
| F9. Preferencias y recomendaciones | 10 | E2 |
| F10. Notificaciones | 11 | E2 |
| F11. Reportes para organizadores | 12 | E2 |
| F12. Seguridad (usuarios y roles) | transversal | E2 |

---

## F1. Lugares y eventos (E1)

### US-01 Registrar un lugar con sus sectores
**Como** organizador **quiero** registrar un lugar (estadio, teatro, sala) con sectores numerados y no numerados **para** poder asignarle mis eventos.

Criterios de aceptación:
- **Dado** un lugar con un sector NUMERADO de 5 filas y 10 asientos por fila, **cuando** lo registro, **entonces** el sector queda con capacidad total 50.
- **Dado** un sector NO_NUMERADO con capacidad 5000 (p. ej. "Campo"), **cuando** registro el lugar, **entonces** el sector queda con ese cupo máximo.
- **Dado** un sector NUMERADO sin filas o sin asientos por fila, **cuando** registro el lugar, **entonces** recibo 400 con el motivo.
- **Dado** un lugar sin ciudad, nombre o dirección, **cuando** lo registro, **entonces** recibo 400.

### US-02 Crear un evento en borrador
**Como** organizador **quiero** crear un evento con nombre, descripción, tipo, organizador, participantes, fecha y lugar **para** prepararlo antes de publicarlo.

Criterios de aceptación:
- **Dado** datos válidos y un lugar existente, **cuando** creo el evento, **entonces** queda en estado BORRADOR.
- **Dado** una fecha que no es futura, **cuando** creo el evento, **entonces** recibo 400.
- **Dado** un lugar inexistente, **cuando** creo el evento, **entonces** recibo 404.

### US-03 Definir el precio de cada sector
**Como** organizador **quiero** definir el precio de cada sector para mi evento **para** que cada evento tenga su propia tarifa.

Criterios de aceptación:
- **Dado** un evento en BORRADOR, **cuando** envío un precio por sector, **entonces** los precios quedan guardados y reemplazan a los anteriores.
- **Dado** un sector que no pertenece al lugar del evento, **cuando** lo precio, **entonces** recibo 400.
- **Dado** un evento ya publicado, **cuando** intento cambiar precios, **entonces** recibo 409.
- **Dado** un precio menor o igual a cero, **cuando** lo envío, **entonces** recibo 400.

---

## F2. Publicación y moderación (E1)

### US-04 Publicar un evento
**Como** organizador **quiero** publicar mi evento **para** que el público pueda encontrarlo y comprar.

Criterios de aceptación:
- **Dado** un evento en BORRADOR con precio en todos los sectores del lugar, **cuando** lo publico, **entonces** pasa a PUBLICADO.
- **Dado** un evento al que le falta el precio de algún sector, **cuando** lo publico, **entonces** recibo 409 indicando los sectores sin precio.
- **Dado** un evento que no está en BORRADOR, **cuando** intento publicarlo, **entonces** recibo 409.

### US-05 Suspender o cancelar cualquier evento
**Como** administrador **quiero** suspender o cancelar cualquier evento **para** resolver problemas operativos.

Criterios de aceptación:
- **Dado** un usuario sin rol ADMIN, **cuando** intenta suspender o cancelar, **entonces** recibo 403.
- **Dado** un evento PUBLICADO o AGOTADO, **cuando** lo suspendo, **entonces** pasa a SUSPENDIDO y deja de aparecer en la búsqueda.
- **Dado** un evento ya CANCELADO o FINALIZADO, **cuando** intento cancelarlo, **entonces** recibo 409.
- **Dado** un evento que cancelo con un motivo, **cuando** el cambio se confirma, **entonces** queda en CANCELADO con su motivo (ver US-16).

---

## F3. Compra con reserva temporal y entradas con QR (E1)

### US-06 Reservar lugares temporalmente
**Como** cliente **quiero** elegir un evento, un sector y (si es numerado) asientos específicos y que el sistema me los reserve unos minutos **para** pagar con tranquilidad.

Criterios de aceptación:
- **Dado** un evento PUBLICADO y asientos libres de un sector numerado, **cuando** reservo, **entonces** recibo una reserva PENDIENTE con un vencimiento a 10 minutos y el total a pagar.
- **Dado** una zona sin asiento con cupo, **cuando** reservo una cantidad, **entonces** el cupo disponible se reduce en esa cantidad.
- **Dado** un asiento que no existe en el sector (fila o número fuera de rango), **cuando** reservo, **entonces** recibo 400.
- **Dado** un evento que no está PUBLICADO o que ya ocurrió, **cuando** reservo, **entonces** recibo 409.
- **Dado** un sector numerado sin asientos indicados, o una zona sin cantidad, **cuando** reservo, **entonces** recibo 400.

### US-07 Pagar la reserva y recibir las entradas
**Como** cliente **quiero** pagar mi reserva **para** recibir mis entradas con código QR.

Criterios de aceptación:
- **Dado** una reserva PENDIENTE vigente y un pago aprobado, **cuando** pago, **entonces** recibo 200, se emite una entrada por lugar con un código QR único y la reserva pasa a CONFIRMADA.
- **Dado** un pago rechazado, **cuando** pago, **entonces** recibo 402, no se emite ninguna entrada, la reserva se cancela y los lugares quedan libres.
- **Dado** una reserva vencida, **cuando** intento pagar, **entonces** recibo 409 y los lugares quedan libres.
- **Dado** una reserva de otro cliente, **cuando** intento pagarla, **entonces** recibo 403.
- **Dado** una compra aprobada, **entonces** queda registrado el detalle (monto, medio de pago y transacción) y la entrada guarda el precio pagado.

### US-08 Ver mis entradas
**Como** cliente **quiero** ver mis entradas con su código QR **para** presentarlas en el ingreso.

Criterios de aceptación:
- **Dado** que compré entradas, **cuando** consulto mis entradas, **entonces** veo cada una con su evento, sector, asiento, estado y código QR.
- **Dado** la entrada de otro cliente, **cuando** la consulto, **entonces** recibo 403.

### US-09 Límite de entradas por cliente
**Como** organizador **quiero** que cada cliente pueda comprar una cantidad máxima de entradas por evento **para** evitar la reventa masiva.

Criterios de aceptación:
- **Dado** un cliente con 4 entradas vendidas o reservadas para un evento, **cuando** intenta reservar una más, **entonces** recibo 409.
- **Dado** una reserva vencida o cancelada del cliente, **cuando** reserva de nuevo, **entonces** esa reserva no cuenta contra el límite.

---

## F4. Garantía de no doble venta (E1)

### US-10 Un asiento nunca se vende dos veces
**Como** cliente **quiero** tener la certeza de que mi asiento es solo mío **para** no encontrarme con otra persona en el ingreso.

Criterios de aceptación:
- **Dado** 20 clientes que intentan reservar el mismo asiento al mismo tiempo, **cuando** se procesan, **entonces** exactamente uno tiene éxito y los demás reciben 409.
- **Dado** un asiento reservado por otro cliente, **cuando** intento reservarlo, **entonces** recibo 409.
- **Dado** que varias instancias del servicio de ventas atienden pedidos a la vez, **entonces** la garantía se mantiene, porque la valida la base de datos.

### US-11 El último lugar de una zona se vende una sola vez
**Como** organizador **quiero** que el cupo de una zona nunca quede negativo **para** no vender más lugares de los que existen.

Criterios de aceptación:
- **Dado** una zona con 1 lugar y 20 clientes que intentan reservarlo a la vez, **entonces** uno tiene éxito y el cupo queda en 0.
- **Dado** una zona con cupo para 5 y 20 clientes, **entonces** se venden exactamente 5 y el cupo nunca es negativo.

### US-12 Reintentar el pago sin cobrar dos veces
**Como** cliente **quiero** poder reintentar el pago si se cortó mi conexión **para** no pagar dos veces.

Criterios de aceptación:
- **Dado** un pago enviado con una clave de idempotencia (header `Idempotency-Key`), **cuando** lo repito con la misma clave, **entonces** recibo el mismo resultado y no se genera un segundo cobro ni entradas duplicadas.
- **Dado** una clave ya usada en otra reserva, **cuando** la reutilizo, **entonces** recibo 409.
- **Dado** una reserva ya pagada, **cuando** intento pagarla con otra clave, **entonces** recibo 409.

### US-13 Los lugares de una reserva vencida se liberan
**Como** cliente **quiero** que los lugares no pagados vuelvan a estar disponibles **para** poder comprarlos.

Criterios de aceptación:
- **Dado** una reserva PENDIENTE que pasó su vencimiento, **cuando** otro cliente reserva esos lugares, **entonces** puede hacerlo.
- **Dado** una reserva pendiente propia, **cuando** la cancelo, **entonces** los lugares quedan libres.

---

## F5. Devoluciones y cancelación de eventos (E1)

### US-14 Solicitar la devolución de una entrada
**Como** cliente **quiero** solicitar la devolución de una entrada hasta 72 horas antes del evento **para** recuperar mi dinero si no puedo asistir.

Criterios de aceptación:
- **Dado** una entrada vendida y un evento que ocurre dentro de más de 72 horas, **cuando** solicito la devolución, **entonces** queda una solicitud PENDIENTE.
- **Dado** un evento que ocurre dentro de menos de 72 horas, **cuando** solicito la devolución, **entonces** recibo 409.
- **Dado** una entrada que ya tiene una solicitud pendiente, **cuando** solicito otra, **entonces** recibo 409.
- **Dado** la entrada de otro cliente, **cuando** solicito su devolución, **entonces** recibo 403.

### US-15 Aprobar o rechazar una devolución
**Como** administrador **quiero** aprobar o rechazar las solicitudes **para** controlar los reintegros.

Criterios de aceptación:
- **Dado** una solicitud pendiente, **cuando** la apruebo, **entonces** la entrada pasa a DEVUELTA, el asiento o lugar de la zona queda libre y se solicita el reintegro a la pasarela de pagos.
- **Dado** una solicitud pendiente, **cuando** la rechazo, **entonces** la entrada sigue VENDIDA y el asiento sigue ocupado.
- **Dado** una solicitud ya resuelta, **cuando** intento resolverla otra vez, **entonces** recibo 409.
- **Dado** un usuario sin rol ADMIN, **cuando** intenta resolver una solicitud, **entonces** recibo 403.

### US-16 Cancelar un evento y reintegrar
**Como** administrador **quiero** cancelar un evento y que sus entradas vendidas pasen a reintegro **para** devolverles el dinero a los compradores.

Criterios de aceptación:
- **Dado** un evento con entradas vendidas, **cuando** se cancela, **entonces** todas sus entradas VENDIDA pasan a A_REINTEGRAR en una única operación atómica.
- **Dado** un evento cancelado, **cuando** un cliente intenta reservar, **entonces** recibe 409.
- **Dado** reservas pendientes del evento cancelado, **entonces** pasan a CANCELADA y liberan sus lugares.
- **Dado** que el servicio de ventas no responde al momento de cancelar, **entonces** el evento queda igualmente CANCELADO y la respuesta indica que el aviso de reintegro quedó pendiente.
- **Dado** que se avisa dos veces la cancelación de un mismo evento, **entonces** la segunda vez no cambia nada.

---

## F6. Búsqueda de eventos (E1)

### US-17 Buscar eventos por múltiples criterios
**Como** cliente **quiero** buscar eventos por tipo, organizador, participante, ciudad y rango de fechas, y ordenarlos por fecha o precio **para** encontrar rápido lo que me interesa.

Criterios de aceptación:
- **Dado** cualquier combinación de filtros, **cuando** busco, **entonces** obtengo solo los eventos que cumplen todos.
- **Dado** que busco, **entonces** solo aparecen eventos a la venta (PUBLICADO o AGOTADO); no los borradores, suspendidos ni cancelados.
- **Dado** que busco por organizador, participante o ciudad, **entonces** la comparación no distingue mayúsculas y alcanza con una parte del nombre.
- **Dado** el orden "fecha" (por defecto), **entonces** los resultados van del evento más próximo al más lejano.
- **Dado** el orden "precio", **entonces** van de menor a mayor según el precio más bajo de cada evento.
- **Dado** un orden inválido, **cuando** busco, **entonces** recibo 400.
- El orden por **relevancia** se incorpora en la entrega 2 junto con las recomendaciones.

---

## F7. Fila virtual (E2)

### US-18 Entrar a la fila virtual
**Como** cliente **quiero** entrar a una fila virtual cuando se abre la venta de un evento de alta demanda **para** comprar de forma ordenada.
- **Dado** un evento marcado de alta demanda, **cuando** se abre la venta y entro, **entonces** recibo mi posición en la fila.
- **Dado** que mi turno llega, **cuando** consulto, **entonces** quedo habilitado para reservar durante un tiempo limitado.

### US-19 Habilitar por tandas
**Como** organizador **quiero** que la fila habilite a los clientes por tandas **para** no saturar el sistema.
- **Dado** una fila con muchos clientes, **entonces** se habilita una cantidad fija por tanda en orden de llegada.
- **Dado** que una persona habilitada no compra a tiempo, **entonces** pierde el turno y se habilita a la siguiente.

---

## F8. Control de acceso con QR (E2)

### US-20 Validar una entrada al ingreso
**Como** personal de acceso **quiero** escanear el QR **para** validar la entrada.
- **Dado** un QR de una entrada vendida de ese evento, **cuando** lo escaneo, **entonces** se acepta y la entrada pasa a USADA.
- **Dado** un QR inexistente o de otro evento, **cuando** lo escaneo, **entonces** se rechaza.
- **Dado** un QR ya usado, **cuando** lo escaneo de nuevo, **entonces** se rechaza (un QR no ingresa dos veces).
- **Dado** una entrada devuelta o a reintegro, **cuando** la escaneo, **entonces** se rechaza.

---

## F9. Preferencias y recomendaciones (E2)

### US-21 Registrar mis preferencias
**Como** cliente **quiero** indicar mis tipos de evento, artistas favoritos y ciudad **para** recibir sugerencias.
- **Dado** que guardo mis preferencias, **cuando** las consulto, **entonces** veo lo guardado.

### US-22 Recibir recomendaciones
**Como** cliente **quiero** que me sugieran eventos según mi historial **para** descubrir nuevos eventos.
- **Dado** que compré entradas para un evento, **cuando** pido recomendaciones, **entonces** veo eventos que compraron también quienes compraron ese evento.
- **Dado** un cliente sin historial, **entonces** las sugerencias se basan en sus preferencias.

---

## F10. Notificaciones (E2)

### US-23 Confirmación de compra
- **Dado** una compra aprobada, **entonces** el cliente recibe una notificación (email o push) con sus entradas.

### US-24 Cambios y cancelaciones
- **Dado** un evento que se cancela o cambia, **entonces** sus compradores reciben una notificación.

### US-25 Artistas favoritos
- **Dado** que un artista favorito de un cliente publica un evento en su ciudad, **entonces** el cliente recibe una notificación.
- **Dado** que el servicio de notificaciones falla, **entonces** la compra no se ve afectada y la notificación se reintenta.

---

## F11. Reportes para organizadores (E2)

### US-26 Consultar el desempeño de mis eventos
**Como** organizador **quiero** ver entradas vendidas y devueltas por sector, porcentaje de ocupación y recaudación **para** evaluar mis eventos.
- **Dado** un evento mío, **cuando** consulto el reporte, **entonces** veo vendidas y devueltas por sector, ocupación y recaudación.
- **Dado** el evento de otro organizador, **cuando** intento consultarlo, **entonces** recibo 403.

---

## F12. Seguridad (E2)

### US-27 Autenticación y roles
**Como** usuario **quiero** iniciar sesión **para** que el sistema sepa quién soy y qué puedo hacer.
- **Dado** credenciales válidas, **cuando** inicio sesión, **entonces** recibo un token JWT con mi rol.
- **Dado** un token ausente, inválido o vencido, **entonces** recibo 401.
- **Dado** un token válido con un rol que no corresponde al endpoint, **entonces** recibo 403.

> En la entrega 1 no hay autenticación: el cliente se identifica con el header `X-Usuario-Id` y las acciones de
> administrador exigen `X-Rol: ADMIN`.
