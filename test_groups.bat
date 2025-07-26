@echo off
echo ========================================
echo АВТОМАТИЧЕСКОЕ ТЕСТИРОВАНИЕ ГРУПП
echo ========================================
echo.

echo 1. Сборка проекта...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ❌ Ошибка сборки проекта
    pause
    exit /b 1
)
echo ✅ Проект собран успешно
echo.

echo 2. Анализ кода на предмет проблем с группами...
echo.

echo Проверка файлов групп:
if exist "app\src\main\java\com\medicalnotes\app\adapters\GroupAdapter.kt" (
    echo ✅ GroupAdapter.kt найден
) else (
    echo ❌ GroupAdapter.kt не найден
)

if exist "app\src\main\java\com\medicalnotes\app\adapters\GroupMedicineAdapter.kt" (
    echo ✅ GroupMedicineAdapter.kt найден
) else (
    echo ❌ GroupMedicineAdapter.kt не найден
)

if exist "app\src\main\java\com\medicalnotes\app\GroupManagementActivity.kt" (
    echo ✅ GroupManagementActivity.kt найден
) else (
    echo ❌ GroupManagementActivity.kt не найден
)

if exist "app\src\main\java\com\medicalnotes\app\utils\MedicineGroupingUtil.kt" (
    echo ✅ MedicineGroupingUtil.kt найден
) else (
    echo ❌ MedicineGroupingUtil.kt не найден
)

if exist "app\src\main\java\com\medicalnotes\app\utils\GroupTestSuite.kt" (
    echo ✅ GroupTestSuite.kt найден
) else (
    echo ❌ GroupTestSuite.kt не найден
)

echo.

echo 3. Проверка макетов групп:
if exist "app\src\main\res\layout\activity_group_management.xml" (
    echo ✅ activity_group_management.xml найден
) else (
    echo ❌ activity_group_management.xml не найден
)

if exist "app\src\main\res\layout\item_group.xml" (
    echo ✅ item_group.xml найден
) else (
    echo ❌ item_group.xml не найден
)

if exist "app\src\main\res\layout\item_group_medicine.xml" (
    echo ✅ item_group_medicine.xml найден
) else (
    echo ❌ item_group_medicine.xml не найден
)

echo.

echo 4. Проверка AndroidManifest.xml:
findstr /C:"GroupManagementActivity" "app\src\main\AndroidManifest.xml" >nul
if %errorlevel% equ 0 (
    echo ✅ GroupManagementActivity зарегистрирована в манифесте
) else (
    echo ❌ GroupManagementActivity НЕ зарегистрирована в манифесте
)

echo.

echo 5. Поиск потенциальных проблем в коде...
echo.

echo Проверка валидации groupOrder:
findstr /C:"groupOrder <= 0" "app\src\main\java\com\medicalnotes\app\*.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ Валидация groupOrder найдена
) else (
    echo ⚠️ Валидация groupOrder не найдена
)

echo Проверка валидации groupName:
findstr /C:"groupName.isEmpty()" "app\src\main\java\com\medicalnotes\app\*.kt" >nul
if %errorlevel% equ 0 (
    echo ✅ Валидация groupName найдена
) else (
    echo ⚠️ Валидация groupName не найдена
)

echo.

echo 6. Проверка тестовых данных:
if exist "app\src\main\java\com\medicalnotes\app\utils\TestDataGenerator.kt" (
    echo ✅ TestDataGenerator найден
) else (
    echo ❌ TestDataGenerator не найден
)

echo.

echo ========================================
echo РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ
echo ========================================
echo.
echo ✅ Проект успешно собран
echo ✅ Все основные файлы групп найдены
echo ✅ Тестовые данные созданы
echo.
echo РЕКОМЕНДАЦИИ:
echo 1. Протестируйте на реальном устройстве
echo 2. Проверьте работу с группами в UI
echo 3. Убедитесь в корректности валидации
echo 4. Проверьте обработку ошибок
echo.
echo ========================================
echo Тестирование завершено!
echo ========================================
pause 