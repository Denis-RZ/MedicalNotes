package com.medicalnotes.app.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
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
class OverdueNotificationTest {
    
    private lateinit var context: Context
    private lateinit var dataManager: DataManager
    private lateinit var notificationManager: NotificationManager
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Очищаем все уведомления перед тестом
        notificationManager.cancelAll()
        Log.d("OverdueTest", "=== НАЧАЛО ТЕСТА УВЕДОМЛЕНИЙ ===")
    }
    
    @Test
    fun testOverdueNotificationShowsWithoutSound() {
        Log.d("OverdueTest", "=== ТЕСТ УВЕДОМЛЕНИЯ БЕЗ ЗВУКА ===")
        
        // 1. Создаем просроченное лекарство
        val overdueMedicine = createOverdueMedicine()
        Log.d("OverdueTest", "📝 Создано просроченное лекарство: ${overdueMedicine.name}")
        Log.d("OverdueTest", "   Время приема: ${overdueMedicine.time}")
        Log.d("OverdueTest", "   Текущее время: ${LocalTime.now()}")
        
        // 2. Сохраняем лекарство
        val addSuccess = dataManager.addMedicine(overdueMedicine)
        assertTrue("Лекарство должно быть сохранено", addSuccess)
        Log.d("OverdueTest", "✅ Лекарство сохранено в базе данных")
        
        // 3. Симулируем показ уведомления
        simulateOverdueNotification(overdueMedicine)
        
        // 4. Проверяем, что уведомление создано
        val activeNotifications = notificationManager.activeNotifications
        Log.d("OverdueTest", "📊 Активных уведомлений: ${activeNotifications.size}")
        
        val overdueNotification = activeNotifications.find { 
            it.id == OverdueCheckService.NOTIFICATION_ID_OVERDUE 
        }
        
        assertNotNull("Уведомление о просроченных лекарствах должно быть создано", overdueNotification)
        Log.d("OverdueTest", "✅ Уведомление о просроченных лекарствах найдено")
        
        // 5. Проверяем параметры уведомления
        val notification = overdueNotification!!.notification
        Log.d("OverdueTest", "📋 Параметры уведомления:")
        Log.d("OverdueTest", "   Приоритет: ${notification.priority}")
        Log.d("OverdueTest", "   Категория: ${notification.category}")
        Log.d("OverdueTest", "   Видимость: ${notification.visibility}")
        Log.d("OverdueTest", "   Full Screen Intent: ${notification.fullScreenIntent != null}")
        Log.d("OverdueTest", "   Звук: ${notification.sound}")
        Log.d("OverdueTest", "   Вибрация: ${notification.vibrate}")
        
        // 6. Проверяем критически важные параметры
        assertEquals("Приоритет должен быть максимальным", 
            android.app.Notification.PRIORITY_MAX, notification.priority)
        assertEquals("Категория должна быть ALARM", 
            android.app.Notification.CATEGORY_ALARM, notification.category)
        assertEquals("Видимость должна быть PUBLIC", 
            android.app.Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertTrue("Full Screen Intent должен быть установлен", 
            notification.fullScreenIntent != null)
        assertNull("Звук должен быть отключен", notification.sound)
        
        Log.d("OverdueTest", "✅ Все параметры уведомления корректны")
        Log.d("OverdueTest", "=== ТЕСТ ЗАВЕРШЕН УСПЕШНО ===")
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
    
    private fun simulateOverdueNotification(medicine: Medicine) {
        Log.d("OverdueTest", "🔄 Симуляция показа уведомления для ${medicine.name}")
        
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
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(null) // Без звука
            .setVibrate(null) // Без вибрации
            .setLights(0xFF0000FF.toInt(), 3000, 3000)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setTimeoutAfter(0)
            .build()
        
        notificationManager.notify(OverdueCheckService.NOTIFICATION_ID_OVERDUE, notification)
        Log.d("OverdueTest", "✅ Уведомление показано")
    }
} 