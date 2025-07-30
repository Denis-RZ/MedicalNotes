# Скрипт для установки Python
Write-Host "Начинаем установку Python..." -ForegroundColor Green

# Скачиваем установщик Python
$url = "https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe"
$output = "python-installer.exe"

Write-Host "Скачиваем установщик Python..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $url -OutFile $output
    Write-Host "Установщик скачан успешно!" -ForegroundColor Green
    
    # Запускаем установщик
    Write-Host "Запускаем установщик Python..." -ForegroundColor Yellow
    Start-Process -FilePath $output -ArgumentList "/quiet", "InstallAllUsers=1", "PrependPath=1" -Wait
    
    Write-Host "Python установлен успешно!" -ForegroundColor Green
    Write-Host "Проверяем установку..." -ForegroundColor Yellow
    
    # Проверяем установку
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    python --version
    
} catch {
    Write-Host "Ошибка при установке: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Попробуйте установить Python вручную с сайта: https://www.python.org/downloads/" -ForegroundColor Yellow
}

# Очищаем временный файл
if (Test-Path $output) {
    Remove-Item $output
    Write-Host "Временный файл удален." -ForegroundColor Gray
} 