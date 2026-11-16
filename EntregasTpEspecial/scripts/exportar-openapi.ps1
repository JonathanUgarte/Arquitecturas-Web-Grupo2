# Exporta la documentacion OpenAPI de los 6 servicios (la publica el gateway en /api-docs/{servicio})
# a docs/openapi-{servicio}.json. Requiere el sistema levantado: docker compose up -d
# Uso (desde la raiz del proyecto):  powershell -ExecutionPolicy Bypass -File scripts/exportar-openapi.ps1

$ErrorActionPreference = "Stop"
$gateway = "http://localhost:8080"
$raiz = Split-Path -Parent $PSScriptRoot
$servicios = "auth", "catalogo", "ventas", "preferencias", "notificaciones", "fila"

foreach ($s in $servicios) {
    $destino = Join-Path $raiz "docs/openapi-$s.json"
    Invoke-WebRequest -UseBasicParsing "$gateway/api-docs/$s" -OutFile $destino
    Write-Host "OK  $s -> docs/openapi-$s.json"
}
Write-Host "Listo: se pueden abrir en https://editor.swagger.io (File > Import file)."
