# 🚨 ДЕТАЛЬНЫЙ ПЛАН ИСПРАВЛЕНИЯ СИСТЕМЫ ПРОСРОЧКИ ЛЕКАРСТВ

## 📋 ТЕКУЩИЕ ПРОБЛЕМЫ

### 1. **Разные временные пороги просрочки**
- `DosageCalculator.isMedicineOverdue()`: **1 час**
- `DosageCalculator.getMedicineStatus()`: **1 минута**  
- `MedicineStatusHelper.isOverdue()`: **15 минут**

### 2. **Дублирование логики просрочки**
- Три разных метода определения просрочки
- Конфликтующие результаты
- Сложность поддержки

### 3. **Конфликт статусов**
- `takenToday` vs `lastTakenTime`
- Рассинхронизация данных
- Неправильные уведомления

## 🎯 ПЛАН ИСПРАВЛЕНИЯ

### **ЭТАП 1: Унификация временных порогов**

#### 1.1 Создать константы в `DosageCalculator.kt`
```kotlin
object DosageCalculator {
    // Унифицированные временные пороги
    private const val OVERDUE_BUFFER_MINUTES = 15L
    private const val OVERDUE_BUFFER_HOURS = 1L
    private const val OVERDUE_BUFFER_SECONDS = OVERDUE_BUFFER_MINUTES * 60L
    
    // Статусы лекарств
    enum class MedicineStatus {
        NOT_TODAY,      // Не сегодня
        UPCOMING,       // Предстоит сегодня
        OVERDUE,        // Просрочено
        TAKEN_TODAY     // Принято сегодня
    }
}
```

#### 1.2 Исправить `DosageCalculator.isMedicineOverdue()`
```kotlin
fun isMedicineOverdue(medicine: Medicine, currentTime: Long = System.currentTimeMillis()): Boolean {
    if (!medicine.isActive || medicine.takenToday) {
        return false
    }
    
    val scheduledTime = medicine.scheduledTime
    val timeDiff = currentTime - scheduledTime
    
    // Используем унифицированный порог: 15 минут
    return timeDiff > OVERDUE_BUFFER_SECONDS * 1000L
}
```

#### 1.3 Исправить `DosageCalculator.getMedicineStatus()`
```kotlin
fun getMedicineStatus(medicine: Medicine, currentTime: Long = System.currentTimeMillis()): MedicineStatus {
    if (!medicine.isActive) {
        return MedicineStatus.NOT_TODAY
    }
    
    if (medicine.takenToday) {
        return MedicineStatus.TAKEN_TODAY
    }
    
    val scheduledTime = medicine.scheduledTime
    val timeDiff = currentTime - scheduledTime
    
    return when {
        timeDiff < 0 -> MedicineStatus.UPCOMING
        timeDiff > OVERDUE_BUFFER_SECONDS * 1000L -> MedicineStatus.OVERDUE
        else -> MedicineStatus.UPCOMING
    }
}
```

#### 1.4 Исправить `MedicineStatusHelper.isOverdue()`
```kotlin
fun isOverdue(medicine: Medicine, currentTime: Long = System.currentTimeMillis()): Boolean {
    // Используем единую логику из DosageCalculator
    return DosageCalculator.isMedicineOverdue(medicine, currentTime)
}
```

### **ЭТАП 2: Убрать дублирование логики**

#### 2.1 Создать единый метод в `DosageCalculator.kt`
```kotlin
object DosageCalculator {
    // Единый метод для определения просрочки
    fun isMedicineOverdue(medicine: Medicine, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (!medicine.isActive || medicine.takenToday) {
            return false
        }
        
        val scheduledTime = medicine.scheduledTime
        val timeDiff = currentTime - scheduledTime
        
        return timeDiff > OVERDUE_BUFFER_SECONDS * 1000L
    }
    
    // Единый метод для получения статуса
    fun getMedicineStatus(medicine: Medicine, currentTime: Long = System.currentTimeMillis()): MedicineStatus {
        if (!medicine.isActive) {
            return MedicineStatus.NOT_TODAY
        }
        
        if (medicine.takenToday) {
            return MedicineStatus.TAKEN_TODAY
        }
        
        val scheduledTime = medicine.scheduledTime
        val timeDiff = currentTime - scheduledTime
        
        return when {
            timeDiff < 0 -> MedicineStatus.UPCOMING
            timeDiff > OVERDUE_BUFFER_SECONDS * 1000L -> MedicineStatus.OVERDUE
            else -> MedicineStatus.UPCOMING
        }
    }
}
```

