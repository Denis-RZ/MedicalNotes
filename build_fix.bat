@echo off
chcp 65001 >nul
echo [32m[1mMedicalNotes - Build Fix Script[0m
echo ===================================
echo [36mВремя запуска:[0m %date% %time%
echo.

echo [36mВыберите действие для исправления:[0m
echo [33m1.[0m Очистить кэш Gradle
echo [33m2.[0m Пересоздать Gradle wrapper
echo [33m3.[0m Исправить проблемы с кириллицей
echo [33m4.[0m Проверить Java версию
echo [33m5.[0m Полная диагностика
echo [33m6.[0m Все исправления сразу
set /p choice="[36mВведите номер (1-6): [0m"

if "%choice%"=="1" goto clean_cache
if "%choice%"=="2" goto recreate_wrapper
if "%choice%"=="3" goto fix_cyrillic
if "%choice%"=="4" goto check_java
if "%choice%"=="5" goto diagnose
if "%choice%"=="6" goto fix_all

:clean_cache
echo.
echo [36m[1mОчистка кэша Gradle...[0m
echo [33mУдаление папок кэша...[0m

if exist ".gradle" (
    echo [33mУдаление .gradle...[0m
    rmdir /s /q ".gradle" 2>nul
    if %errorlevel% equ 0 (
        echo [32m✅ .gradle удален[0m
    ) else (
        echo [31m❌ Ошибка при удалении .gradle[0m
    )
)

if exist "app\build" (
    echo [33mУдаление app\build...[0m
    rmdir /s /q "app\build" 2>nul
    if %errorlevel% equ 0 (
        echo [32m✅ app\build удален[0m
    ) else (
        echo [31m❌ Ошибка при удалении app\build[0m
    )
)

if exist "build" (
    echo [33mУдаление build...[0m
    rmdir /s /q "build" 2>nul
    if %errorlevel% equ 0 (
        echo [32m✅ build удален[0m
    ) else (
        echo [31m❌ Ошибка при удалении build[0m
    )
)

echo [32m✅ Кэш очищен![0m
goto end

:recreate_wrapper
echo.
echo [36m[1mПересоздание Gradle wrapper...[0m
echo [33mУдаление старого wrapper...[0m

if exist "gradle" (
    rmdir /s /q "gradle" 2>nul
    echo [32m✅ Старый wrapper удален[0m
)

if exist "gradlew.bat" (
    del "gradlew.bat" 2>nul
    echo [32m✅ gradlew.bat удален[0m
)

if exist "gradlew" (
    del "gradlew" 2>nul
    echo [32m✅ gradlew удален[0m
)

echo [33mКопирование wrapper из рабочего проекта...[0m
if exist "..\AndroidVoiceOn\gradle" (
    xcopy "..\AndroidVoiceOn\gradle" "gradle" /e /i /y >nul
    if %errorlevel% equ 0 (
        echo [32m✅ Gradle wrapper скопирован[0m
    ) else (
        echo [31m❌ Ошибка при копировании wrapper[0m
    )
) else (
    echo [31m❌ Рабочий проект не найден[0m
)

goto end

:fix_cyrillic
echo.
echo [36m[1mИсправление проблем с кириллицей...[0m
echo [33mЗапуск Python скрипта...[0m
py fix_gradle_path.py
goto end

:check_java
echo.
echo [36m[1mПроверка Java версии...[0m
echo [33mКоманда: java -version[0m
echo.
java -version 2>&1
if %errorlevel% equ 0 (
    echo [32m✅ Java работает корректно[0m
) else (
    echo [31m❌ Java не найдена или не работает[0m
)

echo.
echo [33mПроверка JAVA_HOME...[0m
if defined JAVA_HOME (
    echo [32m✅ JAVA_HOME установлен: %JAVA_HOME%[0m
) else (
    echo [31m❌ JAVA_HOME не установлен[0m
)

goto end

:diagnose
echo.
echo [36m[1mПолная диагностика проекта...[0m
echo.

echo [33m1. Проверка структуры проекта...[0m
if exist "app\build.gradle" (
    echo [32m✅ app\build.gradle найден[0m
) else (
    echo [31m❌ app\build.gradle не найден[0m
)

if exist "build.gradle" (
    echo [32m✅ build.gradle найден[0m
) else (
    echo [31m❌ build.gradle не найден[0m
)

if exist "gradlew.bat" (
    echo [32m✅ gradlew.bat найден[0m
) else (
    echo [31m❌ gradlew.bat не найден[0m
)

echo.
echo [33m2. Проверка Java...[0m
java -version 2>&1
if %errorlevel% equ 0 (
    echo [32m✅ Java работает[0m
) else (
    echo [31m❌ Java не работает[0m
)

echo.
echo [33m3. Проверка Gradle...[0m
if exist "gradlew.bat" (
    call gradlew.bat --version >nul 2>&1
    if %errorlevel% equ 0 (
        echo [32m✅ Gradle wrapper работает[0m
    ) else (
        echo [31m❌ Gradle wrapper не работает[0m
    )
) else (
    echo [31m❌ Gradle wrapper не найден[0m
)

echo.
echo [33m4. Проверка кэша...[0m
if exist ".gradle" (
    echo [33m⚠️  .gradle существует (может быть поврежден)[0m
) else (
    echo [32m✅ .gradle не существует (чисто)[0m
)

if exist "app\build" (
    echo [33m⚠️  app\build существует[0m
) else (
    echo [32m✅ app\build не существует (чисто)[0m
)

goto end

:fix_all
echo.
echo [36m[1mВыполнение всех исправлений...[0m
echo.

echo [33m1. Очистка кэша...[0m
call :clean_cache
echo.

echo [33m2. Пересоздание wrapper...[0m
call :recreate_wrapper
echo.

echo [33m3. Исправление кириллицы...[0m
call :fix_cyrillic
echo.

echo [33m4. Проверка Java...[0m
call :check_java
echo.

echo [32m[1m✅ Все исправления выполнены![0m
goto end

:end
echo.
echo [36m[1mИсправления завершены![0m
echo [33mВремя завершения:[0m %date% %time%
echo.
echo [36mНажмите любую клавишу для выхода...[0m
pause >nul 