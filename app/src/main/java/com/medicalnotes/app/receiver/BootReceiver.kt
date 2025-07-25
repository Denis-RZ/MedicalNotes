package com.medicalnotes.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import java.time.LocalTime
import java.util.*

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                android.util.Log.i("BootReceiver", "Boot completed, restoring medicine alarms")
                // Запускаем сервис уведомлений
                com.medicalnotes.app.service.NotificationService.startService(context)
                // Восстанавливаем будильники
                restoreMedicineAlarms(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                android.util.Log.i("BootReceiver", "Package replaced, restoring medicine alarms")
                // Запускаем сервис уведомлений
                com.medicalnotes.app.service.NotificationService.startService(context)
                // Восстанавливаем будильники
                restoreMedicineAlarms(context)
            }
        }
    }
    
    private fun restoreMedicineAlarms(context: Context) {
        val dataManager = DataManager(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val activeMedicines = dataManager.getActiveMedicines()
        
        activeMedicines.forEach { medicine ->
            scheduleMedicineAlarm(context, alarmManager, medicine)
        }
    }
    
    private fun scheduleMedicineAlarm(context: Context, alarmManager: AlarmManager, medicine: Medicine) {
        val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            action = "com.medicalnotes.app.MEDICINE_REMINDER"
            putExtra("medicine_id", medicine.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
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
        
        // Устанавливаем повторяющийся будильник
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
} 