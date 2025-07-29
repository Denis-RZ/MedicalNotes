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
import com.medicalnotes.app.utils.DataManager
import java.time.LocalTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class NotificationManager(private val context: Context) {
    

    
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
    private var scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    
    // Хранилище активных уведомлений с попытками
    private val activeNotifications = mutableMapOf<Long, NotificationAttempt>()
    
    // ✅ ДОБАВЛЕНО: Хранилище активных звуков для принудительной остановки
    private val activeRingtones = mutableMapOf<Long, android.media.Ringtone>()
    
    // ✅ ДОБАВЛЕНО: Хранилище активных планировщиков для отмены
    private val activeSchedulers = mutableMapOf<Long, java.util.concurrent.ScheduledFuture<*>>()
    
    // ✅ ДОБАВЛЕНО: Глобальное состояние для предотвращения дублирования
    companion object {
        const val CHANNEL_ID_MEDICINE = "medicine_reminders"
        const val CHANNEL_ID_LOW_STOCK = "low_stock_alerts"
        const val CHANNEL_ID_EMERGENCY = "emergency_alerts"
        const val CHANNEL_ID_OVERDUE = "overdue_medicines"
        const val CHANNEL_ID_MEDICINE_CARD = "medicine_card_notifications"
        
        const val NOTIFICATION_ID_MEDICINE = 1001
        const val NOTIFICATION_ID_LOW_STOCK = 1002
        const val NOTIFICATION_ID_EMERGENCY = 1003
        const val NOTIFICATION_ID_OVERDUE = 1004
        const val NOTIFICATION_ID_MEDICINE_CARD = 1005
        
        // Настройки повторных попыток
        const val MAX_RETRY_ATTEMPTS = 5
        const val RETRY_INTERVAL_MINUTES = 15L
        const val ESCALATION_INTERVAL_MINUTES = 30L
        
        // ✅ ДОБАВЛЕНО: Глобальное отслеживание активных уведомлений
        private val globalActiveNotifications = mutableSetOf<Long>()
        
        fun isNotificationActive(medicineId: Long): Boolean {
            return globalActiveNotifications.contains(medicineId)
        }
        
        fun markNotificationActive(medicineId: Long) {
            globalActiveNotifications.add(medicineId)
        }
        
        fun markNotificationInactive(medicineId: Long) {
            globalActiveNotifications.remove(medicineId)
        }
        
        fun clearAllActiveNotifications() {
            globalActiveNotifications.clear()
        }
    }
    
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
                // ✅ ИСПРАВЛЕНО: Убираем звук из канала - он воспроизводится системой и не может быть остановлен!
                // setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), 
                //     AudioAttributes.Builder()
                //         .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                //         .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                //         .build()
                // )
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
                // ✅ ИСПРАВЛЕНО: Убираем звук из канала - он воспроизводится системой и не может быть остановлен!
                // setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                //     AudioAttributes.Builder()
                //         .setUsage(AudioAttributes.USAGE_ALARM)
                //         .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                //         .build()
                // )
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
                
                // ✅ ИСПРАВЛЕНО: Убираем звук из канала - он воспроизводится системой и не может быть остановлен!
                // try {
                //     val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                //     if (alarmUri != null) {
                //         setSound(alarmUri,
                //             AudioAttributes.Builder()
                //                 .setUsage(AudioAttributes.USAGE_ALARM)
                //                 .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                //                 .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                //                 .build()
                //         )
                //         android.util.Log.d("NotificationManager", "✓ Звук будильника настроен")
                //     } else {
                //         // Fallback на системный звук уведомления
                //         val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                //         setSound(notificationUri,
                //             AudioAttributes.Builder()
                //                 .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                //                 .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                //                 .build()
                //         )
                //         android.util.Log.d("NotificationManager", "⚠ Используется звук уведомления (fallback)")
                //     }
                // } catch (e: Exception) {
                //     android.util.Log.e("NotificationManager", "Ошибка настройки звука", e)
                // }
                
                setBypassDnd(true) // Обходит режим "Не беспокоить"
                setShowBadge(true) // Показывает значок на иконке приложения
            }
            
            // Канал для карточек лекарств
            val medicineCardChannel = NotificationChannel(
                CHANNEL_ID_MEDICINE_CARD,
                "Карточки лекарств",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления с карточками лекарств и кнопками действий"
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 1000, 300, 1000, 300, 1000)
                setBypassDnd(true) // Обходит режим "Не беспокоить"
                setShowBadge(true) // Показывает значок на иконке приложения
            }
            
            notificationManager.createNotificationChannels(listOf(medicineChannel, lowStockChannel, emergencyChannel, overdueChannel, medicineCardChannel))
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
            
            // Показываем уведомление с карточкой с учетом количества попыток
            showMedicineCardNotificationWithRetry(medicine, updatedAttempt)
        } else {
            // Первая попытка
            val newAttempt = NotificationAttempt(medicine, 1)
            activeNotifications[medicine.id] = newAttempt
            showMedicineCardNotificationWithRetry(medicine, newAttempt)
        }
        
        // Планируем следующую попытку, если лекарство не принято
        scheduleNextRetry(medicine)
    }
    
    private fun showMedicineCardNotificationWithRetry(medicine: Medicine, attempt: NotificationAttempt) {
        try {
            android.util.Log.d("NotificationManager", "Создание уведомления с карточкой для: ${medicine.name} (попытка ${attempt.attemptCount})")
            
            // Создаем кастомный layout для уведомления с учетом попыток
            val customLayout = createMedicineCardLayoutWithRetry(medicine, attempt)
            
            // Создаем PendingIntent для кнопки "Выпил"
            val takenIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_MEDICINE_TAKEN"
                putExtra("medicine_id", medicine.id)
                putExtra("action", "taken")
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                (medicine.id * 1000 + 1).toInt(),
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Создаем PendingIntent для кнопки "Пропустить"
            val skipIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_MEDICINE_SKIPPED"
                putExtra("medicine_id", medicine.id)
                putExtra("action", "skipped")
            }
            val skipPendingIntent = PendingIntent.getBroadcast(
                context,
                (medicine.id * 1000 + 2).toInt(),
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Создаем PendingIntent для открытия приложения
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                (medicine.id * 1000 + 3).toInt(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Усиленная вибрация в зависимости от количества попыток
            val vibrationPattern = when {
                attempt.attemptCount >= 4 -> longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
                attempt.attemptCount >= 2 -> longArrayOf(0, 1500, 500, 1500, 500, 1500, 500, 1500)
                else -> longArrayOf(0, 1000, 300, 1000, 300, 1000)
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
                else -> "Время принять лекарство!"
            }
            
            // Создаем уведомление
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE_CARD)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle("$urgencyText$attemptText")
                .setContentText("${medicine.name} - ${medicine.dosage}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${medicine.name}\nДозировка: ${medicine.dosage}\nВремя: ${medicine.time}\nПопытка: ${attempt.attemptCount}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_medicine, "✅ Выпил", takenPendingIntent)
                .addAction(R.drawable.ic_medicine, "⏭ Пропустить", skipPendingIntent)
                .setCustomBigContentView(customLayout)
                .setVibrate(vibrationPattern)
                .setLights(0xFF0000, 1000, 1000) // Красный свет
                .build()
            
            // Показываем уведомление
            val notificationId = (NOTIFICATION_ID_MEDICINE_CARD + medicine.id).toInt()
            notificationManager.notify(notificationId, notification)
            
            // Запускаем звук и вибрацию
            try {
                // Вибрация
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(vibrationPattern, 0)
                    }
                }
                
                // Звук
                val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (ringtone != null) {
                    activeRingtones[medicine.id] = ringtone
                    ringtone.play()
                    
                    // Останавливаем звук через 3 секунды
                    handler.postDelayed({
                        try {
                            if (ringtone.isPlaying) {
                                ringtone.stop()
                            }
                            activeRingtones.remove(medicine.id)
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка остановки звука", e)
                        }
                    }, 3000)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка запуска звука/вибрации", e)
            }
            
            android.util.Log.d("NotificationManager", "Уведомление с карточкой создано для: ${medicine.name} (попытка ${attempt.attemptCount})")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка создания уведомления с карточкой", e)
        }
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
            // ✅ ИСПРАВЛЕНО: Убираем звук из уведомления - он воспроизводится системой и не может быть остановлен!
            // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationPattern, 0) // Изменено с -1 на 0
                }
                android.util.Log.d("NotificationManager", "✓ Вибрация запущена (showMedicineNotificationWithRetry)")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка запуска вибрации", e)
            }
        }
        
        // ✅ ДОБАВЛЕНО: Короткий звуковой сигнал при включении вибрации
        try {
            val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            if (ringtone != null) {
                activeRingtones[medicine.id] = ringtone
                ringtone.play()
                android.util.Log.d("NotificationManager", "🔊 КОРОТКИЙ ЗВУК ВКЛЮЧЕН (showMedicineNotificationWithRetry): ${medicine.name}")
                
                // Останавливаем звук через 2 секунды
                handler.postDelayed({
                    try {
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                            android.util.Log.d("NotificationManager", "🔇 КОРОТКИЙ ЗВУК ОСТАНОВЛЕН (showMedicineNotificationWithRetry): ${medicine.name}")
                        }
                        activeRingtones.remove(medicine.id)
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка остановки короткого звука", e)
                    }
                }, 2000) // 2 секунды
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка воспроизведения короткого звука", e)
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
            // ✅ ИСПРАВЛЕНО: Убираем звук из уведомления - он воспроизводится системой и не может быть остановлен!
            // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
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
        
        // ✅ ДОБАВЛЕНО: Подробное логирование начала показа уведомления
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_НАЧАЛО", "Начинаем показ уведомления для: ${medicine.name} (ID: ${medicine.id})")
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_ДАННЫЕ", "Время приема: ${medicine.time}, Принято сегодня: ${medicine.takenToday}")
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_ВРЕМЯ", "Текущее время: ${LocalTime.now()}")
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_СТАТУС", "Статус лекарства: ${com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)}")
        
        // ✅ ДОБАВЛЕНО: Проверка на дублирование уведомлений
        if (isNotificationActive(medicine.id)) {
            android.util.Log.d("NotificationManager", "⚠ Уведомление уже активно для лекарства ${medicine.name}, пропускаем")
            return
        }
        
        // ✅ ДОБАВЛЕНО: Проверка, не принято ли уже лекарство
        val dataManager = DataManager(context)
        val currentMedicine = dataManager.getMedicineById(medicine.id)
        if (currentMedicine?.takenToday == true) {
            android.util.Log.d("NotificationManager", "⚠ Лекарство ${medicine.name} уже принято сегодня, пропускаем уведомление")
            return
        }
        
        // ✅ ДОБАВЛЕНО: Отмечаем уведомление как активное
        markNotificationActive(medicine.id)
        android.util.Log.d("NotificationManager", "✓ Уведомление отмечено как активное для ${medicine.name}")
        
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
            // ✅ ИСПРАВЛЕНО: Убираем звук из уведомления - он воспроизводится системой и не может быть остановлен!
            // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setLights(0xFF0000, 2000, 1000) // Красный свет
            .setOngoing(true)
            .build()
        
        notificationManager.notify((medicine.id + 200000).toInt(), notification)
        android.util.Log.d("NotificationManager", "✓ Уведомление показано (ID: ${(medicine.id + 200000).toInt()})")
        
        // ✅ ДОБАВЛЕНО: Короткий звуковой сигнал при включении вибрации
        try {
            val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            if (ringtone != null) {
                activeRingtones[medicine.id] = ringtone
                ringtone.play()
                android.util.Log.d("NotificationManager", "🔊 КОРОТКИЙ ЗВУК ВКЛЮЧЕН: ${medicine.name} (ID: ${medicine.id})")
                
                // Останавливаем звук через 2 секунды
                handler.postDelayed({
                    try {
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                            android.util.Log.d("NotificationManager", "🔇 КОРОТКИЙ ЗВУК ОСТАНОВЛЕН: ${medicine.name}")
                        }
                        activeRingtones.remove(medicine.id)
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка остановки короткого звука", e)
                    }
                }, 2000) // 2 секунды
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка воспроизведения короткого звука", e)
        }
        
        // Дополнительная вибрация через Vibrator (с возможностью остановки)
        if (vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationPattern, 0) // Изменено с -1 на 0 для возможности остановки
                }
                android.util.Log.d("NotificationManager", "✓ Вибрация запущена")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка запуска вибрации", e)
            }
        }
        
        // Добавляем в активные уведомления
        activeNotifications[medicine.id] = NotificationAttempt(medicine, 1)
        android.util.Log.d("NotificationManager", "✓ Добавлено в активные уведомления")
        
        // ✅ ИСПРАВЛЕНО: Планируем повторное уведомление каждые 5 секунд ТОЛЬКО со звуком
        val scheduledTask = scheduler.scheduleAtFixedRate({
            android.util.Log.d("NotificationManager", "Планируем повторное уведомление для ${medicine.name}")
            android.util.Log.d("🔔 ПЛАНИРОВЩИК_СРАБОТАЛ", "Планировщик сработал для ${medicine.name} (ID: ${medicine.id})")
            android.util.Log.d("🔔 ПЛАНИРОВЩИК_ВРЕМЯ", "Время срабатывания: ${LocalTime.now()}")
            
            // ✅ ДОБАВЛЕНО: Проверяем, не принято ли лекарство
            val dataManager = DataManager(context)
            val currentMedicine = dataManager.getMedicineById(medicine.id)
            android.util.Log.d("🔔 ПЛАНИРОВЩИК_ПРОВЕРКА", "Проверяем лекарство: ${medicine.name} (ID: ${medicine.id})")
            android.util.Log.d("🔔 ПЛАНИРОВЩИК_СТАТУС", "takenToday в базе: ${currentMedicine?.takenToday}")
            android.util.Log.d("🔔 ПЛАНИРОВЩИК_ВРЕМЯ_ПРОВЕРКИ", "Время проверки: ${LocalTime.now()}")
            
            // ✅ ИСПРАВЛЕНО: Проверяем статус и продолжаем только если лекарство не принято
            if (currentMedicine?.takenToday == true) {
                android.util.Log.d("NotificationManager", "Лекарство ${medicine.name} уже принято, повторное уведомление отменено")
                android.util.Log.d("🔔 ПЛАНИРОВЩИК_ОСТАНОВКА", "Планировщик остановлен для ${medicine.name} - лекарство принято")
                // Удаляем из активных уведомлений
                activeNotifications.remove(medicine.id)
                markNotificationInactive(medicine.id)
                // Отменяем планировщик
                val scheduledTask = activeSchedulers[medicine.id]
                if (scheduledTask != null) {
                    scheduledTask.cancel(false)
                    activeSchedulers.remove(medicine.id)
                    android.util.Log.d("🔔 ПЛАНИРОВЩИК_ОТМЕНЕН", "Планировщик отменен для ${medicine.name}")
                }
            } else {
                android.util.Log.d("🔔 ПЛАНИРОВЩИК_ПРОДОЛЖЕНИЕ", "Планировщик продолжает работу для ${medicine.name} - лекарство не принято")
                
                // Проверяем, не было ли лекарство принято
                android.util.Log.d("🔔 ПЛАНИРОВЩИК_АКТИВНЫЕ", "Проверяем активные уведомления: ${activeNotifications.keys}")
                android.util.Log.d("🔔 ПЛАНИРОВЩИК_ПОИСК", "Ищем лекарство ID: ${medicine.id} в активных уведомлениях")
                
                if (activeNotifications.containsKey(medicine.id)) {
                    android.util.Log.d("🔔 ПЛАНИРОВЩИК_НАЙДЕНО", "Лекарство ${medicine.name} найдено в активных уведомлениях - повторяем звук")
                    // Повторяем звук и вибрацию
                    if (vibrator.hasVibrator()) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                                vibrator.vibrate(effect)
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(vibrationPattern, 0) // Изменено с -1 на 0
                            }
                            android.util.Log.d("NotificationManager", "✓ Повторная вибрация запущена")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка повторной вибрации", e)
                        }
                    }
                    
                    // ✅ ДОБАВЛЕНО: Короткий звуковой сигнал при повторной вибрации
                    try {
                        val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        if (ringtone != null) {
                            activeRingtones[medicine.id] = ringtone
                            ringtone.play()
                            android.util.Log.d("🔔 ПЛАНИРОВЩИК_ЗВУК", "Короткий звук при повторной вибрации для ${medicine.name}")
                            
                            // Останавливаем звук через 2 секунды
                            handler.postDelayed({
                                try {
                                    if (ringtone.isPlaying) {
                                        ringtone.stop()
                                        android.util.Log.d("🔔 ПЛАНИРОВЩИК_ЗВУК", "Короткий звук остановлен для ${medicine.name}")
                                    }
                                    activeRingtones.remove(medicine.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("🔔 ПЛАНИРОВЩИК_ЗВУК", "Ошибка остановки короткого звука", e)
                                }
                            }, 2000) // 2 секунды
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("🔔 ПЛАНИРОВЩИК_ЗВУК", "Ошибка воспроизведения короткого звука", e)
                    }
                } else {
                    android.util.Log.d("NotificationManager", "Лекарство ${medicine.name} уже принято, повторное уведомление отменено")
                    android.util.Log.d("🔔 ПЛАНИРОВЩИК_НЕ_НАЙДЕНО", "Лекарство ${medicine.name} НЕ найдено в активных уведомлениях - останавливаем планировщик")
                }
            }
        }, 0, 5, TimeUnit.SECONDS)
        
        // ✅ ДОБАВЛЕНО: Сохраняем планировщик для возможности отмены
        activeSchedulers[medicine.id] = scheduledTask
        android.util.Log.d("🔔 ПЛАНИРОВЩИК_СОХРАНЕН", "Планировщик сохранен для лекарства ID: ${medicine.id}")
        
        android.util.Log.d("NotificationManager", "=== ПОКАЗ УВЕДОМЛЕНИЯ ЗАВЕРШЕН ===")
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_ЗАВЕРШЕНО", "Уведомление для ${medicine.name} (ID: ${medicine.id}) полностью завершено")
        android.util.Log.d("🔔 УВЕДОМЛЕНИЕ_ПЛАНИРОВЩИК", "Планировщик настроен на повтор через 5 секунд для ${medicine.name}")
        
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка при показе уведомления о просроченном лекарстве", e)
            android.util.Log.e("🔔 УВЕДОМЛЕНИЕ_ОШИБКА", "Ошибка показа уведомления для ${medicine.name}: ${e.message}")
        }
    }
    
    fun cancelOverdueNotification(medicineId: Long) {
        android.util.Log.d("NotificationManager", "=== ОТМЕНА УВЕДОМЛЕНИЯ ===")
        android.util.Log.d("NotificationManager", "Лекарство ID: $medicineId")
        android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Функция cancelOverdueNotification() ВЫЗВАНА")
        android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Время начала: ${System.currentTimeMillis()}")
        
        // ✅ ДОБАВЛЕНО: Логирование через addLog для отображения в приложении
        try {
            val mainActivity = context as? com.medicalnotes.app.MainActivity
            mainActivity?.addLog("🔇 NotificationManager: cancelOverdueNotification() ВЫЗВАНА")
            mainActivity?.addLog("🔇 NotificationManager: Лекарство ID: $medicineId")
            mainActivity?.addLog("🔇 NotificationManager: Время начала: ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
        }
        
        // ✅ ДОБАВЛЕНО: Немедленная остановка планировщика
        android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Немедленно останавливаем планировщик для лекарства ID: $medicineId")
        
        // Удаляем из активных уведомлений немедленно
        activeNotifications.remove(medicineId)
        android.util.Log.d("🔇 ОТМЕНА_АКТИВНЫЕ", "Удалено из активных уведомлений: $medicineId")
        
        // Отмечаем уведомление как неактивное
        markNotificationInactive(medicineId)
        android.util.Log.d("🔇 ОТМЕНА_НЕАКТИВНОЕ", "Отмечено как неактивное: $medicineId")
        
        // ✅ ДОБАВЛЕНО: Останавливаем конкретный Ringtone для этого лекарства
        try {
            android.util.Log.d("🔇 ОТМЕНА_RINGTONE", "Проверяем Ringtone для лекарства ID: $medicineId")
            val ringtone = activeRingtones[medicineId]
            if (ringtone != null) {
                android.util.Log.d("🔇 ОТМЕНА_RINGTONE", "Ringtone найден для лекарства ID: $medicineId")
                android.util.Log.d("🔇 ОТМЕНА_RINGTONE", "Ringtone объект: $ringtone")
                android.util.Log.d("🔇 ОТМЕНА_RINGTONE", "isPlaying: ${ringtone.isPlaying}")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Ringtone найден для ID: $medicineId")
                    mainActivity?.addLog("🔇 NotificationManager: isPlaying: ${ringtone.isPlaying}")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
                
                if (ringtone.isPlaying) {
                    ringtone.stop()
                    android.util.Log.d("🔇 RINGTONE_ОСТАНОВЛЕН", "Ringtone остановлен для лекарства ID: $medicineId")
                    android.util.Log.d("🔇 RINGTONE_ОСТАНОВЛЕН", "Время остановки: ${System.currentTimeMillis()}")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Ringtone ОСТАНОВЛЕН для ID: $medicineId")
                        mainActivity?.addLog("🔇 NotificationManager: Время остановки: ${System.currentTimeMillis()}")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                } else {
                    android.util.Log.d("🔇 RINGTONE_ПРОВЕРКА", "Ringtone не играл для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Ringtone не играл для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                activeRingtones.remove(medicineId)
                android.util.Log.d("🔇 RINGTONE_УДАЛЕН", "Ringtone удален из активных для лекарства ID: $medicineId")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Ringtone удален из активных для ID: $medicineId")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            } else {
                android.util.Log.d("🔇 RINGTONE_НЕ_НАЙДЕН", "Ringtone не найден для лекарства ID: $medicineId")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Ringtone не найден для ID: $medicineId")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("🔇 RINGTONE_ОШИБКА", "Ошибка остановки Ringtone для лекарства ID: $medicineId", e)
            
            // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("❌ NotificationManager: Ошибка остановки Ringtone ID: $medicineId")
                mainActivity?.addLog("❌ NotificationManager: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
            }
        }
        
        // ✅ ИСПРАВЛЕНО: Отменяем конкретный планировщик для этого лекарства
        try {
            val scheduledTask = activeSchedulers[medicineId]
            if (scheduledTask != null) {
                android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Найден планировщик для лекарства ID: $medicineId")
                scheduledTask.cancel(false)
                activeSchedulers.remove(medicineId)
                android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Планировщик отменен для лекарства ID: $medicineId")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Планировщик отменен для ID: $medicineId")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            } else {
                android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Планировщик не найден для лекарства ID: $medicineId")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Планировщик не найден для ID: $medicineId")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Ошибка отмены планировщика для лекарства ID: $medicineId", e)
            
            // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("❌ NotificationManager: Ошибка отмены планировщика для ID: $medicineId")
                mainActivity?.addLog("❌ NotificationManager: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
            }
        }
        
        try {
            // ✅ ДОБАВЛЕНО: Отмечаем уведомление как неактивное
            markNotificationInactive(medicineId)
            android.util.Log.d("NotificationManager", "✓ Уведомление отмечено как неактивное")
            
            // ✅ ДОБАВЛЕНО: Отменяем все возможные уведомления для этого лекарства
            notificationManager.cancel((medicineId + 200000).toInt()) // Основное уведомление
            notificationManager.cancel((medicineId + 300000).toInt()) // Кнопка принятия
            notificationManager.cancel((medicineId + 50000).toInt())  // Экстренное уведомление
            notificationManager.cancel(medicineId.toInt())           // Обычное уведомление
            notificationManager.cancel((medicineId + 100000).toInt()) // Подтверждение принятия
            android.util.Log.d("NotificationManager", "✓ Все уведомления отменены")
            
            // ✅ ДОБАВЛЕНО: Логирование в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("🔇 NotificationManager: Все уведомления отменены для ID: $medicineId")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
            }
            
            // Удаляем из активных уведомлений
            activeNotifications.remove(medicineId)
            android.util.Log.d("NotificationManager", "✓ Удалено из активных уведомлений")
            
            // Останавливаем вибрацию принудительно
            try {
                if (vibrator.hasVibrator()) {
                    vibrator.cancel()
                    android.util.Log.d("NotificationManager", "✓ Вибрация остановлена")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Вибрация остановлена для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка остановки вибрации", e)
            }
            
            // ✅ ИЗМЕНЕНО: Останавливаем планировщик для этого лекарства
            try {
                // Не останавливаем весь планировщик, а только удаляем из активных уведомлений
                activeNotifications.remove(medicineId)
                android.util.Log.d("NotificationManager", "✓ Планировщик для лекарства $medicineId остановлен")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка остановки планировщика", e)
            }
            
            // ✅ УЛУЧШЕНО: Полная остановка всех звуков
            try {
                android.util.Log.d("🔇 ОСТАНОВКА_ЗВУКА", "Начинаем остановку звука для лекарства ID: $medicineId")
                
                // Останавливаем звук будильника
                val alarmRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                if (alarmRingtone != null && alarmRingtone.isPlaying) {
                    alarmRingtone.stop()
                    android.util.Log.d("🔇 ЗВУК_ОСТАНОВЛЕН", "Будильник остановлен для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Будильник остановлен для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                } else {
                    android.util.Log.d("🔇 ЗВУК_ПРОВЕРКА", "Будильник не играл для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Будильник не играл для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // Останавливаем звук уведомления
                val notificationRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (notificationRingtone != null && notificationRingtone.isPlaying) {
                    notificationRingtone.stop()
                    android.util.Log.d("🔇 ЗВУК_ОСТАНОВЛЕН", "Уведомление остановлено для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Уведомление остановлено для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                } else {
                    android.util.Log.d("🔇 ЗВУК_ПРОВЕРКА", "Уведомление не играло для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Уведомление не играло для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // Останавливаем системный звук
                val systemRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                if (systemRingtone != null && systemRingtone.isPlaying) {
                    systemRingtone.stop()
                    android.util.Log.d("🔇 ЗВУК_ОСТАНОВЛЕН", "Системный звук остановлен для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Системный звук остановлен для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                } else {
                    android.util.Log.d("🔇 ЗВУК_ПРОВЕРКА", "Системный звук не играл для лекарства ID: $medicineId")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Системный звук не играл для ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                    android.util.Log.d("🔇 AUDIO_MANAGER", "Оригинальная громкость уведомлений: $originalVolume")
                    
                    // Временно отключаем звук уведомлений
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                    android.util.Log.d("🔇 AUDIO_MANAGER", "Звук уведомлений временно отключен")
                    
                    // Восстанавливаем через 100мс
                    Handler(Looper.getMainLooper()).postDelayed({
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                        android.util.Log.d("🔇 AUDIO_MANAGER", "Громкость уведомлений восстановлена: $originalVolume")
                    }, 100)
                    
                } catch (e: Exception) {
                    android.util.Log.e("🔇 AUDIO_MANAGER", "Ошибка управления звуком", e)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("🔇 ОСТАНОВКА_ЗВУКА", "Ошибка остановки звука для лекарства ID: $medicineId", e)
                
                // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("❌ NotificationManager: Ошибка остановки звука для ID: $medicineId")
                    mainActivity?.addLog("❌ NotificationManager: ${e.message}")
                } catch (e2: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
                }
            }
            
            android.util.Log.d("NotificationManager", "=== ОТМЕНА УВЕДОМЛЕНИЯ ЗАВЕРШЕНА ===")
            android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Функция cancelOverdueNotification() ЗАВЕРШЕНА")
            android.util.Log.d("🔇 ОТМЕНА_ПЛАНИРОВЩИК", "Время завершения: ${System.currentTimeMillis()}")
            
            // ✅ ДОБАВЛЕНО: Логирование в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("🔇 NotificationManager: cancelOverdueNotification() ЗАВЕРШЕНА")
                mainActivity?.addLog("🔇 NotificationManager: Время завершения: ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка отмены уведомления", e)
            
            // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("❌ NotificationManager: Ошибка отмены уведомления")
                mainActivity?.addLog("❌ NotificationManager: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
            }
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
        android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Начинаем глобальную остановку всех звуков")
        android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Время начала: ${System.currentTimeMillis()}")
        android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Функция stopVibration() ВЫЗВАНА")
        
        // ✅ ДОБАВЛЕНО: Логирование через addLog для отображения в приложении
        try {
            val mainActivity = context as? com.medicalnotes.app.MainActivity
            mainActivity?.addLog("🔇 NotificationManager: stopVibration() ВЫЗВАНА")
            mainActivity?.addLog("🔇 NotificationManager: Время начала: ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
        }
        
        // ✅ ДОБАВЛЕНО: Немедленная остановка всех планировщиков
        android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ПЛАНИРОВЩИК", "Останавливаем все планировщики")
        val activeCount = activeNotifications.size
        activeNotifications.clear()
        android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ПЛАНИРОВЩИК", "Все активные уведомления очищены: $activeCount")
        
        try {
            // ✅ ДОБАВЛЕНО: Очищаем глобальное состояние
            clearAllActiveNotifications()
            android.util.Log.d("NotificationManager", "✓ Глобальное состояние очищено")
            
            // ✅ ДОБАВЛЕНО: Останавливаем все активные Ringtone
            android.util.Log.d("🔇 АКТИВНЫЕ_RINGTONE", "Останавливаем все активные Ringtone (количество: ${activeRingtones.size})")
            android.util.Log.d("🔇 АКТИВНЫЕ_RINGTONE", "Список активных Ringtone: ${activeRingtones.keys}")
            
            // ✅ ДОБАВЛЕНО: Логирование в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("🔇 NotificationManager: Активных Ringtone: ${activeRingtones.size}")
                mainActivity?.addLog("🔇 NotificationManager: Список Ringtone: ${activeRingtones.keys}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
            }
            
            activeRingtones.forEach { (medicineId, ringtone) ->
                try {
                    android.util.Log.d("🔇 RINGTONE_ПРОВЕРКА", "Проверяем Ringtone для лекарства ID: $medicineId")
                    android.util.Log.d("🔇 RINGTONE_ПРОВЕРКА", "Ringtone объект: $ringtone")
                    android.util.Log.d("🔇 RINGTONE_ПРОВЕРКА", "isPlaying: ${ringtone.isPlaying}")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Проверяем Ringtone ID: $medicineId")
                        mainActivity?.addLog("🔇 NotificationManager: isPlaying: ${ringtone.isPlaying}")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                    
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                        android.util.Log.d("🔇 RINGTONE_ОСТАНОВЛЕН", "Ringtone остановлен для лекарства ID: $medicineId")
                        android.util.Log.d("🔇 RINGTONE_ОСТАНОВЛЕН", "Время остановки: ${System.currentTimeMillis()}")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Ringtone ОСТАНОВЛЕН для ID: $medicineId")
                            mainActivity?.addLog("🔇 NotificationManager: Время остановки: ${System.currentTimeMillis()}")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    } else {
                        android.util.Log.d("🔇 RINGTONE_ПРОВЕРКА", "Ringtone не играл для лекарства ID: $medicineId")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Ringtone не играл для ID: $medicineId")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("🔇 RINGTONE_ОШИБКА", "Ошибка остановки Ringtone для лекарства ID: $medicineId", e)
                    
                    // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("❌ NotificationManager: Ошибка остановки Ringtone ID: $medicineId")
                        mainActivity?.addLog("❌ NotificationManager: ${e.message}")
                    } catch (e2: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
                    }
                }
            }
            activeRingtones.clear()
            android.util.Log.d("🔇 АКТИВНЫЕ_RINGTONE", "Все активные Ringtone очищены")
            
            // ✅ ДОБАВЛЕНО: Логирование в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("🔇 NotificationManager: Все Ringtone очищены")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
            }
            
            // Принудительно останавливаем вибрацию
            if (vibrator.hasVibrator()) {
                try {
                    vibrator.cancel()
                    android.util.Log.d("NotificationManager", "✓ Вибратор остановлен")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Вибратор остановлен")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка остановки вибратора", e)
                }
            } else {
                android.util.Log.d("NotificationManager", "⚠ Вибратор недоступен")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("⚠ NotificationManager: Вибратор недоступен")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            }
            
            // Останавливаем все активные уведомления
            val activeCount2 = activeNotifications.size
            activeNotifications.clear()
            android.util.Log.d("NotificationManager", "✓ Очищено активных уведомлений: $activeCount2")
            
            // ✅ ИЗМЕНЕНО: Останавливаем планировщик и создаем новый
            try {
                if (!scheduler.isShutdown) {
                    scheduler.shutdown()
                    android.util.Log.d("NotificationManager", "✓ Старый планировщик остановлен")
                } else {
                    android.util.Log.d("NotificationManager", "⚠ Планировщик уже был остановлен")
                }
                
                // Создаем новый планировщик
                scheduler = Executors.newScheduledThreadPool(2)
                android.util.Log.d("NotificationManager", "✓ Новый планировщик создан")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка пересоздания планировщика", e)
            }
            
            // ✅ ДОБАВЛЕНО: Принудительная остановка всех уведомлений
            try {
                notificationManager.cancelAll()
                android.util.Log.d("NotificationManager", "✓ Все уведомления отменены")
                
                // ✅ ДОБАВЛЕНО: Логирование в приложение
                try {
                    val mainActivity = context as? com.medicalnotes.app.MainActivity
                    mainActivity?.addLog("🔇 NotificationManager: Все уведомления отменены")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка отмены всех уведомлений", e)
            }
            
            // ✅ УЛУЧШЕНО: Полная остановка всех звуков
            try {
                android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Начинаем глобальную остановку всех звуков")
                
                // Останавливаем звук будильника
                val alarmRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                if (alarmRingtone != null) {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Проверяем будильник: isPlaying = ${alarmRingtone.isPlaying}")
                    if (alarmRingtone.isPlaying) {
                        alarmRingtone.stop()
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Будильник остановлен глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Будильник остановлен глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    } else {
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Будильник не играл глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Будильник не играл глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    }
                } else {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Будильник недоступен")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Будильник недоступен")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // Останавливаем звук уведомления
                val notificationRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (notificationRingtone != null) {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Проверяем уведомление: isPlaying = ${notificationRingtone.isPlaying}")
                    if (notificationRingtone.isPlaying) {
                        notificationRingtone.stop()
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Уведомление остановлено глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Уведомление остановлено глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    } else {
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Уведомление не играло глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Уведомление не играло глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    }
                } else {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Уведомление недоступно")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Уведомление недоступно")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // Останавливаем системный звук
                val systemRingtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                if (systemRingtone != null) {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Проверяем системный звук: isPlaying = ${systemRingtone.isPlaying}")
                    if (systemRingtone.isPlaying) {
                        systemRingtone.stop()
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Системный звук остановлен глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Системный звук остановлен глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    } else {
                        android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Системный звук не играл глобально")
                        
                        // ✅ ДОБАВЛЕНО: Логирование в приложение
                        try {
                            val mainActivity = context as? com.medicalnotes.app.MainActivity
                            mainActivity?.addLog("🔇 NotificationManager: Системный звук не играл глобально")
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                        }
                    }
                } else {
                    android.util.Log.d("🔇 ГЛОБАЛЬНЫЙ_ЗВУК", "Системный звук недоступен")
                    
                    // ✅ ДОБАВЛЕНО: Логирование в приложение
                    try {
                        val mainActivity = context as? com.medicalnotes.app.MainActivity
                        mainActivity?.addLog("🔇 NotificationManager: Системный звук недоступен")
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
                    }
                }
                
                // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                    val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                    android.util.Log.d("🔇 AUDIO_MANAGER", "Оригинальная громкость уведомлений: $originalVolume")
                    
                    // Временно отключаем звук уведомлений
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                    android.util.Log.d("🔇 AUDIO_MANAGER", "Звук уведомлений временно отключен")
                    
                    // Восстанавливаем через 100мс
                    Handler(Looper.getMainLooper()).postDelayed({
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                        android.util.Log.d("🔇 AUDIO_MANAGER", "Громкость уведомлений восстановлена: $originalVolume")
                    }, 100)
                    
                } catch (e: Exception) {
                    android.util.Log.e("🔇 AUDIO_MANAGER", "Ошибка управления звуком", e)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Ошибка глобальной остановки звуков", e)
            }
            
            android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Глобальная остановка завершена")
            android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Время завершения: ${System.currentTimeMillis()}")
            android.util.Log.d("🔇 ГЛОБАЛЬНАЯ_ОСТАНОВКА", "Функция stopVibration() ЗАВЕРШЕНА")
            
            // ✅ ДОБАВЛЕНО: Логирование в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("🔇 NotificationManager: stopVibration() ЗАВЕРШЕНА")
                mainActivity?.addLog("🔇 NotificationManager: Время завершения: ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка остановки вибрации", e)
            
            // ✅ ДОБАВЛЕНО: Логирование ошибки в приложение
            try {
                val mainActivity = context as? com.medicalnotes.app.MainActivity
                mainActivity?.addLog("❌ NotificationManager: Ошибка остановки вибрации")
                mainActivity?.addLog("❌ NotificationManager: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка логирования в приложение", e2)
            }
        }
    }

    fun stopAllAlarms() {
        // Останавливаем вибрацию
        stopVibration()
        
        // Отменяем все уведомления
        cancelAllNotifications()
    }

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ УВЕДОМЛЕНИЙ С КАРТОЧКАМИ =====

    /**
     * Создает уведомление с карточкой лекарства и кнопками действий
     */
    fun showMedicineCardNotification(medicine: Medicine) {
        try {
            android.util.Log.d("NotificationManager", "Создание уведомления с карточкой для: ${medicine.name}")
            
            // Создаем кастомный layout для уведомления
            val customLayout = createMedicineCardLayout(medicine)
            
            // Создаем PendingIntent для кнопки "Выпил"
            val takenIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_MEDICINE_TAKEN"
                putExtra("medicine_id", medicine.id)
                putExtra("action", "taken")
            }
            val takenPendingIntent = PendingIntent.getBroadcast(
                context,
                (medicine.id * 1000 + 1).toInt(),
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Создаем PendingIntent для кнопки "Пропустить"
            val skipIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_MEDICINE_SKIPPED"
                putExtra("medicine_id", medicine.id)
                putExtra("action", "skipped")
            }
            val skipPendingIntent = PendingIntent.getBroadcast(
                context,
                (medicine.id * 1000 + 2).toInt(),
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Создаем PendingIntent для открытия приложения
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                (medicine.id * 1000 + 3).toInt(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Создаем уведомление
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE_CARD)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle("Время принять лекарство!")
                .setContentText("${medicine.name} - ${medicine.dosage}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${medicine.name}\nДозировка: ${medicine.dosage}\nВремя: ${medicine.time}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOngoing(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.ic_medicine, "✅ Выпил", takenPendingIntent)
                .addAction(R.drawable.ic_medicine, "⏭ Пропустить", skipPendingIntent)
                .setCustomBigContentView(customLayout)
                .build()
            
            // Показываем уведомление
            val notificationId = (NOTIFICATION_ID_MEDICINE_CARD + medicine.id).toInt()
            notificationManager.notify(notificationId, notification)
            
            // Запускаем звук и вибрацию
            try {
                // Вибрация
                if (vibrator.hasVibrator()) {
                    val vibrationPattern = longArrayOf(0, 1000, 300, 1000, 300, 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(vibrationPattern, 0)
                    }
                }
                
                // Звук
                val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (ringtone != null) {
                    activeRingtones[medicine.id] = ringtone
                    ringtone.play()
                    
                    // Останавливаем звук через 3 секунды
                    handler.postDelayed({
                        try {
                            if (ringtone.isPlaying) {
                                ringtone.stop()
                            }
                            activeRingtones.remove(medicine.id)
                        } catch (e: Exception) {
                            android.util.Log.e("NotificationManager", "Ошибка остановки звука", e)
                        }
                    }, 3000)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationManager", "Ошибка запуска звука/вибрации", e)
            }
            
            android.util.Log.d("NotificationManager", "Уведомление с карточкой создано для: ${medicine.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка создания уведомления с карточкой", e)
        }
    }

    /**
     * Создает кастомный layout для карточки лекарства с учетом попыток
     */
    private fun createMedicineCardLayoutWithRetry(medicine: Medicine, attempt: NotificationAttempt): android.widget.RemoteViews {
        val remoteViews = android.widget.RemoteViews(context.packageName, R.layout.notification_medicine_card)
        
        // Заполняем данные лекарства с учетом попыток
        val attemptText = if (attempt.attemptCount > 1) {
            " (Попытка ${attempt.attemptCount})"
        } else {
            ""
        }
        
        val urgencyText = when {
            attempt.attemptCount >= 4 -> "КРИТИЧЕСКИ ВАЖНО!"
            attempt.attemptCount >= 2 -> "СРОЧНО!"
            else -> "Время принять лекарство!"
        }
        
        remoteViews.setTextViewText(R.id.textMedicineName, "${medicine.name}$attemptText")
        remoteViews.setTextViewText(R.id.textDosage, "Дозировка: ${medicine.dosage}")
        remoteViews.setTextViewText(R.id.textTime, "Время: ${medicine.time}")
        
        // Настраиваем кнопки (через PendingIntent)
        val takenIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "ACTION_MEDICINE_TAKEN"
            putExtra("medicine_id", medicine.id)
            putExtra("action", "taken")
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id * 1000 + 1).toInt(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val skipIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "ACTION_MEDICINE_SKIPPED"
            putExtra("medicine_id", medicine.id)
            putExtra("action", "skipped")
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id * 1000 + 2).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Привязываем PendingIntent к кнопкам
        remoteViews.setOnClickPendingIntent(R.id.buttonTaken, takenPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.buttonSkip, skipPendingIntent)
        
        return remoteViews
    }

    /**
     * Создает кастомный layout для карточки лекарства
     */
    private fun createMedicineCardLayout(medicine: Medicine): android.widget.RemoteViews {
        val remoteViews = android.widget.RemoteViews(context.packageName, R.layout.notification_medicine_card)
        
        // Заполняем данные лекарства
        remoteViews.setTextViewText(R.id.textMedicineName, medicine.name)
        remoteViews.setTextViewText(R.id.textDosage, "Дозировка: ${medicine.dosage}")
        remoteViews.setTextViewText(R.id.textTime, "Время: ${medicine.time}")
        
        // Настраиваем кнопки (через PendingIntent)
        val takenIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "ACTION_MEDICINE_TAKEN"
            putExtra("medicine_id", medicine.id)
            putExtra("action", "taken")
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id * 1000 + 1).toInt(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val skipIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "ACTION_MEDICINE_SKIPPED"
            putExtra("medicine_id", medicine.id)
            putExtra("action", "skipped")
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (medicine.id * 1000 + 2).toInt(),
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Привязываем PendingIntent к кнопкам
        remoteViews.setOnClickPendingIntent(R.id.buttonTaken, takenPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.buttonSkip, skipPendingIntent)
        
        return remoteViews
    }

    /**
     * Отменяет уведомление с карточкой для конкретного лекарства
     */
    fun cancelMedicineCardNotification(medicineId: Long) {
        val notificationId = (NOTIFICATION_ID_MEDICINE_CARD + medicineId).toInt()
        notificationManager.cancel(notificationId)
        android.util.Log.d("NotificationManager", "Уведомление с карточкой отменено для лекарства ID: $medicineId")
    }

    /**
     * Показывает краткое подтверждение приема лекарства
     */
    fun showMedicineTakenConfirmation(medicineId: Long) {
        try {
            val dataManager = DataManager(context)
            val medicine = dataManager.getMedicineById(medicineId)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle("✅ Лекарство принято")
                .setContentText("${medicine?.name ?: "Лекарство"} - отмечено как принятое")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(3000) // Автоматически исчезает через 3 секунды
                .build()
            
            val notificationId = (NOTIFICATION_ID_MEDICINE + medicineId).toInt()
            notificationManager.notify(notificationId, notification)
            
            android.util.Log.d("NotificationManager", "Подтверждение приема показано для лекарства ID: $medicineId")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка показа подтверждения приема", e)
        }
    }

    /**
     * Показывает краткое подтверждение пропуска лекарства
     */
    fun showMedicineSkippedConfirmation(medicineId: Long) {
        try {
            val dataManager = DataManager(context)
            val medicine = dataManager.getMedicineById(medicineId)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICINE)
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentTitle("⏭ Лекарство пропущено")
                .setContentText("${medicine?.name ?: "Лекарство"} - пропущено")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(3000) // Автоматически исчезает через 3 секунды
                .build()
            
            val notificationId = (NOTIFICATION_ID_MEDICINE + medicineId).toInt()
            notificationManager.notify(notificationId, notification)
            
            android.util.Log.d("NotificationManager", "Подтверждение пропуска показано для лекарства ID: $medicineId")
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Ошибка показа подтверждения пропуска", e)
        }
    }
} 