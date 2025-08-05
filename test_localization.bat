@echo off
echo Testing Localization for Medicine Type Lists
echo ===========================================

echo.
echo English strings.xml contains:
findstr "medicine_type_tablets" app\src\main\res\values\strings.xml

echo.
echo Russian strings.xml contains:
findstr "medicine_type_tablets" app\src\main\res\values-ru\strings.xml

echo.
echo AddMedicineActivity.kt uses:
findstr "getString.*medicine_type_tablets" app\src\main\java\com\medicalnotes\app\AddMedicineActivity.kt

echo.
echo BaseActivity.kt applies language:
findstr "getCurrentLanguage" app\src\main\java\com\medicalnotes\app\BaseActivity.kt

echo.
echo ===========================================
echo Test completed!
pause 