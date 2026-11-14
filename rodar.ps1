# Sobe o backend (porta 8080) e o frontend (porta 5173) juntos. Windows / PowerShell.
# Uso:  powershell -ExecutionPolicy Bypass -File .\rodar.ps1
# Para ligar a IA do chat, antes de rodar:  $env:ANTHROPIC_API_KEY = "sua-chave"
# Ctrl+C encerra os dois.

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$url = 'http://localhost:5173'

foreach ($cmd in 'java', 'mvn', 'npm') {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Host "Nao encontrei '$cmd' no PATH. Instale Java 21+, Maven 3.9+ e Node 20+." -ForegroundColor Red
        exit 1
    }
}

function Test-Porta($porta) {
    # O Vite pode escutar so em IPv6 (::1) e o Spring em IPv4; testa os dois.
    foreach ($ip in [System.Net.IPAddress]::Loopback, [System.Net.IPAddress]::IPv6Loopback) {
        $c = New-Object System.Net.Sockets.TcpClient($ip.AddressFamily)
        try { $c.Connect($ip, $porta); return $true } catch { } finally { $c.Close() }
    }
    return $false
}

foreach ($porta in 8080, 5173) {
    if (Test-Porta $porta) {
        Write-Host "A porta $porta ja esta em uso. Feche o programa que esta usando e rode de novo." -ForegroundColor Red
        exit 1
    }
}

if (-not (Test-Path (Join-Path $raiz 'frontend\node_modules'))) {
    Write-Host 'Instalando dependencias do frontend (so na primeira vez)...'
    Push-Location (Join-Path $raiz 'frontend')
    try { npm install; if ($LASTEXITCODE -ne 0) { exit 1 } } finally { Pop-Location }
}

if ($env:ANTHROPIC_API_KEY) {
    Write-Host 'Chat: IA ligada (ANTHROPIC_API_KEY encontrada).' -ForegroundColor Green
} else {
    Write-Host 'Chat: modo demonstracao (defina ANTHROPIC_API_KEY para ligar a IA).' -ForegroundColor Yellow
}

Write-Host 'Subindo backend e frontend...'
$back = Start-Process cmd.exe -ArgumentList '/c', 'mvn -q spring-boot:run' `
    -WorkingDirectory (Join-Path $raiz 'backend') -NoNewWindow -PassThru
$front = Start-Process cmd.exe -ArgumentList '/c', 'npm run dev' `
    -WorkingDirectory (Join-Path $raiz 'frontend') -NoNewWindow -PassThru

try {
    $pronto = $false
    for ($i = 0; $i -lt 180 -and -not $back.HasExited -and -not $front.HasExited; $i++) {
        if ((Test-Porta 8080) -and (Test-Porta 5173)) { $pronto = $true; break }
        Start-Sleep -Seconds 1
    }
    if ($pronto) {
        Write-Host ''
        Write-Host '==================================================' -ForegroundColor Green
        Write-Host "  Ciclus rodando. Abra no navegador: $url" -ForegroundColor Green
        Write-Host '  Ctrl+C para encerrar.' -ForegroundColor Green
        Write-Host '==================================================' -ForegroundColor Green
        Write-Host ''
    } else {
        Write-Host 'O backend ou o frontend nao subiu. Veja as mensagens acima.' -ForegroundColor Red
    }
    while ($pronto -and -not $back.HasExited -and -not $front.HasExited) {
        Start-Sleep -Seconds 1
    }
} finally {
    foreach ($p in $back, $front) {
        if ($p -and -not $p.HasExited) {
            & taskkill.exe /T /F /PID $p.Id 2>$null | Out-Null
        }
    }
    Write-Host 'Ciclus encerrado.'
}
