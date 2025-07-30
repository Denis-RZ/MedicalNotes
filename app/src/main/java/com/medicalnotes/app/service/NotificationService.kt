package com.medicalnotes.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.medicalnotes.app.MainActivity
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.receiver.MedicineAlarmReceiver
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.NotificationScheduler
import java.time.LocalTime
import java.util.*

class NotificationService : Service() {
    
    companion object {
        const val CHANNEL_ID = "medical_notes_service"
        const val NOTIFICATION_ID = 1000
        
        fun startService(context: Context) {
            val intent = Intent(context, NotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, NotificationService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var dataManager: DataManager
    private lateinit var alarmManager: AlarmManager
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("NotificationService", "Сервис создан")
        dataManager = DataManager(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("NotificationService", "Сервис запущен с действием: ${intent?.action}")
        
        when (intent?.action) {
            "START_FOREGROUND" -> startForeground()
            "SCHEDULE_MEDICINE" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    val medicine = dataManager.getMedicineById(medicineId)
                    medicine?.let { scheduleMedicineAlarm(it) }
                }
            }
            "CANCEL_MEDICINE" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    cancelMedicineAlarm(medicineId)
                }
            }
            "SCHEDULE_ALL" -> scheduleAllMedicines()
            "CHECK_AND_RESTORE" -> {
                startForeground()
                checkAndRestoreNotifications()
            }
            else -> {
                // Если сервис запущен без действия, запускаем в фоновом режиме
                startForeground()
                scheduleAllMedicines()
                
                // ✅ ДОБАВЛЕНО: Запускаем сервис проверки просроченных лекарств
                try {
                    com.medicalnotes.app.service.OverdueCheckService.startService(this)
                    android.util.Log.d("NotificationService", "Сервис проверки просроченных лекарств запущен")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationService", "Ошибка запуска сервиса проверки просроченных лекарств", e)
                }
            }
        }
        
        // Возвращаем START_STICKY для автоматического перезапуска
        return START_STICKY
    }
    
    private fun startForeground() {
        try {
            android.util.Log.d("NotificationService", "Запуск сервиса в foreground режиме")
            
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Medical Notes")
                .setContentText("Служба уведомлений активна")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
            
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d("NotificationService", "Foreground сервис запущен")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка запуска foreground сервиса", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Medical Notes Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Фоновая служба для уведомлений о лекарствах"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                android.util.Log.d("NotificationService", "Канал уведомлений создан")
                
            } catch (e: Exception) {
                android.util.Log.e("NotificationService", "Ошибка создания канала уведомлений", e)
            }
        }
    }
    
    private fun scheduleMedicineAlarm(medicine: Medicine) {
        try {
            android.util.Log.d("NotificationService", "Планирование уведомления для: ${medicine.name}")
            
            val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicine.id)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                medicine.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Вычисляем время следующего уведомления
            val now = LocalTime.now()
            val medicineTime = medicine.time
            
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            
            if (medicineTime.isAfter(now)) {
                // Уведомление сегодня
                calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
                calendar.set(Calendar.MINUTE, medicineTime.minute)
                calendar.set(Calendar.SECOND, 0)
                android.util.Log.d("NotificationService", "Уведомление для ${medicine.name} сегодня в ${medicineTime}")
            } else {
                // Уведомление завтра
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
                calendar.set(Calendar.MINUTE, medicineTime.minute)
                calendar.set(Calendar.SECOND, 0)
                android.util.Log.d("NotificationService", "Уведомление для ${medicine.name} завтра в ${medicineTime}")
            }
            
            // Устанавливаем ежедневный будильник
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("NotificationService", "Точный будильник установлен (setExactAndAllowWhileIdle) для ${medicine.name}")
                
                // Планируем следующий будильник через день
                scheduleNextDayAlarm(medicine, calendar.timeInMillis + AlarmManager.INTERVAL_DAY)
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("NotificationService", "Точный будильник установлен (setExact) для ${medicine.name}")
                
                // Планируем следующий будильник через день
                scheduleNextDayAlarm(medicine, calendar.timeInMillis + AlarmManager.INTERVAL_DAY)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка планирования уведомления для ${medicine.name}", e)
        }
    }
    
    private fun cancelMedicineAlarm(medicineId: Long) {
        try {
            android.util.Log.d("NotificationService", "Отмена уведомления для лекарства ID: $medicineId")
            
            val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicineId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                medicineId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            android.util.Log.d("NotificationService", "Уведомление отменено для лекарства ID: $medicineId")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка отмены уведомления для лекарства ID: $medicineId", e)
        }
    }
    
    private fun scheduleAllMedicines() {
        try {
            android.util.Log.d("NotificationService", "Планирование всех уведомлений")
            
            val activeMedicines = dataManager.getActiveMedicines()
            android.util.Log.d("NotificationService", "Найдено активных лекарств: ${activeMedicines.size}")
            
            activeMedicines.forEach { medicine ->
                scheduleMedicineAlarm(medicine)
            }
            
            android.util.Log.d("NotificationService", "Все уведомления запланированы")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка планирования всех уведомлений", e)
        }
    }
    
    private fun scheduleNextDayAlarm(medicine: Medicine, nextDayTime: Long) {
        try {
            android.util.Log.d("NotificationService", "Планирование следующего дня для: ${medicine.name}")
            
            val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicine.id)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                (medicine.id + 1000).toInt(), // Уникальный ID для следующего дня
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextDayTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextDayTime,
                    pendingIntent
                )
            }
            
            android.util.Log.d("NotificationService", "Следующий день запланирован для ${medicine.name} на ${Date(nextDayTime)}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка планирования следующего дня для ${medicine.name}", e)
        }
    }
    
    private fun checkAndRestoreNotifications() {
        try {
            android.util.Log.d("NotificationService", "=== ПРОВЕРКА И ВОССТАНОВЛЕНИЕ УВЕДОМЛЕНИЙ ===")
            
            val activeMedicines = dataManager.getActiveMedicines()
            android.util.Log.d("NotificationService", "Найдено активных лекарств: ${activeMedicines.size}")
            
            var restoredCount = 0
            
            activeMedicines.forEach { medicine ->
                // Проверяем, есть ли уже запланированное уведомление
                val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
                    action = "com.medicalnotes.app.MEDICINE_REMINDER"
                    putExtra("medicine_id", medicine.id)
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    medicine.id.toInt(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                
                if (pendingIntent == null) {
                    // Уведомление не запланировано, планируем его
                    android.util.Log.d("NotificationService", "Восстанавливаем уведомление для: ${medicine.name}")
                    scheduleMedicineAlarm(medicine)
                    restoredCount++
                } else {
                    android.util.Log.d("NotificationService", "Уведомление уже запланировано для: ${medicine.name}")
                }
            }
            
            android.util.Log.d("NotificationService", "Восстановлено уведомлений: $restoredCount")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка проверки и восстановления уведомлений", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("NotificationService", "Сервис уничтожен")
        
        try {
            // Отменяем все будильники при остановке сервиса
            val activeMedicines = dataManager.getActiveMedicines()
            activeMedicines.forEach { medicine ->
                cancelMedicineAlarm(medicine.id)
            }
            android.util.Log.d("NotificationService", "Все будильники отменены")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Ошибка при уничтожении сервиса", e)
        }
    }
} 