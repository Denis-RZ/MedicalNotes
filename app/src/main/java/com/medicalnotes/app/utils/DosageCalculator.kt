package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object DosageCalculator {
    
    /**
     * Проверяет, нужно ли принимать лекарство в указанную дату
     */
    fun shouldTakeMedicine(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        android.util.Log.d("DosageCalculator", "=== ПРОВЕРКА ЛЕКАРСТВА ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "  - Дата: $date")
        android.util.Log.d("DosageCalculator", "  - Дата начала: $startDate")
        android.util.Log.d("DosageCalculator", "  - Группа ID: ${medicine.groupId}")
        android.util.Log.d("DosageCalculator", "  - Частота: ${medicine.frequency}")
        android.util.Log.d("DosageCalculator", "  - Активно: ${medicine.isActive}")
        android.util.Log.d("DosageCalculator", "  - Принято сегодня: ${medicine.takenToday}")
        
        // Если дата раньше начала приема
        if (date.isBefore(startDate)) {
            android.util.Log.d("DosageCalculator", "  - Результат: false (дата раньше начала)")
            return false
        }
        
        // Если лекарство в группе, используем логику группы
        if (medicine.groupId != null) {
            android.util.Log.d("DosageCalculator", "  - Используем групповую логику")
            return shouldTakeMedicineInGroup(medicine, date)
        }
        
        // Обычная логика для лекарств не в группе
        val result = when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                val shouldTake = daysSinceStart % 2L == 0L
                android.util.Log.d("DosageCalculator", "  - Дней с начала: $daysSinceStart")
                android.util.Log.d("DosageCalculator", "  - Через день: $shouldTake")
                shouldTake
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.WEEKLY -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 7L == 0L
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
        }
        
        android.util.Log.d("DosageCalculator", "  - Результат: $result")
        return result
    }
    

    
    /**
     * Получает времена приема для указанной даты
     */
    fun getDoseTimesForDate(medicine: Medicine, date: LocalDate): List<LocalTime> {
        if (!shouldTakeMedicine(medicine, date)) {
            return emptyList()
        }
        
        return if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
            medicine.doseTimes
        } else {
            listOf(medicine.time)
        }
    }
    
    /**
     * Получает все активные лекарства для указанной даты
     */
    fun getActiveMedicinesForDate(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        android.util.Log.d("DosageCalculator", "=== ФИЛЬТРАЦИЯ ЛЕКАРСТВ ===")
        android.util.Log.d("DosageCalculator", "Дата: $date")
        android.util.Log.d("DosageCalculator", "Всего лекарств: ${medicines.size}")
        
        val activeMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicine(medicine, date)
            android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - Активно: $isActive")
            android.util.Log.d("DosageCalculator", "  - По расписанию: $shouldTake")
            android.util.Log.d("DosageCalculator", "  - Принято сегодня: ${medicine.takenToday}")
            android.util.Log.d("DosageCalculator", "  - Включается в список: ${isActive && shouldTake}")
            isActive && shouldTake
        }
        
        android.util.Log.d("DosageCalculator", "Активных по расписанию: ${activeMedicines.size}")
        
        val notTakenToday = activeMedicines.filter { medicine ->
            // Проверяем, было ли лекарство принято сегодня
            val lastTakenDate = if (medicine.lastTakenTime > 0) {
                java.time.LocalDate.ofEpochDay(medicine.lastTakenTime / (24 * 60 * 60 * 1000))
            } else {
                java.time.LocalDate.MIN
            }
            
            val notTaken = lastTakenDate != date
            android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - Последний прием: $lastTakenDate")
            android.util.Log.d("DosageCalculator", "  - Не принято сегодня: $notTaken")
            notTaken
        }
        
        android.util.Log.d("DosageCalculator", "Не принятых сегодня: ${notTakenToday.size}")
        
        return notTakenToday.map { medicine ->
            // Проверяем статус лекарства для отображения
            val status = getMedicineStatus(medicine, date)
            val isOverdue = status == MedicineStatus.OVERDUE
            android.util.Log.d("DosageCalculator", "Финальное лекарство: ${medicine.name}, статус: $status")
            medicine.copy(isOverdue = isOverdue)
        }
    }
    
    /**
     * Получает лекарства для указанного времени в указанную дату
     */
    fun getMedicinesForTime(medicines: List<Medicine>, date: LocalDate, time: LocalTime): List<Medicine> {
        return getActiveMedicinesForDate(medicines, date).filter { medicine ->
            getDoseTimesForDate(medicine, date).any { doseTime ->
                doseTime.hour == time.hour && doseTime.minute == time.minute
            }
        }
    }
    
    /**
     * Получает следующий день приема для лекарства
     */
    fun getNextDosageDate(medicine: Medicine, fromDate: LocalDate = LocalDate.now()): LocalDate? {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        if (fromDate.isBefore(startDate)) {
            return startDate
        }
        
        var currentDate = fromDate
        repeat(30) { // Ищем в пределах месяца
            if (shouldTakeMedicine(medicine, currentDate)) {
                return currentDate
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return null
    }
    
    /**
     * Получает описание схемы приема для отображения
     */
    fun getDosageDescription(medicine: Medicine): String {
        val frequencyText = when (medicine.frequency) {
            DosageFrequency.DAILY -> "каждый день"
            DosageFrequency.EVERY_OTHER_DAY -> "через день"
            DosageFrequency.TWICE_A_WEEK -> "2 раза в неделю"
            DosageFrequency.THREE_TIMES_A_WEEK -> "3 раза в неделю"
            DosageFrequency.WEEKLY -> "раз в неделю"
            DosageFrequency.CUSTOM -> "по расписанию"
        }
        
        val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
            val times = medicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
            "в ${times.joinToString(", ")}"
        } else {
            "в ${medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
        }
        
        return "$frequencyText $timeText"
    }
    
    /**
     * Проверяет, пропущено ли лекарство (устаревший метод)
     */
    fun isMedicineMissed(medicine: Medicine, date: LocalDate = LocalDate.now()): Boolean {
        return isMedicineOverdue(medicine, date)
    }
    
    /**
     * Проверяет, просрочено ли лекарство
     */
    fun isMedicineOverdue(medicine: Medicine, date: LocalDate = LocalDate.now()): Boolean {
        if (!shouldTakeMedicine(medicine, date)) {
            return false
        }
        
        val doseTimes = getDoseTimesForDate(medicine, date)
        val now = java.time.LocalDateTime.now()
        
        return doseTimes.any { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            val timeDiff = java.time.Duration.between(doseDateTime, now)
            
            // Считаем просроченным, если прошло больше 1 часа после времени приема
            timeDiff.toHours() > 1 && doseDateTime.isBefore(now)
        }
    }
    
    /**
     * Получает статус лекарства для отображения
     */
    fun getMedicineStatus(medicine: Medicine, date: LocalDate = LocalDate.now()): MedicineStatus {
        android.util.Log.d("DosageCalculator", "=== ОПРЕДЕЛЕНИЕ СТАТУСА ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "Дата: $date")
        
        if (!shouldTakeMedicine(medicine, date)) {
            android.util.Log.d("DosageCalculator", "Статус: NOT_TODAY (не по расписанию)")
            return MedicineStatus.NOT_TODAY
        }
        
        // Проверяем, было ли лекарство принято сегодня
        val lastTakenDate = if (medicine.lastTakenTime > 0) {
            java.time.LocalDate.ofEpochDay(medicine.lastTakenTime / (24 * 60 * 60 * 1000))
        } else {
            java.time.LocalDate.MIN
        }
        
        android.util.Log.d("DosageCalculator", "Последний прием: $lastTakenDate")
        
        if (lastTakenDate == date) {
            android.util.Log.d("DosageCalculator", "Статус: TAKEN_TODAY (уже принято)")
            return MedicineStatus.TAKEN_TODAY
        }
        
        val doseTimes = getDoseTimesForDate(medicine, date)
        val now = java.time.LocalDateTime.now()
        
        android.util.Log.d("DosageCalculator", "Времена приема: $doseTimes")
        android.util.Log.d("DosageCalculator", "Текущее время: $now")
        
        // Проверяем, есть ли приемы в будущем сегодня
        val futureDoses = doseTimes.filter { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            doseDateTime.isAfter(now)
        }
        
        // Проверяем, есть ли просроченные приемы
        val overdueDoses = doseTimes.filter { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            val timeDiff = java.time.Duration.between(doseDateTime, now)
            // Считаем просроченным, если прошло больше 1 минуты после времени приема
            timeDiff.toMinutes() > 1 && doseDateTime.isBefore(now)
        }
        
        android.util.Log.d("DosageCalculator", "Будущие приемы: $futureDoses")
        android.util.Log.d("DosageCalculator", "Просроченные приемы: $overdueDoses")
        
        return when {
            overdueDoses.isNotEmpty() -> {
                android.util.Log.d("DosageCalculator", "Статус: OVERDUE (просрочено)")
                MedicineStatus.OVERDUE
            }
            futureDoses.isNotEmpty() -> {
                android.util.Log.d("DosageCalculator", "Статус: UPCOMING (предстоит)")
                MedicineStatus.UPCOMING
            }
            else -> {
                android.util.Log.d("DosageCalculator", "Статус: OVERDUE (время прошло)")
                MedicineStatus.OVERDUE
            }
        }
    }
    
    /**
     * Получает следующее время приема для лекарства
     */
    fun getNextDoseTime(medicine: Medicine, date: LocalDate = LocalDate.now()): LocalTime? {
        if (!shouldTakeMedicine(medicine, date)) {
            return null
        }
        
        val doseTimes = getDoseTimesForDate(medicine, date)
        val now = java.time.LocalDateTime.now()
        
        return doseTimes.find { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            doseDateTime.isAfter(now)
        }
    }
    
    /**
     * Проверяет, нужно ли принимать лекарство в группе
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        android.util.Log.d("DosageCalculator", "=== ГРУППОВАЯ ЛОГИКА ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "  - Группа ID: ${medicine.groupId}")
        android.util.Log.d("DosageCalculator", "  - Группа: ${medicine.groupName}")
        android.util.Log.d("DosageCalculator", "  - Порядок в группе: ${medicine.groupOrder}")
        android.util.Log.d("DosageCalculator", "  - Частота: ${medicine.frequency}")
        android.util.Log.d("DosageCalculator", "  - Дата начала: $startDate")
        android.util.Log.d("DosageCalculator", "  - Проверяемая дата: $date")
        android.util.Log.d("DosageCalculator", "  - Дней с начала: $daysSinceStart")
        
        // Логика группы "через день"
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0 или 1)
            val groupDay = (daysSinceStart % 2).toInt()
            // Лекарство должно приниматься только в свой день группы
            val shouldTake = groupDay == (medicine.groupOrder - 1)
            android.util.Log.d("DosageCalculator", "  - День группы: $groupDay")
            android.util.Log.d("DosageCalculator", "  - Порядок лекарства: ${medicine.groupOrder}")
            android.util.Log.d("DosageCalculator", "  - Нужно принимать: $shouldTake")
            android.util.Log.d("DosageCalculator", "  - Логика: groupDay($groupDay) == (groupOrder-1)(${medicine.groupOrder - 1})")
            return shouldTake
        }
        
        // Для других частот используем обычную логику
        val result = when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.TWICE_A_WEEK -> {
                daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.WEEKLY -> {
                daysSinceStart % 7L == 0L
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
            else -> false
        }
        
        android.util.Log.d("DosageCalculator", "  - Результат для других частот: $result")
        return result
    }
}

enum class MedicineStatus {
    NOT_TODAY,      // Не сегодня
    UPCOMING,       // Предстоит сегодня
    OVERDUE,        // Просрочено
    TAKEN_TODAY     // Принято сегодня
} 