# Script para gerar a versão de Release do aplicativo

$ProjectPath = $PSScriptRoot
Set-Location -Path $ProjectPath

Write-Host "Iniciando a compilação de Release..." -ForegroundColor Cyan

# Executa o wrapper do gradle para gerar o APK de release
.\gradlew assembleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build concluído com sucesso!" -ForegroundColor Green
    
    $ApkDir = Join-Path -Path $ProjectPath -ChildPath "app\build\outputs\apk\release"
    
    if (Test-Path $ApkDir) {
        Write-Host "Abrindo a pasta onde o APK foi gerado..." -ForegroundColor Yellow
        Start-Process explorer.exe $ApkDir
    } else {
        Write-Host "Pasta do APK não encontrada: $ApkDir" -ForegroundColor Red
    }
} else {
    Write-Host "Ocorreu um erro durante a compilação." -ForegroundColor Red
}

Write-Host "Pressione qualquer tecla para sair..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
