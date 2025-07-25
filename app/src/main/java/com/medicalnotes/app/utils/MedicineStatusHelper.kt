package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
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
        val today = LocalDate.now()
        val startDate = LocalDateTime.ofInstant(
            Date(medicine.startDate).toInstant(), 
            ZoneId.systemDefault()
        ).toLocalDate()
        
        return when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val dayOfWeek = today.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val dayOfWeek = today.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
            DosageFrequency.WEEKLY -> {
                val dayOfWeek = today.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = today.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
        }
    }
    
    /**
     * Проверяет, просрочено ли принятие лекарства
     */
    fun isOverdue(medicine: Medicine): Boolean {
        if (!shouldTakeToday(medicine) || medicine.takenToday) {
            return false
        }
        
        val now = LocalTime.now()
        val medicineTime = medicine.time
        
        return now.isAfter(medicineTime) || now.equals(medicineTime)
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
     * Получает статус лекарства для отображения (устаревший метод)
     */
    fun getMedicineStatus(medicine: Medicine): MedicineStatus {
        return when {
            !medicine.isActive -> MedicineStatus.NOT_TODAY
            !shouldTakeToday(medicine) -> MedicineStatus.NOT_TODAY
            medicine.takenToday -> MedicineStatus.TAKEN_TODAY
            isOverdue(medicine) -> MedicineStatus.OVERDUE
            else -> MedicineStatus.UPCOMING
        }
    }
    
    /**
     * Обновляет статус лекарства
     */
    fun updateMedicineStatus(medicine: Medicine): Medicine {
        val shouldTake = shouldTakeToday(medicine)
        val overdue = isOverdue(medicine)
        
        return medicine.copy(
            shouldTakeToday = shouldTake,
            isOverdue = overdue
        )
    }
    
    /**
     * Отмечает лекарство как принятое
     */
    fun markAsTaken(medicine: Medicine): Medicine {
        return medicine.copy(
            takenToday = true,
            takenAt = System.currentTimeMillis(),
            isOverdue = false
        )
    }
    
    /**
     * Сбрасывает статус принятия для нового дня
     */
    fun resetDailyStatus(medicine: Medicine): Medicine {
        return medicine.copy(
            takenToday = false,
            takenAt = 0,
            isOverdue = false
        )
    }
}

 