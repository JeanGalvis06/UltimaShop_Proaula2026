# Carga la configuracion privada de Brevo (.env.brevo) como variables de entorno del proceso.
# No imprime valores. Fuerza MAIL_ENABLED=false para NO enviar correos reales todavia.
#
# Uso (importantisimo: punto inicial para que las variables queden en tu sesion):
#     . .\cargar-brevo.ps1
#
# Si se ejecuta como ".\cargar-brevo.ps1" las variables mueren al terminar el script.
# El archivo .env.brevo esta ignorado por Git (local-dev/.gitignore, regla .env.*).

$ErrorActionPreference = 'Stop'
$ruta = Join-Path $PSScriptRoot '.env.brevo'

if (-not (Test-Path -LiteralPath $ruta)) {
    throw "No se encontro la configuracion privada en: $ruta"
}

# Solo se aceptan estas claves; cualquier otra se ignora con aviso (sin mostrar su valor).
$permitidas = @(
    'BREVO_API_KEY',
    'MAIL_FROM_EMAIL',
    'MAIL_FROM_NAME',
    'APP_BASE_URL',
    'MAIL_ENABLED'
)

$cargadas = @()
$omitidas = @()

foreach ($linea in (Get-Content -LiteralPath $ruta)) {
    if ([string]::IsNullOrWhiteSpace($linea)) { continue }
    if ($linea.TrimStart().StartsWith('#')) { continue }

    if ($linea -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
        Write-Warning 'Se ignoro una linea con formato no reconocido (valor no mostrado).'
        continue
    }

    $clave = $Matches[1]
    $valor = $Matches[2]

    if ($clave -notin $permitidas) {
        $omitidas += $clave
        continue
    }

    # Permite valores entre comillas simples o dobles.
    if ($valor.Length -ge 2 -and
        (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or
         ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
        $valor = $valor.Substring(1, $valor.Length - 2)
    }

    [System.Environment]::SetEnvironmentVariable($clave, $valor, 'Process')
    $cargadas += $clave
}

# Decision explicita: pase lo que diga el archivo, NO habilitar envios reales todavia.
[System.Environment]::SetEnvironmentVariable('MAIL_ENABLED', 'false', 'Process')

Write-Output "Variables cargadas (sin valores): $($cargadas -join ', ')"
if ($omitidas.Count -gt 0) {
    Write-Warning "Claves no permitidas ignoradas: $($omitidas -join ', ')"
}
Write-Output 'MAIL_ENABLED = false (forzado por el cargador; sin envios reales).'
Write-Output 'Para que persistan en tu sesion, invoca el script con punto: . .\cargar-brevo.ps1'
