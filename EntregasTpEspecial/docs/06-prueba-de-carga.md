# Prueba de carga del flujo de compra (JMeter)

## Objetivo

Verificar que el sistema soporta la **apertura de venta de un evento masivo**: muchos clientes comprando en los mismos
segundos, con tiempos de respuesta aceptables, sin errores y, sobre todo, **sin vender dos veces la misma entrada**.

## Escenarios (`carga/compra-masiva.jmx`)

| Escenario | Qué hace cada usuario virtual | Qué se mide |
|---|---|---|
| **1. Apertura de venta** (por defecto 200 usuarios que arrancan en 20 s) | Se registra, inicia sesión, ve el evento, reserva 1 lugar en el Campo, paga y consulta sus entradas: el flujo completo, a través del gateway y con JWT. | Tiempos de respuesta (promedio, p90, p95, p99), errores y throughput de cada paso. |
| **2. Asiento disputado** (por defecto 100 usuarios a la vez) | Se registra, inicia sesión y, **sincronizados con un temporizador**, todos intentan reservar el **mismo asiento** (Platea, fila 1, asiento 1). | Que exactamente **uno** obtenga 201 y el resto 409. La verificación final falla si hubo doble venta. |

El escenario 2 es la prueba de concurrencia más exigente: 100 pedidos llegando en el mismo instante por el mismo
asiento, contra PostgreSQL real y con todos los servicios en Docker.

## Cómo ejecutarla

**Requisitos:** Apache JMeter 5.6 (https://jmeter.apache.org/download_jmeter.cgi, se descomprime y listo; usa el Java 21
que ya tienen) y el sistema levantado **de cero**, porque la prueba usa el evento 1 de los datos de prueba y no tiene que
estar cancelado por una corrida anterior de Postman:

```powershell
cd EntregasTpEspecial
docker compose down -v
docker compose up --build -d
# esperar ~2 minutos y verificar que http://localhost:8080/eventos/1 responde
```

**Modo consola (recomendado para medir: la interfaz gráfica consume recursos y distorsiona los tiempos):**

```powershell
cd EntregasTpEspecial
<carpeta de jmeter>\bin\jmeter.bat -n -t carga\compra-masiva.jmx -l carga\resultados.jtl -e -o carga\reporte
```

Al terminar, abrir `carga\reporte\index.html`: es el informe HTML de JMeter con todas las métricas y gráficos.

**Parámetros** (opcionales, con `-J`): `-Jusuarios=500 -Jrampa=30 -Jdisputa=200`. Por ejemplo, para una carga mayor:

```powershell
<carpeta de jmeter>\bin\jmeter.bat -n -t carga\compra-masiva.jmx -Jusuarios=500 -Jrampa=30 -l carga\resultados-500.jtl -e -o carga\reporte-500
```

Para repetir la prueba hay que volver a levantar el sistema de cero (el Campo tiene 5000 lugares y el asiento disputado
ya quedó tomado). Para abrir el plan en la interfaz gráfica: `jmeter.bat` → File → Open → `carga\compra-masiva.jmx`.

