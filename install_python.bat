@echo off
echo Начинаем установку Python...

REM Скачиваем установщик Python
echo Скачиваем установщик Python...
powershell -Command "Invoke-WebRequest -Uri 'https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe' -OutFile 'python-installer.exe'"

if exist python-installer.exe (
    echo Установщик скачан успешно!
    echo Запускаем установщик Python...
    python-installer.exe /quiet InstallAllUsers=1 PrependPath=1
    echo Python установлен успешно!
    
    REM Проверяем установку
    echo Проверяем установку...
    python --version
    
    REM Очищаем временный файл
    del python-installer.exe
    echo Временный файл удален.
) else (
    echo Ошибка при скачивании установщика.
    echo Попробуйте установить Python вручную с сайта: https://www.python.org/downloads/
)

pause 