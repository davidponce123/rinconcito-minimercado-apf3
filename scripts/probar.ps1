# Pruebas unitarias JUnit/Mockito, sin necesitar levantar PostgreSQL.
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
mvn clean test
if ($LASTEXITCODE -ne 0) { throw "Algunas pruebas fallaron. Leer salida de Maven." }
