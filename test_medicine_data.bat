@echo off
echo === ДЕТАЛЬНЫЙ АНАЛИЗ ДАННЫХ ЛЕКАРСТВ ===
echo.

echo Очищаем старые логи...
adb logcat -c

echo.
echo Запускаем приложение...
echo.

echo Ожидаем 10 секунд для полной загрузки...
timeout /t 10 /nobreak >nul

echo.
echo === СОБИРАЕМ ДЕТАЛЬНЫЕ ЛОГИ ===
echo.

echo Ищем логи DosageCalculator...
adb logcat -d | findstr /i "DosageCalculator" > dosage_calculator.log

echo Ищем логи MedicineStatusHelper...
adb logcat -d | findstr /i "MedicineStatusHelper" > medicine_status.log

echo Ищем логи проверки лекарств...
adb logcat -d | findstr /i "ПРОВЕРКА\|ПРОВЕРЯЕМ\|Статус" > medicine_check.log

echo Ищем информацию о конкретных лекарствах...
adb logcat -d | findstr /i "Липетор\|Фубуксусат" > specific_medicines.log

echo.
echo === АНАЛИЗ DOSAGECALCULATOR ===
echo.
if exist dosage_calculator.log (
    type dosage_calculator.log
) else (
    echo Логи DosageCalculator не найдены
)

echo.
echo === АНАЛИЗ MEDICINESTATUSHELPER ===
echo.
if exist medicine_status.log (
    type medicine_status.log
) else (
    echo Логи MedicineStatusHelper не найдены
)

echo.
echo === АНАЛИЗ ПРОВЕРКИ ЛЕКАРСТВ ===
echo.
if exist medicine_check.log (
    type medicine_check.log
) else (
    echo Логи проверки лекарств не найдены
)

echo.
echo === ИНФОРМАЦИЯ О КОНКРЕТНЫХ ЛЕКАРСТВАХ ===
echo.
if exist specific_medicines.log (
    type specific_medicines.log
) else (
    echo Информация о конкретных лекарствах не найдена
)

echo.
echo === РЕКОМЕНДАЦИИ ПО ИСПРАВЛЕНИЮ ===
echo.
echo Если лекарства показывают NOT_TODAY:
echo.
echo 1. Проверьте частоту приема в настройках лекарств
echo    - Должна быть "Каждый день" для ежедневного приема
echo.
echo 2. Проверьте дату начала приема (startDate)
echo    - Должна быть в прошлом или сегодня
echo.
echo 3. Проверьте, не находятся ли лекарства в группе
echo    - Если в группе "через день", сегодня может быть не их день
echo.
echo 4. Проверьте активность лекарств (isActive)
echo    - Должно быть true
echo.
echo 5. Проверьте время приема
echo    - Липетор: 21:52 (вечер)
echo    - Фубуксусат: 19:15 (вечер)
echo.

echo === СЛЕДУЮЩИЕ ШАГИ ===
echo.
echo 1. Откройте приложение
echo 2. Перейдите в "Управление лекарствами"
echo 3. Проверьте настройки Липетора и Фубуксусата
echo 4. Убедитесь, что частота приема = "Каждый день"
echo 5. Убедитесь, что лекарства активны
echo 6. Если в группе, проверьте настройки группы
echo.

pause 