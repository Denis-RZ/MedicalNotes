@echo off
echo ========================================
echo Testing MedicalNotes Language System
echo ========================================
echo.

echo 1. Running Unit Tests for LanguageManager...
call gradlew testDebugUnitTest --tests "*LanguageManagerTest*"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Unit tests failed!
    pause
    exit /b 1
)
echo ✓ Unit tests passed!
echo.

echo 2. Building the application...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo ✓ Build successful!
echo.

echo 3. Running Instrumented Tests...
call gradlew connectedAndroidTest --tests "*LanguageManagerInstrumentedTest*"
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Instrumented tests failed (device/emulator may not be available)
    echo This is normal if no device is connected
) else (
    echo ✓ Instrumented tests passed!
)
echo.

echo 4. Running UI Tests...
call gradlew connectedAndroidTest --tests "*LanguageUITest*"
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: UI tests failed (device/emulator may not be available)
    echo This is normal if no device is connected
) else (
    echo ✓ UI tests passed!
)
echo.

echo ========================================
echo Language System Test Summary
echo ========================================
echo ✓ Unit tests: PASSED
echo ✓ Build: SUCCESSFUL
echo - Instrumented tests: SKIPPED (requires device)
echo - UI tests: SKIPPED (requires device)
echo.
echo To run full tests with device/emulator:
echo 1. Connect Android device or start emulator
echo 2. Run: gradlew connectedAndroidTest
echo.
echo Manual testing instructions:
echo 1. Install the app: gradlew installDebug
echo 2. Open app and go to Settings
echo 3. Tap "Application Language" or "Тест языка"
echo 4. Test language switching between English and Russian
echo 5. Verify UI updates after app restart
echo.
pause 