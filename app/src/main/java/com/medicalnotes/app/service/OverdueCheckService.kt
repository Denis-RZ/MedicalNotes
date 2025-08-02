package com.medicalnotes.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.medicalnotes.app.MainActivity
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.utils.MedicineStatusHelper
import com.medicalnotes.app.utils.NotificationManager
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class OverdueCheckService : Service() {
    
    companion object {
        const val CHANNEL_ID = "overdue_check_service"
        const val CHANNEL_ID_OVERDUE = "overdue_medicines"
        const val NOTIFICATION_ID = 2000
        const val NOTIFICATION_ID_OVERDUE = 2001
        private const val CHECK_INTERVAL = 300000L //  ИСПРАВЛЕНО: Увеличиваем до 5 минут (было 60 секунд)
        private const val MIN_CHECK_INTERVAL = 60000L // Минимальный интервал 1 минута (было 30 секунд)
        private const val EDITING_CHECK_INTERVAL = 120000L // 2 минуты при редактировании
        
        //  ДОБАВЛЕНО: Флаг для отслеживания активности редактирования
        private val isEditingActive = AtomicBoolean(false)
        
        fun startService(context: Context) {
            val intent = Intent(context, OverdueCheckService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, OverdueCheckService::class.java)
            context.stopService(intent)
        }
        
        //  ДОБАВЛЕНО: Методы для управления состоянием редактирования
        fun setEditingActive(active: Boolean) {
            isEditingActive.set(active)
            android.util.Log.d("OverdueCheckService", "Редактирование ${if (active) "активировано" else "деактивировано"}")
        }
        
        fun isCurrentlyEditing(): Boolean {
            return isEditingActive.get()
        }
        
        /**
         * ДОБАВЛЕНО: Принудительная остановка вибрации из OverdueCheckService
         */
        fun forceStopVibration(context: Context) {
            try {
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ВИБРАЦИИ ИЗ СЛУЖБЫ ===")
                
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.os.VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (vibrator.hasVibrator()) {
                    vibrator.cancel()
                    android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена из службы")
                }
                
                // Дополнительная остановка через системный сервис
                val systemVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                systemVibrator?.let { sysVib ->
                    if (sysVib.hasVibrator()) {
                        sysVib.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Системная вибрация остановлена из службы")
                    }
                }
                
                // Отменяем уведомления службы
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.let { nm ->
                    nm.cancel(NOTIFICATION_ID)
                    nm.cancel(NOTIFICATION_ID_OVERDUE)
                    android.util.Log.d("OverdueCheckService", "✓ Уведомления службы отменены")
                }
                
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ЗАВЕРШЕНА ===")
                
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка принудительной остановки вибрации", e)
            }
        }
    }
    
    private lateinit var dataManager: DataManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var handler: Handler
    private lateinit var checkRunnable: Runnable
    
    private var hasOverdueMedicines = false
    private var originalNotificationVolume = 0
    private var originalMediaVolume = 0
    private var originalSystemVolume = 0
    private var originalRingVolume = 0
    private var originalVibrationEnabled = true
    private var lastCheckTime = 0L //  ДОБАВЛЕНО: Отслеживание времени последней проверки
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("OverdueCheckService", " Запуск сервиса проверки просроченных лекарств")
        
        dataManager = DataManager(this)
        notificationManager = NotificationManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        handler = Handler(Looper.getMainLooper())
        
        createNotificationChannel()
        
        // Сохраняем оригинальные настройки
        originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        originalMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        originalRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        originalVibrationEnabled = vibrator.hasVibrator()
        
        android.util.Log.d("OverdueCheckService", " Сервис проверки просроченных лекарств запущен")
        android.util.Log.d("OverdueCheckService", "Оригинальная громкость уведомлений: $originalNotificationVolume")
        android.util.Log.d("OverdueCheckService", "Оригинальная громкость медиа: $originalMediaVolume")
        android.util.Log.d("OverdueCheckService", "Оригинальная громкость системы: $originalSystemVolume")
        android.util.Log.d("OverdueCheckService", "Оригинальная громкость звонка: $originalRingVolume")
        android.util.Log.d("OverdueCheckService", "Вибрация включена: $originalVibrationEnabled")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverdueCheckService", "Сервис проверки просроченных лекарств запущен")
        
        //  ИСПРАВЛЕНО: Немедленно запускаем foreground сервис в onStartCommand
        startForeground()
        
        // Запускаем периодическую проверку
        startPeriodicCheck()
        
        // Возвращаем START_STICKY для автоматического перезапуска
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("OverdueCheckService", "Сервис проверки просроченных лекарств остановлен")
        
        // Останавливаем периодическую проверку
        stopPeriodicCheck()
        
        // Восстанавливаем оригинальные настройки
        restoreOriginalSettings()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Канал для фонового сервиса
                val serviceChannel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Проверка просроченных лекарств",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Фоновая служба для проверки просроченных лекарств"
                    setShowBadge(false)
                }
                
                // ДОБАВЛЕНО: Канал для уведомлений о просроченных лекарствах
                val overdueChannel = android.app.NotificationChannel(
                    CHANNEL_ID_OVERDUE,
                    "Просроченные лекарства",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о просроченных лекарствах"
                    enableVibration(true)
                    enableLights(true)
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.createNotificationChannel(serviceChannel)
                notificationManager.createNotificationChannel(overdueChannel)
                android.util.Log.d("OverdueCheckService", "Каналы уведомлений созданы")
                
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка создания каналов уведомлений", e)
            }
        }
    }
    
    private fun startForeground() {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Проверка лекарств")
                .setContentText("Мониторинг просроченных лекарств активен")
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
            
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d("OverdueCheckService", "Foreground сервис запущен")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка запуска foreground сервиса", e)
        }
    }
    
    private fun startPeriodicCheck() {
        checkRunnable = object : Runnable {
            override fun run() {
                //  ИСПРАВЛЕНО: Проверяем, не активно ли редактирование
                if (!isEditingActive.get()) {
                    checkOverdueMedicines()
                } else {
                    android.util.Log.d("OverdueCheckService", " Пропускаем проверку - активно редактирование")
                }
                
                //  ИСПРАВЛЕНО: Динамический интервал в зависимости от активности редактирования
                val interval = if (isEditingActive.get()) {
                    EDITING_CHECK_INTERVAL // 2 минуты при редактировании
                } else {
                    CHECK_INTERVAL // 5 минут в обычном режиме
                }
                
                handler.postDelayed(this, interval)
            }
        }
        
        handler.post(checkRunnable)
        android.util.Log.d("OverdueCheckService", "Периодическая проверка запущена (каждые ${CHECK_INTERVAL/1000} секунд)")
    }
    
    private fun stopPeriodicCheck() {
        handler.removeCallbacks(checkRunnable)
        android.util.Log.d("OverdueCheckService", "Периодическая проверка остановлена")
    }
    
    private fun checkOverdueMedicines() {
        try {
            //  ДОБАВЛЕНО: Проверка минимального интервала между проверками
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCheckTime < MIN_CHECK_INTERVAL) {
                android.util.Log.d("OverdueCheckService", " Пропускаем проверку - слишком часто")
                return
            }
            lastCheckTime = currentTime
            
            android.util.Log.d("OverdueCheckService", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
            
            //  ИСПРАВЛЕНО: Добавляем синхронизацию для предотвращения конфликтов
            synchronized(this) {
                val allMedicines = dataManager.getActiveMedicines()
                val today = LocalDate.now()
                val currentLocalTime = LocalTime.now()
                
                //  ИСПРАВЛЕНО: Используем DosageCalculator для правильной фильтрации лекарств на сегодня
                val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
                
                android.util.Log.d("OverdueCheckService", "Всего лекарств в базе: ${allMedicines.size}")
                android.util.Log.d("OverdueCheckService", "Лекарств на сегодня: ${todayMedicines.size}")
                
                var foundOverdue = false
                var overdueCount = 0
                
                for (medicine in todayMedicines) {
                    if (medicine.remainingQuantity > 0) {
                        val status = MedicineStatusHelper.getMedicineStatus(medicine)
                        
                        android.util.Log.d("OverdueCheckService", "ПРОВЕРКА: ${medicine.name} - Статус: $status, Время: ${medicine.time}, Принято сегодня: ${medicine.takenToday}")
                        
                        if (status == MedicineStatus.OVERDUE) {
                            foundOverdue = true
                            overdueCount++
                            
                            android.util.Log.d("OverdueCheckService", "НАЙДЕНО ПРОСРОЧЕННОЕ ЛЕКАРСТВО: ${medicine.name}")
                            android.util.Log.d("OverdueCheckService", "  - Время приема: ${medicine.time}")
                            android.util.Log.d("OverdueCheckService", "  - Текущее время: $currentLocalTime")
                            android.util.Log.d("OverdueCheckService", "  - Остаток: ${medicine.remainingQuantity}")
                        }
                    }
                }
                
                android.util.Log.d("OverdueCheckService", "Всего просроченных лекарств: $overdueCount")
                
                // ДОБАВЛЕНО: Логирование статистики просроченных лекарств
                if (overdueCount > 0) {
                    logOverdueStatistics(todayMedicines.filter { 
                        MedicineStatusHelper.getMedicineStatus(it) == MedicineStatus.OVERDUE 
                    })
                }
                
                // Если статус изменился, обновляем настройки
                if (foundOverdue != hasOverdueMedicines) {
                    hasOverdueMedicines = foundOverdue
                    
                    if (foundOverdue) {
                        android.util.Log.d("OverdueCheckService", " ОБНАРУЖЕНЫ ПРОСРОЧЕННЫЕ ЛЕКАРСТВА - ОТКЛЮЧАЕМ ЗВУК И ВИБРАЦИЮ")
                        disableSoundAndVibration()
                        
                        // ДОБАВЛЕНО: Показываем уведомление о просроченных лекарствах
                        val overdueMedicines = todayMedicines.filter { 
                            MedicineStatusHelper.getMedicineStatus(it) == MedicineStatus.OVERDUE 
                        }
                        showOverdueNotification(overdueMedicines)
                    } else {
                        android.util.Log.d("OverdueCheckService", " ПРОСРОЧЕННЫХ ЛЕКАРСТВ НЕТ - ВОССТАНАВЛИВАЕМ ЗВУК И ВИБРАЦИЮ")
                        restoreOriginalSettings()
                        
                        // ДОБАВЛЕНО: Отменяем уведомление о просроченных лекарствах
                        cancelOverdueNotification()
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка проверки просроченных лекарств", e)
        }
    }
    
    private fun disableSoundAndVibration() {
        try {
            android.util.Log.d("OverdueCheckService", "=== ОТКЛЮЧЕНИЕ ЗВУКА И ВИБРАЦИИ ===")
            
            // ИСПРАВЛЕНО: Отключаем ТОЛЬКО звук уведомлений
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений отключен")
            
            // НЕ отключаем медиа, систему и звонки - оставляем для других приложений
            android.util.Log.d("OverdueCheckService", "✓ Медиа, система и звонки оставлены включенными")
            
            //  УЛУЧШЕНО: Более агрессивная остановка вибрации
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
                android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (первая попытка)")
                
                // Дополнительная остановка через небольшие интервалы
                handler.postDelayed({
                    try {
                        vibrator.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (вторая попытка)")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка второй остановки вибрации", e)
                    }
                }, 100)
                
                handler.postDelayed({
                    try {
                        vibrator.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (третья попытка)")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка третьей остановки вибрации", e)
                    }
                }, 500)
                
                handler.postDelayed({
                    try {
                        vibrator.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (четвертая попытка)")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка четвертой остановки вибрации", e)
                    }
                }, 1000)
            }
            
            // Принудительно останавливаем все активные уведомления
            notificationManager.cancelAllNotifications()
            android.util.Log.d("OverdueCheckService", "✓ Все уведомления отменены")
            
            //  ДОБАВЛЕНО: Принудительная остановка через AudioManager
            try {
                val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                
                // Временно отключаем звук
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                
                // Восстанавливаем через 2 секунды
                handler.postDelayed({
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                        android.util.Log.d("OverdueCheckService", "✓ Громкость восстановлена: $originalVolume")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка восстановления громкости", e)
                    }
                }, 2000)
                
                android.util.Log.d("OverdueCheckService", "✓ Принудительная остановка через AudioManager выполнена")
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка AudioManager", e)
            }
            
            android.util.Log.d("OverdueCheckService", " ЗВУК И ВИБРАЦИЯ ПОЛНОСТЬЮ ОТКЛЮЧЕНЫ")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка отключения звука и вибрации", e)
        }
    }
    
    private fun restoreOriginalSettings() {
        try {
            android.util.Log.d("OverdueCheckService", "=== ВОССТАНОВЛЕНИЕ ОРИГИНАЛЬНЫХ НАСТРОЕК ===")
            
            // ИСПРАВЛЕНО: Восстанавливаем оригинальные настройки
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений восстановлен: $originalNotificationVolume")
            
            // Восстанавливаем оригинальные настройки медиа, системы и звонка
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMediaVolume, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук медиа восстановлен: $originalMediaVolume")
            
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук системы восстановлен: $originalSystemVolume")
            
            audioManager.setStreamVolume(AudioManager.STREAM_RING, originalRingVolume, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук звонка восстановлен: $originalRingVolume")
            
            android.util.Log.d("OverdueCheckService", " ОРИГИНАЛЬНЫЕ НАСТРОЙКИ ВОССТАНОВЛЕНЫ")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка восстановления оригинальных настроек", e)
        }
    }

    private fun startVibration() {
        try {
            //  ИСПРАВЛЕНО: Используем современный подход вместо deprecated VIBRATOR_SERVICE
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000)
                }
                
                // ИСПРАВЛЕНО: Автоматическая остановка вибрации через 2 секунды
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        if (vibrator.hasVibrator()) {
                            vibrator.cancel()
                            android.util.Log.d("OverdueCheckService", "✓ Вибрация автоматически остановлена")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка автоматической остановки вибрации", e)
                    }
                }, 2000) // 2 секунды
            }
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Error starting vibration", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Показ уведомления о просроченных лекарствах
     */
    private fun showOverdueNotification(overdueMedicines: List<Medicine>) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("show_overdue_medicines", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val medicineNames = overdueMedicines.joinToString(", ") { it.name }
            val overdueCount = overdueMedicines.size
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
                .setContentTitle("Просроченные лекарства")
                .setContentText("У вас $overdueCount просроченных лекарств")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Просроченные лекарства: $medicineNames\n\nПожалуйста, примите их как можно скорее."))
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID_OVERDUE, notification)
            android.util.Log.d("OverdueCheckService", "✓ Уведомление о просроченных лекарствах показано")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка показа уведомления о просроченных лекарствах", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Отмена уведомления о просроченных лекарствах
     */
    private fun cancelOverdueNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_OVERDUE)
            android.util.Log.d("OverdueCheckService", "✓ Уведомление о просроченных лекарствах отменено")
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка отмены уведомления о просроченных лекарствах", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Логирование статистики просроченных лекарств
     */
    private fun logOverdueStatistics(overdueMedicines: List<Medicine>) {
        try {
            android.util.Log.d("OverdueCheckService", "=== СТАТИСТИКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
            android.util.Log.d("OverdueCheckService", "Количество просроченных: ${overdueMedicines.size}")
            
            overdueMedicines.forEach { medicine ->
                val overdueMinutes = java.time.temporal.ChronoUnit.MINUTES.between(medicine.time, java.time.LocalTime.now())
                android.util.Log.d("OverdueCheckService", "- ${medicine.name}: просрочено на ${overdueMinutes} минут")
                android.util.Log.d("OverdueCheckService", "  Время приема: ${medicine.time}, Текущее время: ${java.time.LocalTime.now()}")
                android.util.Log.d("OverdueCheckService", "  Остаток: ${medicine.remainingQuantity}, Принято сегодня: ${medicine.takenToday}")
            }
            
            android.util.Log.d("OverdueCheckService", "=== КОНЕЦ СТАТИСТИКИ ===")
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка логирования статистики", e)
        }
    }
} 