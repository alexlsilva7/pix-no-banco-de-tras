# PowerShell Script: watch_and_run.ps1
# Monitora alterações nos fontes, compila, instala nos dispositivos e grava a tela.

$ErrorActionPreference = "Continue"

# Define o caminho do ADB
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (!(Test-Path $adb)) {
    $adb = "adb"
}

# Função para listar os IDs de dispositivos conectados
function Get-Devices {
    $devicesList = & $adb devices
    $devices = @()
    foreach ($line in $devicesList) {
        if ($line -match "^([^\s]+)\s+device$") {
            $devices += $Matches[1]
        }
    }
    return $devices
}

# Função para compilar o APK
function Build-App {
    Write-Host "`n=== Compilando o aplicativo ===" -ForegroundColor Cyan
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Erro na compilação!" -ForegroundColor Red
        return $false
    }
    Write-Host "Compilação bem sucedida!" -ForegroundColor Green
    return $true
}

# Função para instalar e abrir nos celulares
function Deploy-And-Start($devices) {
    $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
    if (!(Test-Path $apkPath)) {
        Write-Host "APK não encontrado em $apkPath" -ForegroundColor Red
        return $false
    }
    
    Write-Host "`n=== Instalando e iniciando nos celulares ===" -ForegroundColor Cyan
    foreach ($dev in $devices) {
        Write-Host "Dispositivo ${dev}: Instalando..." -ForegroundColor Gray
        & $adb -s $dev install -r $apkPath
        
        Write-Host "Dispositivo ${dev}: Iniciando MainActivity..." -ForegroundColor Gray
        & $adb -s $dev shell am start -n com.aistudio.pixbancotras.lxqtzj/com.example.MainActivity
    }
    return $true
}

# Função para iniciar gravações de tela em background nos celulares
function Start-Recording($devices) {
    Write-Host "`n=== Iniciando gravação de tela ===" -ForegroundColor Cyan
    $processes = @{}
    foreach ($dev in $devices) {
        Write-Host "Dispositivo ${dev}: Iniciando gravação em /sdcard/record.mp4..." -ForegroundColor Gray
        # Inicia o screenrecord em background via Start-Process
        # --size 720x1280 garante compatibilidade em mais aparelhos
        $proc = Start-Process -FilePath $adb -ArgumentList "-s $dev shell screenrecord --size 720x1280 --bit-rate 2000000 /sdcard/record.mp4" -PassThru -NoNewWindow
        $processes[$dev] = $proc
    }
    return $processes
}

# Função para parar gravações e trazer arquivos para o PC
function Stop-And-Pull-Recording($devices, $processes, $recordingIndex) {
    Write-Host "`n=== Parando gravações e trazendo vídeos ===" -ForegroundColor Cyan
    
    # Envia sinal de interrupção (Ctrl+C / SIGINT) para o screenrecord no aparelho fechar o arquivo MP4 corretamente
    foreach ($dev in $devices) {
        Write-Host "Dispositivo ${dev}: Finalizando gravação..." -ForegroundColor Gray
        & $adb -s $dev shell pkill -INT screenrecord
        
        # Fecha o processo local do adb associado se ainda estiver rodando
        $proc = $processes[$dev]
        if ($proc -and !$proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    }
    
    # Aguarda 2 segundos para o processo finalizar a gravação e fechar o arquivo
    Start-Sleep -Seconds 2
    
    # Cria pasta recordings local se não existir
    if (!(Test-Path "./recordings")) {
        New-Item -ItemType Directory -Path "./recordings" | Out-Null
    }
    
    # Faz o pull do arquivo final de vídeo
    foreach ($dev in $devices) {
        $cleanDevName = $dev -replace "[^a-zA-Z0-9]", "_"
        $localPath = "./recordings/record_${cleanDevName}_cycle${recordingIndex}.mp4"
        Write-Host "Dispositivo ${dev}: Puxando vídeo gravado para $localPath" -ForegroundColor Green
        & $adb -s $dev pull /sdcard/record.mp4 $localPath
        
        # Remove arquivo temporário do celular
        & $adb -s $dev shell rm /sdcard/record.mp4 | Out-Null
    }
}

# Início do Script Principal
$devices = Get-Devices
if ($devices.Count -eq 0) {
    Write-Host "Aviso: Nenhum celular conectado via ADB inicialmente. Conecte os celulares para monitorá-los." -ForegroundColor Yellow
} else {
    Write-Host "Celulares detectados inicialmente:" -ForegroundColor Green
    foreach ($d in $devices) {
        Write-Host " - $d" -ForegroundColor Green
    }
}

$recordingIndex = 1
$activeProcesses = $null

# Executa o ciclo inicial se houver dispositivos
if ($devices.Count -gt 0) {
    if (Build-App) {
        if (Deploy-And-Start $devices) {
            $activeProcesses = Start-Recording $devices
        }
    }
} else {
    # Se não houver aparelhos no início, apenas faz a compilação de validação
    Build-App | Out-Null
}

$watchPath = Resolve-Path "app/src/main"
Write-Host "`nMonitorando alterações em: $watchPath" -ForegroundColor Magenta
Write-Host "Modifique qualquer arquivo do código e salve para reiniciar o ciclo automaticamente." -ForegroundColor Magenta
Write-Host "Pressione CTRL+C para encerrar o monitoramento." -ForegroundColor Yellow

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $watchPath
$watcher.IncludeSubdirectories = $true
$watcher.EnableRaisingEvents = $true

try {
    while ($true) {
        # Aguarda eventos por até 1 segundo
        $change = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, 1000)
        
        if ($change.TimedOut) {
            continue
        }
        
        # Debounce de 1 segundo para evitar disparos múltiplos ao salvar vários arquivos
        Start-Sleep -Seconds 1
        
        Write-Host "`n[Alteração Detectada] Arquivo: $($change.Name) ($($change.ChangeType))" -ForegroundColor Yellow
        
        # Para gravação atual nos celulares que estavam gravando
        if ($activeProcesses) {
            Stop-And-Pull-Recording $devices $activeProcesses $recordingIndex
            $recordingIndex++
            $activeProcesses = $null
        }
        
        # Busca a lista atualizada de dispositivos conectados no momento da alteração!
        $devices = Get-Devices
        if ($devices.Count -eq 0) {
            Write-Host "Nenhum celular conectado via ADB neste ciclo. Pulando implantação..." -ForegroundColor Yellow
            continue
        }
        
        Write-Host "Celulares ativos neste ciclo:" -ForegroundColor Green
        foreach ($d in $devices) {
            Write-Host " - $d" -ForegroundColor Green
        }
        
        # Executa compilação, implantação e inicia nova gravação nos aparelhos atuais
        if (Build-App) {
            if (Deploy-And-Start $devices) {
                $activeProcesses = Start-Recording $devices
            }
        }
    }
}
finally {
    Write-Host "`nEncerrando monitoramento e limpando gravações pendentes..." -ForegroundColor Yellow
    if ($activeProcesses) {
        Stop-And-Pull-Recording $devices $activeProcesses $recordingIndex
    }
    $watcher.Dispose()
    Write-Host "Concluído!" -ForegroundColor Green
}
