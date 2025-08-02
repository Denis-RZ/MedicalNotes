chcp 65001 >nul
echo [32m[1mMedicalNotes - Android Build System[0m
echo ======================================
echo [36mВремя запуска:[0m %date% %time%
echo.

REM Устанавливаем переменные окружения для Gradle
set GRADLE_OPTS=-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8
set JAVA_OPTS=-Xmx4096m -XX:MaxPermSize=1024m
set GRADLE_USER_HOME=C:\gradle_home_clean

echo [36mНастройки Gradle:[0m
echo [33m- Память:[0m 4GB
echo [33m- Кодировка:[0m UTF-8
echo [33m- Gradle Home:[0m %GRADLE_USER_HOME%
echo.

echo [36m[1mОчистка проекта...[0m
echo [33mКоманда:[0m gradlew.bat clean
echo.
call gradlew.bat clean --info
echo.

echo [36m[1mАвтоматическая сборка Debug APK...[0m
echo [33mКоманда:[0m gradlew.bat assembleDebug --parallel --max-workers=8
echo.
call gradlew.bat assembleDebug --parallel --max-workers=8 --info
if %errorlevel% equ 0 (
    echo.
    echo [32m[1m✅ Debug APK успешно собран![0m
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do echo [33mРазмер APK:[0m %%~zA байт
        echo [33mПуть к APK:[0m %%~fA
    ) else (
        echo [31m❌ APK файл не найден!
    )
) else (
    echo.
    echo [31m[1m❌ Ошибка при сборке Debug APK[0m
)

echo.
echo [36m[1mСборка завершена![0m
echo [33mВремя завершения:[0m %date% %time% 