Mientras corre, en **Grafana** (http://localhost:3000) se ve la cantidad de líneas de log por servicio y los errores.

## Resultados

Corrida del 16/11/2026 con los valores por defecto (200 usuarios con rampa de 20 s en el escenario 1, 100 usuarios
simultáneos en el escenario 2), contra el sistema completo en Docker Desktop (16 contenedores) en una sola PC.
Equipo: _(completar: procesador: Intel Core I7, 8gb RAM y 7.606GiB memoria asignada a Docker )_.

**Resumen de JMeter:** `1501 pedidos en 29 s = 51,9 pedidos/s · promedio 193 ms · mínimo 5 ms · máximo 1256 ms · errores: 0 (0,00 %)`

### Escenario 1: apertura de venta (200 usuarios, rampa de 20 s)

| Paso | Muestras | Error % | Promedio (ms) | Mediana (ms) | p90 (ms) | p95 (ms) | p99 (ms) | Máx. (ms) | Throughput (/s) |
|---|---|---|---|---|---|---|---|---|---|
| 01 Registrarse | 300 | 0,00 % | 279 | 146 | 721 | 907 | 1026 | 1049 | 11,6 |
| 02 Login | 300 | 0,00 % | 287 | 127 | 873 | 1066 | 1175 | 1199 | 11,3 |
| 03 Ver el evento | 200 | 0,00 % | 27 | 12 | 60 | 158 | 181 | 228 | 10,5 |
| 04 Reservar 1 lugar en el Campo | 200 | 0,00 % | 131 | 37 | 363 | 814 | 1167 | 1256 | 10,6 |
| 05 Pagar | 200 | 0,00 % | 96 | 32 | 249 | 505 | 741 | 761 | 11,1 |
| 06 Ver mis entradas | 200 | 0,00 % | 38 | 17 | 113 | 154 | 195 | 218 | 11,5 |

(Registro y login tienen 300 muestras porque los 100 usuarios del escenario 2 también se registran e inician sesión.)

### Escenario 2: 100 usuarios por el mismo asiento, al mismo instante

| Resultado | Cantidad |
|---|---|
| Pedidos de reserva del mismo asiento | 100 (todos respondieron 201 o 409: 0 % de errores) |
| Reservas exitosas (201) | **1** |
| Rechazadas por asiento ya tomado (409) | 99 |
| Verificación final "el asiento se reservó UNA sola vez" | **OK** |
| Tiempo de respuesta | promedio 618 ms, p95 842 ms, máximo 858 ms (100 pedidos en menos de 1 s: 115,6 pedidos/s) |

### Criterios de aceptación

| Criterio | Objetivo | Resultado |
|---|---|---|
| Errores en el flujo de compra | 0 % | **0 %** en 1501 pedidos |
| p95 de "Reservar" | < 2000 ms | **814 ms** |
| p95 de "Pagar" | < 2000 ms | **505 ms** |
| Asiento disputado | exactamente 1 reserva | **1 reserva, 99 rechazos** |

## Análisis

- **No hubo ningún error en los 1501 pedidos**, y la garantía central se cumplió bajo presión: de 100 clientes que
  pidieron el mismo asiento en el mismo instante, **uno solo** lo obtuvo y los otros 99 recibieron 409. La restricción
  de unicidad de la base resolvió la carrera.
- **Registro y login son los pasos más lentos** (p95 de 0,9 y 1,1 s). Es esperable: las contraseñas se procesan con
  **BCrypt**, que es lento a propósito para que no se puedan adivinar por fuerza bruta, y además se hacen 300 en pocos
  segundos. En una venta real los clientes ya están registrados y su token dura 60 minutos, así que este costo no
  coincide con la apertura de la venta.
- **Ver el evento es el paso más rápido** (mediana 12 ms, p95 158 ms) gracias a la **caché de Redis**: 200 clientes
  pidieron el mismo evento y la base no recibió esas lecturas.
- **Reservar** tiene mediana baja (37 ms) pero una cola más larga (p95 814 ms, máx. 1,26 s). Es el efecto del **bloqueo
  pesimista** sobre el cupo del Campo: las reservas de la misma zona se ordenan de a una, que es justamente lo que impide
  que el cupo quede negativo. Algunos clientes esperan su turno unos cientos de milisegundos.
- **Pagar** (p95 505 ms) incluye la pasarela simulada, la emisión de las entradas y la publicación del evento en RabbitMQ;
  el aviso y el historial se procesan después, sin demorar la respuesta.
- En el **asiento disputado**, el promedio de 618 ms refleja que los 100 pedidos llegaron juntos (115 por segundo) y
  compitieron por la misma fila de la base. Aun así, todos se respondieron en menos de 0,9 s.
- **Limitación de la prueba:** todo corre en una misma PC (los 16 contenedores y JMeter comparten CPU y memoria), así
  que los tiempos absolutos son una cota pesimista; en servidores separados serían menores. Lo importante es que, con
  todos los clientes comprando en los mismos segundos, no aparecieron errores ni ventas duplicadas. Se puede repetir con
  más carga (`-Jusuarios=500 -Jdisputa=200`) para buscar el punto en que los tiempos empiezan a degradarse.
- En una venta real masiva, la **fila virtual** limitaría cuántos clientes reservan a la vez (tandas de 50 cada 5 s): esta
  prueba es el peor caso, con todos comprando sin fila.

## Cómo escalaría

- **Ventas** es el cuello de botella natural (escritura con bloqueos). Al ser *stateless*, se pueden levantar varias
  instancias detrás del gateway: la garantía de no doble venta está en la base, no en la memoria de una instancia.
- La **fila virtual** regula el ingreso en las aperturas masivas.
- La **caché** saca de la base las lecturas del catálogo.
- El pool de conexiones a la base (HikariCP, 10 por defecto) se puede ajustar según la cantidad de instancias.
