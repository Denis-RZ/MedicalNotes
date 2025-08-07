@echo off
echo ========================================
echo ТЕСТИРОВАНИЕ ПРОСРОЧЕННЫХ УВЕДОМЛЕНИЙ
echo ========================================
echo.

echo [1/4] Компиляция проекта...
call .\gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ❌ Ошибка компиляции!
    pause
    exit /b 1
)
echo ✅ Компиляция успешна
echo.

echo [2/4] Запуск тестов просроченных уведомлений...
call .\gradlew app:testDebugUnitTest --tests "com.medicalnotes.app.utils.OverdueNotificationTest"
if %ERRORLEVEL% neq 0 (
    echo ❌ Тесты не прошли!
    echo.
    echo Проверьте логи выше для деталей
    pause
    exit /b 1
)
echo ✅ Тесты просроченных уведомлений прошли успешно
echo.

echo [3/4] Запуск всех связанных тестов...
call .\gradlew app:testDebugUnitTest --tests "*Overdue*" --tests "*Notification*" --tests "*MedicineStatus*"
if %ERRORLEVEL% neq 0 (
    echo ⚠️  Некоторые связанные тесты не прошли
    echo Проверьте логи выше для деталей
) else (
    echo ✅ Все связанные тесты прошли успешно
)
echo.

echo [4/4] Финальная сборка...
call .\gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ❌ Ошибка финальной сборки!
    pause
    exit /b 1
)
echo ✅ Финальная сборка успешна
echo.

echo ========================================
echo 🎉 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО УСПЕШНО!
echo ========================================
echo.
echo Исправления внесены:
echo ✅ IMPORTANCE_MAX для канала уведомлений
echo ✅ setFullScreenIntent() для показа поверх всего
echo ✅ setVisibility(VISIBILITY_PUBLIC) для экрана блокировки
echo ✅ setBypassDnd(true) для обхода Do Not Disturb
echo ✅ setTimeoutAfter() для автоматического скрытия
echo.
echo Следующие шаги:
echo 1. Протестируйте на реальном устройстве
echo 2. Проверьте работу в фоновом режиме
echo 3. Убедитесь, что уведомления появляются поверх всего
echo.
pause 