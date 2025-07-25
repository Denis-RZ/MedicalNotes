#!/usr/bin/env pwsh
# MedicalNotes - Android Build System (PowerShell)
# Универсальный скрипт сборки Android проекта

param(
    [Parameter(Position=0)]
    [ValidateSet("debug", "release", "clean", "version", "fix", "help")]
    [string]$Action = "help"
)

# Настройки цветов для PowerShell
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Header = "Magenta"
}

function Write-Header {
    param([string]$Message)
    Write-Host "`n" -NoNewline
    Write-Host "=" * 60 -ForegroundColor $Colors.Header
    Write-Host $Message -ForegroundColor $Colors.Header
    Write-Host "=" * 60 -ForegroundColor $Colors.Header
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor $Colors.Success
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor $Colors.Error
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor $Colors.Info
}

function Write-Warning {
    param([string]$Message)
    Write-Host "⚠️  $Message" -ForegroundColor $Colors.Warning
}

function Show-Menu {
    Write-Header "MedicalNotes - Android Build System"
    Write-Host "Время запуска: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor $Colors.Info
    Write-Host ""
    
    # Устанавливаем переменные окружения
    $env:GRADLE_OPTS = "-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8"
    $env:JAVA_OPTS = "-Xmx4096m -XX:MaxPermSize=1024m"
    $env:GRADLE_USER_HOME = "C:\gradle_home_clean"
    
    Write-Host "Настройки Gradle:" -ForegroundColor $Colors.Info
    Write-Host "- Память: 4GB" -ForegroundColor $Colors.Warning
    Write-Host "- Кодировка: UTF-8" -ForegroundColor $Colors.Warning
    Write-Host "- Gradle Home: $env:GRADLE_USER_HOME" -ForegroundColor $Colors.Warning
    Write-Host ""
    
    Write-Host "Выберите тип сборки:" -ForegroundColor $Colors.Info
    Write-Host "1. Debug APK (быстрая сборка)" -ForegroundColor $Colors.Warning
    Write-Host "2. Release APK (продакшн)" -ForegroundColor $Colors.Warning
    Write-Host "3. Clean проект (очистка)" -ForegroundColor $Colors.Warning
    Write-Host "4. Проверить версию Gradle" -ForegroundColor $Colors.Warning
    Write-Host "5. Исправить проблемы Gradle" -ForegroundColor $Colors.Warning
    Write-Host "6. Выход" -ForegroundColor $Colors.Warning
    
    $choice = Read-Host "`nВведите номер (1-6)"
    
    switch ($choice) {
        "1" { Build-Debug }
        "2" { Build-Release }
        "3" { Clean-Project }
        "4" { Check-Version }
        "5" { Fix-Gradle }
        "6" { exit }
        default { 
            Write-Error "Неверный выбор. Попробуйте снова."
            Show-Menu
        }
    }
}

function Build-Debug {
    Write-Header "Сборка Debug APK"
    Write-Info "Команда: gradlew.bat assembleDebug --parallel --max-workers=8"
    Write-Host ""
    
    try {
        $result = & ".\gradlew.bat" "assembleDebug" "--parallel" "--max-workers=8" 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Success "Debug APK успешно собран!"
            $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
            if (Test-Path $apkPath) {
                $size = (Get-Item $apkPath).Length
                $sizeMB = [math]::Round($size / 1MB, 2)
                Write-Info "Размер APK: $sizeMB МБ"
            }
        } else {
            Write-Error "Ошибка при сборке Debug APK"
            Write-Host $result -ForegroundColor $Colors.Error
        }
    }
    catch {
        Write-Error "Исключение при сборке: $($_.Exception.Message)"
    }
}

function Build-Release {
    Write-Header "Сборка Release APK"
    Write-Info "Команда: gradlew.bat assembleRelease --parallel --max-workers=8"
    Write-Host ""
    
    try {
        $result = & ".\gradlew.bat" "assembleRelease" "--parallel" "--max-workers=8" 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Success "Release APK успешно собран!"
            $apkPath = "app\build\outputs\apk\release\app-release.apk"
            if (Test-Path $apkPath) {
                $size = (Get-Item $apkPath).Length
                $sizeMB = [math]::Round($size / 1MB, 2)
                Write-Info "Размер APK: $sizeMB МБ"
            }
        } else {
            Write-Error "Ошибка при сборке Release APK"
            Write-Host $result -ForegroundColor $Colors.Error
        }
    }
    catch {
        Write-Error "Исключение при сборке: $($_.Exception.Message)"
    }
}

function Clean-Project {
    Write-Header "Очистка проекта"
    Write-Info "Команда: gradlew.bat clean"
    Write-Host ""
    
    try {
        $result = & ".\gradlew.bat" "clean" 2>&1
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Success "Проект очищен!"
        } else {
            Write-Error "Ошибка при очистке"
            Write-Host $result -ForegroundColor $Colors.Error
        }
    }
    catch {
        Write-Error "Исключение при очистке: $($_.Exception.Message)"
    }
}

function Check-Version {
    Write-Header "Проверка версии Gradle"
    Write-Info "Команда: gradlew.bat --version"
    Write-Host ""
    
    try {
        $result = & ".\gradlew.bat" "--version" 2>&1
        Write-Host $result
    }
    catch {
        Write-Error "Исключение при проверке версии: $($_.Exception.Message)"
    }
}

function Fix-Gradle {
    Write-Header "Исправление проблем Gradle"
    Write-Info "Запуск Python скрипта исправления..."
    Write-Host ""
    
    try {
        if (Test-Path "fix_gradle_path.py") {
            $result = & "py" "fix_gradle_path.py" 2>&1
            Write-Host $result
        } else {
            Write-Error "Файл fix_gradle_path.py не найден"
        }
    }
    catch {
        Write-Error "Исключение при исправлении: $($_.Exception.Message)"
    }
}

function Show-Help {
    Write-Header "Справка по использованию"
    Write-Host "Использование:" -ForegroundColor $Colors.Info
    Write-Host "  .\build.ps1 [действие]" -ForegroundColor $Colors.Warning
    Write-Host ""
    Write-Host "Действия:" -ForegroundColor $Colors.Info
    Write-Host "  debug    - Сборка Debug APK" -ForegroundColor $Colors.Warning
    Write-Host "  release  - Сборка Release APK" -ForegroundColor $Colors.Warning
    Write-Host "  clean    - Очистка проекта" -ForegroundColor $Colors.Warning
    Write-Host "  version  - Проверка версии Gradle" -ForegroundColor $Colors.Warning
    Write-Host "  fix      - Исправление проблем Gradle" -ForegroundColor $Colors.Warning
    Write-Host "  help     - Показать это сообщение" -ForegroundColor $Colors.Warning
    Write-Host ""
    Write-Host "Примеры:" -ForegroundColor $Colors.Info
    Write-Host "  .\build.ps1 debug" -ForegroundColor $Colors.Warning
    Write-Host "  .\build.ps1 clean" -ForegroundColor $Colors.Warning
    Write-Host "  .\build.ps1" -ForegroundColor $Colors.Warning
}

# Основная логика
switch ($Action) {
    "debug" { Build-Debug }
    "release" { Build-Release }
    "clean" { Clean-Project }
    "version" { Check-Version }
    "fix" { Fix-Gradle }
    "help" { Show-Help }
    default { Show-Menu }
}

Write-Host ""
Write-Host "Сборка завершена!" -ForegroundColor $Colors.Header
Write-Host "Время завершения: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor $Colors.Info 