#### 2.2 Удалить дублирующие методы
- Удалить `MedicineStatusHelper.isOverdue()` 
- Удалить старые версии методов в `DosageCalculator`
- Обновить все вызовы на новые методы

#### 2.3 Обновить `OverdueCheckService.kt`
```kotlin
class OverdueCheckService : Service() {
    private fun checkOverdueMedicines() {
        val medicines = medicineRepository.getAllMedicines()
        val currentTime = System.currentTimeMillis()
        
        val overdueMedicines = medicines.filter { medicine ->
            // Используем единый метод
            DosageCalculator.isMedicineOverdue(medicine, currentTime)
        }
        
        // Остальная логика...
    }
}
```

### **ЭТАП 3: Исправить конфликт статусов**

#### 3.1 Создать единый подход к статусам
```kotlin
data class Medicine(
    // ... существующие поля ...
    
    // Единый подход: takenToday для текущего дня
    var takenToday: Boolean = false,
    
    // lastTakenTime только для истории и логики "через день"
    var lastTakenTime: Long = 0L
)
```

#### 3.2 Исправить логику обновления статусов
```kotlin
// В EditMedicineActivity.kt
private fun markMedicineAsTaken(medicine: Medicine) {
    val updatedMedicine = medicine.copy(
        takenToday = true,
        lastTakenTime = System.currentTimeMillis()
    )
    
    medicineRepository.updateMedicine(updatedMedicine)
}

// В MainActivity.kt - сброс статуса в полночь
private fun resetDailyStatus() {
    val medicines = medicineRepository.getAllMedicines()
    medicines.forEach { medicine ->
        if (medicine.takenToday) {
            val updatedMedicine = medicine.copy(takenToday = false)
            medicineRepository.updateMedicine(updatedMedicine)
        }
    }
}
```

#### 3.3 Создать `StatusManager.kt`
```kotlin
object StatusManager {
    fun markAsTaken(medicine: Medicine): Medicine {
        return medicine.copy(
            takenToday = true,
            lastTakenTime = System.currentTimeMillis()
        )
    }
    
    fun resetDailyStatus(medicine: Medicine): Medicine {
        return medicine.copy(takenToday = false)
    }
    
    fun isTakenToday(medicine: Medicine): Boolean {
        return medicine.takenToday
    }
    
    fun wasTakenYesterday(medicine: Medicine, currentDate: LocalDate): Boolean {
        if (medicine.lastTakenTime <= 0) return false
        
        val lastTakenDate = java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        
        return lastTakenDate == currentDate.minusDays(1)
    }
}
```

### **ЭТАП 4: Обновить все компоненты**

#### 4.1 Обновить `MainViewModel.kt`
```kotlin
class MainViewModel : ViewModel() {
    fun loadTodayMedicines() {
        viewModelScope.launch(Dispatchers.IO) {
            val allMedicines = medicineRepository.getAllMedicines()
            val today = DateUtils.getCurrentDate()
            
            // Используем единую логику
            val todayMedicines = allMedicines.filter { medicine ->
                DosageCalculator.shouldTakeMedicine(medicine, today, allMedicines) &&
                !StatusManager.isTakenToday(medicine)
            }
            
            _todayMedicines.postValue(todayMedicines)
        }
    }
}
```

#### 4.2 Обновить `MedicineAdapter.kt`
```kotlin
class MedicineAdapter : RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {
    private fun getMedicineStatus(medicine: Medicine): String {
        return when (DosageCalculator.getMedicineStatus(medicine)) {
            DosageCalculator.MedicineStatus.OVERDUE -> "ПРОСРОЧЕНО"
            DosageCalculator.MedicineStatus.UPCOMING -> "ПРЕДСТОИТ"
            DosageCalculator.MedicineStatus.TAKEN_TODAY -> "ПРИНЯТО"
            DosageCalculator.MedicineStatus.NOT_TODAY -> "НЕ СЕГОДНЯ"
        }
    }
}
```

