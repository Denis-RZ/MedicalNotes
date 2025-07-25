package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import java.time.LocalTime

/**
 * Утилита для проверки сохранения всех полей лекарств
 */
class MedicineFieldValidator(private val context: Context) {
    
    companion object {
        private const val TAG = "MedicineFieldValidator"
    }
    
    /**
     * Проверяет, что все поля лекарства правильно сохранились
     */
    fun validateMedicineFields(originalMedicine: Medicine, savedMedicine: Medicine?): Boolean {
        if (savedMedicine == null) {
            Log.e(TAG, "Saved medicine is null")
            return false
        }
        
        val errors = mutableListOf<String>()
        
        // Проверяем основные поля
        if (originalMedicine.name != savedMedicine.name) {
            errors.add("name: ${originalMedicine.name} != ${savedMedicine.name}")
        }
        
        if (originalMedicine.dosage != savedMedicine.dosage) {
            errors.add("dosage: ${originalMedicine.dosage} != ${savedMedicine.dosage}")
        }
        
        if (originalMedicine.quantity != savedMedicine.quantity) {
            errors.add("quantity: ${originalMedicine.quantity} != ${savedMedicine.quantity}")
        }
        
        if (originalMedicine.notes != savedMedicine.notes) {
            errors.add("notes: ${originalMedicine.notes} != ${savedMedicine.notes}")
        }
        
        if (originalMedicine.isInsulin != savedMedicine.isInsulin) {
            errors.add("isInsulin: ${originalMedicine.isInsulin} != ${savedMedicine.isInsulin}")
        }
        
        if (originalMedicine.time != savedMedicine.time) {
            errors.add("time: ${originalMedicine.time} != ${savedMedicine.time}")
        }
        
        if (originalMedicine.frequency != savedMedicine.frequency) {
            errors.add("frequency: ${originalMedicine.frequency} != ${savedMedicine.frequency}")
        }
        
        if (originalMedicine.multipleDoses != savedMedicine.multipleDoses) {
            errors.add("multipleDoses: ${originalMedicine.multipleDoses} != ${savedMedicine.multipleDoses}")
        }
        
        if (originalMedicine.dosesPerDay != savedMedicine.dosesPerDay) {
            errors.add("dosesPerDay: ${originalMedicine.dosesPerDay} != ${savedMedicine.dosesPerDay}")
        }
        
        if (originalMedicine.doseTimes != savedMedicine.doseTimes) {
            errors.add("doseTimes: ${originalMedicine.doseTimes} != ${savedMedicine.doseTimes}")
        }
        
        if (originalMedicine.dosageTimes != savedMedicine.dosageTimes) {
            errors.add("dosageTimes: ${originalMedicine.dosageTimes} != ${savedMedicine.dosageTimes}")
        }
        
        if (originalMedicine.customDays != savedMedicine.customDays) {
            errors.add("customDays: ${originalMedicine.customDays} != ${savedMedicine.customDays}")
        }
        
        if (originalMedicine.customTimes != savedMedicine.customTimes) {
            errors.add("customTimes: ${originalMedicine.customTimes} != ${savedMedicine.customTimes}")
        }
        
        if (originalMedicine.startDate != savedMedicine.startDate) {
            errors.add("startDate: ${originalMedicine.startDate} != ${savedMedicine.startDate}")
        }
        
        if (errors.isNotEmpty()) {
            Log.e(TAG, "Medicine field validation failed:")
            errors.forEach { error ->
                Log.e(TAG, "  $error")
            }
            return false
        }
        
        Log.d(TAG, "Medicine field validation passed")
        return true
    }
    
    /**
     * Создает тестовое лекарство со всеми полями
     */
    fun createTestMedicine(): Medicine {
        return Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "500 мг",
            quantity = 30,
            remainingQuantity = 25,
            time = LocalTime.of(8, 0),
            notes = "Тестовые заметки",
            isInsulin = true,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING, DosageTime.EVENING),
            customDays = listOf(1, 2, 3, 4, 5),
            customTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
            startDate = System.currentTimeMillis(),
            multipleDoses = true,
            dosesPerDay = 2,
            doseTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        )
    }
} 