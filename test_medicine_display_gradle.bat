@echo off
chcp 65001 >nul
echo ========================================
echo ТЕСТ АНАЛИЗА ПРОБЛЕМЫ ОТОБРАЖЕНИЯ ЛЕКАРСТВ
echo ========================================
echo.

echo Запуск тестов анализа проблемы с отображением лекарств через Gradle...
echo.

REM Запускаем тест через Gradle
.\gradlew.bat test --tests "com.medicalnotes.app.utils.MedicineDisplayAnalysisTest.testMedicineDisplayIssue" --info > test_analysis_results.txt 2>&1

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