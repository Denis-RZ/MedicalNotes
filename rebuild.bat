@echo off
chcp 65001 >nul
echo 🏥 MedicalNotes - Пересборка APK
echo ======================================
echo 📅 Время начала: %date% %time%
echo.

echo 🔨 Очистка проекта...
call gradlew.bat clean --quiet
echo.

echo 🔨 Сборка Debug APK...
call gradlew.bat assembleDebug --quiet
echo.

if %errorlevel% equ 0 (
    echo ✅ Сборка завершена успешно!
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do (
            echo 📱 APK создан: %%~fA
            echo 📊 Размер: %%~zA байт
        )
    ) else (
        echo ❌ APK файл не найден!
    )
) else (
    echo ❌ Ошибка при сборке!
)

echo.
echo 📅 Время завершения: %date% %time%
pause 