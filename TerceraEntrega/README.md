# Tercera Entrega - Ejercicio Integrador

API REST del Ejercicio Integrador de Arquitecturas Web.

## Estructura

- `controller`: expone los endpoints REST.
- `service`: contiene la logica del ejercicio y convierte entidades a DTO.
- `repository`: acceso a datos con Spring Data JPA.
- `dto`: objetos utilizados por la API.
- `entities`: entidades JPA.
- `resources/data`: CSV utilizados para la carga inicial.

## Pageable

`Pageable` se utiliza donde aporta paginacion y ordenamiento dinamico:

- **c)** todos los estudiantes.
- **e)** estudiantes filtrados por genero.
- **g)** estudiantes filtrados por carrera y ciudad.

Ejemplo:

```text
?page=0&size=10&sort=apellido,asc
```

`Pageable` no reemplaza los filtros. En el punto **g)** la consulta utiliza parametros posicionales JPQL:

- `?1`: nombre de la carrera.
- `?2`: ciudad de residencia.

No se utiliza `ORDER BY ?` porque un parametro JPQL representa un valor, no el nombre de una propiedad. El orden dinamico se envia mediante `Pageable`.

Los puntos **f** y **h** tienen un orden exigido por la consigna, por eso ese orden se mantiene en la consulta/logica y no queda a eleccion del cliente.

## Levantar MySQL

Desde la carpeta `TerceraEntrega`:

```powershell
docker compose up -d
docker compose ps
```

Luego ejecutar `TerceraEntregaApplication` desde IntelliJ.

Servidor:

```text
http://localhost:8080
```

## Pruebas en Postman

### a) Dar de alta un estudiante

```text
POST http://localhost:8080/api/estudiantes
```

Body -> raw -> JSON:

```json
{
  "nombres": "Agustin",
  "apellido": "Rebord",
  "edad": 21,
  "genero": "MASCULINO",
  "numeroDocumento": "99999999",
  "ciudadResidencia": "Tandil",
  "numeroLibretaUniversitaria": "999999"
}
```

### b) Matricular un estudiante

```text
POST http://localhost:8080/api/inscripciones
```

```json
{
  "libreta": "999999",
  "idCarrera": 1,
  "anioInscripcion": 2026
}
```

El `idCarrera` debe existir en la base.

### c) Recuperar todos y especificar ordenamiento

Por apellido ascendente:

```text
GET http://localhost:8080/api/estudiantes?page=0&size=10&sort=apellido,asc
```

Por edad descendente:

```text
GET http://localhost:8080/api/estudiantes?page=0&size=10&sort=edad,desc
```

### d) Recuperar por libreta universitaria

```text
GET http://localhost:8080/api/estudiantes/libreta/999999
```

### e) Recuperar por genero

```text
GET http://localhost:8080/api/estudiantes/genero/MASCULINO?page=0&size=10&sort=apellido,asc
```

### f) Carreras con estudiantes inscriptos

```text
GET http://localhost:8080/api/carreras/con-inscriptos
```

La consulta devuelve las carreras ordenadas por cantidad de inscriptos de mayor a menor.

### g) Estudiantes de una carrera filtrados por ciudad

```text
GET http://localhost:8080/api/estudiantes/carrera/TUARI/ciudad/Tandil?page=0&size=10&sort=apellido,asc
```

Cambiar `TUARI` y `Tandil` por valores existentes en la base si es necesario.

### h) Reporte de carreras

```text
GET http://localhost:8080/api/carreras/reporte
```

Devuelve inscriptos y egresados por anio, con carreras alfabeticamente y anios cronologicamente.
