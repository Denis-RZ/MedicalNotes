package com.medicalnotes.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.NotificationManager

class MedicineAlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_MEDICINE_TAKEN" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    android.util.Log.d("MedicineAlarmReceiver", "=== КНОПКА 'ВЫПИЛ' НАЖАТА ===")
                    android.util.Log.d("MedicineAlarmReceiver", "Лекарство ID: $medicineId")
                    
                    val notificationManager = NotificationManager(context)
                    val dataManager = DataManager(context)
                    
                    // Останавливаем уведомление с карточкой
                    notificationManager.cancelMedicineCardNotification(medicineId)
                    
                    // Отмечаем лекарство как принятое
                    try {
                        val medicine = dataManager.getMedicineById(medicineId)
                        medicine?.let {
                            dataManager.decrementMedicineQuantity(medicineId)
                            android.util.Log.d("MedicineAlarmReceiver", "✓ Лекарство отмечено как принятое")
                            
                            // Показываем краткое подтверждение
                            notificationManager.showMedicineTakenConfirmation(medicineId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MedicineAlarmReceiver", "Ошибка отметки лекарства как принятого", e)
                    }
                }
            }
            "ACTION_MEDICINE_SKIPPED" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    android.util.Log.d("MedicineAlarmReceiver", "=== КНОПКА 'ПРОПУСТИТЬ' НАЖАТА ===")
                    android.util.Log.d("MedicineAlarmReceiver", "Лекарство ID: $medicineId")
                    
                    val notificationManager = NotificationManager(context)
                    
                    // Отменяем уведомление с карточкой
                    notificationManager.cancelMedicineCardNotification(medicineId)
                    
                    // Показываем краткое подтверждение пропуска
                    notificationManager.showMedicineSkippedConfirmation(medicineId)
                }
            }
            "com.medicalnotes.app.MEDICINE_REMINDER" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    android.util.Log.d("MedicineAlarmReceiver", "=== ПОЛУЧЕНО УВЕДОМЛЕНИЕ О ЛЕКАРСТВЕ ===")
                    android.util.Log.d("MedicineAlarmReceiver", "Лекарство ID: $medicineId")
                    
                    val dataManager = DataManager(context)
                    val notificationManager = NotificationManager(context)
                    
                    val medicine = dataManager.getMedicineById(medicineId)
                    medicine?.let {
                        android.util.Log.d("MedicineAlarmReceiver", "Найдено лекарство: ${it.name}")
                        android.util.Log.d("MedicineAlarmReceiver", "  - Активно: ${it.isActive}")
                        android.util.Log.d("MedicineAlarmReceiver", "  - Остаток: ${it.remainingQuantity}")
                        android.util.Log.d("MedicineAlarmReceiver", "  - Время приема: ${it.time}")
                        
                        if (it.isActive && it.remainingQuantity > 0) {
                            // Проверяем, не было ли лекарство принято недавно
                            val timeSinceLastDose = System.currentTimeMillis() - it.lastTakenTime
                            val oneHourInMillis = 60 * 60 * 1000L
                            
                            android.util.Log.d("MedicineAlarmReceiver", "Время с последнего приема: ${timeSinceLastDose/1000/60} минут")
                            
                            if (timeSinceLastDose > oneHourInMillis) {
                                android.util.Log.d("MedicineAlarmReceiver", "Показываем уведомление для: ${it.name}")
                                
                                // ✅ ДОБАВЛЕНО: Принудительно показываем уведомление с карточкой
                                try {
                                    notificationManager.showMedicineReminder(it)
                                    android.util.Log.d("MedicineAlarmReceiver", "✓ Уведомление показано для: ${it.name}")
                                } catch (e: Exception) {
                                    android.util.Log.e("MedicineAlarmReceiver", "Ошибка показа уведомления", e)
                                }
                                
                                // ✅ ДОБАВЛЕНО: Планируем следующее уведомление на завтра
                                try {
                                    scheduleNextDayNotification(context, it)
                                    android.util.Log.d("MedicineAlarmReceiver", "✓ Следующее уведомление запланировано для: ${it.name}")
                                } catch (e: Exception) {
                                    android.util.Log.e("MedicineAlarmReceiver", "Ошибка планирования следующего уведомления", e)
                                }
                            } else {
                                android.util.Log.d("MedicineAlarmReceiver", "Лекарство принято недавно, уведомление не показываем")
                            }
                        } else {
                            android.util.Log.d("MedicineAlarmReceiver", "Лекарство неактивно или закончилось")
                        }
                    } ?: run {
                        android.util.Log.e("MedicineAlarmReceiver", "Лекарство с ID $medicineId не найдено")
                    }
                }
            }
            "com.medicalnotes.app.MEDICINE_TAKEN" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    android.util.Log.d("MedicineAlarmReceiver", "=== ПРИЕМ ЛЕКАРСТВА ЧЕРЕЗ УВЕДОМЛЕНИЕ ===")
                    android.util.Log.d("MedicineAlarmReceiver", "Лекарство ID: $medicineId")
                    
                    val notificationManager = NotificationManager(context)
                    val dataManager = DataManager(context)
                    
                    // ✅ ДОБАВЛЕНО: Проверяем актуальность статуса лекарства перед воспроизведением звука
                    try {
                        val medicine = dataManager.getMedicineById(medicineId)
                        if (medicine != null) {
                            val currentStatus = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                            android.util.Log.d("MedicineAlarmReceiver", "Текущий статус лекарства: $currentStatus")
                            
                            // Если лекарство больше не просрочено, не воспроизводим звук
                            if (currentStatus != com.medicalnotes.app.utils.MedicineStatus.OVERDUE) {
                                android.util.Log.d("MedicineAlarmReceiver", "Лекарство больше не просрочено, звук не воспроизводится")
                                return
                            }
                            
                            // ✅ ДОБАВЛЕНО: Дополнительная проверка - если лекарство было принято недавно
                            if (medicine.takenToday) {
                                android.util.Log.d("MedicineAlarmReceiver", "Лекарство уже принято сегодня, звук не воспроизводится")
                                return
                            }
                            
                            // ✅ ДОБАВЛЕНО: Проверяем, не было ли время изменено недавно
                            val timeSinceUpdate = System.currentTimeMillis() - medicine.updatedAt
                            val fiveMinutesInMillis = 5 * 60 * 1000L
                            if (timeSinceUpdate < fiveMinutesInMillis) {
                                android.util.Log.d("MedicineAlarmReceiver", "Лекарство было обновлено недавно (${timeSinceUpdate}ms назад), звук не воспроизводится")
                                return
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MedicineAlarmReceiver", "Ошибка проверки статуса лекарства", e)
                    }
                    
                    // ✅ ИСПРАВЛЕНО: Более мягкая остановка уведомлений
                    try {
                        android.util.Log.d("🔇 RECEIVER_НАЧАЛО", "Начинаем остановку уведомлений для лекарства ID: $medicineId")
                        
                        // Останавливаем вибрацию
                        notificationManager.stopVibration()
                        android.util.Log.d("🔇 RECEIVER_ВИБРАЦИЯ", "stopVibration() выполнен для лекарства ID: $medicineId")
                        
                        // Отменяем уведомления для этого лекарства
                        notificationManager.cancelOverdueNotification(medicineId)
                        notificationManager.cancelMedicineNotification(medicineId)
                        android.util.Log.d("🔇 RECEIVER_УВЕДОМЛЕНИЯ", "Уведомления отменены для лекарства ID: $medicineId")
                        
                        android.util.Log.d("🔇 RECEIVER_ЗАВЕРШЕНО", "Уведомления остановлены для лекарства ID: $medicineId")
                    } catch (e: Exception) {
                        android.util.Log.e("MedicineAlarmReceiver", "Ошибка остановки уведомлений", e)
                    }
                    
                    // Отмечаем лекарство как принятое в базе данных
                    try {
                        val medicine = dataManager.getMedicineById(medicineId)
                        medicine?.let {
                            dataManager.decrementMedicineQuantity(medicineId)
                            android.util.Log.d("MedicineAlarmReceiver", "✓ Количество лекарства уменьшено")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MedicineAlarmReceiver", "Ошибка обновления базы данных", e)
                    }
                    
                    // Показываем подтверждение
                    try {
                        notificationManager.markMedicineAsTaken(medicineId)
                        android.util.Log.d("MedicineAlarmReceiver", "✓ Подтверждение показано")
                    } catch (e: Exception) {
                        android.util.Log.e("MedicineAlarmReceiver", "Ошибка показа подтверждения", e)
                    }
                    
                    // ✅ ИЗМЕНЕНО: Убираем Toast уведомление, которое может воспроизводить звук
                    // try {
                    //     android.widget.Toast.makeText(
                    //         context,
                    //         "Лекарство принято!",
                    //         android.widget.Toast.LENGTH_SHORT
                    //     ).show()
                    //     android.util.Log.d("MedicineAlarmReceiver", "✓ Toast показан")
                    // } catch (e: Exception) {
                    //     android.util.Log.e("MedicineAlarmReceiver", "Ошибка показа Toast", e)
                    // }
                    
                    android.util.Log.d("MedicineAlarmReceiver", "=== ПРИЕМ ЛЕКАРСТВА ЗАВЕРШЕН ===")
                }
            }
            "com.medicalnotes.app.LOW_STOCK_ALERT" -> {
                val medicineId = intent.getLongExtra("medicine_id", -1)
                if (medicineId != -1L) {
                    val dataManager = DataManager(context)
                    val notificationManager = NotificationManager(context)
                    
                    val medicine = dataManager.getMedicineById(medicineId)
                    medicine?.let {
                        notificationManager.showLowStockAlert(it)
                    }
                }
            }
            "com.medicalnotes.app.EMERGENCY_ALERT" -> {
                val message = intent.getStringExtra("message") ?: "Экстренное уведомление"
                val notificationManager = NotificationManager(context)
                notificationManager.showEmergencyAlert(message)
            }
        }
    }
    
    // ✅ ДОБАВЛЕНО: Планирование следующего уведомления на завтра
    private fun scheduleNextDayNotification(context: Context, medicine: Medicine) {
        try {
            android.util.Log.d("MedicineAlarmReceiver", "Планирование следующего уведомления для: ${medicine.name}")
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = "com.medicalnotes.app.MEDICINE_REMINDER"
                putExtra("medicine_id", medicine.id)
            }
            
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                medicine.id.toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            // Вычисляем время на завтра
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, medicine.time.hour)
            calendar.set(java.util.Calendar.MINUTE, medicine.time.minute)
            calendar.set(java.util.Calendar.SECOND, 0)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            android.util.Log.d("MedicineAlarmReceiver", "Следующее уведомление запланировано для ${medicine.name} на завтра в ${medicine.time}")
            
        } catch (e: Exception) {
            android.util.Log.e("MedicineAlarmReceiver", "Ошибка планирования следующего уведомления для ${medicine.name}", e)
        }
    }
} 