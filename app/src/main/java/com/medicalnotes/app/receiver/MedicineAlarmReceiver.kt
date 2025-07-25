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
                    val notificationManager = NotificationManager(context)
                    val dataManager = DataManager(context)
                    
                    // Немедленно останавливаем вибрацию
                    notificationManager.stopVibration()
                    
                    // Отмечаем лекарство как принятое в базе данных
                    val medicine = dataManager.getMedicineById(medicineId)
                    medicine?.let {
                        dataManager.decrementMedicineQuantity(medicineId)
                    }
                    
                    // Показываем подтверждение
                    notificationManager.markMedicineAsTaken(medicineId)
                    
                    // Показываем Toast уведомление
                    android.widget.Toast.makeText(
                        context,
                        "Лекарство принято!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
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