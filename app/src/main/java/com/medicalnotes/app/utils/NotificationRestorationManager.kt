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
 * Утилита для проверки и восстановления уведомлений при запуске приложения
 */
class NotificationRestorationManager(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dataManager = DataManager(context)
    
    /**
     * Проверяет и восстанавливает все уведомления
     */
    fun checkAndRestoreNotifications() {
        try {
            android.util.Log.d("NotificationRestorationManager", "Начинаем проверку и восстановление уведомлений")
            
            // ИСПРАВЛЕНО: Используем ту же логику, что и "Лекарства на сегодня"
            val allMedicines = dataManager.loadMedicines()
            val today = java.time.LocalDate.now()
            val todayMedicines = com.medicalnotes.app.utils.DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
            
            android.util.Log.d("NotificationRestorationManager", "Всего лекарств в базе: ${allMedicines.size}")
            android.util.Log.d("NotificationRestorationManager", "Лекарств на сегодня (для восстановления): ${todayMedicines.size}")
            
            // Подробное логирование для отладки
            todayMedicines.forEach { medicine ->
                android.util.Log.d("NotificationRestorationManager", "Проверяем уведомление для: ${medicine.name}")
                android.util.Log.d("NotificationRestorationManager", "  - Время: ${medicine.time}")
                android.util.Log.d("NotificationRestorationManager", "  - Частота: ${medicine.frequency}")
                android.util.Log.d("NotificationRestorationManager", "  - Группа: ${medicine.groupName}")
                android.util.Log.d("NotificationRestorationManager", "  - Порядок в группе: ${medicine.groupOrder}")
            }
            
            todayMedicines.forEach { medicine ->
                if (isNotificationScheduled(medicine)) {
                    android.util.Log.d("NotificationRestorationManager", "Уведомление для ${medicine.name} уже запланировано")
                } else {
                    android.util.Log.d("NotificationRestorationManager", "Восстанавливаем уведомление для ${medicine.name}")
                    scheduleMedicineNotification(medicine)
                }
            }
            
            android.util.Log.d("NotificationRestorationManager", "Проверка и восстановление уведомлений завершено")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRestorationManager", "Ошибка проверки и восстановления уведомлений", e)
        }
    }
    
    /**
     * Проверяет, запланировано ли уведомление для лекарства
     */
    private fun isNotificationScheduled(medicine: Medicine): Boolean {
        try {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicine.id)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            return pendingIntent != null
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRestorationManager", "Ошибка проверки уведомления для ${medicine.name}", e)
            return false
        }
    }
    
    /**
     * Планирует уведомление для лекарства
     */
    private fun scheduleMedicineNotification(medicine: Medicine) {
        try {
            android.util.Log.d("NotificationRestorationManager", "Планирование уведомления для: ${medicine.name}")
            
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
                android.util.Log.d("NotificationRestorationManager", "Уведомление для ${medicine.name} сегодня в ${medicineTime}")
            } else {
                // Уведомление завтра
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
                calendar.set(Calendar.MINUTE, medicineTime.minute)
                calendar.set(Calendar.SECOND, 0)
                android.util.Log.d("NotificationRestorationManager", "Уведомление для ${medicine.name} завтра в ${medicineTime}")
            }
            
            // Устанавливаем точный будильник
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
            
            android.util.Log.d("NotificationRestorationManager", "Уведомление запланировано для ${medicine.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRestorationManager", "Ошибка планирования уведомления для ${medicine.name}", e)
        }
    }
    
    /**
     * Отменяет все уведомления
     */
    fun cancelAllNotifications() {
        try {
            android.util.Log.d("NotificationRestorationManager", "Отмена всех уведомлений")
            
            val activeMedicines = dataManager.getActiveMedicines()
            activeMedicines.forEach { medicine ->
                cancelMedicineNotification(medicine)
            }
            
            android.util.Log.d("NotificationRestorationManager", "Все уведомления отменены")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRestorationManager", "Ошибка отмены уведомлений", e)
        }
    }
    
    /**
     * Отменяет уведомление для конкретного лекарства
     */
    private fun cancelMedicineNotification(medicine: Medicine) {
        try {
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
            
            alarmManager.cancel(pendingIntent)
            android.util.Log.d("NotificationRestorationManager", "Уведомление отменено для ${medicine.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationRestorationManager", "Ошибка отмены уведомления для ${medicine.name}", e)
        }
    }
} 