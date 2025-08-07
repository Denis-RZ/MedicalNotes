@echo off
chcp 65001 >nul
echo ========================================
echo ТЕСТ АНАЛИЗА КОНКРЕТНЫХ ДАТ ИЗ XML
echo ========================================
echo.

echo Запуск теста анализа конкретных дат из XML...
echo.

REM Компилируем и запускаем тест
kotlinc test_specific_dates.kt -include-runtime -d test_dates.jar
if %errorlevel% neq 0 (
    echo ОШИБКА: Не удалось скомпилировать тест
    pause
    exit /b 1
)

echo.
echo Запуск теста...
echo.

java -jar test_dates.jar > test_dates_results.txt 2>&1

echo.
echo Результаты теста сохранены в файл: test_dates_results.txt
echo.

REM Показываем результаты
type test_dates_results.txt

echo.
echo ========================================
echo ТЕСТ ЗАВЕРШЕН
echo ========================================
echo.
echo Результаты сохранены в файл: test_dates_results.txt
echo.

pause 