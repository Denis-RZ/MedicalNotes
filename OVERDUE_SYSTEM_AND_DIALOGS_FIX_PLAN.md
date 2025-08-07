# 🚨 ПЛАН ИСПРАВЛЕНИЯ ЛОГИКИ ПРОСРОЧКИ И ДИАЛОГОВЫХ ОКОН

## 📋 ОБЗОР ПРОБЛЕМ

### 🎯 **Проблема 1: Логика просрочки**
- **3 разных временных порога** (1 минута, 1 час, 15 минут)
- **Дублирование логики** в 3 разных местах
- **Конфликт статусов** (`takenToday`, `lastTakenTime`, `takenAt`, `isOverdue`)

### 🎯 **Проблема 2: Диалоговые окна**
- **4 типа уведомлений** (обычные, просроченные, системные, toast)
- **Дублирование уведомлений** (могут показываться одновременно)
- **Сложная логика остановки** звуков и вибрации
- **Конфликт приоритетов** между каналами

## 🎯 ЕДИНЫЙ ПЛАН РЕШЕНИЯ

### **ЭТАП 1: УНИФИКАЦИЯ ЛОГИКИ ПРОСРОЧКИ (30 минут)**

#### **1.1 Создать константы и StatusManager (10 минут)**

**Файл:** `app/src/main/java/com/medicalnotes/app/utils/DosageCalculator.kt`
```kotlin
object DosageCalculator {
    // Унифицированные временные пороги
    private const val OVERDUE_BUFFER_MINUTES = 15L
    private const val OVERDUE_BUFFER_SECONDS = OVERDUE_BUFFER_MINUTES * 60L
    private const val OVERDUE_BUFFER_MILLIS = OVERDUE_BUFFER_SECONDS * 1000L
    
    // Статусы лекарств
    enum class MedicineStatus {
        NOT_TODAY,      // Не сегодня
        UPCOMING,       // Предстоит сегодня
        OVERDUE,        // Просрочено
        TAKEN_TODAY     // Принято сегодня
    }
}
```

**Новый файл:** `app/src/main/java/com/medicalnotes/app/utils/StatusManager.kt`
```kotlin
package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import java.time.LocalDate
import java.time.ZoneId

object StatusManager {
    /**
     * Отмечает лекарство как принятое
     */
    fun markAsTaken(medicine: Medicine): Medicine {
        val currentTime = System.currentTimeMillis()
        return medicine.copy(
            takenToday = true,
            lastTakenTime = currentTime
        )
    }
    
    /**
     * Сбрасывает статус для нового дня
     */
    fun resetDailyStatus(medicine: Medicine): Medicine {
        return medicine.copy(takenToday = false)
    }
    
    /**
     * Проверяет, принято ли лекарство сегодня
     */
    fun isTakenToday(medicine: Medicine): Boolean {
        return medicine.takenToday
    }
    
    /**
     * Проверяет, было ли лекарство принято вчера
     */
    fun wasTakenYesterday(medicine: Medicine, currentDate: LocalDate): Boolean {
        if (medicine.lastTakenTime <= 0) return false
        
        val lastTakenDate = java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        
        return lastTakenDate == currentDate.minusDays(1)
    }
    
    /**
     * Получает единый статус лекарства
     */
    fun getMedicineStatus(medicine: Medicine): DosageCalculator.MedicineStatus {
        return DosageCalculator.getMedicineStatus(medicine)
    }
}
```

#### **1.2 Исправить DosageCalculator (10 минут)**

