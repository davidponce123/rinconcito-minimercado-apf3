# Ejecutar en PowerShell desde VS Code: .\scripts\iniciar.ps1
$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
if (-not (Test-Path ".env")) {
    Write-Host "Falta .env. Copia .env.example a .env, luego coloca tus contrasenas." -ForegroundColor Yellow
    exit 1
}
Get-Content ".env" -Encoding UTF8 | ForEach-Object {
    $linea = $_.Trim()
    if ($linea -match '^([A-Za-z_][A-Za-z_0-9]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1],$matches[2],"Process")
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Write-Error "Instala JDK 25 (LTS) y configura PATH"; exit 1 }
$versionJava = (& $env:ComSpec /d /c "java -version 2>&1" | Out-String)
if ($versionJava -notmatch 'version "(\d+)') { Write-Error "No se pudo determinar la versión de Java"; exit 1 }
if ([int]$matches[1] -lt 25) { Write-Error "Este proyecto requiere JDK 25 o posterior; versión detectada: $($matches[1])"; exit 1 }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Write-Error "Instala Maven y configura PATH"; exit 1 }
if ($env:DB_PASSWORD -like "ESCRIBE_AQUI*") { Write-Error "Cambia DB_PASSWORD en .env"; exit 1 }
if ($env:ADMIN_PASSWORD -like "CREA_UNA*") { Write-Error "Cambia ADMIN_PASSWORD en .env"; exit 1 }
Write-Host "Iniciando Rinconcito en http://localhost:8080/login" -ForegroundColor Cyan
mvn spring-boot:run
