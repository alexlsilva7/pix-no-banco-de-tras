$ErrorActionPreference = "Continue"

$ProjectPath = $PSScriptRoot
Set-Location -Path $ProjectPath

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

# Função para compilar o APK de Release
function Build-Release {
    Write-Host "`n=== Compilando o aplicativo (Release) ===" -ForegroundColor Cyan
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    & .\gradlew.bat assembleRelease
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Erro na compilação!" -ForegroundColor Red
        return $false
    }
    Write-Host "Compilação bem sucedida!" -ForegroundColor Green
    return $true
}

# Função para instalar e abrir nos celulares
function Deploy-And-Start($devices) {
    # Procura o APK gerado na pasta de release
    $apkDir = Join-Path -Path $ProjectPath -ChildPath "app\build\outputs\apk\release"
    if (!(Test-Path $apkDir)) {
        Write-Host "Pasta do APK não encontrada: $apkDir" -ForegroundColor Red
        return $false
    }
    
    $apkFile = Get-ChildItem -Path $apkDir -Filter "*.apk" | Select-Object -First 1
    if ($null -eq $apkFile) {
        Write-Host "Nenhum APK encontrado em $apkDir" -ForegroundColor Red
        return $false
    }
    
    $apkPath = $apkFile.FullName
    Write-Host "`n=== Instalando e iniciando nos celulares ===" -ForegroundColor Cyan
    foreach ($dev in $devices) {
        Write-Host "Dispositivo ${dev}: Instalando APK..." -ForegroundColor Gray
        & $adb -s $dev install -r $apkPath
        
        Write-Host "Dispositivo ${dev}: Iniciando MainActivity..." -ForegroundColor Gray
        & $adb -s $dev shell am start -n com.alexlopes.pixdrive/com.alexlopes.pixdrive.MainActivity
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

if (Build-Release) {
    if ($devices.Count -gt 0) {
        Deploy-And-Start $devices
    }
    
    $apkDir = Join-Path -Path $ProjectPath -ChildPath "app\build\outputs\apk\release"
    if (Test-Path $apkDir) {
        Write-Host "`nAbrindo a pasta onde o APK foi gerado..." -ForegroundColor Yellow
        Start-Process explorer.exe $apkDir
    }
}

Write-Host "Pressione qualquer tecla para sair..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
