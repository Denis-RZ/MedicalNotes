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
            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    android.util.Log.i("BootReceiver", "Package replaced via data, restoring medicine alarms")
                    // Запускаем сервис уведомлений
                    com.medicalnotes.app.service.NotificationService.startService(context)
                    // Восстанавливаем будильники
                    restoreMedicineAlarms(context)
                }
            }
        }
    }
    
    private fun restoreMedicineAlarms(context: Context) {
        try {
            android.util.Log.d("BootReceiver", "Начинаем восстановление уведомлений о лекарствах")
            
            //  ИСПРАВЛЕНО: Используем ту же логику, что и "Лекарства на сегодня"
            val dataManager = DataManager(context)
            val allMedicines = dataManager.loadMedicines()
            val today = java.time.LocalDate.now()
            val todayMedicines = com.medicalnotes.app.utils.DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
            
            android.util.Log.d("BootReceiver", "Всего лекарств в базе: ${allMedicines.size}")
            android.util.Log.d("BootReceiver", "Лекарств на сегодня (для восстановления): ${todayMedicines.size}")
            
            // Подробное логирование для отладки
            todayMedicines.forEach { medicine ->
                android.util.Log.d("BootReceiver", "Восстанавливаем уведомление для: ${medicine.name}")
                android.util.Log.d("BootReceiver", "  - Время: ${medicine.time}")
                android.util.Log.d("BootReceiver", "  - Частота: ${medicine.frequency}")
                android.util.Log.d("BootReceiver", "  - Группа: ${medicine.groupName}")
                android.util.Log.d("BootReceiver", "  - Порядок в группе: ${medicine.groupOrder}")
            }
            
            //  ИЗМЕНЕНО: Используем новую утилиту для восстановления уведомлений
            val restorationManager = com.medicalnotes.app.utils.NotificationRestorationManager(context)
            restorationManager.checkAndRestoreNotifications()
            
            // Перепланировать все будильники карточек
            try { 
                com.medicalnotes.app.utils.NotificationScheduler(context).scheduleAll() 
                android.util.Log.d("BootReceiver", "Карточки лекарств перепланированы")
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Ошибка перепланирования карточек", e)
            }
            
            android.util.Log.d("BootReceiver", "Восстановление уведомлений завершено")
            
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Ошибка восстановления уведомлений", e)
        }
    }
    
    private fun scheduleMedicineAlarm(context: Context, alarmManager: AlarmManager, medicine: Medicine) {
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
                android.util.Log.d("BootReceiver", "Уведомление для ${medicine.name} сегодня в ${medicineTime}")
            } else {
                // Уведомление завтра
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, medicineTime.hour)
                calendar.set(Calendar.MINUTE, medicineTime.minute)
                calendar.set(Calendar.SECOND, 0)
                android.util.Log.d("BootReceiver", "Уведомление для ${medicine.name} завтра в ${medicineTime}")
            }
            
            // Устанавливаем повторяющийся будильник
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("BootReceiver", "Будильник установлен (setExactAndAllowWhileIdle) для ${medicine.name}")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("BootReceiver", "Будильник установлен (setExact) для ${medicine.name}")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Ошибка установки будильника для ${medicine.name}", e)
        }
    }
} 