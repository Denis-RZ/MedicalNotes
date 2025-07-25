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
        dataManager = DataManager(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            else -> {
                // Если сервис запущен без действия, запускаем в фоновом режиме
                startForeground()
                scheduleAllMedicines()
            }
        }
        
        return START_STICKY
    }
    
    private fun startForeground() {
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
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medical Notes Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновая служба для уведомлений о лекарствах"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun scheduleMedicineAlarm(medicine: Medicine) {
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
        } else {
            // Уведомление завтра
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
            calendar.set(Calendar.MINUTE, medicineTime.minute)
            calendar.set(Calendar.SECOND, 0)
        }
        
        // Устанавливаем ежедневный будильник
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }
    
    private fun cancelMedicineAlarm(medicineId: Long) {
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
    }
    
    private fun scheduleAllMedicines() {
        val activeMedicines = dataManager.getActiveMedicines()
        activeMedicines.forEach { medicine ->
            scheduleMedicineAlarm(medicine)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Отменяем все будильники при остановке сервиса
        val activeMedicines = dataManager.getActiveMedicines()
        activeMedicines.forEach { medicine ->
            cancelMedicineAlarm(medicine.id)
        }
    }
} 