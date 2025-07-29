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
                    val dataManager = DataManager(context)
                    val notificationManager = NotificationManager(context)
                    
                    val medicine = dataManager.getMedicineById(medicineId)
                    medicine?.let {
                        if (it.isActive && it.remainingQuantity > 0) {
                            // Проверяем, не было ли лекарство принято недавно
                            val timeSinceLastDose = System.currentTimeMillis() - it.lastTakenTime
                            val oneHourInMillis = 60 * 60 * 1000L
                            
                            if (timeSinceLastDose > oneHourInMillis) {
                                notificationManager.showMedicineReminder(it)
                            }
                        }
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
                    
                    // ✅ УЛУЧШЕНО: Немедленно останавливаем вибрацию и уведомления
                    try {
                        android.util.Log.d("🔇 RECEIVER_НАЧАЛО", "Начинаем остановку всех звуков и уведомлений для лекарства ID: $medicineId")
                        
                        // Принудительно останавливаем все вибрации и звуки
                        notificationManager.stopVibration()
                        android.util.Log.d("🔇 RECEIVER_ЗВУК", "stopVibration() выполнен для лекарства ID: $medicineId")
                        
                        // Отменяем конкретное уведомление для этого лекарства
                        notificationManager.cancelOverdueNotification(medicineId)
                        android.util.Log.d("🔇 RECEIVER_УВЕДОМЛЕНИЕ", "cancelOverdueNotification() выполнен для лекарства ID: $medicineId")
                        
                        // Дополнительно отменяем все уведомления для этого лекарства
                        notificationManager.cancelMedicineNotification(medicineId)
                        android.util.Log.d("🔇 RECEIVER_ВСЕ_УВЕДОМЛЕНИЯ", "cancelMedicineNotification() выполнен для лекарства ID: $medicineId")
                        
                        // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
                        try {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                            
                            // Временно отключаем звуки уведомлений
                            val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                            
                            // Восстанавливаем громкость через 100мс
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                                    android.util.Log.d("🔇 RECEIVER_ВОССТАНОВЛЕНИЕ", "Громкость уведомлений восстановлена для лекарства ID: $medicineId")
                                } catch (e: Exception) {
                                    android.util.Log.e("🔇 RECEIVER_ОШИБКА", "Ошибка восстановления громкости для лекарства ID: $medicineId", e)
                                }
                            }, 100)
                            
                            android.util.Log.d("🔇 RECEIVER_AUDIOMANAGER", "AudioManager использован для остановки звука лекарства ID: $medicineId")
                        } catch (e: Exception) {
                            android.util.Log.e("🔇 RECEIVER_AUDIOMANAGER", "Ошибка использования AudioManager для лекарства ID: $medicineId", e)
                        }
                        
                        android.util.Log.d("🔇 RECEIVER_ЗАВЕРШЕНО", "Все вибрации, звуки и уведомления остановлены для лекарства ID: $medicineId")
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
} 