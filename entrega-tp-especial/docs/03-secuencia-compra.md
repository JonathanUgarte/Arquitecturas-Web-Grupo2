# Diagrama de secuencia: compra de una entrada numerada

Flujo completo de la compra de un asiento numerado, incluyendo los casos en que **otro cliente ya tomó el asiento**,
**el pago es rechazado** y **la reserva vence**.

```mermaid
sequenceDiagram
    actor C as Cliente
    participant G as API Gateway
    participant V as ventas-service
    participant K as catalogo-service
    participant DB as BD ventas
    participant P as Pasarela de pagos (simulada)

    Note over C,P: 1) RESERVA TEMPORAL
    C->>G: POST /reservas (evento, sector, asientos)
    G->>V: POST /reservas
    V->>K: GET /interno/eventos/{e}/sectores/{s}
    K-->>V: estado del evento, fecha, filas, asientos y precio
    V->>V: valida evento PUBLICADO, fecha futura y asientos existentes
    V->>DB: transaccion: libera vencidas, valida limite por cliente, inserta reserva y asientos (UNIQUE)
    alt otro cliente ya tomo el asiento
        DB-->>V: violacion de unicidad y rollback
        V-->>C: 409 asiento ya reservado o vendido
    else asiento libre
        DB-->>V: confirmada
        V-->>C: 201 reserva PENDIENTE (vence en 10 minutos)
    end

    Note over C,P: 2) PAGO (Idempotency-Key en el header)
    C->>G: POST /reservas/{id}/pago
    G->>V: POST /reservas/{id}/pago
    V->>DB: transaccion 1: bloquea la reserva y valida dueno, estado y vencimiento
    alt reserva vencida
        V->>DB: libera los asientos y marca VENCIDA
        V-->>C: 409 la reserva vencio
    else reserva vigente
        V->>DB: reserva EN_PAGO y compra PROCESANDO
        Note over V,P: la llamada a la pasarela va FUERA de toda transaccion
        V->>P: cobrar(monto, medio de pago)
        alt pago aprobado
            P-->>V: aprobado y numero de transaccion
            V->>DB: transaccion 3: reserva CONFIRMADA, compra APROBADA, entradas con QR unico
            V-->>C: 200 compra y entradas
        else pago rechazado
            P-->>V: rechazado
            V->>DB: transaccion 3: reserva CANCELADA, compra RECHAZADA, libera los asientos
            V-->>C: 402 pago rechazado
        end
    end

    Note over C,V: 3) REINTENTO POR CORTE DE CONEXION
    C->>G: POST /reservas/{id}/pago (misma Idempotency-Key)
    G->>V: POST /reservas/{id}/pago
    V->>DB: busca la compra por clave de idempotencia
    DB-->>V: compra ya resuelta
    V-->>C: mismo resultado, sin cobrar de nuevo
```

## Qué pasa en cada caso

| Situación | Resultado | Estado final |
|---|---|---|
| Asiento libre y pago aprobado | 201 y luego 200, entradas emitidas | Reserva `CONFIRMADA`, compra `APROBADA`, entradas `VENDIDA` |
| Otro cliente ya tomó el asiento | 409 en la reserva | No se crea nada (rollback) |
| Pago rechazado | 402 | Reserva `CANCELADA`, compra `RECHAZADA`, asiento libre |
| Reserva vencida sin pagar | 409 al pagar | Reserva `VENCIDA`, asiento libre |
| Reintento con la misma clave | Mismo resultado | Sin cobro duplicado ni entradas duplicadas |
| Catálogo no responde | 503 en la reserva | No se crea nada |
