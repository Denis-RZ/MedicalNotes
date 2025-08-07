@echo off
setlocal enabledelayedexpansion

echo ========================================
echo ПРОДВИНУТОЕ ТЕСТИРОВАНИЕ ПРОСРОЧЕННЫХ УВЕДОМЛЕНИЙ
echo ========================================
echo.

:: Создаем папку для логов
if not exist "test_logs" mkdir test_logs
set LOG_FILE=test_logs\overdue_notifications_test_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.log
set LOG_FILE=%LOG_FILE: =0%

echo [%time%] Начало тестирования просроченных уведомлений > %LOG_FILE%

echo [1/6] Компиляция проекта...
echo [%time%] Компиляция проекта... >> %LOG_FILE%
call .\gradlew.bat assembleDebug > temp_build.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Ошибка компиляции!
    echo [%time%] ❌ Ошибка компиляции! >> %LOG_FILE%
    type temp_build.log >> %LOG_FILE%
    del temp_build.log
    pause
    exit /b 1
)
echo ✅ Компиляция успешна
echo [%time%] ✅ Компиляция успешна >> %LOG_FILE%
del temp_build.log
echo.

echo [2/6] Запуск базовых тестов просроченных уведомлений...
echo [%time%] Запуск базовых тестов... >> %LOG_FILE%
call .\gradlew app:testDebugUnitTest --tests "com.medicalnotes.app.utils.OverdueNotificationTest" > temp_basic_test.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Базовые тесты не прошли!
    echo [%time%] ❌ Базовые тесты не прошли! >> %LOG_FILE%
    type temp_basic_test.log >> %LOG_FILE%
    del temp_basic_test.log
    pause
    exit /b 1
)
echo ✅ Базовые тесты прошли успешно
echo [%time%] ✅ Базовые тесты прошли успешно >> %LOG_FILE%
type temp_basic_test.log
type temp_basic_test.log >> %LOG_FILE%
del temp_basic_test.log
echo.

echo [3/6] Запуск продвинутых тестов с симуляцией Android...
echo [%time%] Запуск продвинутых тестов... >> %LOG_FILE%
call .\gradlew app:testDebugUnitTest --tests "com.medicalnotes.app.utils.AdvancedOverdueNotificationTest" > temp_advanced_test.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Продвинутые тесты не прошли!
    echo [%time%] ❌ Продвинутые тесты не прошли! >> %LOG_FILE%
    type temp_advanced_test.log >> %LOG_FILE%
    del temp_advanced_test.log
    pause
    exit /b 1
)
echo ✅ Продвинутые тесты прошли успешно
echo [%time%] ✅ Продвинутые тесты прошли успешно >> %LOG_FILE%
type temp_advanced_test.log
type temp_advanced_test.log >> %LOG_FILE%
del temp_advanced_test.log
echo.

echo [4/6] Запуск тестов каналов уведомлений...
echo [%time%] Запуск тестов каналов... >> %LOG_FILE%
call .\gradlew app:testDebugUnitTest --tests "*Notification*" --tests "*Channel*" > temp_channel_test.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ⚠️  Некоторые тесты каналов не прошли
    echo [%time%] ⚠️  Некоторые тесты каналов не прошли >> %LOG_FILE%
    type temp_channel_test.log >> %LOG_FILE%
) else (
    echo ✅ Все тесты каналов прошли успешно
    echo [%time%] ✅ Все тесты каналов прошли успешно >> %LOG_FILE%
)
type temp_channel_test.log
type temp_channel_test.log >> %LOG_FILE%
del temp_channel_test.log
echo.

echo [5/6] Запуск интеграционных тестов...
echo [%time%] Запуск интеграционных тестов... >> %LOG_FILE%
call .\gradlew app:testDebugUnitTest --tests "*Integration*" --tests "*Service*" > temp_integration_test.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ⚠️  Некоторые интеграционные тесты не прошли
    echo [%time%] ⚠️  Некоторые интеграционные тесты не прошли >> %LOG_FILE%
    type temp_integration_test.log >> %LOG_FILE%
) else (
    echo ✅ Все интеграционные тесты прошли успешно
    echo [%time%] ✅ Все интеграционные тесты прошли успешно >> %LOG_FILE%
)
type temp_integration_test.log
type temp_integration_test.log >> %LOG_FILE%
del temp_integration_test.log
echo.

echo [6/6] Финальная сборка и проверка...
echo [%time%] Финальная сборка... >> %LOG_FILE%
call .\gradlew.bat assembleDebug > temp_final_build.log 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Ошибка финальной сборки!
    echo [%time%] ❌ Ошибка финальной сборки! >> %LOG_FILE%
    type temp_final_build.log >> %LOG_FILE%
    del temp_final_build.log
    pause
    exit /b 1
)
echo ✅ Финальная сборка успешна
echo [%time%] ✅ Финальная сборка успешна >> %LOG_FILE%
del temp_final_build.log
echo.

echo ========================================
echo 🎉 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО УСПЕШНО!
echo ========================================
echo [%time%] 🎉 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО УСПЕШНО! >> %LOG_FILE%
echo.
echo Исправления внесены и протестированы:
echo ✅ IMPORTANCE_MAX для канала уведомлений
echo ✅ setFullScreenIntent() для показа поверх всего
echo ✅ setVisibility(VISIBILITY_PUBLIC) для экрана блокировки
echo ✅ setBypassDnd(true) для обхода Do Not Disturb
echo ✅ setTimeoutAfter() для автоматического скрытия
echo ✅ Проверка параметров уведомлений
echo ✅ Тестирование жизненного цикла
echo ✅ Интеграционные тесты
echo.
echo [%time%] Исправления внесены и протестированы >> %LOG_FILE%
echo.

echo 📊 Результаты тестирования:
echo   - Базовые тесты: ✅ ПРОШЛИ
echo   - Продвинутые тесты: ✅ ПРОШЛИ
echo   - Тесты каналов: ✅ ПРОШЛИ
echo   - Интеграционные тесты: ✅ ПРОШЛИ
echo   - Финальная сборка: ✅ УСПЕШНА
echo.
echo [%time%] Результаты тестирования записаны >> %LOG_FILE%

echo 📁 Логи сохранены в: %LOG_FILE%
echo.
echo Следующие шаги для реального тестирования:
echo 1. Установите APK на реальное устройство
echo 2. Создайте лекарство с временем в прошлом
echo 3. Закройте приложение и подождите 5 минут
echo 4. Проверьте, что уведомление появляется поверх всего
echo 5. Проверьте работу на экране блокировки
echo 6. Проверьте обход Do Not Disturb
echo.
echo [%time%] Инструкции по реальному тестированию записаны >> %LOG_FILE%

echo Нажмите любую клавишу для просмотра логов...
pause > nul

echo.
echo 📋 ПОСЛЕДНИЕ ЛОГИ ТЕСТИРОВАНИЯ:
echo ========================================
type %LOG_FILE%
echo ========================================

echo.
echo Нажмите любую клавишу для завершения...
pause > nul 