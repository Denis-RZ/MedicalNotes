package com.medicalnotes.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.medicalnotes.app.repository.MedicineRepository
import com.medicalnotes.app.utils.MedicineStatusHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyResetService : Service() {
    
    private val TAG = "DailyResetService"
    private lateinit var medicineRepository: MedicineRepository
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DailyResetService started")
        
        medicineRepository = MedicineRepository(this)
        
        CoroutineScope(Dispatchers.IO).launch {
            resetDailyStatus()
        }
        
        return START_NOT_STICKY
    }
    
    private suspend fun resetDailyStatus() {
        try {
            Log.d(TAG, "Starting daily reset of medicine status")
            
            val medicines = medicineRepository.getAllMedicines()
            var resetCount = 0
            
            medicines.forEach { medicine ->
                val updatedMedicine = MedicineStatusHelper.resetDailyStatus(medicine)
                medicineRepository.updateMedicine(updatedMedicine)
                resetCount++
            }
            
            Log.d(TAG, "Daily reset completed. Reset $resetCount medicines")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily reset", e)
        }
    }
} 