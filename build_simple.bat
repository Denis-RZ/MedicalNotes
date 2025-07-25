@echo off
echo MedicalNotes - Android Build System (Simple)
echo ===========================================
echo Время запуска: %date% %time%
echo.

REM Устанавливаем переменные окружения для Gradle
set GRADLE_OPTS=-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8
set JAVA_OPTS=-Xmx4096m -XX:MaxPermSize=1024m
set GRADLE_USER_HOME=C:\gradle_home_clean

echo Настройки Gradle:
echo - Память: 4GB
echo - Кодировка: UTF-8
echo - Gradle Home: %GRADLE_USER_HOME%
echo.

echo Выберите тип сборки:
echo 1. Debug APK (быстрая сборка)
echo 2. Release APK (продакшн)
echo 3. Clean проект (очистка)
echo 4. Проверить версию Gradle
echo 5. Исправить проблемы Gradle
set /p choice="Введите номер (1-5): "

if "%choice%"=="1" goto debug
if "%choice%"=="2" goto release
if "%choice%"=="3" goto clean
if "%choice%"=="4" goto version
if "%choice%"=="5" goto fix

:debug
echo.
echo Сборка Debug APK...
echo Команда: gradlew.bat assembleDebug --parallel --max-workers=8
echo.
call gradlew.bat assembleDebug --parallel --max-workers=8
if %errorlevel% equ 0 (
    echo.
    echo SUCCESS: Debug APK успешно собран!
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo Размер APK: %%~zA байт
    )
) else (
    echo.
    echo ERROR: Ошибка при сборке Debug APK
)
goto end

:release
echo.
echo Сборка Release APK...
echo Команда: gradlew.bat assembleRelease --parallel --max-workers=8
echo.
call gradlew.bat assembleRelease --parallel --max-workers=8
if %errorlevel% equ 0 (
    echo.
    echo SUCCESS: Release APK успешно собран!
    if exist "app\build\outputs\apk\release\app-release.apk" (
        for %%A in ("app\build\outputs\apk\release\app-release.apk") do echo Размер APK: %%~zA байт
    )
) else (
    echo.
    echo ERROR: Ошибка при сборке Release APK
)
goto end

:clean
echo.
echo Очистка проекта...
echo Команда: gradlew.bat clean
echo.
call gradlew.bat clean
if %errorlevel% equ 0 (
    echo.
    echo SUCCESS: Проект очищен!
) else (
    echo.
    echo ERROR: Ошибка при очистке
)
goto end

:version
echo.
echo Проверка версии Gradle...
echo Команда: gradlew.bat --version
echo.
call gradlew.bat --version
goto end

:fix
echo.
echo Исправление проблем Gradle...
echo Запуск Python скрипта исправления...
echo.
py fix_gradle_path.py
goto end

:end
echo.
echo Сборка завершена!
echo Время завершения: %date% %time%
echo.
echo Нажмите любую клавишу для выхода...
pause >nul 