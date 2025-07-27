package com.medicalnotes.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.medicalnotes.app.MainActivity
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.receiver.MedicineAlarmReceiver
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID_MEDICINE = "medicine_reminders"
        const val CHANNEL_ID_LOW_STOCK = "low_stock_alerts"
        const val CHANNEL_ID_EMERGENCY = "emergency_alerts"
        const val CHANNEL_ID_OVERDUE = "overdue_medicines"
        
        const val NOTIFICATION_ID_MEDICINE = 1001
        const val NOTIFICATION_ID_LOW_STOCK = 1002
        const val NOTIFICATION_ID_EMERGENCY = 1003
        const val NOTIFICATION_ID_OVERDUE = 1004
        
        // Настройки повторных попыток
        const val MAX_RETRY_ATTEMPTS = 5
        const val RETRY_INTERVAL_MINUTES = 15L
        const val ESCALATION_INTERVAL_MINUTES = 30L
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        @Suppress("NewApi")
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vibratorManager.defaultVibrator
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private val handler = Handler(Looper.getMainLooper())
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    
    // Хранилище активных уведомлений с попытками
    private val activeNotifications = mutableMapOf<Long, NotificationAttempt>()
    
    data class NotificationAttempt(
        val medicine: Medicine,
        val attemptCount: Int = 0,
        val lastAttemptTime: Long = System.currentTimeMillis(),
        val isEscalated: Boolean = false
    )
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Канал для напоминаний о лекарствах
            val medicineChannel = NotificationChannel(
                CHANNEL_ID_MEDICINE,
                "Напоминания о лекарствах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о времени приема лекарств"
                enableVibration(true)
                enableLights(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), 
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            
            // Канал для предупреждений о низком запасе
            val lowStockChannel = NotificationChannel(
                CHANNEL_ID_LOW_STOCK,
                "Предупреждения о запасе",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о низком запасе лекарств"
                enableVibration(true)
            }
            
            // Канал для экстренных уведомлений
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Экстренные уведомления",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Экстренные уведомления о лекарствах"
                enableVibration(true)
                enableLights(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            
            // Канал для просроченных лекарств
            val overdueChannel = NotificationChannel(
                CHANNEL_ID_OVERDUE,
                "Просроченные лекарства",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о просроченных лекарствах"
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
                setBypassDnd(true) // Обходит режим "Не беспокоить"
            }
            
            notificationManager.createNotificationChannels(listOf(medicineChannel, lowStockChannel, emergencyChannel, overdueChannel))
        }
    }
    
    fun showMedicineReminder(medicine: Medicine) {
        // Проверяем, есть ли уже активное уведомление для этого лекарства
        val existingAttempt = activeNotifications[medicine.id]
        
        if (existingAttempt != null) {
            // Увеличиваем счетчик попыток
            val updatedAttempt = existingAttempt.copy(
                attemptCount = existingAttempt.attemptCount + 1,
                lastAttemptTime = System.currentTimeMillis()
            )
            activeNotifications[medicine.id] = updatedAttempt
            
            // Показываем уведомление с учетом количества попыток
            showMedicineNotificationWithRetry(medicine, updatedAttempt)
        } else {
            // Первая попытка
            val newAttempt = NotificationAttempt(medicine, 1)
            activeNotifications[medicine.id] = newAttempt
            showMedicineNotificationWithRetry(medicine, newAttempt)
        }
        
        // Планируем следующую попытку, если лекарство не принято
        scheduleNextRetry(medicine)
    }
    
    private fun showMedicineNotificationWithRetry(medicine: Medicine, attempt: NotificationAttempt) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicine_id", medicine.id)
            putExtra("retry_attempt", attempt.attemptCount)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Кнопка "Выпить препарат"
        val takeMedicineIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "com.medicalnotes.app.MEDICINE_TAKEN"
            putExtra("medicine_id", medicine.id)
        }
        
        val takeMedicinePendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id + 200000).toInt(),
            takeMedicineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Усиленная вибрация в зависимости от количества попыток
        val vibrationPattern = when {
            attempt.attemptCount >= 4 -> longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
            attempt.attemptCount >= 2 -> longArrayOf(0, 1500, 500, 1500, 500, 1500, 500, 1500)
            else -> longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
        }
        
        // Текст уведомления с указанием попытки
        val attemptText = if (attempt.attemptCount > 1) {
            " (Попытка ${attempt.attemptCount})"
        } else {
            ""
        }
        
        val urgencyText = when {
            attempt.attemptCount >= 4 -> "КРИТИЧЕСКИ ВАЖНО!"
            attempt.attemptCount >= 2 -> "СРОЧНО!"
            else -> "ВРЕМЯ ПРИЕМА ЛЕКАРСТВА!"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$urgencyText$attemptText")
            .setContentText("${medicine.name} - ${medicine.dosage}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${medicine.name} - ${medicine.dosage}\nОсталось: ${medicine.remainingQuantity} шт.\n${medicine.notes}\n\nПопытка уведомления: ${attempt.attemptCount}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibrationPattern)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setLights(0xFF0000, 1000, 1000) // Красный свет
            .setOngoing(true) // Постоянное уведомление
            .addAction(
                R.drawable.ic_launcher_foreground,
                "ВЫПИТЬ ПРЕПАРАТ",
                takeMedicinePendingIntent
            )
            .build()
        
        notificationManager.notify(medicine.id.toInt(), notification)
        
        // Дополнительная вибрация через Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationPattern, -1)
            }
        }
        
        // Если это 3-я или больше попытка, показываем экстренное уведомление
        if (attempt.attemptCount >= 3) {
            showEmergencyEscalation(medicine, attempt)
        }
    }
    
    private fun showEmergencyEscalation(medicine: Medicine, attempt: NotificationAttempt) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicine_id", medicine.id)
            putExtra("emergency_escalation", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            (medicine.id + 50000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Кнопка "Выпить препарат" для экстренного уведомления
        val takeMedicineIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "com.medicalnotes.app.MEDICINE_TAKEN"
            putExtra("medicine_id", medicine.id)
        }
        
        val takeMedicinePendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id + 300000).toInt(),
            takeMedicineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("КРИТИЧЕСКОЕ НАПОМИНАНИЕ!")
            .setContentText("Вы не приняли ${medicine.name} уже ${attempt.attemptCount} раз!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("ВНИМАНИЕ! Вы не приняли лекарство ${medicine.name} (${medicine.dosage})\n\nЭто ${attempt.attemptCount}-е напоминание!\n\nПожалуйста, примите лекарство немедленно!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 3000, 1000, 3000, 1000, 3000))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setLights(0xFF0000, 2000, 1000) // Красный свет
            .setOngoing(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "ВЫПИТЬ ПРЕПАРАТ",
                takeMedicinePendingIntent
            )
            .build()
        
        notificationManager.notify((medicine.id + 50000).toInt(), notification)
    }
    
    private fun scheduleNextRetry(medicine: Medicine) {
        val attempt = activeNotifications[medicine.id] ?: return
        
        // Если достигли максимального количества попыток, останавливаемся
        if (attempt.attemptCount >= MAX_RETRY_ATTEMPTS) {
            return
        }
        
        // Вычисляем интервал для следующей попытки
        val intervalMinutes = when {
            attempt.attemptCount >= 4 -> ESCALATION_INTERVAL_MINUTES
            else -> RETRY_INTERVAL_MINUTES
        }
        
        scheduler.schedule({
            // Проверяем, не было ли лекарство принято
            val dataManager = DataManager(context)
            val currentMedicine = dataManager.getMedicineById(medicine.id)
            
            if (currentMedicine != null && currentMedicine.isActive && currentMedicine.remainingQuantity > 0) {
                // Проверяем, не было ли лекарство принято с момента последнего уведомления
                val timeSinceLastAttempt = System.currentTimeMillis() - attempt.lastAttemptTime
                val timeSinceLastDose = System.currentTimeMillis() - currentMedicine.lastTakenTime
                
                // Если прошло достаточно времени и лекарство не принято, показываем следующее уведомление
                if (timeSinceLastAttempt > TimeUnit.MINUTES.toMillis(intervalMinutes) && 
                    timeSinceLastDose > TimeUnit.MINUTES.toMillis(intervalMinutes)) {
                    
                    handler.post {
                        showMedicineReminder(medicine)
                    }
                }
            }
        }, intervalMinutes, TimeUnit.MINUTES)
    }
    
    fun cancelMedicineNotification(medicineId: Long) {
        android.util.Log.d("NotificationManager", "=== ОТМЕНА УВЕДОМЛЕНИЙ ЛЕКАРСТВА ===")
        android.util.Log.d("NotificationManager", "ID лекарства: $medicineId")
        
        try {
            // Отменяем обычное уведомление
            notificationManager.cancel(medicineId.toInt())
            android.util.Log.d("NotificationManager", "✓ Обычное уведомление отменено")
            
            // Отменяем экстренное уведомление
            notificationManager.cancel((medicineId + 50000).toInt())
            android.util.Log.d("NotificationManager", "✓ Экстренное уведомление отменено")
            
            // Отменяем уведомление о просроченном лекарстве
            notificationManager.cancel((medicineId + 200000).toInt())
            android.util.Log.d("NotificationManager", "✓ Уведомление о просроченном лекарстве отменено")
            
            // Удаляем из активных уведомлений
            val wasRemoved = activeNotifications.remove(medicineId) != null
            android.util.Log.d("NotificationManager", "✓ Удалено из активных: $wasRemoved")
            
            android.util.Log.d("NotificationManager", "=== ОТМЕНА УВЕДОМЛЕНИЙ ЗАВЕРШЕНА ===")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка при отмене уведомлений лекарства", e)
        }
    }
    
    fun showOverdueMedicineNotification(medicine: Medicine) {
        android.util.Log.d("NotificationManager", "=== ПОКАЗ УВЕДОМЛЕНИЯ О ПРОСРОЧЕННОМ ЛЕКАРСТВЕ ===")
        android.util.Log.d("NotificationManager", "Лекарство: ${medicine.name} (ID: ${medicine.id})")
        
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("medicine_id", medicine.id)
                putExtra("overdue", true)
            }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            (medicine.id + 200000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Кнопка "Выпить препарат"
        val takeMedicineIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "com.medicalnotes.app.MEDICINE_TAKEN"
            putExtra("medicine_id", medicine.id)
        }
        
        val takeMedicinePendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id + 300000).toInt(),
            takeMedicineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Усиленная вибрация для просроченных лекарств
        val vibrationPattern = longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_OVERDUE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 ПРОСРОЧЕНО! Выпейте препарат СРОЧНО!")
            .setContentText("${medicine.name} - время приема прошло!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🚨 ВНИМАНИЕ! Лекарство ${medicine.name} (${medicine.dosage}) просрочено!\n\n⏰ Время приема уже прошло!\n\n💊 Пожалуйста, примите лекарство немедленно!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "💊 ВЫПИТЬ ПРЕПАРАТ", takeMedicinePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibrationPattern)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setLights(0xFF0000, 2000, 1000) // Красный свет
            .setOngoing(true)
            .build()
        
        notificationManager.notify((medicine.id + 200000).toInt(), notification)
        android.util.Log.d("NotificationManager", "✓ Уведомление показано (ID: ${(medicine.id + 200000).toInt()})")
        
        // Дополнительная вибрация через Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationPattern, -1)
            }
        }
        
        // Добавляем в активные уведомления
        activeNotifications[medicine.id] = NotificationAttempt(medicine, 1)
        android.util.Log.d("NotificationManager", "✓ Добавлено в активные уведомления")
        
        // Планируем повторное уведомление каждые 5 секунд с усиленным звуком
        scheduler.schedule({
            android.util.Log.d("NotificationManager", "Планируем повторное уведомление для ${medicine.name}")
            // Проверяем, не было ли лекарство принято
            if (activeNotifications.containsKey(medicine.id)) {
                // Повторяем звук и вибрацию
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(vibrationPattern, -1)
                    }
                }
                
                // Показываем уведомление снова
                showOverdueMedicineNotification(medicine)
            } else {
                android.util.Log.d("NotificationManager", "Лекарство ${medicine.name} уже принято, повторное уведомление отменено")
            }
        }, 5, TimeUnit.SECONDS)
        
        android.util.Log.d("NotificationManager", "=== ПОКАЗ УВЕДОМЛЕНИЯ ЗАВЕРШЕН ===")
        
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка при показе уведомления о просроченном лекарстве", e)
        }
    }
    
    fun markMedicineAsTaken(medicineId: Long) {
        // Немедленно останавливаем вибрацию
        stopVibration()
        
        // Отменяем все уведомления для этого лекарства
        cancelMedicineNotification(medicineId)
        
        // Показываем подтверждение
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicine_taken", medicineId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            (medicineId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Лекарство принято!")
            .setContentText("Отлично! Лекарство принято вовремя.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        notificationManager.notify((medicineId + 100000).toInt(), notification)
    }
    
    fun showLowStockAlert(medicine: Medicine) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("low_stock_medicine_id", medicine.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            (medicine.id + 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOW_STOCK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Низкий запас лекарства")
            .setContentText("${medicine.name} - осталось ${medicine.remainingQuantity} шт.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        notificationManager.notify((medicine.id + 10000).toInt(), notification)
    }
    
    fun showEmergencyAlert(message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("emergency", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_EMERGENCY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Экстренное уведомление")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_EMERGENCY, notification)
        
        // Длительная вибрация для экстренных уведомлений
        if (vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        activeNotifications.clear()
        scheduler.shutdown()
    }

    fun stopVibration() {
        android.util.Log.d("NotificationManager", "=== ОСТАНОВКА ВИБРАЦИИ ===")
        
        try {
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
                android.util.Log.d("NotificationManager", "✓ Вибратор остановлен")
            } else {
                android.util.Log.d("NotificationManager", "⚠ Вибратор недоступен")
            }
            
            // Останавливаем все активные уведомления
            val activeCount = activeNotifications.size
            activeNotifications.clear()
            android.util.Log.d("NotificationManager", "✓ Очищено активных уведомлений: $activeCount")
            
            // Останавливаем планировщик
            if (!scheduler.isShutdown) {
                scheduler.shutdown()
                android.util.Log.d("NotificationManager", "✓ Планировщик остановлен")
            } else {
                android.util.Log.d("NotificationManager", "⚠ Планировщик уже был остановлен")
            }
            
            android.util.Log.d("NotificationManager", "=== ОСТАНОВКА ВИБРАЦИИ ЗАВЕРШЕНА ===")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка при остановке вибрации", e)
        }
    }

    fun stopAllAlarms() {
        // Останавливаем вибрацию
        stopVibration()
        
        // Отменяем все уведомления
        cancelAllNotifications()
    }
} 