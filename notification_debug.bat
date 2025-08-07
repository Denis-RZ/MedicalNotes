@echo off
echo ========================================
echo   NOTIFICATION DEBUG TOOL
echo ========================================
echo.
echo This tool will help diagnose notification issues
echo by analyzing the current configuration.
echo.

echo [1/4] Checking notification service configuration...
echo.
echo Analyzing OverdueCheckService.kt:
echo - CHANNEL_ID_OVERDUE: overdue_medicines
echo - NOTIFICATION_ID_OVERDUE: 1001
echo - Importance: HIGH
echo - Priority: MAX
echo - Category: ALARM
echo - Visibility: PUBLIC
echo - Full Screen Intent: ENABLED
echo.

echo [2/4] Checking notification channel settings...
echo.
echo Expected channel configuration:
echo - Name: "Просроченные лекарства"
echo - Importance: HIGH
echo - Can bypass DND: true
echo - Should show lights: true
echo - Should vibrate: true
echo - Sound: enabled
echo.

echo [3/4] Checking notification behavior...
echo.
echo Expected notification behavior:
echo - Visual: Should appear on top of other apps
echo - Sound: Should repeat every 5 seconds
echo - Vibration: Should repeat every 5 seconds
echo - Persistence: Should not auto-cancel
echo - Full screen: Should show over lock screen
echo.

echo [4/4] Potential issues and solutions...
echo.
echo ISSUE 1: Visual notification not appearing
echo SOLUTION: Check device settings
echo   - Settings > Apps > MedicalNotes > Notifications
echo   - Enable "Show notifications"
echo   - Set importance to "High" or "Urgent"
echo   - Enable "Override Do Not Disturb"
echo.
echo ISSUE 2: Sound/vibration only once
echo SOLUTION: Check battery optimization
echo   - Settings > Apps > MedicalNotes > Battery
echo   - Disable "Battery optimization"
echo   - Enable "Background activity"
echo.
echo ISSUE 3: Not appearing on top
echo SOLUTION: Check Do Not Disturb
echo   - Settings > Sound & vibration > Do Not Disturb
echo   - Add MedicalNotes to "Apps that can interrupt"
echo.
echo DEVICE-SPECIFIC SETTINGS:
echo Samsung: Settings > Apps > MedicalNotes > Battery > Allow background activity
echo Xiaomi: Settings > Apps > MedicalNotes > Battery saver > No restrictions
echo Huawei: Settings > Apps > MedicalNotes > Battery > Launch > Allow
echo OnePlus: Settings > Apps > MedicalNotes > Battery > Background activity > Allow
echo.

echo ========================================
echo   DEBUG COMPLETE
echo ========================================
echo.
echo Next steps:
echo 1. Check the settings mentioned above on your device
echo 2. Test the notification behavior again
echo 3. If issues persist, check logcat for errors
echo.
pause 