**Обновить методы в `DosageCalculator.kt`:**
```kotlin
/**
 * Единый метод для определения просрочки
 */
fun isMedicineOverdue(medicine: Medicine, date: LocalDate = LocalDate.now()): Boolean {
    if (!shouldTakeMedicine(medicine, date)) {
        return false
    }
    
    val doseTimes = getDoseTimesForDate(medicine, date)
    val now = java.time.LocalDateTime.now()
    
    return doseTimes.any { doseTime ->
        val doseDateTime = date.atTime(doseTime)
        val timeDiff = java.time.Duration.between(doseDateTime, now)
        
        // ИСПРАВЛЕНО: Используем унифицированный порог 15 минут
        timeDiff.toMinutes() > OVERDUE_BUFFER_MINUTES && doseDateTime.isBefore(now)
    }
}

/**
 * Единый метод для получения статуса
 */
fun getMedicineStatus(medicine: Medicine, date: LocalDate = LocalDate.now()): MedicineStatus {
    if (!shouldTakeMedicine(medicine, date)) {
        return MedicineStatus.NOT_TODAY
    }
    
    if (medicine.takenToday) {
        return MedicineStatus.TAKEN_TODAY
    }
    
    val doseTimes = getDoseTimesForDate(medicine, date)
    val now = java.time.LocalDateTime.now()
    
    // Проверяем просроченные приемы
    val overdueDoses = doseTimes.filter { doseTime ->
        val doseDateTime = date.atTime(doseTime)
        val timeDiff = java.time.Duration.between(doseDateTime, now)
        // ИСПРАВЛЕНО: Используем унифицированный порог 15 минут
        timeDiff.toMinutes() > OVERDUE_BUFFER_MINUTES && doseDateTime.isBefore(now)
    }
    
    return when {
        overdueDoses.isNotEmpty() -> MedicineStatus.OVERDUE
        doseTimes.any { it.atDate(date).isAfter(now) } -> MedicineStatus.UPCOMING
        else -> MedicineStatus.OVERDUE
    }
}
```

#### **1.3 Упростить модель Medicine (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/models/Medicine.kt`:**
```kotlin
data class Medicine(
    // ... существующие поля ...
    
    // Единый подход к статусам
    var takenToday: Boolean = false,      // Принято сегодня
    var lastTakenTime: Long = 0L,         // Время последнего приема (для истории и логики "через день")
    
    // УДАЛЯЕМ дублирующие поля:
    // var takenAt: Long = 0L,             // Дублирует lastTakenTime
    // var isOverdue: Boolean = false,     // Вычисляемое поле
    // var shouldTakeToday: Boolean = false // Вычисляемое поле
)
```

#### **1.4 Обновить MedicineStatusHelper (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/utils/MedicineStatusHelper.kt`:**
```kotlin
fun isOverdue(medicine: Medicine): Boolean {
    // ИСПРАВЛЕНО: Используем единую логику из DosageCalculator
    return DosageCalculator.isMedicineOverdue(medicine)
}

fun getMedicineStatus(medicine: Medicine): MedicineStatus {
    // ИСПРАВЛЕНО: Используем единую логику из DosageCalculator
    return DosageCalculator.getMedicineStatus(medicine)
}
```

### **ЭТАП 2: УНИФИКАЦИЯ СИСТЕМЫ УВЕДОМЛЕНИЙ (45 минут)**

#### **2.1 Создать единый NotificationManager (15 минут)**