#### 4.3 Обновить `OverdueCheckService.kt`
```kotlin
class OverdueCheckService : Service() {
    private fun checkOverdueMedicines() {
        val medicines = medicineRepository.getAllMedicines()
        val currentTime = System.currentTimeMillis()
        
        val overdueMedicines = medicines.filter { medicine ->
            DosageCalculator.isMedicineOverdue(medicine, currentTime)
        }
        
        if (overdueMedicines.isNotEmpty()) {
            showOverdueNotifications(overdueMedicines)
            startRepeatingSoundAndVibration()
        } else {
            stopRepeatingSoundAndVibration()
        }
    }
}
```

### **ЭТАП 5: Тестирование**

#### 5.1 Создать тесты для новой логики
```kotlin
// В test/ directory
class DosageCalculatorTest {
    @Test
    fun testUnifiedOverdueLogic() {
        val medicine = Medicine(
            name = "Test Medicine",
            isActive = true,
            takenToday = false,
            scheduledTime = System.currentTimeMillis() - (20 * 60 * 1000) // 20 минут назад
        )
        
        // Должно быть просроченным (больше 15 минут)
        assertTrue(DosageCalculator.isMedicineOverdue(medicine))
        assertEquals(MedicineStatus.OVERDUE, DosageCalculator.getMedicineStatus(medicine))
    }
}
```

#### 5.2 Создать тесты для статусов
```kotlin
class StatusManagerTest {
    @Test
    fun testTakenTodayLogic() {
        val medicine = Medicine(name = "Test")
        
        // Изначально не принято
        assertFalse(StatusManager.isTakenToday(medicine))
        
        // Отмечаем как принятое
        val takenMedicine = StatusManager.markAsTaken(medicine)
        assertTrue(StatusManager.isTakenToday(takenMedicine))
        
        // Сбрасываем статус
        val resetMedicine = StatusManager.resetDailyStatus(takenMedicine)
        assertFalse(StatusManager.isTakenToday(resetMedicine))
    }
}
```

## 📝 ПОРЯДОК ВЫПОЛНЕНИЯ

### **Шаг 1: Создать константы и базовые методы**
1. Добавить константы в `DosageCalculator.kt`
2. Создать `StatusManager.kt`
3. Исправить `DosageCalculator.isMedicineOverdue()`

### **Шаг 2: Унифицировать логику**
1. Исправить `DosageCalculator.getMedicineStatus()`
2. Удалить дублирующие методы
3. Обновить `MedicineStatusHelper.kt`

### **Шаг 3: Исправить статусы**
1. Обновить логику в `EditMedicineActivity.kt`
2. Обновить `MainActivity.kt`
3. Создать сброс статусов в полночь

### **Шаг 4: Обновить компоненты**
1. Обновить `MainViewModel.kt`
2. Обновить `MedicineAdapter.kt`
3. Обновить `OverdueCheckService.kt`

### **Шаг 5: Тестирование**
1. Создать unit тесты
2. Протестировать на реальных данных
3. Проверить уведомления

## 🎯 ОЖИДАЕМЫЕ РЕЗУЛЬТАТЫ

### ✅ **После исправления:**
- **Единый временной порог**: 15 минут везде
- **Одна логика просрочки**: только в `DosageCalculator`
- **Синхронизированные статусы**: `takenToday` и `lastTakenTime`
- **Корректные уведомления**: без ложных срабатываний
- **Простота поддержки**: меньше дублирования кода

### 🚨 **Что изменится:**
- Лекарства будут считаться просроченными через **15 минут** (вместо 1 минуты/1 часа)
- Уведомления будут более предсказуемыми
- Статусы лекарств будут синхронизированы
- Код станет проще для понимания и поддержки

## 🔧 ФАЙЛЫ ДЛЯ ИЗМЕНЕНИЯ

1. `app/src/main/java/com/medicalnotes/app/utils/DosageCalculator.kt`
2. `app/src/main/java/com/medicalnotes/app/utils/MedicineStatusHelper.kt`
3. `app/src/main/java/com/medicalnotes/app/utils/StatusManager.kt` (новый)
4. `app/src/main/java/com/medicalnotes/app/viewmodels/MainViewModel.kt`
5. `app/src/main/java/com/medicalnotes/app/adapters/MedicineAdapter.kt`
6. `app/src/main/java/com/medicalnotes/app/service/OverdueCheckService.kt`
7. `app/src/main/java/com/medicalnotes/app/EditMedicineActivity.kt`
8. `app/src/main/java/com/medicalnotes/app/MainActivity.kt`

Хотите, чтобы я начал реализацию этого плана? 