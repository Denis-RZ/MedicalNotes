@echo off
echo Просмотр логов приложения MedicalNotes...
adb logcat -s MainActivity EditMedicine AddMedicine DataManager LocalTimeAdapter DosageCalculator
pause 