**Новый файл:** `app/src/main/java/com/medicalnotes/app/utils/UnifiedNotificationManager.kt`
```kotlin
package com.medicalnotes.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.medicalnotes.app.MainActivity
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine

object UnifiedNotificationManager {
    
    private const val CHANNEL_ID_MEDICINE = "medicine_notifications"
    private const val CHANNEL_ID_OVERDUE = "overdue_medicines"
    private const val NOTIFICATION_ID_MEDICINE = 1001
    private const val NOTIFICATION_ID_OVERDUE = 1002
    
    /**
     * Создает каналы уведомлений
     */
    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Канал для обычных уведомлений о лекарствах
        val medicineChannel = NotificationChannel(
            CHANNEL_ID_MEDICINE,
            "Уведомления о лекарствах",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о времени приема лекарств"
            enableVibration(true)
            setBypassDnd(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        
        // Канал для просроченных лекарств
        val overdueChannel = NotificationChannel(
            CHANNEL_ID_OVERDUE,
            "Просроченные лекарства",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о просроченных лекарствах"
            enableVibration(true)
            setBypassDnd(true) // Обходит Do Not Disturb
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        
        notificationManager.createNotificationChannels(listOf(medicineChannel, overdueChannel))
    }
    
    /**
     * Показывает уведомление о лекарстве
     */
    fun showMedicineNotification(context: Context, medicine: Medicine, isOverdue: Boolean = false) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = if (isOverdue) CHANNEL_ID_OVERDUE else CHANNEL_ID_MEDICINE
        val notificationId = if (isOverdue) NOTIFICATION_ID_OVERDUE else NOTIFICATION_ID_MEDICINE
        
        // Создаем intent для открытия приложения
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_medicine", true)
            putExtra("medicine_id", medicine.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Создаем intent для отметки "Принял"
        val takeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("take_medicine", true)
            putExtra("medicine_id", medicine.id)
        }
        
        val takePendingIntent = PendingIntent.getActivity(
            context, 1, takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Создаем уведомление
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(if (isOverdue) "🚨 ПРОСРОЧЕНО: ${medicine.name}" else "⏰ Время приема: ${medicine.name}")
            .setContentText(medicine.dosage)
            .setSmallIcon(R.drawable.ic_medicine)
            .setContentIntent(pendingIntent)
            .setPriority(if (isOverdue) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isOverdue) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOngoing(isOverdue) // Постоянное только для просроченных
            .addAction(R.drawable.ic_check, "✅ Принял", takePendingIntent)
            .addAction(R.drawable.ic_skip, "⏭ Пропустить", createSkipPendingIntent(context, medicine))
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Показывает уведомление о группе просроченных лекарств
     */
    fun showOverdueGroupNotification(context: Context, overdueMedicines: List<Medicine>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val medicineNames = overdueMedicines.joinToString(", ") { it.name }
        val overdueCount = overdueMedicines.size
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_overdue_medicines", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val takeAllIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("take_all_medicines", true)
            putExtra("medicine_ids", ArrayList(overdueMedicines.map { it.id }))
        }
        
        val takeAllPendingIntent = PendingIntent.getActivity(
            context, 1, takeAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_OVERDUE)
            .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
            .setContentText("У вас $overdueCount просроченных лекарств")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: $medicineNames\n\nПожалуйста, примите их как можно скорее!"))
            .setSmallIcon(R.drawable.ic_medicine)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setLights(0xFF0000FF.toInt(), 3000, 3000)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setTimeoutAfter(0)
            .addAction(R.drawable.ic_check, "✅ Принял все", takeAllPendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_OVERDUE, notification)
    }
    
    /**
     * Отменяет все уведомления
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
    
    /**
     * Отменяет уведомления для конкретного лекарства
     */
    fun cancelMedicineNotifications(context: Context, medicineId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_MEDICINE)
        notificationManager.cancel(NOTIFICATION_ID_OVERDUE)
    }
    
    private fun createSkipPendingIntent(context: Context, medicine: Medicine): PendingIntent {
        val skipIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("skip_medicine", true)
            putExtra("medicine_id", medicine.id)
        }
        
        return PendingIntent.getActivity(
            context, 2, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
```

#### **2.2 Обновить OverdueCheckService (15 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/service/OverdueCheckService.kt`:**
```kotlin
class OverdueCheckService : Service() {
    
    // ... существующий код ...
    
    /**
     * ИСПРАВЛЕНО: Упрощенная проверка просроченных лекарств
     */
    private fun checkOverdueMedicines() {
        try {
            val allMedicines = dataManager.loadMedicines()
            val today = LocalDate.now()
            
            // Используем единую логику
            val overdueMedicines = allMedicines.filter { medicine ->
                DosageCalculator.isMedicineOverdue(medicine, today)
            }
            
            if (overdueMedicines.isNotEmpty()) {
                // Показываем единое уведомление о группе просроченных лекарств
                UnifiedNotificationManager.showOverdueGroupNotification(this, overdueMedicines)
                
                // Запускаем повторяющиеся звуки и вибрацию
                if (!hasOverdueMedicines) {
                    startRepeatingSoundAndVibration()
                    hasOverdueMedicines = true
                }
            } else {
                // Останавливаем звуки и вибрацию если нет просроченных
                if (hasOverdueMedicines) {
                    stopRepeatingSoundAndVibration()
                    restoreOriginalSettings()
                    UnifiedNotificationManager.cancelAllNotifications(this)
                    hasOverdueMedicines = false
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка проверки просроченных лекарств", e)
        }
    }
    
    // УДАЛЯЕМ старые методы showOverdueNotification и showSystemAlert
    // Они заменены на UnifiedNotificationManager
}
```

