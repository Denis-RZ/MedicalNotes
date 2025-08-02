package com.medicalnotes.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.receiver.MedicineAlarmReceiver
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * Утилита для автоматического планирования уведомлений о лекарствах
 */
class NotificationScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Чистая функция для вычисления времени следующего триггера (для тестирования)
     */
    data class TriggerPlan(val triggerAtMs: Long, val markOverdue: Boolean)
    
    companion object {
        /**
         * Статическая версия для тестирования (без Android зависимостей)
         */
        fun computeNextTriggerStatic(
            now: LocalDateTime,
            targetLocalTime: LocalTime,
            isEdit: Boolean
        ): TriggerPlan {
            val targetDateTime = now.toLocalDate().atTime(targetLocalTime)
            
            return if (isEdit && targetLocalTime <= now.toLocalTime()) {
                // ASAP режим: если редактируем на прошедшее время
                val asapTime = now.plusMinutes(1) // +1 минута от текущего времени
                TriggerPlan(
                    triggerAtMs = asapTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    markOverdue = true
                )
            } else {
                // Обычный режим: сегодня в targetLocalTime
                val triggerDateTime = if (targetDateTime.isBefore(now)) {
                    // Если время уже прошло и это не редактирование, переносим на завтра
                    if (!isEdit) {
                        now.toLocalDate().plusDays(1).atTime(targetLocalTime)
                    } else {
                        targetDateTime
                    }
                } else {
                    targetDateTime
                }
                
                TriggerPlan(
                    triggerAtMs = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    markOverdue = false
                )
            }
        }
    }
    
    fun computeNextTrigger(
        now: LocalDateTime,
        targetLocalTime: LocalTime,
        isEdit: Boolean
    ): TriggerPlan {
        return computeNextTriggerStatic(now, targetLocalTime, isEdit)
    }
    
    /**
     * Отменяет будильник для конкретного лекарства
     */
    fun cancelAlarm(medicineId: Long) {
        try {
            android.util.Log.d("NotificationScheduler", "Отмена будильника для лекарства ID: $medicineId")
            
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_SHOW_MEDICINE_CARD"
                putExtra("medicine_id", medicineId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                android.util.Log.d("NotificationScheduler", "Будильник отменен для лекарства ID: $medicineId")
            } else {
                android.util.Log.d("NotificationScheduler", "Будильник не найден для лекарства ID: $medicineId")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка отмены будильника для лекарства ID: $medicineId", e)
        }
    }
    
    /**
     * Планирует уведомление с учетом редактирования
     */
    fun scheduleConsideringEdit(med: Medicine, isEdit: Boolean) {
        try {
            android.util.Log.d("NotificationScheduler", "Планирование уведомления для: ${med.name} (isEdit: $isEdit)")
            
            // ИСПРАВЛЕНО: Проверяем групповую логику перед планированием
            val today = java.time.LocalDate.now()
            
            if (!isEdit) {
                // Для новых лекарств проверяем, нужно ли принимать сегодня
                val shouldTake = DosageCalculator.shouldTakeMedicine(med, today)
                if (!shouldTake) {
                    android.util.Log.d("NotificationScheduler", "Лекарство ${med.name} не нужно принимать сегодня, пропускаем планирование")
                    return
                }
            }
            
            val now = LocalDateTime.now()
            val triggerPlan = computeNextTrigger(now, med.time, isEdit)
            
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "ACTION_SHOW_MEDICINE_CARD"
                putExtra("medicine_id", med.id)
                if (triggerPlan.markOverdue) {
                    putExtra("overdue", true)
                }
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                med.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerPlan.triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerPlan.triggerAtMs,
                    pendingIntent
                )
            }
            
            val triggerTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(triggerPlan.triggerAtMs),
                ZoneId.systemDefault()
            )
            
            android.util.Log.d("NotificationScheduler", "Будильник установлен для ${med.name} на ${triggerTime.toLocalTime()} (overdue: ${triggerPlan.markOverdue})")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка планирования уведомления для ${med.name}", e)
        }
    }
    
    /**
     * Перепланирует уведомление при редактировании
     */
    fun rescheduleOnEdit(med: Medicine) {
        try {
            android.util.Log.d("NotificationScheduler", "Перепланирование уведомления для: ${med.name}")
            cancelAlarm(med.id)
            scheduleConsideringEdit(med, isEdit = true)
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка перепланирования уведомления для ${med.name}", e)
        }
    }
    
    /**
     * Планирует уведомления для всех активных лекарств
     */
    fun scheduleAllMedicineNotifications() {
        try {
            android.util.Log.d("NotificationScheduler", "Планирование уведомлений для всех лекарств")
            
            val dataManager = DataManager(context)
            val allActiveMedicines = dataManager.getActiveMedicines()
            
            android.util.Log.d("NotificationScheduler", "Найдено активных лекарств: ${allActiveMedicines.size}")
            
            // ИСПРАВЛЕНО: Фильтруем лекарства с учетом групповой логики
            val today = java.time.LocalDate.now()
            
            val medicinesToSchedule = allActiveMedicines.filter { medicine ->
                val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
                android.util.Log.d("NotificationScheduler", "Лекарство: ${medicine.name}")
                android.util.Log.d("NotificationScheduler", "  - Группа: ${medicine.groupName}")
                android.util.Log.d("NotificationScheduler", "  - Порядок: ${medicine.groupOrder}")
                android.util.Log.d("NotificationScheduler", "  - Частота: ${medicine.frequency}")
                android.util.Log.d("NotificationScheduler", "  - Нужно принимать сегодня: $shouldTake")
                shouldTake
            }
            
            android.util.Log.d("NotificationScheduler", "Лекарств для планирования сегодня: ${medicinesToSchedule.size}")
            
            medicinesToSchedule.forEach { medicine ->
                scheduleConsideringEdit(medicine, isEdit = false)
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
                cancelAlarm(medicine.id)
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
            cancelAlarm(medicine.id)
            
            // Создаем новое уведомление
            scheduleConsideringEdit(medicine, isEdit = true)
            
            android.util.Log.d("NotificationScheduler", "Уведомление обновлено для: ${medicine.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("NotificationScheduler", "Ошибка обновления уведомления для ${medicine.name}", e)
        }
    }
    
    fun scheduleForMedicine(medicine: com.medicalnotes.app.models.Medicine) {
        val intent = android.content.Intent(context, com.medicalnotes.app.receiver.MedicineAlarmReceiver::class.java).apply {
            action = "ACTION_SHOW_MEDICINE_CARD"
            putExtra("medicine_id", medicine.id)
        }
        val pi = android.app.PendingIntent.getBroadcast(
            context, medicine.id.toInt(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, medicine.time.hour)
            set(java.util.Calendar.MINUTE, medicine.time.minute)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun scheduleAll() {
        val dm = com.medicalnotes.app.utils.DataManager(context)
        dm.loadMedicines().filter { it.isActive }.forEach { scheduleForMedicine(it) }
    }
} 