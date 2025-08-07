@echo off
chcp 65001 >nul
echo ========================================
echo ТЕСТ АНАЛИЗА ПРОБЛЕМЫ ОТОБРАЖЕНИЯ ЛЕКАРСТВ
echo ========================================
echo.

echo Запуск тестов анализа проблемы с отображением лекарств...
echo.

REM Компилируем и запускаем тест
kotlinc test_medicine_display_analysis.kt -include-runtime -d test_analysis.jar
if %errorlevel% neq 0 (
    echo ОШИБКА: Не удалось скомпилировать тест
    pause
    exit /b 1
)

echo.
echo Запуск теста...
echo.

java -jar test_analysis.jar > test_analysis_results.txt 2>&1

echo.
echo Результаты теста сохранены в файл: test_analysis_results.txt
echo.

REM Показываем результаты
type test_analysis_results.txt

echo.
echo ========================================
echo ТЕСТ ЗАВЕРШЕН
echo ========================================
echo.
echo Результаты сохранены в файл: test_analysis_results.txt
echo.

pause 