@echo off
echo === ТЕСТИРОВАНИЕ УДАЛЕНИЯ ЛЕКАРСТВ ===
echo.

echo 1. Собираем приложение...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ОШИБКА: Не удалось собрать приложение
    pause
    exit /b 1
)

echo.
echo 2. Запускаем тесты удаления...
call gradlew test --tests "com.medicalnotes.app.utils.DeleteMedicineTest"
if %errorlevel% neq 0 (
    echo ОШИБКА: Тесты удаления не прошли
    pause
    exit /b 1
)

echo.
echo 3. Запускаем простые тесты удаления...
call gradlew test --tests "com.medicalnotes.app.utils.SimpleDeleteTest"
if %errorlevel% neq 0 (
    echo ОШИБКА: Простые тесты удаления не прошли
    pause
    exit /b 1
)

echo.
echo === ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===
echo Все тесты удаления прошли успешно!
pause 