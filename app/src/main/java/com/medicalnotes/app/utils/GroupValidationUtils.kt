package com.medicalnotes.app.utils

import android.util.Log
import com.medicalnotes.app.models.Medicine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Утилита для проверки и исправления проблем с группировкой лекарств
 */
object GroupValidationUtils {
    
    private const val TAG = "GroupValidationUtils"
    
    /**
     * Проверяет корректность настройки группы лекарств
     */
    fun validateGroup(medicines: List<Medicine>): Boolean {
        Log.d(TAG, "=== ПРОВЕРКА ГРУППИРОВКИ ЛЕКАРСТВ ===")
        
        val groupedMedicines = medicines.filter { it.groupId != null }
        
        if (groupedMedicines.isEmpty()) {
            Log.d(TAG, "Нет сгруппированных лекарств")
            return true
        }
        
        // Группируем по groupId
        val groups = groupedMedicines.groupBy { it.groupId }
        
        var isValid = true
        
        groups.forEach { (groupId, groupMedicines) ->
            Log.d(TAG, "Проверяем группу $groupId (${groupMedicines.size} лекарств)")
            
            // Проверяем, что все лекарства в группе имеют одинаковую частоту
            val frequencies = groupMedicines.map { it.frequency }.distinct()
            if (frequencies.size > 1) {
                Log.w(TAG, "⚠️ Группа $groupId: разные частоты приема: $frequencies")
                isValid = false
            }
            
            // Проверяем, что groupOrder уникален в группе
            val orders = groupMedicines.map { it.groupOrder }
            val uniqueOrders = orders.distinct()
            if (orders.size != uniqueOrders.size) {
                Log.w(TAG, "⚠️ Группа $groupId: дублирующиеся порядки: $orders")
                isValid = false
            }
            
            // Проверяем, что groupOrder начинается с 1
            val minOrder = orders.minOrNull() ?: 0
            if (minOrder != 1) {
                Log.w(TAG, "⚠️ Группа $groupId: порядок не начинается с 1 (минимальный: $minOrder)")
                isValid = false
            }
            
            // Проверяем, что groupOrder последовательный
            val expectedOrders = (1..orders.size).toList()
            if (orders.sorted() != expectedOrders) {
                Log.w(TAG, "⚠️ Группа $groupId: непоследовательные порядки: $orders (ожидалось: $expectedOrders)")
                isValid = false
            }
            
            groupMedicines.forEach { medicine ->
                Log.d(TAG, "  - ${medicine.name}: order=${medicine.groupOrder}, frequency=${medicine.frequency}")
            }
        }
        
        if (isValid) {
            Log.d(TAG, "✅ Группировка корректна")
        } else {
            Log.w(TAG, "❌ Обнаружены проблемы с группировкой")
        }
        
        return isValid
    }
    
    /**
     * Исправляет проблемы с группировкой
     */
    fun fixGroupIssues(medicines: List<Medicine>): List<Medicine> {
        Log.d(TAG, "=== ИСПРАВЛЕНИЕ ПРОБЛЕМ С ГРУППИРОВКОЙ ===")
        
        val groupedMedicines = medicines.filter { it.groupId != null }
        
        if (groupedMedicines.isEmpty()) {
            return medicines
        }
        
        val groups = groupedMedicines.groupBy { it.groupId }
        val fixedMedicines = medicines.toMutableList()
        
        groups.forEach { (groupId, groupMedicines) ->
            Log.d(TAG, "Исправляем группу $groupId")
            
            // Сортируем по времени приема для определения порядка
            val sortedGroup = groupMedicines.sortedBy { it.time }
            
            sortedGroup.forEachIndexed { index, medicine ->
                val newOrder = index + 1
                val fixedMedicine = medicine.copy(groupOrder = newOrder)
                
                val medicineIndex = fixedMedicines.indexOfFirst { it.id == medicine.id }
                if (medicineIndex != -1) {
                    fixedMedicines[medicineIndex] = fixedMedicine
                    Log.d(TAG, "Исправлен порядок для ${medicine.name}: ${medicine.groupOrder} -> $newOrder")
                }
            }
        }
        
        return fixedMedicines
    }
    
    /**
     * Проверяет логику группы для конкретной даты
     */
    fun validateGroupLogic(medicines: List<Medicine>, date: LocalDate): Boolean {
        Log.d(TAG, "=== ПРОВЕРКА ЛОГИКИ ГРУППЫ ДЛЯ ДАТЫ $date ===")
        
        val groupedMedicines = medicines.filter { it.groupId != null }
        
        if (groupedMedicines.isEmpty()) {
            return true
        }
        
        val groups = groupedMedicines.groupBy { it.groupId }
        var isValid = true
        
        groups.forEach { (groupId, groupMedicines) ->
            Log.d(TAG, "Проверяем логику группы $groupId")
            
            val shouldTakeResults = groupMedicines.map { medicine ->
                val shouldTake = shouldTakeMedicineInGroup(medicine, date)
                Log.d(TAG, "  - ${medicine.name} (order=${medicine.groupOrder}): shouldTake=$shouldTake")
                medicine to shouldTake
            }
            
            // Проверяем, что только одно лекарство в группе должно приниматься в день
            val shouldTakeCount = shouldTakeResults.count { it.second }
            
            if (shouldTakeCount > 1) {
                Log.w(TAG, "⚠️ Группа $groupId: несколько лекарств должны приниматься в один день ($shouldTakeCount)")
                isValid = false
            } else if (shouldTakeCount == 0) {
                Log.d(TAG, "Группа $groupId: ни одно лекарство не должно приниматься сегодня")
            } else {
                val medicineToTake = shouldTakeResults.find { it.second }?.first
                Log.d(TAG, "✅ Группа $groupId: должно приниматься ${medicineToTake?.name}")
            }
        }
        
        return isValid
    }
    
    /**
     * Копия логики shouldTakeMedicineInGroup из DosageCalculator
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        if (medicine.frequency == com.medicalnotes.app.models.DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder % 2 == 1 -> groupDay == 0
                medicine.groupOrder % 2 == 0 -> groupDay == 1
                else -> false
            }
            return shouldTake
        }
        return false
    }
} 