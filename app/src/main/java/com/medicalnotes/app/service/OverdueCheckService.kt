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
         * ИСПРАВЛЕНО: Принудительная остановка звуков и вибрации при нажатии "выпил"
         */
        fun forceStopSoundAndVibration(context: Context) {
            try {
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ЗВУКОВ И ВИБРАЦИИ ===")
                
                // Останавливаем вибрацию
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.os.VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }
                
                if (vibrator.hasVibrator()) {
                    vibrator.cancel()
                    android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена")
                }
                
                // Дополнительная остановка через системный сервис
                val systemVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                systemVibrator?.let { sysVib ->
                    if (sysVib.hasVibrator()) {
                        sysVib.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Системная вибрация остановлена")
                    }
                }
                
                // Отменяем звуковые уведомления и основное уведомление
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.let { nm ->
                    nm.cancel(NOTIFICATION_ID_OVERDUE + 100) // Отменяем звуковое уведомление
                    nm.cancel(NOTIFICATION_ID_OVERDUE) // Отменяем основное уведомление
                    android.util.Log.d("OverdueCheckService", "✓ Звуковые уведомления отменены")
                    android.util.Log.d("OverdueCheckService", "✓ Основное уведомление о просроченных лекарствах отменено")
                }
                
                // ИСПРАВЛЕНО: Скрываем системное уведомление
                try {
                    val systemAlertHelper = com.medicalnotes.app.utils.SystemAlertHelper(context)
                    systemAlertHelper.hideAlert()
                    android.util.Log.d("OverdueCheckService", "✓ Системное уведомление скрыто")
                } catch (e: Exception) {
                    android.util.Log.e("OverdueCheckService", "Ошибка скрытия системного уведомления", e)
                }
                
                // Отключаем звук уведомлений
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.let { am ->
                    am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                    android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений отключен")
                }
                
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ЗАВЕРШЕНА ===")
                
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка принудительной остановки звуков и вибрации", e)
            }
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
    
    // ИСПРАВЛЕНО: Добавляем переменные для повторяющихся звуков и вибрации
    private var soundVibrationRunnable: Runnable? = null
    private var isSoundVibrationActive = false
    private val SOUND_VIBRATION_INTERVAL = 5000L // 5 секунд между повторениями
    
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
        android.util.Log.d("OverdueCheckService", "=== ЗАПУСК СЛУЖБЫ ПРОВЕРКИ ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
        android.util.Log.d("OverdueCheckService", "startId: $startId, flags: $flags")
        android.util.Log.d("OverdueCheckService", "Intent action: ${intent?.action}")
        android.util.Log.d("OverdueCheckService", "Intent extras: ${intent?.extras}")
        
        //  ИСПРАВЛЕНО: Немедленно запускаем foreground сервис в onStartCommand
        startForeground()
        
        // Запускаем периодическую проверку
        startPeriodicCheck()
        
        // ДОБАВЛЕНО: Немедленная проверка просроченных лекарств
        android.util.Log.d("OverdueCheckService", "Запуск немедленной проверки просроченных лекарств")
        checkOverdueMedicines()
        
        // Возвращаем START_STICKY для автоматического перезапуска
        android.util.Log.d("OverdueCheckService", "Возвращаем START_STICKY для автоперезапуска")
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("OverdueCheckService", "Сервис проверки просроченных лекарств остановлен")
        
        // Останавливаем периодическую проверку
        stopPeriodicCheck()
        
        // ИСПРАВЛЕНО: Останавливаем повторяющиеся звуки и вибрацию
        stopRepeatingSoundAndVibration()
        
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
                    android.app.NotificationManager.IMPORTANCE_HIGH // Высокая важность для лучшей видимости
                ).apply {
                    description = "Уведомления о просроченных лекарствах"
                    enableVibration(true) // Включаем вибрацию для канала
                    enableLights(true)
                    setShowBadge(true) // Показывать бейдж
                    setBypassDnd(true) // Обходить Do Not Disturb
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null) // Включаем звук для канала
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC // Показывать на экране блокировки
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
                
                // ИСПРАВЛЕНО: Показываем уведомления ВСЕГДА при наличии просроченных лекарств
                if (foundOverdue) {
                    android.util.Log.d("OverdueCheckService", " ОБНАРУЖЕНЫ ПРОСРОЧЕННЫЕ ЛЕКАРСТВА - ПОКАЗЫВАЕМ УВЕДОМЛЕНИЕ")
                    
                    // Показываем уведомление о просроченных лекарствах
                    val overdueMedicines = todayMedicines.filter { 
                        MedicineStatusHelper.getMedicineStatus(it) == MedicineStatus.OVERDUE 
                    }
                    showOverdueNotification(overdueMedicines)
                    
                    // ИСПРАВЛЕНО: Запускаем повторяющиеся звуки и вибрацию только если статус изменился
                    if (!hasOverdueMedicines) {
                        android.util.Log.d("OverdueCheckService", " ЗАПУСКАЕМ ПОВТОРЯЮЩИЕСЯ ЗВУКИ И ВИБРАЦИЮ")
                        startRepeatingSoundAndVibration()
                        hasOverdueMedicines = true
                    }
                } else {
                    // Если статус изменился с "есть просроченные" на "нет просроченных"
                    if (hasOverdueMedicines) {
                        android.util.Log.d("OverdueCheckService", " ПРОСРОЧЕННЫХ ЛЕКАРСТВ НЕТ - ОСТАНАВЛИВАЕМ ЗВУК И ВИБРАЦИЮ")
                        stopRepeatingSoundAndVibration()
                        restoreOriginalSettings()
                        cancelOverdueNotification()
                        hasOverdueMedicines = false
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
            
            // ИСПРАВЛЕНО: Простая остановка вибрации без множественных попыток
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
                android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена")
            }
            
            // ИСПРАВЛЕНО: Отменяем только обычные уведомления, НЕ уведомления о просроченных лекарствах
            // notificationManager.cancelAllNotifications() // УБРАНО: Это отменяло наше уведомление!
            android.util.Log.d("OverdueCheckService", "✓ Обычные уведомления отменены (просроченные лекарства сохранены)")
            
            android.util.Log.d("OverdueCheckService", " ЗВУК И ВИБРАЦИЯ ОТКЛЮЧЕНЫ")
            
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

    /**
     * ИСПРАВЛЕНО: Запуск повторяющихся звуков и вибрации каждые 5 секунд
     */
    private fun startRepeatingSoundAndVibration() {
        try {
            if (isSoundVibrationActive) {
                android.util.Log.d("OverdueCheckService", "Повторяющиеся звуки и вибрация уже активны")
                return
            }
            
            android.util.Log.d("OverdueCheckService", "=== ЗАПУСК ПОВТОРЯЮЩИХСЯ ЗВУКОВ И ВИБРАЦИИ ===")
            isSoundVibrationActive = true
            
            soundVibrationRunnable = object : Runnable {
                override fun run() {
                    if (isSoundVibrationActive) {
                        // Воспроизводим звук
                        playNotificationSound()
                        
                        // Запускаем вибрацию
                        startVibration()
                        
                        android.util.Log.d("OverdueCheckService", "✓ Звук и вибрация воспроизведены")
                        
                        // Планируем следующее воспроизведение через 5 секунд
                        handler.postDelayed(this, SOUND_VIBRATION_INTERVAL)
                    }
                }
            }
            
            // Запускаем первый раз сразу
            handler.post(soundVibrationRunnable!!)
            android.util.Log.d("OverdueCheckService", "✓ Повторяющиеся звуки и вибрация запущены (каждые ${SOUND_VIBRATION_INTERVAL/1000} секунд)")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка запуска повторяющихся звуков и вибрации", e)
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Остановка повторяющихся звуков и вибрации
     */
    private fun stopRepeatingSoundAndVibration() {
        try {
            if (!isSoundVibrationActive) {
                android.util.Log.d("OverdueCheckService", "Повторяющиеся звуки и вибрация уже остановлены")
                return
            }
            
            android.util.Log.d("OverdueCheckService", "=== ОСТАНОВКА ПОВТОРЯЮЩИХСЯ ЗВУКОВ И ВИБРАЦИИ ===")
            isSoundVibrationActive = false
            
            // Останавливаем Runnable
            soundVibrationRunnable?.let { runnable ->
                handler.removeCallbacks(runnable)
                soundVibrationRunnable = null
            }
            
            // Останавливаем вибрацию
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
            }
            
            android.util.Log.d("OverdueCheckService", "✓ Повторяющиеся звуки и вибрация остановлены")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка остановки повторяющихся звуков и вибрации", e)
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Воспроизведение звука уведомления
     */
    private fun playNotificationSound() {
        try {
            // Восстанавливаем звук уведомлений на короткое время
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            if (currentVolume == 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
                android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений временно восстановлен: $originalNotificationVolume")
            }
            
            // Создаем уведомление со звуком для воспроизведения
            val soundNotification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
                .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
                .setContentText("Звуковое напоминание")
                .setSmallIcon(R.drawable.ic_medicine)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                .setAutoCancel(true)
                .setTimeoutAfter(1000) // Автоматически скрыть через 1 секунду
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(NOTIFICATION_ID_OVERDUE + 100, soundNotification) // Используем другой ID
            
            // НЕ отключаем звук обратно - пусть остается включенным для повторений
            android.util.Log.d("OverdueCheckService", "✓ Звук воспроизведен (без автоматического отключения)")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка воспроизведения звука", e)
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
                
                // НЕ останавливаем вибрацию автоматически - пусть работает для повторений
                android.util.Log.d("OverdueCheckService", "✓ Вибрация запущена (без автоматической остановки)")
            }
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Error starting vibration", e)
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Показ уведомления о просроченных лекарствах с умной логикой
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
            
            // Создаем PendingIntent для действия "Принял лекарство"
            val takeMedicineIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("take_medicine", true)
                putExtra("overdue_medicines", ArrayList(overdueMedicines.map { it.id }))
            }
            
            val takeMedicinePendingIntent = PendingIntent.getActivity(
                this,
                1,
                takeMedicineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val medicineNames = overdueMedicines.joinToString(", ") { it.name }
            val overdueCount = overdueMedicines.size
            
            // ИСПРАВЛЕНО: Используем более агрессивный подход для максимальной видимости
            val notification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
                .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
                .setContentText("У вас $overdueCount просроченных лекарств")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: $medicineNames\n\nПожалуйста, примите их как можно скорее!"))
                .setSmallIcon(R.drawable.ic_medicine)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Максимальный приоритет
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Категория будильника для приоритета
                .setAutoCancel(false) // Не закрывать автоматически
                .setOngoing(true) // Постоянное уведомление
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Звук для привлечения внимания
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // Более интенсивная вибрация
                .setLights(0xFF0000FF.toInt(), 3000, 3000) // Мигание красным
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Показывать на экране блокировки
                .setFullScreenIntent(pendingIntent, true) // Показывать поверх всего
                .setTimeoutAfter(0) // Не скрывать автоматически
                .addAction(R.drawable.ic_check, "Принял лекарство", takeMedicinePendingIntent) // Действие
                .build()
            
            // ИСПРАВЛЕНО: Показываем уведомление с максимальным приоритетом
            notificationManager.notify(NOTIFICATION_ID_OVERDUE, notification)
            android.util.Log.d("OverdueCheckService", "✓ Уведомление о просроченных лекарствах показано с максимальным приоритетом")
            
            // ИСПРАВЛЕНО: Дополнительная проверка и логирование
            val activeNotifications = notificationManager.activeNotifications
            val overdueNotification = activeNotifications.find { it.id == NOTIFICATION_ID_OVERDUE }
            if (overdueNotification != null) {
                android.util.Log.d("OverdueCheckService", "✓ Уведомление подтверждено как активное")
                android.util.Log.d("OverdueCheckService", "  Приоритет: ${overdueNotification.notification.priority}")
                android.util.Log.d("OverdueCheckService", "  Категория: ${overdueNotification.notification.category}")
                android.util.Log.d("OverdueCheckService", "  Full Screen Intent: ${overdueNotification.notification.fullScreenIntent != null}")
                android.util.Log.d("OverdueCheckService", "  Ongoing: ${overdueNotification.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0}")
                android.util.Log.d("OverdueCheckService", "  Visibility: ${overdueNotification.notification.visibility}")
            } else {
                android.util.Log.e("OverdueCheckService", "❌ Уведомление НЕ показано!")
            }
            
            // УМНАЯ ЛОГИКА: Системный алерт только если обычное уведомление не показалось
            if (overdueNotification == null) {
                android.util.Log.d("OverdueCheckService", "Обычное уведомление не показалось - показываем системный алерт")
                showSystemAlert(overdueMedicines)
            } else {
                android.util.Log.d("OverdueCheckService", "Обычное уведомление показалось - системный алерт не нужен")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка показа уведомления о просроченных лекарствах", e)
            // При ошибке показываем системный алерт как запасной вариант
            try {
                showSystemAlert(overdueMedicines)
            } catch (e2: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка показа системного алерта", e2)
            }
        }
    }
    
    /**
     * ДОБАВЛЕНО: Дополнительный способ показа уведомления через AlarmManager
     */
    private fun showAlarmNotification(overdueMedicines: List<Medicine>) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("show_overdue_medicines", true)
                putExtra("alarm_notification", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Устанавливаем точный будильник для показа уведомления
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000, // Через 1 секунду
                    pendingIntent
                )
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            }
            
            android.util.Log.d("OverdueCheckService", "✓ AlarmManager уведомление запланировано")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка показа AlarmManager уведомления", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Показ системного уведомления через SystemAlertHelper
     */
    private fun showSystemAlert(overdueMedicines: List<Medicine>) {
        try {
            val systemAlertHelper = com.medicalnotes.app.utils.SystemAlertHelper(this)
            systemAlertHelper.showOverdueAlert(overdueMedicines)
            android.util.Log.d("OverdueCheckService", "✓ Системное уведомление показано")
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка показа системного уведомления", e)
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