#### **2.3 Обновить NotificationService (10 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/service/NotificationService.kt`:**
```kotlin
class NotificationService : Service() {
    
    // ... существующий код ...
    
    /**
     * ИСПРАВЛЕНО: Показ уведомления о лекарстве
     */
    private fun showMedicineNotification(medicine: Medicine) {
        // Проверяем, не просрочено ли лекарство
        val isOverdue = DosageCalculator.isMedicineOverdue(medicine)
        
        // Используем единый менеджер уведомлений
        UnifiedNotificationManager.showMedicineNotification(this, medicine, isOverdue)
    }
}
```

#### **2.4 Обновить MainActivity (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/MainActivity.kt`:**
```kotlin
class MainActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем каналы уведомлений
        UnifiedNotificationManager.createNotificationChannels(this)
        
        // ... остальной код ...
    }
    
    /**
     * ИСПРАВЛЕНО: Упрощенная обработка intent от уведомлений
     */
    private fun handleNotificationIntent() {
        try {
            val takeMedicine = intent.getBooleanExtra("take_medicine", false)
            val takeAllMedicines = intent.getBooleanExtra("take_all_medicines", false)
            val skipMedicine = intent.getBooleanExtra("skip_medicine", false)
            val showOverdueMedicines = intent.getBooleanExtra("show_overdue_medicines", false)
            
            if (takeMedicine) {
                val medicineId = intent.getLongExtra("medicine_id", -1L)
                if (medicineId != -1L) {
                    markMedicineAsTaken(medicineId)
                    UnifiedNotificationManager.cancelMedicineNotifications(this, medicineId)
                }
            }
            
            if (takeAllMedicines) {
                val medicineIds = intent.getParcelableArrayListExtra("medicine_ids", Long::class.java)
                if (!medicineIds.isNullOrEmpty()) {
                    markMedicinesAsTaken(medicineIds)
                    UnifiedNotificationManager.cancelAllNotifications(this)
                    OverdueCheckService.forceStopSoundAndVibration(this)
                }
            }
            
            if (skipMedicine) {
                val medicineId = intent.getLongExtra("medicine_id", -1L)
                if (medicineId != -1L) {
                    skipMedicine(medicineId)
                    UnifiedNotificationManager.cancelMedicineNotifications(this, medicineId)
                }
            }
            
            if (showOverdueMedicines) {
                // Просто обновляем UI
                loadTodayMedicines()
                checkOverdueMedicines()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error handling notification intent", e)
        }
    }
    
    private fun markMedicineAsTaken(medicineId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataManager = DataManager(this@MainActivity)
            val allMedicines = dataManager.loadMedicines()
            
            val updatedMedicines = allMedicines.map { medicine ->
                if (medicine.id == medicineId) {
                    StatusManager.markAsTaken(medicine)
                } else {
                    medicine
                }
            }
            
            dataManager.saveMedicines(updatedMedicines)
            
            lifecycleScope.launch(Dispatchers.Main) {
                loadTodayMedicines()
                checkOverdueMedicines()
                android.widget.Toast.makeText(this@MainActivity, "Лекарство помечено как принятое", 
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun markMedicinesAsTaken(medicineIds: ArrayList<Long>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataManager = DataManager(this@MainActivity)
            val allMedicines = dataManager.loadMedicines()
            
            val updatedMedicines = allMedicines.map { medicine ->
                if (medicineIds.contains(medicine.id)) {
                    StatusManager.markAsTaken(medicine)
                } else {
                    medicine
                }
            }
            
            dataManager.saveMedicines(updatedMedicines)
            
            lifecycleScope.launch(Dispatchers.Main) {
                loadTodayMedicines()
                checkOverdueMedicines()
                android.widget.Toast.makeText(this@MainActivity, "Лекарства помечены как принятые", 
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun skipMedicine(medicineId: Long) {
        // Логика пропуска лекарства
        android.widget.Toast.makeText(this, "Лекарство пропущено", 
            android.widget.Toast.LENGTH_SHORT).show()
    }
}
```

