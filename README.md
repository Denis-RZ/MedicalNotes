# Medical Notes Android Application

## Project Overview
This is a Kotlin-based Android application for managing medicine schedules and reminders. The app allows users to add medicines, set schedules, receive notifications, and track medicine intake.

## Project Structure

### Core Components
- **MainActivity.kt** - Main activity displaying medicine list and handling medicine actions
- **EditMedicineActivity.kt** - Activity for editing existing medicine records
- **MainViewModel.kt** - ViewModel providing data to MainActivity and handling business logic
- **MedicineRepository.kt** - Repository layer for data operations
- **DataManager.kt** - Manages loading, saving, adding, updating, and deleting medicine data to/from JSON files
- **DosageCalculator.kt** - Calculates dosage and manages medicine status
- **NotificationManager.kt** - Centralized class for managing notifications
- **OverdueCheckService.kt** - Background service for checking overdue medicines

### Data Models
- **Medicine.kt** - Data structure for medicine records
- **DosageFrequency** - Enum for medicine frequency (DAILY, EVERY_OTHER_DAY, etc.)
- **MedicineStatus** - Enum for medicine status (NOT_TODAY, UPCOMING, OVERDUE, TAKEN_TODAY)

### Adapters
- **MainMedicineAdapter.kt** - Adapter for "Today's Medicines" screen
- **MedicineGridAdapter.kt** - Adapter for medicine grid display

### Localization
- **strings.xml** (values/) - English string resources
- **strings.xml** (values-ru/) - Russian string resources
- **LanguageChangeManager.kt** - Manages language switching

## Current Critical Issues

### 1. Medicine Not Appearing in "Today's Medicines" After Editing
**Problem**: When a medicine is edited, especially when changing frequency to "every other day" (через день), it disappears from the "Today's Medicines" screen even though it should be displayed.

**User's Specific Issue**: 
- Medicine appears correctly when frequency is "daily" (каждый день)
- Medicine disappears when frequency is changed to "every other day" (через день)
- This happens after editing, not after initial creation

**Root Cause**: The `startDate` calculation in `DosageCalculator.kt` was using incorrect integer division:
```kotlin
// OLD (INCORRECT)
val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))

// NEW (CORRECT)
val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
    .atZone(java.time.ZoneId.systemDefault())
    .toLocalDate()
```

**Status**: ✅ **FIXED** - The fix has been applied and tested. Both `DosageCalculatorDebugTest.kt` and `EveryOtherDayProblemTest.kt` pass successfully.

**What to Test**: 
1. Create a medicine with "daily" frequency - should appear in "Today's Medicines"
2. Edit the medicine and change frequency to "every other day" - should still appear in "Today's Medicines"
3. Check logs to ensure `MainViewModel.todayMedicines.value` returns the correct number of medicines

### 2. Overdue Notifications Not Appearing on Top
**Problem**: Overdue notifications are not appearing on top of everything when the application is closed or in the background.

**User's Issue**: Notifications don't show up when app is in background or closed, even though overdue medicines are detected.

**Status**: 🔄 **PENDING** - Needs investigation and fix.

**What to Test**:
1. Set a medicine time to a few minutes in the future
2. Close the app completely
3. Wait for the time to pass
4. Check if notification appears on top of other apps
5. Check if notification has proper priority (PRIORITY_MAX)

### 3. Vibration Cannot Be Stopped
**Problem**: Vibration cannot be stopped by any means, including the "stop all vibration" button in settings.

**User's Issue**: When vibration starts (from notifications), it cannot be stopped even by pressing the "stop all vibration" button in app settings.

**Status**: 🔄 **PENDING** - Needs investigation and fix.

**What to Test**:
1. Create a medicine with time in the future
2. Wait for notification with vibration
3. Try to stop vibration using the "stop all vibration" button in settings
4. Check if vibration continues despite the button press
5. Check if `VibratorManager.cancel()` is being called properly

### 4. Double Sound Signals from Notifications
**Problem**: Notifications are producing double sound signals.

**User's Issue**: When notifications appear, the sound plays twice instead of once.

**Status**: 🔄 **PENDING** - Needs investigation and fix.

**What to Test**:
1. Create a medicine with time in the future
2. Wait for notification to appear
3. Listen for sound - should play only once
4. Check if `NotificationManager.showOverdueMedicineNotification()` is called multiple times
5. Check if `MainActivity` is also creating notifications (double creation)

### 5. "Take Medicine" Button Issues
**Problem**: 
- When the "Take Medicine" button is pressed, the medicine record does not disappear from the "Today's Medicines" screen, although the remaining quantity decreases.
- When editing, the "Taken" button remains disabled even if the time has not passed.

**User's Issue**: 
- Medicine card should disappear from "Today's Medicines" after pressing "Take Medicine" button
- "Take Medicine" button should be enabled/disabled based on time and status
- Button text should be localized

**Status**: 🔄 **PARTIALLY FIXED** - The medicine disappearing logic has been fixed, but button state issues remain.

**What to Test**:
1. Press "Take Medicine" button - medicine should disappear from "Today's Medicines"
2. Edit a medicine - "Take Medicine" button should be enabled if time hasn't passed
3. Check if button text is localized (English/Russian)
4. Verify `medicine.takenToday` is set to true after taking medicine

## Recent Fixes Applied

### 1. Fixed startDate Calculation in DosageCalculator.kt
```kotlin
// Before (incorrect)
val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))

// After (correct)
val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
    .atZone(java.time.ZoneId.systemDefault())
    .toLocalDate()
```

