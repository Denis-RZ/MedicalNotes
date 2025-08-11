package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.utils.StatusManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

object MedicineStatusHelper {
    
    /**
     * Проверяет, нужно ли принимать лекарство сегодня
     */
    fun shouldTakeToday(medicine: Medicine): Boolean {
        android.util.Log.d("MedicineStatusHelper", "=== ПРОВЕРКА РАСПИСАНИЯ ===")
        android.util.Log.d("MedicineStatusHelper", "Лекарство: ${medicine.name}")
        android.util.Log.d("MedicineStatusHelper", "  - ID: ${medicine.id}")
        android.util.Log.d("MedicineStatusHelper", "  - Активно: ${medicine.isActive}")
        android.util.Log.d("MedicineStatusHelper", "  - groupId: ${medicine.groupId}")
        android.util.Log.d("MedicineStatusHelper", "  - groupName: ${medicine.groupName}")
        android.util.Log.d("MedicineStatusHelper", "  - groupOrder: ${medicine.groupOrder}")
        android.util.Log.d("MedicineStatusHelper", "  - groupFrequency: ${medicine.groupFrequency}")
        
        val today = LocalDate.now()
        val startDate = LocalDateTime.ofInstant(
            Date(medicine.startDate).toInstant(), 
            ZoneId.systemDefault()
        ).toLocalDate()
        
        android.util.Log.d("MedicineStatusHelper", "  - Сегодня: $today")
        android.util.Log.d("MedicineStatusHelper", "  - Дата начала: $startDate")
        android.util.Log.d("MedicineStatusHelper", "  - Частота: ${medicine.frequency}")
        android.util.Log.d("MedicineStatusHelper", "  - startDate в миллисекундах: ${medicine.startDate}")
        
        // Проверяем, не в группе ли лекарство
        if (medicine.groupId != null) {
            android.util.Log.d("MedicineStatusHelper", "  - Лекарство в группе, используем DosageCalculator")
            val groupResult = DosageCalculator.shouldTakeMedicine(medicine, today)
            android.util.Log.d("MedicineStatusHelper", "  - Результат DosageCalculator: $groupResult")
            return groupResult
        }
        
        val result = when (medicine.frequency) {
            DosageFrequency.DAILY -> {
                android.util.Log.d("MedicineStatusHelper", "  - Ежедневно: true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
                val shouldTake = daysSinceStart % 2L == 0L
                android.util.Log.d("MedicineStatusHelper", "  - Через день: дней с начала = $daysSinceStart, принимать = $shouldTake")
                shouldTake
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val dayOfWeek = today.dayOfWeek.value
                val shouldTake = medicine.customDays.contains(dayOfWeek)
                android.util.Log.d("MedicineStatusHelper", "  - Дважды в неделю: день недели = $dayOfWeek, принимать = $shouldTake")
                android.util.Log.d("MedicineStatusHelper", "  - Настроенные дни: ${medicine.customDays}")
                shouldTake
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val dayOfWeek = today.dayOfWeek.value
                val shouldTake = medicine.customDays.contains(dayOfWeek)
                android.util.Log.d("MedicineStatusHelper", "  - Трижды в неделю: день недели = $dayOfWeek, принимать = $shouldTake")
                android.util.Log.d("MedicineStatusHelper", "  - Настроенные дни: ${medicine.customDays}")
                shouldTake
            }
            DosageFrequency.WEEKLY -> {
                val dayOfWeek = today.dayOfWeek.value
                val shouldTake = medicine.customDays.contains(dayOfWeek)
                android.util.Log.d("MedicineStatusHelper", "  - Еженедельно: день недели = $dayOfWeek, принимать = $shouldTake")
                android.util.Log.d("MedicineStatusHelper", "  - Настроенные дни: ${medicine.customDays}")
                shouldTake
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = today.dayOfWeek.value
                val shouldTake = medicine.customDays.contains(dayOfWeek)
                android.util.Log.d("MedicineStatusHelper", "  - Пользовательское: день недели = $dayOfWeek, принимать = $shouldTake")
                android.util.Log.d("MedicineStatusHelper", "  - Настроенные дни: ${medicine.customDays}")
                shouldTake
            }
        }
        
        android.util.Log.d("MedicineStatusHelper", "  - РЕЗУЛЬТАТ: $result")
        return result
    }
    
    /**
     * Проверяет, просрочено ли принятие лекарства
     */
    fun isOverdue(medicine: Medicine): Boolean {
        // ИСПРАВЛЕНО: Используем единую логику из DosageCalculator
        return DosageCalculator.isMedicineOverdue(medicine)
    }
    
    /**
     * Проверяет, активно ли лекарство для показа в списке активных
     */
    fun isActiveForToday(medicine: Medicine): Boolean {
        return medicine.isActive && 
               shouldTakeToday(medicine) && 
               !medicine.takenToday
    }
    
    /**
     * Получает статус лекарства для отображения
     */
    fun getMedicineStatus(medicine: Medicine): DosageCalculator.MedicineStatus {
        // ИСПРАВЛЕНО: Используем единую логику из DosageCalculator
        return DosageCalculator.getMedicineStatus(medicine)
    }
    
    /**
     * Обновляет статус лекарства
     */
    fun updateMedicineStatus(medicine: Medicine): Medicine {
        // ИСПРАВЛЕНО: Используем StatusManager
        return StatusManager.markAsTaken(medicine)
    }
    
    /**
     * Отмечает лекарство как принятое
     */
    fun markAsTaken(medicine: Medicine): Medicine {
        // ИСПРАВЛЕНО: Используем StatusManager
        return StatusManager.markAsTaken(medicine)
    }
    
    /**
     * Сбрасывает статус принятия для нового дня
     */
    fun resetDailyStatus(medicine: Medicine): Medicine {
        // ИСПРАВЛЕНО: Используем StatusManager
        return StatusManager.resetDailyStatus(medicine)
    }
}

 