### **ЭТАП 3: ОБНОВЛЕНИЕ КОМПОНЕНТОВ (20 минут)**

#### **3.1 Обновить MainViewModel (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/viewmodels/MainViewModel.kt`:**
```kotlin
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
```

#### **3.2 Обновить MedicineAdapter (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/adapters/MedicineAdapter.kt`:**
```kotlin
private fun getMedicineStatus(medicine: Medicine): String {
    return when (StatusManager.getMedicineStatus(medicine)) {
        DosageCalculator.MedicineStatus.OVERDUE -> "🚨 ПРОСРОЧЕНО"
        DosageCalculator.MedicineStatus.UPCOMING -> "⏰ ПРЕДСТОИТ"
        DosageCalculator.MedicineStatus.TAKEN_TODAY -> "✅ ПРИНЯТО"
        DosageCalculator.MedicineStatus.NOT_TODAY -> "📅 НЕ СЕГОДНЯ"
    }
}
```

#### **3.3 Обновить EditMedicineActivity (5 минут)**

**Обновить `app/src/main/java/com/medicalnotes/app/EditMedicineActivity.kt`:**
```kotlin
private fun markMedicineAsTaken(medicine: Medicine) {
    val updatedMedicine = StatusManager.markAsTaken(medicine)
    medicineRepository.updateMedicine(updatedMedicine)
    
    // Отменяем уведомления для этого лекарства
    UnifiedNotificationManager.cancelMedicineNotifications(this, medicine.id)
}
```

#### **3.4 Добавить сброс статусов в полночь (5 минут)**

**Добавить в `app/src/main/java/com/medicalnotes/app/MainActivity.kt`:**
```kotlin
private fun resetDailyStatuses() {
    lifecycleScope.launch(Dispatchers.IO) {
        val dataManager = DataManager(this@MainActivity)
        val allMedicines = dataManager.loadMedicines()
        
        val updatedMedicines = allMedicines.map { medicine ->
            if (medicine.takenToday) {
                StatusManager.resetDailyStatus(medicine)
            } else {
                medicine
            }
        }
        
        dataManager.saveMedicines(updatedMedicines)
        
        // Обновляем UI
        lifecycleScope.launch(Dispatchers.Main) {
            loadTodayMedicines()
        }
    }
}
```

### **ЭТАП 4: ТЕСТИРОВАНИЕ (15 минут)**

#### **4.1 Создать тесты (10 минут)**

**Новый файл:** `app/src/test/java/com/medicalnotes/app/utils/DosageCalculatorTest.kt`
```kotlin
package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

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
        assertEquals(DosageCalculator.MedicineStatus.OVERDUE, DosageCalculator.getMedicineStatus(medicine))
    }
    
    @Test
    fun testNotOverdueLogic() {
        val medicine = Medicine(
            name = "Test Medicine",
            isActive = true,
            takenToday = false,
            scheduledTime = System.currentTimeMillis() - (10 * 60 * 1000) // 10 минут назад
        )
        
        // Не должно быть просроченным (меньше 15 минут)
        assertFalse(DosageCalculator.isMedicineOverdue(medicine))
        assertEquals(DosageCalculator.MedicineStatus.UPCOMING, DosageCalculator.getMedicineStatus(medicine))
    }
}
```

