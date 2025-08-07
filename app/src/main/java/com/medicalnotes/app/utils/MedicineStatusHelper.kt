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
        android.util.Log.d("MedicineStatusHelper", "=== ПРОВЕРКА ПРОСРОЧЕННОСТИ ===")
        android.util.Log.d("MedicineStatusHelper", "Лекарство: ${medicine.name}")
        android.util.Log.d("MedicineStatusHelper", "  - По расписанию сегодня: ${shouldTakeToday(medicine)}")
        android.util.Log.d("MedicineStatusHelper", "  - Принято сегодня: ${medicine.takenToday}")
        
        if (!shouldTakeToday(medicine) || medicine.takenToday) {
            android.util.Log.d("MedicineStatusHelper", "  - НЕ ПРОСРОЧЕНО: не по расписанию или уже принято")
            return false
        }
        
        val now = LocalTime.now()
        val medicineTime = medicine.time
        
        // ДОБАВЛЕНО: Буфер времени 15 минут
        val bufferMinutes = 15L
        val overdueTime = medicineTime.plusMinutes(bufferMinutes)
        
        android.util.Log.d("MedicineStatusHelper", "  - Текущее время: $now")
        android.util.Log.d("MedicineStatusHelper", "  - Время приема: $medicineTime")
        android.util.Log.d("MedicineStatusHelper", "  - Время с буфером: $overdueTime")
        android.util.Log.d("MedicineStatusHelper", "  - Текущее время после времени с буфером: ${now.isAfter(overdueTime)}")
        
        val isOverdue = now.isAfter(overdueTime)
        android.util.Log.d("MedicineStatusHelper", "  - ПРОСРОЧЕНО: $isOverdue")
        
        return isOverdue
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
        android.util.Log.d("MedicineStatusHelper", "=== ОПРЕДЕЛЕНИЕ СТАТУСА ===")
        android.util.Log.d("MedicineStatusHelper", "Лекарство: ${medicine.name}")
        android.util.Log.d("MedicineStatusHelper", "  - ID: ${medicine.id}")
        android.util.Log.d("MedicineStatusHelper", "  - Активно: ${medicine.isActive}")
        android.util.Log.d("MedicineStatusHelper", "  - Принято сегодня: ${medicine.takenToday}")
        android.util.Log.d("MedicineStatusHelper", "  - Время приема: ${medicine.time}")
        android.util.Log.d("MedicineStatusHelper", "  - Текущее время: ${LocalTime.now()}")
        android.util.Log.d("MedicineStatusHelper", "  - Частота: ${medicine.frequency}")
        android.util.Log.d("MedicineStatusHelper", "  - Дата начала: ${medicine.startDate}")
        android.util.Log.d("MedicineStatusHelper", "  - groupId: ${medicine.groupId}")
        android.util.Log.d("MedicineStatusHelper", "  - groupName: ${medicine.groupName}")
        android.util.Log.d("MedicineStatusHelper", "  - groupOrder: ${medicine.groupOrder}")
        android.util.Log.d("MedicineStatusHelper", "  - groupFrequency: ${medicine.groupFrequency}")
        
        val shouldTake = shouldTakeToday(medicine)
        val overdue = isOverdue(medicine)
        
        android.util.Log.d("MedicineStatusHelper", "  - По расписанию сегодня: $shouldTake")
        android.util.Log.d("MedicineStatusHelper", "  - Просрочено: $overdue")
        
        val status = when {
            !medicine.isActive -> {
                android.util.Log.d("MedicineStatusHelper", "  - СТАТУС: NOT_TODAY (не активно)")
                android.util.Log.d("MedicineStatusHelper", "  - ПРИЧИНА: isActive = false")
                MedicineStatus.NOT_TODAY
            }
            medicine.takenToday -> {
                android.util.Log.d("MedicineStatusHelper", "  - СТАТУС: TAKEN_TODAY (уже принято)")
                MedicineStatus.TAKEN_TODAY
            }
            overdue -> {
                android.util.Log.d("MedicineStatusHelper", "  - СТАТУС: OVERDUE (просрочено)")
                MedicineStatus.OVERDUE
            }
            shouldTake -> {
                android.util.Log.d("MedicineStatusHelper", "  - СТАТУС: UPCOMING (предстоит)")
                MedicineStatus.UPCOMING
            }
            else -> {
                android.util.Log.d("MedicineStatusHelper", "  - СТАТУС: NOT_TODAY (не по расписанию)")
                android.util.Log.d("MedicineStatusHelper", "  - ПРИЧИНА: shouldTakeToday = false")
                android.util.Log.d("MedicineStatusHelper", "  - ДЕТАЛИ: частота=${medicine.frequency}, группа=${medicine.groupId != null}")
                MedicineStatus.NOT_TODAY
            }
        }
        
        android.util.Log.d("MedicineStatusHelper", "  - ФИНАЛЬНЫЙ СТАТУС: $status")
        return status
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
        val currentTime = System.currentTimeMillis()
        android.util.Log.d("MedicineStatusHelper", "=== ОТМЕТКА КАК ПРИНЯТОЕ ===")
        android.util.Log.d("MedicineStatusHelper", "Лекарство: ${medicine.name}")
        android.util.Log.d("MedicineStatusHelper", "  - Старый lastTakenTime: ${medicine.lastTakenTime}")
        android.util.Log.d("MedicineStatusHelper", "  - Новый lastTakenTime: $currentTime")
        
        return medicine.copy(
            takenToday = true,
            takenAt = currentTime,
            lastTakenTime = currentTime, // ДОБАВЛЕНО: обновляем lastTakenTime
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

 