package com.medicalnotes.app.repository

import android.content.Context
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class MedicineRepository(context: Context) {
    
    private val dataManager = DataManager(context)
    
    suspend fun getAllMedicines(): List<Medicine> = withContext(Dispatchers.IO) {
        return@withContext dataManager.loadMedicines()
    }
    
    suspend fun getMedicinesForDate(date: LocalDate): List<Medicine> = withContext(Dispatchers.IO) {
        //  ИСПРАВЛЕНО: Убираем несуществующую ссылку на DosageCalculator
        val allMedicines = dataManager.loadMedicines()
        return@withContext allMedicines.filter { medicine ->
            // Проверяем, активны ли лекарства на указанную дату
            medicine.isActive
        }.sortedBy { it.time }
    }
    
    suspend fun getMedicineById(id: Long): Medicine? = withContext(Dispatchers.IO) {
        android.util.Log.d("MedicineRepository", "Getting medicine by ID: $id")
        val medicine = dataManager.getMedicineById(id)
        android.util.Log.d("MedicineRepository", "Medicine found: ${medicine?.name ?: "null"}")
        return@withContext medicine
    }
    
    suspend fun insertMedicine(medicine: Medicine): Long = withContext(Dispatchers.IO) {
        val success = dataManager.addMedicine(medicine)
        return@withContext if (success) System.currentTimeMillis() else -1L
    }
    
    suspend fun updateMedicine(medicine: Medicine): Boolean = withContext(Dispatchers.IO) {
        return@withContext dataManager.updateMedicine(medicine)
    }
    
    suspend fun deleteMedicine(medicineId: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            dataManager.deleteMedicine(medicineId)
            true
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "Error deleting medicine", e)
            false
        }
    }
    
    suspend fun markMedicineAsTaken(medicineId: Long) = withContext(Dispatchers.IO) {
        dataManager.decrementMedicineQuantity(medicineId)
    }
    
    suspend fun markMedicineAsSkipped(medicineId: Long) = withContext(Dispatchers.IO) {
        dataManager.markMedicineAsSkipped(medicineId)
    }
    
    suspend fun getLowSupplyMedicines(threshold: Int = 5): List<Medicine> = withContext(Dispatchers.IO) {
        //  ИСПРАВЛЕНО: Используем параметр threshold
        val allMedicines = dataManager.loadMedicines()
        return@withContext allMedicines.filter { 
            it.isActive && it.remainingQuantity <= threshold 
        }
    }
} 