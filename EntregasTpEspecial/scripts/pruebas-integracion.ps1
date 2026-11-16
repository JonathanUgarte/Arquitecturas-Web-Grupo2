# Pruebas de integracion entre microservicios: levanta el sistema completo con docker compose (16 contenedores)
# y corre la coleccion de Postman entera con Newman (en un contenedor, no hace falta instalar nada).
# Cada request pasa por el gateway y atraviesa los servicios reales: Postgres, MongoDB, RabbitMQ y Redis.
# Uso (desde la raiz del proyecto):  powershell -ExecutionPolicy Bypass -File scripts/pruebas-integracion.ps1
#   -SinReconstruir   reutiliza el sistema ya levantado (no hace down/up)

param([switch]$SinReconstruir)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

if (-not $SinReconstruir) {
    Write-Host "== Levantando el sistema desde cero (docker compose down -v / up --build) =="
    docker compose down -v
    docker compose up --build -d
    if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo" }
}

Write-Host "== Esperando a que el gateway y los servicios respondan =="
$listo = $false
for ($i = 1; $i -le 60; $i++) {
    try {
        $evento = Invoke-WebRequest -UseBasicParsing "http://localhost:8080/eventos/1" -TimeoutSec 5
        $docs = Invoke-WebRequest -UseBasicParsing "http://localhost:8080/api-docs/fila" -TimeoutSec 5
        if ($evento.StatusCode -eq 200 -and $docs.StatusCode -eq 200) { $listo = $true; break }
    } catch { }
    Start-Sleep -Seconds 5
}
if (-not $listo) { throw "El sistema no respondio en 5 minutos (ver: docker compose ps / docker compose logs)" }
Start-Sleep -Seconds 10   # margen para que terminen de conectarse los consumidores de RabbitMQ

Write-Host "== Corriendo la coleccion de Postman con Newman =="
New-Item -ItemType Directory -Force -Path (Join-Path $raiz "carga/integracion") | Out-Null
docker run --rm `
    -v "${raiz}/postman:/etc/newman" `
    -v "${raiz}/carga/integracion:/etc/newman/reporte" `
    postman/newman:6-alpine run TicketYa.postman_collection.json `
    --env-var "baseUrl=http://host.docker.internal:8080" `
    --delay-request 50 `
    --reporters cli,junit --reporter-junit-export /etc/newman/reporte/resultado.xml

if ($LASTEXITCODE -ne 0) {
    Write-Host "Hubo pruebas fallidas (detalle arriba y en carga/integracion/resultado.xml)" -ForegroundColor Red
    exit 1
}
Write-Host "Todas las pruebas de integracion pasaron (reporte JUnit: carga/integracion/resultado.xml)" -ForegroundColor Green
