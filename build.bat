@echo off
chcp 65001 >nul
echo [32m[1mMedicalNotes - Android Build System[0m
echo ======================================
echo [36mВремя запуска:[0m %date% %time%
echo.

REM Устанавливаем переменные окружения для Gradle
set GRADLE_OPTS=-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8
set JAVA_OPTS=-Xmx4096m -XX:MaxPermSize=1024m
set GRADLE_USER_HOME=C:\gradle_home_clean

echo [36mНастройки Gradle:[0m
echo [33m- Память:[0m 4GB
echo [33m- Кодировка:[0m UTF-8
echo [33m- Gradle Home:[0m %GRADLE_USER_HOME%
echo.

echo [36mВыберите тип сборки:[0m
echo [33m1.[0m Debug APK (быстрая сборка)
echo [33m2.[0m Release APK (продакшн)
echo [33m3.[0m Clean проект (очистка)
echo [33m4.[0m Проверить версию Gradle
echo [33m5.[0m Исправить проблемы Gradle
set /p choice="[36mВведите номер (1-5): [0m"

if "%choice%"=="1" goto debug
if "%choice%"=="2" goto release
if "%choice%"=="3" goto clean
if "%choice%"=="4" goto version
if "%choice%"=="5" goto fix

:debug
echo.
echo [36m[1mСборка Debug APK...[0m
echo [33mКоманда:[0m gradlew.bat assembleDebug --parallel --max-workers=8
echo.
call gradlew.bat assembleDebug --parallel --max-workers=8
if %errorlevel% equ 0 (
    echo.
    echo [32m[1m✅ Debug APK успешно собран![0m
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo [33mРазмер APK:[0m %%~zA байт
    )
) else (
    echo.
    echo [31m[1m❌ Ошибка при сборке Debug APK[0m
)
goto end

:release
echo.
echo [36m[1mСборка Release APK...[0m
echo [33mКоманда:[0m gradlew.bat assembleRelease --parallel --max-workers=8
echo.
call gradlew.bat assembleRelease --parallel --max-workers=8
if %errorlevel% equ 0 (
    echo.
    echo [32m[1m✅ Release APK успешно собран![0m
    if exist "app\build\outputs\apk\release\app-release.apk" (
        for %%A in ("app\build\outputs\apk\release\app-release.apk") do echo [33mРазмер APK:[0m %%~zA байт
    )
) else (
    echo.
    echo [31m[1m❌ Ошибка при сборке Release APK[0m
)
goto end

:clean
echo.
echo [36m[1mОчистка проекта...[0m
echo [33mКоманда:[0m gradlew.bat clean
echo.
call gradlew.bat clean
if %errorlevel% equ 0 (
    echo.
    echo [32m[1m✅ Проект очищен![0m
) else (
    echo.
    echo [31m[1m❌ Ошибка при очистке[0m
)
goto end

:version
echo.
echo [36m[1mПроверка версии Gradle...[0m
echo [33mКоманда:[0m gradlew.bat --version
echo.
call gradlew.bat --version
goto end

:fix
echo.
echo [36m[1mИсправление проблем Gradle...[0m
echo [33mЗапуск Python скрипта исправления...[0m
echo.
py fix_gradle_path.py
goto end

:end
echo.
echo [36m[1mСборка завершена![0m
echo [33mВремя завершения:[0m %date% %time%
echo.
echo [36mНажмите любую клавишу для выхода...[0m
pause >nul 