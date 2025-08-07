@echo off
echo === ДЕБАГ ДАННЫХ ЛЕКАРСТВ ===
echo.

echo Запускаем приложение с отладочными логами...
echo.

echo Ожидаем 5 секунд для загрузки...
timeout /t 5 /nobreak >nul

echo.
echo === АНАЛИЗ ЛОГОВ ===
echo Ищем информацию о лекарствах в логах...

adb logcat -d | findstr /i "DosageCalculator\|MedicineStatusHelper\|ПРОВЕРКА\|Липетор\|Фубуксусат" > medicine_debug.log

echo.
echo === РЕЗУЛЬТАТЫ АНАЛИЗА ===
type medicine_debug.log

echo.
echo === РЕКОМЕНДАЦИИ ===
echo.
echo Если лекарства показывают статус NOT_TODAY, возможные причины:
echo 1. Неправильная частота приема (не DAILY)
echo 2. Неправильная дата начала (startDate)
echo 3. Лекарства в группе с частотой "через день"
echo 4. Лекарства неактивны (isActive = false)
echo.
echo Проверьте настройки лекарств в приложении.
echo.

pause 