@echo off
echo ========================================
echo   NOTIFICATION BEHAVIOR TEST FRAMEWORK
echo ========================================
echo.
echo This test will analyze the notification system behavior
echo using Robolectric framework on Windows without Android emulators.
echo.
echo Starting comprehensive notification analysis...
echo.

REM Run the specific notification behavior test
echo [1/3] Running NotificationBehaviorTest...
.\gradlew.bat test --tests "com.medicalnotes.app.utils.NotificationBehaviorTest" --info

echo.
echo [2/3] Running comprehensive notification analysis...
.\gradlew.bat test --tests "com.medicalnotes.app.utils.NotificationBehaviorTest.comprehensive notification analysis" --info

echo.
echo [3/3] Running all notification-related tests...
.\gradlew.bat test --tests "*Notification*" --info

echo.
echo ========================================
echo   TEST COMPLETION SUMMARY
echo ========================================
echo.
echo The notification behavior test framework has completed.
echo.
echo This test framework helps identify potential issues with:
echo   - Notification channel configuration
echo   - Full screen intent behavior
echo   - Sound and vibration settings
echo   - Notification priority and visibility
echo   - Repeating notification logic
echo.
echo If tests pass but real device behavior differs, check:
echo   - Device battery optimization settings
echo   - App notification permissions
echo   - Do Not Disturb mode settings
echo   - Android version compatibility
echo   - Device manufacturer customizations
echo.
echo Test results are saved in build/reports/tests/
echo.
pause 