### 2. Fixed Medicine Disappearing After "Take Medicine"
Changed filtering logic in `DosageCalculator.getActiveMedicinesForDate()`:
```kotlin
// Before
.filter { medicine -> medicine.lastTakenDate != date }

// After
.filter { medicine -> !medicine.takenToday }
```

### 3. Fixed EditMedicineActivity startDate Reset
Ensured `startDate` is always reset when frequency changes:
```kotlin
if (originalMedicine.frequency != selectedFrequency) {
    val currentTime = System.currentTimeMillis()
    medicine.startDate = currentTime
    // Reset other fields...
}
```

### 4. Fixed MainViewModel LiveData Updates
Updated to use proper coroutine dispatchers and LiveData updates:
```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
    _todayMedicines.postValue(todayMedicines)
}
```

## Testing

### Test Files Created
- **EveryOtherDayProblemTest.kt** - Tests the specific "every other day" frequency issue
- **DosageCalculatorDebugTest.kt** - Tests startDate calculation and EVERY_OTHER_DAY logic

### Test Results
- ✅ **EveryOtherDayProblemTest.kt** - PASSED - Confirms the fix works for "every other day" frequency
- ✅ **DosageCalculatorDebugTest.kt** - PASSED - Confirms startDate calculation is correct

### Running Tests
```bash
# Run all tests
.\gradlew app:testDebugUnitTest

# Run specific test
.\gradlew app:testDebugUnitTest --tests "com.medicalnotes.app.utils.EveryOtherDayProblemTest"

# Run debug test
.\gradlew app:testDebugUnitTest --tests "com.medicalnotes.app.utils.DosageCalculatorDebugTest"
```

### What Tests Verify
1. **EveryOtherDayProblemTest.kt**:
   - Creates medicine with "every other day" frequency
   - Verifies it appears in "Today's Medicines" when startDate = today
   - Tests the full chain: DataManager → MedicineRepository → MainViewModel
   - Compares behavior with "daily" frequency

2. **DosageCalculatorDebugTest.kt**:
   - Tests startDate conversion from milliseconds to LocalDate
   - Verifies "every other day" logic works correctly
   - Tests medicine should be taken on start date, not next day, then every other day

## Build Commands

### Compile APK
```bash
.\gradlew.bat assembleDebug
```

### Clean and Rebuild
```bash
.\gradlew clean
.\gradlew.bat assembleDebug
```

## User Preferences (from memory)
- Prefers to build only the main project, not tests
- Prefers using `.\gradlew.bat assembleDebug` for compilation
- Prefers tests to be run before compiling to avoid multiple installations
- Prefers English commit messages
- Prefers build scripts without pause prompts
- Prefers console logs in English
- Prefers storing data in XML files instead of databases
- Prefers full text labels instead of icons in UI

## Current Status
The main issue with medicines not appearing after editing (especially with "every other day" frequency) has been **RESOLVED**. The critical `startDate` calculation bug has been fixed and tested.

## User's Latest Feedback
- **Issue #1**: "Medicine not appearing after editing" - ✅ **FIXED** (confirmed by tests)
- **Issue #2**: "Overdue notifications not appearing on top" - 🔄 **PENDING**
- **Issue #3**: "Vibration cannot be stopped" - 🔄 **PENDING** 
- **Issue #4**: "Double sound signals" - 🔄 **PENDING**
- **Issue #5**: "Take Medicine button issues" - 🔄 **PARTIALLY FIXED**

## Next Steps for New Developer
1. **First Priority**: Verify the "every other day" fix works in real app
   - Create medicine with "daily" frequency → should appear in "Today's Medicines"
   - Edit medicine → change to "every other day" → should still appear in "Today's Medicines"
   - Check logs: `MainViewModel.todayMedicines.value` should return correct number

2. **Second Priority**: Address remaining issues in order:
   - Overdue notifications not appearing on top
   - Vibration cannot be stopped  
   - Double sound signals from notifications
   - "Take Medicine" button state issues

3. **Always**: Run tests before compilation to avoid multiple installations
4. **Always**: Check logs to ensure fixes are working as expected

## Key Files to Focus On
- `app/src/main/java/com/medicalnotes/app/utils/DosageCalculator.kt` - Core logic for medicine scheduling
- `app/src/main/java/com/medicalnotes/app/MainActivity.kt` - Main UI and medicine actions
- `app/src/main/java/com/medicalnotes/app/service/OverdueCheckService.kt` - Notification service
- `app/src/main/java/com/medicalnotes/app/utils/NotificationManager.kt` - Notification management

## Important Notes
- The app uses JSON files for data storage (not database)
- All UI text should be localized (English/Russian)
- The app has extensive logging for debugging
- Tests should be run before compilation to avoid multiple installations
- The user is very particular about testing and verification before releases

## Critical Debugging Information
- **Log Tag**: Look for logs with "DosageCalculator", "MainViewModel", "MainActivity"
- **Key Log Messages**: 
  - "=== ОБСЕРВЕР ЛЕКАРСТВ НА СЕГОДНЯ ===" - Today's medicines observer
  - "Получено лекарств из ViewModel: X" - Number of medicines from ViewModel
  - "ПРОВЕРКА: [Medicine Name] - Статус: [Status]" - Medicine status check
- **Expected Behavior**: When medicine has "every other day" frequency and startDate = today, it should appear in "Today's Medicines"
- **Test Environment**: Robolectric tests pass, but real app behavior needs verification

## User Communication Style
- User prefers detailed testing before compilation
- User provides extensive logs for debugging
- User is frustrated with repeated issues and wants thorough verification
- User speaks Russian but prefers English in code and logs
- User wants concrete test steps and verification methods 