package com.medicalnotes.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.receiver.MedicineAlarmReceiver
import java.time.LocalTime
import java.util.*

/**
 * Утилита для автоматического планирования уведомлений о лекарствах
 */
class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Планирует уведомление для конкретного лекарства
     */
    fun scheduleMedicineNotification(medicine: Medicine) {
        try {
            android.util.Log.d("NotificationScheduler", "Планирование уведомления для: ${medicine.name}")
            
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
                android.util.Log.d("NotificationScheduler", "Уведомление для ${medicine.name} сегодня в ${medicineTime}")
            } else {
                // Уведомление завтра
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
                calendar.set(Calendar.MINUTE, medicineTime.minute)
                calendar.set(Calendar.SECOND, 0)
                android.util.Log.d("NotificationScheduler", "Уведомление для ${medicine.name} завтра в ${medicineTime}")
            }
            
            // Устанавливаем повторяющийся будильник
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                android.util.Log.d("NotificationScheduler", "Повторяющийся будильник установлен (setRepeating) для ${medicine.name}")
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                android.util.Log.d("NotificationScheduler", "Повторяющийся будильник установлен (setRepeating) для ${medicine.name}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка планирования уведомления для ${medicine.name}", e)
        }
    }
    
    /**
     * Отменяет уведомление для конкретного лекарства
     */
    fun cancelMedicineNotification(medicineId: Long) {
        try {
            android.util.Log.d("NotificationScheduler", "Отмена уведомления для лекарства ID: $medicineId")
            
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicineId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            android.util.Log.d("NotificationScheduler", "Уведомление отменено для лекарства ID: $medicineId")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка отмены уведомления для лекарства ID: $medicineId", e)
        }
    }
    
    /**
     * Планирует уведомления для всех активных лекарств
     */
    fun scheduleAllMedicineNotifications() {
        try {
            android.util.Log.d("NotificationScheduler", "Планирование уведомлений для всех лекарств")
            
            val dataManager = DataManager(context)
            val activeMedicines = dataManager.getActiveMedicines()
            
            android.util.Log.d("NotificationScheduler", "Найдено активных лекарств: ${activeMedicines.size}")
            
            activeMedicines.forEach { medicine ->
                scheduleMedicineNotification(medicine)
            }
            
            android.util.Log.d("NotificationScheduler", "Все уведомления запланированы")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка планирования всех уведомлений", e)
        }
    }
    
    /**
     * Отменяет все уведомления
     */
    fun cancelAllMedicineNotifications() {
        try {
            android.util.Log.d("NotificationScheduler", "Отмена всех уведомлений")
            
            val dataManager = DataManager(context)
            val activeMedicines = dataManager.getActiveMedicines()
            
            activeMedicines.forEach { medicine ->
                cancelMedicineNotification(medicine.id)
            }
            
            android.util.Log.d("NotificationScheduler", "Все уведомления отменены")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка отмены всех уведомлений", e)
        }
    }
    
    /**
     * Обновляет уведомление для лекарства (отменяет старое и создает новое)
     */
    fun updateMedicineNotification(medicine: Medicine) {
        try {
            android.util.Log.d("NotificationScheduler", "Обновление уведомления для: ${medicine.name}")
            
            // Отменяем старое уведомление
            cancelMedicineNotification(medicine.id)
            
            // Создаем новое уведомление
            scheduleMedicineNotification(medicine)
            
            android.util.Log.d("NotificationScheduler", "Уведомление обновлено для: ${medicine.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка обновления уведомления для ${medicine.name}", e)
        }
    }
} 