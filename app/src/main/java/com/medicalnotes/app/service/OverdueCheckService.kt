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
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.utils.MedicineStatusHelper
import com.medicalnotes.app.utils.NotificationManager
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class OverdueCheckService : Service() {
    
    companion object {
        const val CHANNEL_ID = "overdue_check_service"
        const val NOTIFICATION_ID = 2000
        private const val CHECK_INTERVAL = 10000L // 10 секунд
        
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
    }
    
    private lateinit var dataManager: DataManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var handler: Handler
    private lateinit var checkRunnable: Runnable
    
    private var hasOverdueMedicines = false
    private var originalNotificationVolume = 0
    private var originalVibrationEnabled = true
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("OverdueCheckService", "Сервис проверки просроченных лекарств создан")
        
        dataManager = DataManager(this)
        notificationManager = NotificationManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        handler = Handler(Looper.getMainLooper())
        
        createNotificationChannel()
        startForeground()
        
        // Сохраняем оригинальные настройки
        originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        originalVibrationEnabled = vibrator.hasVibrator()
        
        android.util.Log.d("OverdueCheckService", "Оригинальная громкость уведомлений: $originalNotificationVolume")
        android.util.Log.d("OverdueCheckService", "Вибрация включена: $originalVibrationEnabled")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverdueCheckService", "Сервис проверки просроченных лекарств запущен")
        
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
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Проверка просроченных лекарств",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Фоновая служба для проверки просроченных лекарств"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.createNotificationChannel(channel)
                android.util.Log.d("OverdueCheckService", "Канал уведомлений создан")
                
            } catch (e: Exception) {
                android.util.Log.e("OverdueCheckService", "Ошибка создания канала уведомлений", e)
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
                checkOverdueMedicines()
                handler.postDelayed(this, CHECK_INTERVAL)
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
            android.util.Log.d("OverdueCheckService", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
            
            val allMedicines = dataManager.getActiveMedicines()
            val today = LocalDate.now()
            val currentTime = LocalTime.now()
            
            var foundOverdue = false
            var overdueCount = 0
            
            for (medicine in allMedicines) {
                if (medicine.isActive && medicine.remainingQuantity > 0) {
                    val status = MedicineStatusHelper.getMedicineStatus(medicine)
                    
                    if (status == MedicineStatus.OVERDUE) {
                        foundOverdue = true
                        overdueCount++
                        
                        android.util.Log.d("OverdueCheckService", "НАЙДЕНО ПРОСРОЧЕННОЕ ЛЕКАРСТВО: ${medicine.name}")
                        android.util.Log.d("OverdueCheckService", "  - Время приема: ${medicine.time}")
                        android.util.Log.d("OverdueCheckService", "  - Текущее время: $currentTime")
                        android.util.Log.d("OverdueCheckService", "  - Остаток: ${medicine.remainingQuantity}")
                    }
                }
            }
            
            android.util.Log.d("OverdueCheckService", "Всего просроченных лекарств: $overdueCount")
            
            // Если статус изменился, обновляем настройки
            if (foundOverdue != hasOverdueMedicines) {
                hasOverdueMedicines = foundOverdue
                
                if (foundOverdue) {
                    android.util.Log.d("OverdueCheckService", "🚨 ОБНАРУЖЕНЫ ПРОСРОЧЕННЫЕ ЛЕКАРСТВА - ОТКЛЮЧАЕМ ЗВУК И ВИБРАЦИЮ")
                    disableSoundAndVibration()
                } else {
                    android.util.Log.d("OverdueCheckService", "✅ ПРОСРОЧЕННЫХ ЛЕКАРСТВ НЕТ - ВОССТАНАВЛИВАЕМ ЗВУК И ВИБРАЦИЮ")
                    restoreOriginalSettings()
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка проверки просроченных лекарств", e)
        }
    }
    
    private fun disableSoundAndVibration() {
        try {
            android.util.Log.d("OverdueCheckService", "=== ОТКЛЮЧЕНИЕ ЗВУКА И ВИБРАЦИИ ===")
            
            // Отключаем звук уведомлений
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений отключен")
            
            // Отключаем звук медиа
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук медиа отключен")
            
            // Отключаем звук системы
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук системы отключен")
            
            // Отключаем звук звонка
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук звонка отключен")
            
            // ✅ УЛУЧШЕНО: Более агрессивная остановка вибрации
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
                android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (первая попытка)")
                
                // Дополнительная остановка через небольшие интервалы
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        vibrator.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (вторая попытка)")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка второй остановки вибрации", e)
                    }
                }, 100)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        vibrator.cancel()
                        android.util.Log.d("OverdueCheckService", "✓ Вибрация остановлена (третья попытка)")
                    } catch (e: Exception) {
                        android.util.Log.e("OverdueCheckService", "Ошибка третьей остановки вибрации", e)
                    }
                }, 500)
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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
            
            // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
            try {
                val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                
                // Временно отключаем звук
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                
                // Восстанавливаем через 2 секунды
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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
            
            android.util.Log.d("OverdueCheckService", "🚨 ЗВУК И ВИБРАЦИЯ ПОЛНОСТЬЮ ОТКЛЮЧЕНЫ")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка отключения звука и вибрации", e)
        }
    }
    
    private fun restoreOriginalSettings() {
        try {
            android.util.Log.d("OverdueCheckService", "=== ВОССТАНОВЛЕНИЕ ОРИГИНАЛЬНЫХ НАСТРОЕК ===")
            
            // Восстанавливаем звук уведомлений
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук уведомлений восстановлен: $originalNotificationVolume")
            
            // Восстанавливаем звук медиа (50% от максимума)
            val maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMediaVolume / 2, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук медиа восстановлен: ${maxMediaVolume / 2}")
            
            // Восстанавливаем звук системы (50% от максимума)
            val maxSystemVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxSystemVolume / 2, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук системы восстановлен: ${maxSystemVolume / 2}")
            
            // Восстанавливаем звук звонка (50% от максимума)
            val maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRingVolume / 2, 0)
            android.util.Log.d("OverdueCheckService", "✓ Звук звонка восстановлен: ${maxRingVolume / 2}")
            
            android.util.Log.d("OverdueCheckService", "✅ ОРИГИНАЛЬНЫЕ НАСТРОЙКИ ВОССТАНОВЛЕНЫ")
            
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Ошибка восстановления оригинальных настроек", e)
        }
    }

    private fun startVibration() {
        try {
            // ✅ ИСПРАВЛЕНО: Используем современный подход вместо deprecated VIBRATOR_SERVICE
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
            }
        } catch (e: Exception) {
            android.util.Log.e("OverdueCheckService", "Error starting vibration", e)
        }
    }
} 