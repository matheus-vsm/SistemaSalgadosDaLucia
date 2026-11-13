[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('test', 'start', 'reload', 'stop')]
    [string]$Action,

    [Parameter(Mandatory = $true)]
    [string]$NginxHome
)

$ErrorActionPreference = 'Stop'
$nginxDirectory = (Resolve-Path -LiteralPath $NginxHome).Path
$nginxExecutable = Join-Path $nginxDirectory 'nginx.exe'
$mimeTypes = Join-Path $nginxDirectory 'conf/mime.types'
foreach ($requiredFile in @($nginxExecutable, $mimeTypes)) {
    if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
        throw "Arquivo ausente na distribuicao do Nginx: $requiredFile"
    }
}

$repositoryDirectory = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$frontendDirectory = Join-Path $repositoryDirectory 'frontend'
$runtimeDirectory = Join-Path $PSScriptRoot '.runtime'
$configurationFile = Join-Path $runtimeDirectory 'nginx.conf'
$pidFile = Join-Path $runtimeDirectory 'logs/nginx.pid'

function ConvertTo-NginxPath([string]$Path) {
    # Nginx aceita barras normais no Windows; escapar caracteres da sua sintaxe.
    return $Path.Replace('\', '/').Replace('$', '\$').Replace('"', '\"')
}

New-Item -ItemType Directory -Path (Join-Path $runtimeDirectory 'logs') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $runtimeDirectory 'temp') -Force | Out-Null
$prefix = (ConvertTo-NginxPath $runtimeDirectory) + '/'
$configPath = ConvertTo-NginxPath $configurationFile

function Invoke-Nginx([string[]]$NginxArguments) {
    & $nginxExecutable -p $prefix -c $configPath @NginxArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Nginx falhou (codigo $LASTEXITCODE). Consulte $runtimeDirectory/logs/error.log"
    }
}

function Test-InstancePid {
    # nginx -t pode criar o arquivo de PID vazio no Windows.
    return (Test-Path -LiteralPath $pidFile) -and
        ((Get-Content -LiteralPath $pidFile -Raw) -match '^\s*[1-9][0-9]*\s*$')
}

if ($Action -eq 'stop') {
    if (-not (Test-InstancePid)) {
        throw 'Esta instancia nao possui PID. Verifique se o Nginx foi iniciado pelo auxiliar.'
    }
    Invoke-Nginx @('-s', 'quit')
    Write-Host 'Encerramento solicitado para a instancia deste projeto.'
    return
}

if ($Action -eq 'start' -and (Test-InstancePid)) {
    throw 'Ja existe um PID desta instancia. Use reload ou stop; confira o processo antes de remover um PID antigo.'
}
if ($Action -eq 'reload' -and -not (Test-InstancePid)) {
    throw 'Esta instancia nao foi iniciada. Use start primeiro.'
}

$backendUrl = 'http://127.0.0.1:8080'
if ($null -ne $env:BACKEND_URL) {
    $backendUrl = $env:BACKEND_URL.Trim().TrimEnd('/')
}
$backendUri = $null
if ($backendUrl -cnotmatch '\Ahttps?://[A-Za-z0-9._\-\[\]:]+\z' -or
    -not [System.Uri]::TryCreate($backendUrl, [System.UriKind]::Absolute, [ref]$backendUri) -or
    [string]::IsNullOrEmpty($backendUri.Host)) {
    throw 'BACKEND_URL deve conter apenas http(s)://host:porta, sem caminho, credenciais, query ou fragmento.'
}

$template = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'nginx.conf.template') -Raw
$configuration = $template.Replace('__MIME_TYPES__', (ConvertTo-NginxPath $mimeTypes)).Replace(
    '__FRONTEND_ROOT__', (ConvertTo-NginxPath $frontendDirectory)).Replace('__BACKEND_URL__', $backendUrl)
[System.IO.File]::WriteAllText($configurationFile, $configuration, [System.Text.UTF8Encoding]::new($false))
Invoke-Nginx @('-t')

switch ($Action) {
    'start' {
        $listener = Get-NetTCPConnection -State Listen -LocalPort 5500 -ErrorAction SilentlyContinue
        if ($listener) {
            throw 'Porta 5500 ocupada. Encerre o Python/Live Server ou o processo que usa essa porta.'
        }
        # A janela fica oculta; nao usa os arquivos de PID/config da instalacao.
        Start-Process -FilePath $nginxExecutable -WorkingDirectory $runtimeDirectory -WindowStyle Hidden `
            -ArgumentList "-p `"$prefix`" -c `"$configPath`""
        Start-Sleep -Milliseconds 500
        if (-not (Test-InstancePid)) {
            throw "Nginx nao iniciou. Consulte $runtimeDirectory/logs/error.log"
        }
        Write-Host 'Frontend disponivel em http://127.0.0.1:5500'
    }
    'reload' { Invoke-Nginx @('-s', 'reload') }
}
