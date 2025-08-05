package com.medicalnotes.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.NotificationManager
import com.medicalnotes.app.utils.NotificationScheduler

/**
 * Receiver для обработки нажатий на кнопки в уведомлениях с карточками лекарств
 */
class NotificationButtonReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationButtonReceiver"
        
        // Actions для кнопок
        const val ACTION_BUTTON_TAKEN = "com.medicalnotes.ACTION_BUTTON_TAKEN"
        const val ACTION_BUTTON_SNOOZE = "com.medicalnotes.ACTION_BUTTON_SNOOZE"
        const val ACTION_BUTTON_SKIP = "com.medicalnotes.ACTION_BUTTON_SKIP"
        const val ACTION_BUTTON_DETAILS = "com.medicalnotes.ACTION_BUTTON_DETAILS"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medicineId = intent.getLongExtra("medicine_id", -1)
        
        if (medicineId == -1L) {
            Log.e(TAG, "Не получен ID лекарства")
            return
        }
        
        Log.d(TAG, "Получено действие: $action для лекарства ID: $medicineId")
        
        when (action) {
            ACTION_BUTTON_TAKEN -> handleMedicineTaken(context, medicineId)
            ACTION_BUTTON_SNOOZE -> handleMedicineSnooze(context, medicineId)
            ACTION_BUTTON_SKIP -> handleMedicineSkip(context, medicineId)
            ACTION_BUTTON_DETAILS -> handleMedicineDetails(context, medicineId)
            else -> Log.w(TAG, "Неизвестное действие: $action")
        }
    }
    
    /**
     * Обработка нажатия кнопки "Принял"
     */
    private fun handleMedicineTaken(context: Context, medicineId: Long) {
        try {
            val dataManager = DataManager(context)
            val medicine = dataManager.getMedicineById(medicineId)
            
            if (medicine != null) {
                // Отмечаем лекарство как принятое
                dataManager.markMedicineAsTaken(medicineId)
                
                // Отменяем уведомление
                val notificationManager = NotificationManager(context)
                val notificationId = (NotificationManager.NOTIFICATION_ID_MEDICINE_CARD + medicineId).toInt()
                notificationManager.cancelNotification(notificationId)
                
                // Останавливаем вибрацию и звук
                notificationManager.forceStopAllNotifications()
                
                Log.d(TAG, "Лекарство ${medicine.name} отмечено как принятое")
            } else {
                Log.e(TAG, "Лекарство с ID $medicineId не найдено")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обработке принятия лекарства", e)
        }
    }
    
    /**
     * Обработка нажатия кнопки "Отложить"
     */
    private fun handleMedicineSnooze(context: Context, medicineId: Long) {
        try {
            val dataManager = DataManager(context)
            val medicine = dataManager.getMedicineById(medicineId)
            
            if (medicine != null) {
                // Отменяем текущее уведомление
                val notificationManager = NotificationManager(context)
                val notificationId = (NotificationManager.NOTIFICATION_ID_MEDICINE_CARD + medicineId).toInt()
                notificationManager.cancelNotification(notificationId)
                
                // Планируем новое уведомление через 10 минут
                val scheduler = NotificationScheduler(context)
                scheduler.scheduleMedicineNotification(medicine, 10) // 10 минут
                
                Log.d(TAG, "Уведомление для ${medicine.name} отложено на 10 минут")
            } else {
                Log.e(TAG, "Лекарство с ID $medicineId не найдено")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при откладывании уведомления", e)
        }
    }
    
    /**
     * Обработка нажатия кнопки "Пропустить"
     */
    private fun handleMedicineSkip(context: Context, medicineId: Long) {
        try {
            val dataManager = DataManager(context)
            val medicine = dataManager.getMedicineById(medicineId)
            
            if (medicine != null) {
                // Отменяем уведомление
                val notificationManager = NotificationManager(context)
                val notificationId = (NotificationManager.NOTIFICATION_ID_MEDICINE_CARD + medicineId).toInt()
                notificationManager.cancelNotification(notificationId)
                
                // Останавливаем вибрацию и звук
                notificationManager.forceStopAllNotifications()
                
                Log.d(TAG, "Уведомление для ${medicine.name} пропущено")
            } else {
                Log.e(TAG, "Лекарство с ID $medicineId не найдено")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при пропуске уведомления", e)
        }
    }
    
    /**
     * Обработка нажатия кнопки "Подробности"
     */
    private fun handleMedicineDetails(context: Context, medicineId: Long) {
        try {
            // Открываем детальную информацию о лекарстве
            val intent = Intent(context, com.medicalnotes.app.MedicineCardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("medicine_id", medicineId)
            }
            context.startActivity(intent)
            
            Log.d(TAG, "Открыта детальная информация для лекарства ID: $medicineId")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при открытии деталей лекарства", e)
        }
    }
} 