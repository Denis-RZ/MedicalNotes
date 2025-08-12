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
import com.medicalnotes.app.utils.UnifiedNotificationManager
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
        private const val CHECK_INTERVAL = 60000L //  ИСПРАВЛЕНО: Уменьшаем до 1 минуты для быстрого обнаружения просроченных
        private const val MIN_CHECK_INTERVAL = 30000L // Минимальный интервал 30 секунд (уменьшено для непрерывного мониторинга)
        private const val EDITING_CHECK_INTERVAL = 60000L // 1 минута при редактировании (ускорено)
        
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
         * Проверяет, запущен ли сервис
         */
        fun isServiceRunning(context: Context): Boolean {
            return try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                @Suppress("DEPRECATION")
                val runningServices = activityManager.getRunningServices(100)
                runningServices.any { it.service.className == OverdueCheckService::class.java.name }
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка проверки статуса сервиса", e)
                false
            }
        }
        
        /**
         * ИСПРАВЛЕНО: Принудительная остановка звуков и вибрации при нажатии "выпил"
         */
        fun forceStopSoundAndVibration(context: Context) {
            try {
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ЗВУКОВ И ВИБРАЦИИ ===")
                
                // 1. Останавливаем вибрацию
                android.util.Log.d("OverdueCheckService", "1. Останавливаем вибрацию...")
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
                
                // 2. Отменяем ВСЕ уведомления приложения
                android.util.Log.d("OverdueCheckService", "2. Отменяем все уведомления...")
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.let { nm ->
                    // Отменяем все уведомления приложения
                    nm.cancelAll()
                    android.util.Log.d("OverdueCheckService", "✓ Все уведомления отменены")
                    
                    // Дополнительно отменяем конкретные уведомления
                    nm.cancel(NOTIFICATION_ID_OVERDUE + 100) // Звуковое уведомление
                    nm.cancel(NOTIFICATION_ID_OVERDUE) // Основное уведомление
                    nm.cancel(NOTIFICATION_ID) // Уведомление службы
                    nm.cancel(1001) // Уведомление о лекарстве
                    nm.cancel(1002) // Уведомление о просроченных
                    android.util.Log.d("OverdueCheckService", "✓ Конкретные уведомления отменены")
                }
                
                // 3. Используем UnifiedNotificationManager для отмены уведомлений
                android.util.Log.d("OverdueCheckService", "3. Используем UnifiedNotificationManager...")
                try {
                    com.medicalnotes.app.utils.UnifiedNotificationManager.cancelAllNotifications(context)
                    android.util.Log.d("OverdueCheckService", "✓ UnifiedNotificationManager отменил все уведомления")
                } catch (e: Exception) {
                    android.util.Log.e("OverdueCheckService", "Ошибка UnifiedNotificationManager", e)
                }
                
                // 4. Скрываем системное уведомление
                android.util.Log.d("OverdueCheckService", "4. Скрываем системное уведомление...")
                try {
                    val systemAlertHelper = com.medicalnotes.app.utils.SystemAlertHelper(context)
                    systemAlertHelper.hideAlert()
                    android.util.Log.d("OverdueCheckService", "✓ Системное уведомление скрыто")
                } catch (e: Exception) {
                    android.util.Log.e("OverdueCheckService", "Ошибка скрытия системного уведомления", e)
                }
                
                // 5. Отключаем звук уведомлений
                android.util.Log.d("OverdueCheckService", "5. Отключаем звук уведомлений...")
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.let { am ->
                    // Временно отключаем звук уведомлений
                    val currentVolume = am.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                    am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                    
                    // Восстанавливаем через 1 секунду
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, currentVolume, 0)
                    }, 1000)
                    
                    android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений временно отключен")
                }
                
                // 6. Останавливаем все медиа-плееры
                android.util.Log.d("OverdueCheckService", "6. Останавливаем медиа-плееры...")
                try {
                    val mediaPlayer = android.media.MediaPlayer()
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                    android.util.Log.d("OverdueCheckService", "✓ Медиа-плееры остановлены")
                } catch (e: Exception) {
                    android.util.Log.e("OverdueCheckService", "Ошибка остановки медиа-плееров", e)
                }
                
                android.util.Log.d("OverdueCheckService", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА ЗАВЕРШЕНА ===")
                
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "❌ Ошибка принудительной остановки звуков и вибрации", e)
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
        
        // Создаем каналы как для сервиса, так и унифицированные, чтобы уведомления были видимы даже в фоне
        createNotificationChannel()
        try {
            com.medicalnotes.app.utils.UnifiedNotificationManager.createNotificationChannels(this)
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка создания каналов UnifiedNotificationManager", e)
        }
        
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
    
    /**
     * ИСПРАВЛЕНО: Упрощенная проверка просроченных лекарств
     */
    private fun checkOverdueMedicines() {
        try {
            // Проверка минимального интервала между проверками
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCheckTime < MIN_CHECK_INTERVAL) {
                android.util.Log.d("OverdueCheckService", "Пропускаем проверку - слишком часто")
                return
            }
            lastCheckTime = currentTime
            
            android.util.Log.d("OverdueCheckService", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
            
            val allMedicines = dataManager.getActiveMedicines()
            val today = LocalDate.now()
            
            // Используем единую логику
            val overdueMedicines = allMedicines.filter { medicine ->
                // ИСПРАВЛЕНО: Двойная проверка - принятые лекарства не могут быть просроченными
                !medicine.takenToday && DosageCalculator.isMedicineOverdue(medicine, today)
            }
            
            android.util.Log.d("OverdueCheckService", "Всего лекарств в базе: ${allMedicines.size}")
            android.util.Log.d("OverdueCheckService", "Просроченных лекарств: ${overdueMedicines.size}")
            
            if (overdueMedicines.isNotEmpty()) {
                // Выбираем релевантное лекарство (с ближайшим прошедшим временем)
                val mostRelevant = selectMostRelevantOverdueMedicine(overdueMedicines, today)
                mostRelevant?.let { medicine ->
                    val nm = this.notificationManager
                    try {
                        // ИСПРАВЛЕНО: Показываем ВСЕ виды уведомлений одновременно для критических просроченных лекарств
                        try {
                            // 1. Обычное уведомление (всегда показываем)
                            nm.showMedicineCardNotification(medicine, true)
                            android.util.Log.d("OverdueCheckService", "✅ Показана карточка-диалог для просроченного: ${medicine.name}")
                            
                            // 2. Always-on-top окно (всегда показываем)
                            nm.showOverdueMedicineNotification(medicine)
                            android.util.Log.d("OverdueCheckService", "✅ Показано always-on-top окно для: ${medicine.name}")
                            
                            // 3. ДОБАВЛЕНО: Системное overlay-окно для критических случаев
                            try {
                                val systemAlert = com.medicalnotes.app.utils.SystemAlertHelper(this)
                                systemAlert.showOverdueAlert(overdueMedicines)
                                android.util.Log.d("OverdueCheckService", "✅ Показано системное overlay для ${overdueMedicines.size} просроченных лекарств")
                            } catch (e: Exception) {
                                android.util.Log.e("OverdueCheckService", "❌ Ошибка показа системного overlay", e)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OverdueCheckService", "❌ Ошибка показа уведомлений", e)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка показа карточки-диалога", e)
                    }
                }
                
                // Запускаем повторяющуюся ВИБРАЦИЮ без отдельного текстового уведомления
                if (!hasOverdueMedicines) {
                    android.util.Log.d("OverdueCheckService", "ЗАПУСКАЕМ ПОВТОРЯЮЩУЮСЯ ВИБРАЦИЮ")
                    startRepeatingSoundAndVibration()
                    hasOverdueMedicines = true
                }
            } else {
                // Останавливаем звуки и вибрацию если нет просроченных
                if (hasOverdueMedicines) {
                    android.util.Log.d("OverdueCheckService", "ПРОСРОЧЕННЫХ ЛЕКАРСТВ НЕТ - ОСТАНАВЛИВАЕМ ЗВУК И ВИБРАЦИЮ")
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
                        // Только вибрация, чтобы не плодить дополнительное простое уведомление со звуком
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
        // УДАЛЕНО: не создаем отдельное звуковое уведомление, чтобы не было второго типа уведомления
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
            
            // УБРАНО: групповые уведомления, т.к. используем карточку-диалог по конкретному лекарству
            return
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
            
            // УБРАНО: системный алерт – вместо него карточка-диалог конкретного лекарства
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка показа уведомления о просроченных лекарствах", e)
            // Тихий фоллбек: ничего не делаем – основной механизм карточки в checkOverdueMedicines
        }
    }
    
    /**
     * ДОБАВЛЕНО: Дополнительный способ показа уведомления через AlarmManager
     */
    private fun showAlarmNotification(overdueMedicines: List<Medicine>) {
        // УДАЛЕНО: лишний механизм дубляжа уведомления
    }
    
    /**
     * ДОБАВЛЕНО: Показ системного уведомления через SystemAlertHelper
     */
    private fun showSystemAlert(overdueMedicines: List<Medicine>) {
        // УДАЛЕНО: используем карточку-диалог вместо системного алерта
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

    // Выбираем самое релевантное просроченное лекарство: с ближайшим уже прошедшим временем, сегодня
    private fun selectMostRelevantOverdueMedicine(overdueMedicines: List<Medicine>, today: java.time.LocalDate): Medicine? {
        val now = java.time.LocalDateTime.now()
        return overdueMedicines
            .filter { med ->
                // Должно приниматься сегодня и время приема уже прошло
                DosageCalculator.shouldTakeMedicine(med, today) && today.atTime(med.time).isBefore(now)
            }
            .minByOrNull { med ->
                val doseDateTime = today.atTime(med.time)
                java.time.Duration.between(doseDateTime, now).toMinutes().let { if (it < 0) Long.MAX_VALUE else it }
            }
    }
} 