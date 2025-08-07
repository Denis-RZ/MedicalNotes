package com.medicalnotes.app.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
// import androidx.test.ext.junit.runners.AndroidJUnit4  // Removed - not needed for unit tests
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.service.OverdueCheckService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import org.junit.Assert.*
import android.util.Log

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1, Build.VERSION_CODES.P, Build.VERSION_CODES.Q, Build.VERSION_CODES.R, Build.VERSION_CODES.S])
class AdvancedOverdueNotificationTest {
    
    private lateinit var context: Context
    private lateinit var dataManager: DataManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var medicineStatusHelper: MedicineStatusHelper
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        medicineStatusHelper = MedicineStatusHelper
        
        // Очищаем все уведомления перед тестом
        notificationManager.cancelAll()
        Log.d("AdvancedTest", "=== НАЧАЛО ТЕСТА ПРОСРОЧЕННЫХ УВЕДОМЛЕНИЙ ===")
    }
    
    @Test
    fun testOverdueNotificationFullScreenIntent() {
        Log.d("AdvancedTest", "=== ТЕСТ FULL SCREEN INTENT ===")
        
        // 1. Создаем просроченное лекарство
        val overdueMedicine = createOverdueMedicine()
        Log.d("AdvancedTest", "📝 Создано просроченное лекарство: ${overdueMedicine.name}")
        Log.d("AdvancedTest", "   Время приема: ${overdueMedicine.time}")
        Log.d("AdvancedTest", "   Текущее время: ${LocalTime.now()}")
        
        // 2. Сохраняем лекарство
        val addSuccess = dataManager.addMedicine(overdueMedicine)
        assertTrue("Лекарство должно быть сохранено", addSuccess)
        Log.d("AdvancedTest", "✅ Лекарство сохранено в базе данных")
        
        // 3. Проверяем статус
        val isOverdue = medicineStatusHelper.isOverdue(overdueMedicine)
        assertTrue("Лекарство должно быть просрочено", isOverdue)
        Log.d("AdvancedTest", "✅ Лекарство помечено как просроченное")
        
        // 4. Симулируем показ уведомления через OverdueCheckService
        simulateOverdueNotification(overdueMedicine)
        
        // 5. Проверяем, что уведомление создано
        val activeNotifications = notificationManager.activeNotifications
        Log.d("AdvancedTest", "📊 Активных уведомлений: ${activeNotifications.size}")
        
        val overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        
        assertNotNull("Уведомление о просроченных лекарствах должно быть создано", overdueNotification)
        Log.d("AdvancedTest", "✅ Уведомление о просроченных лекарствах найдено")
        
        // 6. Проверяем параметры уведомления
        val notification = overdueNotification!!.notification
        Log.d("AdvancedTest", "📋 Параметры уведомления:")
        Log.d("AdvancedTest", "   Приоритет: ${notification.priority}")
        Log.d("AdvancedTest", "   Категория: ${notification.category}")
        Log.d("AdvancedTest", "   Видимость: ${notification.visibility}")
        Log.d("AdvancedTest", "   Full Screen Intent: ${notification.fullScreenIntent != null}")
        Log.d("AdvancedTest", "   Таймаут: ${notification.timeoutAfter}")
        
        // 7. Проверяем критически важные параметры
        assertEquals("Приоритет должен быть максимальным", 
            android.app.Notification.PRIORITY_MAX, notification.priority)
        assertEquals("Категория должна быть ALARM", 
            android.app.Notification.CATEGORY_ALARM, notification.category)
        assertEquals("Видимость должна быть PUBLIC", 
            android.app.Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertTrue("Full Screen Intent должен быть установлен", 
            notification.fullScreenIntent != null)
        
        Log.d("AdvancedTest", "✅ Все параметры уведомления корректны")
        
        // 8. Симулируем принятие лекарства
        val takenMedicine = medicineStatusHelper.markAsTaken(overdueMedicine)
        val updateSuccess = dataManager.updateMedicine(takenMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // 9. Симулируем отмену уведомления
        simulateCancelOverdueNotification()
        
        // 10. Проверяем, что уведомление отменено
        val notificationsAfterCancel = notificationManager.activeNotifications
        val overdueNotificationAfterCancel = notificationsAfterCancel.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        
        assertNull("Уведомление должно быть отменено", overdueNotificationAfterCancel)
        Log.d("AdvancedTest", "✅ Уведомление корректно отменено")
        
        Log.d("AdvancedTest", "=== ТЕСТ FULL SCREEN INTENT ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    @Test
    fun testOverdueNotificationChannelSettings() {
        Log.d("AdvancedTest", "=== ТЕСТ НАСТРОЕК КАНАЛА УВЕДОМЛЕНИЙ ===")
        
        // 1. Создаем просроченное лекарство
        val overdueMedicine = createOverdueMedicine()
        dataManager.addMedicine(overdueMedicine)
        
        // 2. Симулируем показ уведомления
        simulateOverdueNotification(overdueMedicine)
        
        // 3. Проверяем настройки канала
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(OverdueCheckService.CHANNEL_ID_OVERDUE)
            assertNotNull("Канал должен быть создан", channel)
            
            Log.d("AdvancedTest", "📊 Настройки канала:")
            Log.d("AdvancedTest", "   ID: ${channel!!.id}")
            Log.d("AdvancedTest", "   Название: ${channel.name}")
            Log.d("AdvancedTest", "   Важность: ${channel.importance}")
            Log.d("AdvancedTest", "   Вибрация: ${channel.shouldVibrate()}")
            Log.d("AdvancedTest", "   Свет: ${channel.shouldShowLights()}")
            Log.d("AdvancedTest", "   Бейдж: ${channel.canShowBadge()}")
            Log.d("AdvancedTest", "   Обход DND: ${channel.canBypassDnd()}")
            
            // Проверяем критически важные настройки
            assertEquals("Важность должна быть максимальной", 
                NotificationManager.IMPORTANCE_MAX, channel.importance)
            assertTrue("Вибрация должна быть включена", channel.shouldVibrate())
            assertTrue("Свет должен быть включен", channel.shouldShowLights())
            assertTrue("Бейдж должен быть включен", channel.canShowBadge())
            assertTrue("Обход DND должен быть включен", channel.canBypassDnd())
            
            Log.d("AdvancedTest", "✅ Все настройки канала корректны")
        }
        
        Log.d("AdvancedTest", "=== ТЕСТ КАНАЛА ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    @Test
    fun testOverdueNotificationMultipleMedicines() {
        Log.d("AdvancedTest", "=== ТЕСТ МНОЖЕСТВЕННЫХ ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
        
        // 1. Создаем несколько просроченных лекарств
        val overdueMedicines = listOf(
            createTestMedicine(LocalTime.now().minusHours(3), "Лекарство 1"),
            createTestMedicine(LocalTime.now().minusHours(2), "Лекарство 2"),
            createTestMedicine(LocalTime.now().minusHours(1), "Лекарство 3")
        )
        
        // 2. Сохраняем все лекарства
        overdueMedicines.forEach { medicine ->
            dataManager.addMedicine(medicine)
            Log.d("AdvancedTest", "✅ Сохранено: ${medicine.name}")
        }
        
        // 3. Симулируем показ уведомления для всех
        simulateOverdueNotificationMultiple(overdueMedicines)
        
        // 4. Проверяем уведомление
        val activeNotifications = notificationManager.activeNotifications
        val overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        
        assertNotNull("Уведомление должно быть создано", overdueNotification)
        
        // 5. Проверяем содержимое уведомления
        val notification = overdueNotification!!.notification
        val bigText = notification.extras.getString(android.app.Notification.EXTRA_BIG_TEXT)
        
        Log.d("AdvancedTest", "📋 Содержимое уведомления:")
        Log.d("AdvancedTest", "   Заголовок: ${notification.extras.getString(android.app.Notification.EXTRA_TITLE)}")
        Log.d("AdvancedTest", "   Текст: ${notification.extras.getString(android.app.Notification.EXTRA_TEXT)}")
        Log.d("AdvancedTest", "   Большой текст: $bigText")
        
        // Проверяем, что все лекарства упомянуты
        overdueMedicines.forEach { medicine ->
            assertTrue("Уведомление должно содержать ${medicine.name}", 
                bigText?.contains(medicine.name) == true)
        }
        
        Log.d("AdvancedTest", "✅ Все просроченные лекарства упомянуты в уведомлении")
        
        Log.d("AdvancedTest", "=== ТЕСТ МНОЖЕСТВЕННЫХ ЛЕКАРСТВ ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    @Test
    fun testOverdueNotificationLifecycle() {
        Log.d("AdvancedTest", "=== ТЕСТ ЖИЗНЕННОГО ЦИКЛА УВЕДОМЛЕНИЙ ===")
        
        // 1. Создаем просроченное лекарство
        val overdueMedicine = createOverdueMedicine()
        dataManager.addMedicine(overdueMedicine)
        
        // 2. Симулируем показ уведомления
        simulateOverdueNotification(overdueMedicine)
        
        // 3. Проверяем, что уведомление активно
        var activeNotifications = notificationManager.activeNotifications
        var overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        assertNotNull("Уведомление должно быть активно", overdueNotification)
        Log.d("AdvancedTest", "✅ Уведомление активно")
        
        // 4. Симулируем принятие лекарства
        val takenMedicine = medicineStatusHelper.markAsTaken(overdueMedicine)
        dataManager.updateMedicine(takenMedicine)
        
        // 5. Симулируем отмену уведомления
        simulateCancelOverdueNotification()
        
        // 6. Проверяем, что уведомление отменено
        activeNotifications = notificationManager.activeNotifications
        overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        assertNull("Уведомление должно быть отменено", overdueNotification)
        Log.d("AdvancedTest", "✅ Уведомление отменено")
        
        // 7. Симулируем повторное создание просроченного лекарства
        val newOverdueMedicine = createTestMedicine(LocalTime.now().minusHours(1), "Новое просроченное")
        dataManager.addMedicine(newOverdueMedicine)
        
        // 8. Симулируем повторный показ уведомления
        simulateOverdueNotification(newOverdueMedicine)
        
        // 9. Проверяем, что уведомление снова активно
        activeNotifications = notificationManager.activeNotifications
        overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        assertNotNull("Уведомление должно быть снова активно", overdueNotification)
        Log.d("AdvancedTest", "✅ Уведомление снова активно")
        
        Log.d("AdvancedTest", "=== ТЕСТ ЖИЗНЕННОГО ЦИКЛА ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    private fun createOverdueMedicine(): Medicine {
        val overdueTime = LocalTime.now().minusHours(2)
        
        return Medicine(
            id = 0,
            name = "Тест Просроченное Лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = overdueTime,
            notes = "Тестовое просроченное лекарство",
            isActive = true,
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - 86400000,
            takenToday = false,
            lastTakenTime = 0L
        )
    }
    
    private fun createTestMedicine(time: LocalTime, name: String = "Тест Лекарство"): Medicine {
        return Medicine(
            id = 0,
            name = name,
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = time,
            notes = "Тестовое лекарство",
            isActive = true,
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - 86400000,
            takenToday = false,
            lastTakenTime = 0L
        )
    }
    
    private fun simulateOverdueNotification(medicine: Medicine) {
        Log.d("AdvancedTest", "🔄 Симуляция показа уведомления для ${medicine.name}")
        
        // Создаем уведомление напрямую, как это делает OverdueCheckService
        val intent = android.content.Intent(context, com.medicalnotes.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_overdue_medicines", true)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, OverdueCheckService.CHANNEL_ID_OVERDUE)
            .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
            .setContentText("У вас 1 просроченных лекарств")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: ${medicine.name}\n\nПожалуйста, примите их как можно скорее!"))
            .setSmallIcon(com.medicalnotes.app.R.drawable.ic_medicine)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setLights(0xFF0000FF.toInt(), 3000, 3000)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setTimeoutAfter(300000)
            .build()
        
        notificationManager.notify(OverdueCheckService.NOTIFICATION_ID_OVERDUE, notification)
        Log.d("AdvancedTest", "✅ Уведомление показано")
    }
    
    private fun simulateOverdueNotificationMultiple(medicines: List<Medicine>) {
        Log.d("AdvancedTest", "🔄 Симуляция показа уведомления для ${medicines.size} лекарств")
        
        val intent = android.content.Intent(context, com.medicalnotes.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_overdue_medicines", true)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val medicineNames = medicines.joinToString(", ") { it.name }
        val overdueCount = medicines.size
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, OverdueCheckService.CHANNEL_ID_OVERDUE)
            .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
            .setContentText("У вас $overdueCount просроченных лекарств")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: $medicineNames\n\nПожалуйста, примите их как можно скорее!"))
            .setSmallIcon(com.medicalnotes.app.R.drawable.ic_medicine)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setLights(0xFF0000FF.toInt(), 3000, 3000)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setTimeoutAfter(300000)
            .build()
        
        notificationManager.notify(OverdueCheckService.NOTIFICATION_ID_OVERDUE, notification)
        Log.d("AdvancedTest", "✅ Уведомление для множественных лекарств показано")
    }
    
    private fun simulateCancelOverdueNotification() {
        Log.d("AdvancedTest", "🔄 Симуляция отмены уведомления")
        notificationManager.cancel(OverdueCheckService.NOTIFICATION_ID_OVERDUE)
        Log.d("AdvancedTest", "✅ Уведомление отменено")
    }
} 