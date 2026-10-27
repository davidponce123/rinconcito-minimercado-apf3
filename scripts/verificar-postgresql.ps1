
# Verificacion de PostgreSQL - Rinconcito APF3
$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$archivoEnv = Join-Path $raiz ".env"

# 1. Comprobar que exista la configuracion local
if (-not (Test-Path $archivoEnv)) {
    throw "No existe .env. Copia .env.example a .env."
}

# 2. Leer las variables sin mostrar contrasenas
$config = @{}

foreach ($linea in Get-Content $archivoEnv -Encoding UTF8) {
    $linea = $linea.Trim()

    if ($linea -eq "" -or $linea.StartsWith("#")) {
        continue
    }

    if ($linea -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $config[$Matches[1]] = $Matches[2].Trim()
    }
}

# 3. Validar campos obligatorios
$requeridas = @(
    "DB_URL",
    "DB_USER",
    "DB_PASSWORD",
    "ADMIN_USER",
    "ADMIN_PASSWORD"
)

foreach ($nombre in $requeridas) {
    if (-not $config.ContainsKey($nombre) -or
        [string]::IsNullOrWhiteSpace($config[$nombre])) {
        throw "Falta configurar $nombre en .env."
    }
}

# 4. Evitar credenciales de ejemplo
if ($config["DB_PASSWORD"].StartsWith("ESCRIBE_AQUI")) {
    throw "Debes configurar tu contraseña real de PostgreSQL."
}

if ($config["ADMIN_PASSWORD"].Length -lt 12 -or
    $config["ADMIN_PASSWORD"].StartsWith("CREA_UNA")) {
    throw "ADMIN_PASSWORD debe contener al menos 12 caracteres."
}

# 5. Comprobar la URL JDBC
$url = $config["DB_URL"]

if ($url -notmatch '^jdbc:postgresql://') {
    throw "DB_URL debe comenzar con jdbc:postgresql://"
}

try {
    $direccion = [Uri]$url.Substring(5)
} catch {
    throw "La URL de PostgreSQL no tiene un formato valido."
}

if ([string]::IsNullOrWhiteSpace($direccion.Host) -or
    $direccion.AbsolutePath -eq "/") {
    throw "DB_URL debe incluir el servidor y la base de datos."
}

$puerto = if ($direccion.Port -gt 0) {
    $direccion.Port
} else {
    5432
}

# 6. Comprobar conectividad TCP
$cliente = [System.Net.Sockets.TcpClient]::new()
$conexion = $null

try {
    $conexion = $cliente.BeginConnect(
        $direccion.Host, $puerto, $null, $null
    )

    if (-not $conexion.AsyncWaitHandle.WaitOne(3000)) {
        throw "Tiempo de espera agotado."
    }

    $cliente.EndConnect($conexion)

    Write-Host "Configuracion local valida." -ForegroundColor Green
    Write-Host "Servidor accesible: $($direccion.Host):$puerto" -ForegroundColor Green
    Write-Host "La conexion TCP responde correctamente."

} catch {
    throw "No se puede acceder al servidor PostgreSQL. Comprueba que el servicio este iniciado y el puerto sea correcto. Detalle: $($_.Exception.Message)"
} finally {
    if ($null -ne $conexion) {
        $conexion.AsyncWaitHandle.Close()
    }
    $cliente.Dispose()
}
