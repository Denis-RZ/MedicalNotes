@echo off
echo ========================================
echo ТЕСТ ПРОБЛЕМЫ СМЕНЫ ЯЗЫКОВ
echo ========================================
echo.

echo 1. Запускаем unit тесты...
call gradlew testDebugUnitTest --tests "*LanguageManagerTest*"
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ОШИБКА: Unit тесты не прошли!
    pause
    exit /b 1
)
echo ✓ Unit тесты прошли успешно
echo.

echo 2. Запускаем тест проблемы смены языков...
call gradlew testDebugUnitTest --tests "*SimpleLanguageTest*"
if %ERRORLEVEL% NEQ 0 (
    echo ❌ ОШИБКА: Тест проблемы не прошел!
    pause
    exit /b 1
)
echo ✓ Тест проблемы выполнен
echo.

echo 3. Проверяем отчеты тестов...
if exist "app\build\reports\tests\testDebugUnitTest\index.html" (
    echo ✓ Отчеты тестов созданы
    echo Отчет находится в: app\build\reports\tests\testDebugUnitTest\index.html
) else (
    echo ❌ ОШИБКА: Отчеты тестов не созданы!
)
echo.

echo 4. Анализ проблемы:
echo.
echo ПРОБЛЕМА: Настройки языка сохраняются, но UI не меняется
echo.
echo Причины:
echo - BaseActivity может не применять язык правильно
echo - Обновление конфигурации может не работать
echo - Контекст может не обновляться
echo.
echo Решение:
echo - Проверить BaseActivity.onCreate
echo - Проверить LanguageManager.applyLanguage
echo - Добавить принудительное обновление UI
echo.
pause 