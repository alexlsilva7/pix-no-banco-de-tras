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

# Início do Script Principal
$devices = Get-Devices
if ($devices.Count -eq 0) {
    Write-Host "Aviso: Nenhum celular conectado via ADB." -ForegroundColor Yellow
} else {
    Write-Host "Celulares detectados:" -ForegroundColor Green
    foreach ($d in $devices) {
        Write-Host " - $d" -ForegroundColor Green
    }
}

if (Build-App) {
    if ($devices.Count -gt 0) {
        Deploy-And-Start $devices
    }
}