**Новый файл:** `app/src/test/java/com/medicalnotes/app/utils/StatusManagerTest.kt`
```kotlin
package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*

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

#### **4.2 Тестирование на реальных данных (5 минут)**

1. **Скомпилировать проект:**
   ```bash
   .\gradlew.bat assembleDebug
   ```

2. **Протестировать уведомления:**
   - Создать лекарство с временем приема в прошлом
   - Проверить появление уведомления о просрочке
   - Нажать "✅ Принял" и проверить остановку звуков
   - Проверить обновление UI

3. **Проверить логику "через день":**
   - Создать лекарства с частотой "через день"
   - Проверить корректное отображение на разные даты

## 🎯 ОЖИДАЕМЫЕ РЕЗУЛЬТАТЫ

### ✅ **После исправления:**

#### **Логика просрочки:**
- **Единый временной порог**: 15 минут везде
- **Одна логика просрочки**: только в `DosageCalculator`
- **Синхронизированные статусы**: `takenToday` и `lastTakenTime`
- **Упрощенная модель**: без дублирующих полей

#### **Диалоговые окна:**
- **Единая система уведомлений**: `UnifiedNotificationManager`
- **Нет дублирования**: только один тип уведомления с разными приоритетами
- **Простая логика остановки**: централизованное управление
- **Унифицированные приоритеты**: четкая иерархия каналов

### 🚨 **Что изменится:**
- Лекарства будут считаться просроченными через **15 минут** (вместо 1 минуты/1 часа)
- Уведомления будут **предсказуемыми** и **не дублироваться**
- Статусы лекарств будут **синхронизированы**
- Код станет **проще** для понимания и поддержки
- **Меньше ложных срабатываний** уведомлений

## 📝 ПОРЯДОК ВЫПОЛНЕНИЯ

### **Шаг 1: Унификация логики (30 минут)**
1. ✅ Создать константы и `StatusManager.kt`
2. ✅ Исправить `DosageCalculator.kt`
3. ✅ Упростить модель `Medicine.kt`
4. ✅ Обновить `MedicineStatusHelper.kt`

### **Шаг 2: Унификация уведомлений (45 минут)**
1. ✅ Создать `UnifiedNotificationManager.kt`
2. ✅ Обновить `OverdueCheckService.kt`
3. ✅ Обновить `NotificationService.kt`
4. ✅ Обновить `MainActivity.kt`

### **Шаг 3: Обновление компонентов (20 минут)**
1. ✅ Обновить `MainViewModel.kt`
2. ✅ Обновить `MedicineAdapter.kt`
3. ✅ Обновить `EditMedicineActivity.kt`
4. ✅ Добавить сброс статусов в полночь

### **Шаг 4: Тестирование (15 минут)**
1. ✅ Создать unit тесты
2. ✅ Протестировать на реальных данных
3. ✅ Проверить уведомления и логику "через день"

## 🔧 ФАЙЛЫ ДЛЯ ИЗМЕНЕНИЯ

1. `app/src/main/java/com/medicalnotes/app/utils/DosageCalculator.kt`
2. `app/src/main/java/com/medicalnotes/app/utils/StatusManager.kt` (новый)
3. `app/src/main/java/com/medicalnotes/app/utils/UnifiedNotificationManager.kt` (новый)
4. `app/src/main/java/com/medicalnotes/app/utils/MedicineStatusHelper.kt`
5. `app/src/main/java/com/medicalnotes/app/models/Medicine.kt`
6. `app/src/main/java/com/medicalnotes/app/service/OverdueCheckService.kt`
7. `app/src/main/java/com/medicalnotes/app/service/NotificationService.kt`
8. `app/src/main/java/com/medicalnotes/app/MainActivity.kt`
9. `app/src/main/java/com/medicalnotes/app/viewmodels/MainViewModel.kt`
10. `app/src/main/java/com/medicalnotes/app/adapters/MedicineAdapter.kt`
11. `app/src/main/java/com/medicalnotes/app/EditMedicineActivity.kt`

**Общее время реализации: ~110 минут (1 час 50 минут)**

## 🎯 ЗАКЛЮЧЕНИЕ

Этот план **полностью решает** обе проблемы:

1. **Логика просрочки** - унифицирована в одном месте с единым порогом 15 минут
2. **Диалоговые окна** - заменены единой системой уведомлений без дублирования

**Результат:** Простая, понятная и надежная система уведомлений о лекарствах!

Хотите, чтобы я начал реализацию этого плана прямо сейчас? 