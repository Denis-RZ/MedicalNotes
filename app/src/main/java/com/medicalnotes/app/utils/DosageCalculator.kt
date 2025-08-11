package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import android.content.Context

object DosageCalculator {
    
    // Унифицированные временные пороги
    private const val OVERDUE_BUFFER_MINUTES = 5L  // Reduced from 15 to 5 minutes for faster overdue detection
    private const val OVERDUE_BUFFER_SECONDS = OVERDUE_BUFFER_MINUTES * 60L
    private const val MAX_OVERDUE_HOURS = 24L  // Maximum hours to consider overdue (24 hours = full day)
    private const val OVERDUE_BUFFER_MILLIS = OVERDUE_BUFFER_SECONDS * 1000L
    
    // Статусы лекарств
    enum class MedicineStatus {
        NOT_TODAY,      // Не сегодня
        UPCOMING,       // Предстоит сегодня
        OVERDUE,        // Просрочено
        TAKEN_TODAY     // Принято сегодня
    }
    
    /**
     * Проверяет, нужно ли принимать лекарство в указанную дату с валидацией групп
     */
    fun shouldTakeMedicine(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>? = null): Boolean {
        // ИСПРАВЛЕНО: Правильное преобразование миллисекунд в LocalDate
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        
        android.util.Log.d("DosageCalculator", "=== ПРОВЕРКА ЛЕКАРСТВА ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "  - Дата: $date")
        android.util.Log.d("DosageCalculator", "  - startDate: $startDate")
        android.util.Log.d("DosageCalculator", "  - groupId: ${medicine.groupId}")
        android.util.Log.d("DosageCalculator", "  - groupName: ${medicine.groupName}")
        android.util.Log.d("DosageCalculator", "  - groupOrder: ${medicine.groupOrder}")
        android.util.Log.d("DosageCalculator", "  - frequency: ${medicine.frequency}")
        
        // Если дата раньше начала приема
        if (date.isBefore(startDate)) {
            android.util.Log.d("DosageCalculator", "  - Дата раньше начала приема, возвращаем false")
            return false
        }
        
        // Если лекарство в группе, используем логику группы с валидацией
        if (medicine.groupId != null) {
            android.util.Log.d("DosageCalculator", "  - Лекарство в группе, используем групповую логику")
            return shouldTakeMedicineInGroup(medicine, date, allMedicines)
        }
        
        // Обычная логика для лекарств не в группе
        android.util.Log.d("DosageCalculator", "  - Лекарство НЕ в группе, используем обычную логику")
        val result = when (medicine.frequency) {
            DosageFrequency.DAILY -> {
                android.util.Log.d("DosageCalculator", "  - Частота DAILY, возвращаем true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                val shouldTake = daysSinceStart % 2L == 0L
                android.util.Log.d("DosageCalculator", "  - Частота EVERY_OTHER_DAY, дней с начала: $daysSinceStart, должно принимать: $shouldTake")
                shouldTake
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                val shouldTake = daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
                android.util.Log.d("DosageCalculator", "  - Частота TWICE_A_WEEK, дней с начала: $daysSinceStart, должно принимать: $shouldTake")
                shouldTake
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                val shouldTake = daysSinceStart % 2L == 0L
                android.util.Log.d("DosageCalculator", "  - Частота THREE_TIMES_A_WEEK, дней с начала: $daysSinceStart, должно принимать: $shouldTake")
                shouldTake
            }
            DosageFrequency.WEEKLY -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                val shouldTake = daysSinceStart % 7L == 0L
                android.util.Log.d("DosageCalculator", "  - Частота WEEKLY, дней с начала: $daysSinceStart, должно принимать: $shouldTake")
                shouldTake
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                val shouldTake = medicine.customDays.contains(dayOfWeek)
                android.util.Log.d("DosageCalculator", "  - Частота CUSTOM, день недели: $dayOfWeek, должно принимать: $shouldTake")
                shouldTake
            }
        }
        android.util.Log.d("DosageCalculator", "  - Итоговый результат: $result")
        return result
    }
    

    
    /**
     * Получает времена приема для указанной даты
     */
    fun getDoseTimesForDate(medicine: Medicine, date: LocalDate): List<LocalTime> {
        if (!shouldTakeMedicine(medicine, date, null)) {
            return emptyList()
        }
        
        return if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
            medicine.doseTimes
        } else {
            listOf(medicine.time)
        }
    }
    
    /**
     * Получает активные лекарства для указанной даты (для календаря - без фильтрации по takenToday)
     */
    fun getActiveMedicinesForDateForCalendar(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        android.util.Log.d("DosageCalculator", "Фильтрация лекарств для календаря: ${medicines.size} лекарств для даты $date")
        
        android.util.Log.d("DosageCalculator", "=== ФИЛЬТРАЦИЯ АКТИВНЫХ ЛЕКАРСТВ ДЛЯ КАЛЕНДАРЯ ===")
        val activeMedicines = medicines.filter { medicine ->
            android.util.Log.d("DosageCalculator", "🔍 ФИЛЬТРАЦИЯ КАЛЕНДАРЬ: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - groupId: ${medicine.groupId}")
            android.util.Log.d("DosageCalculator", "  - groupName: ${medicine.groupName}")
            android.util.Log.d("DosageCalculator", "  - groupOrder: ${medicine.groupOrder}")
            android.util.Log.d("DosageCalculator", "  - groupStartDate: ${medicine.groupStartDate}")
            android.util.Log.d("DosageCalculator", "  - groupFrequency: ${medicine.groupFrequency}")
            
            val isActive = medicine.isActive
            android.util.Log.d("DosageCalculator", "  - isActive: $isActive")
            
            android.util.Log.d("DosageCalculator", "  - ВЫЗЫВАЕМ shouldTakeMedicine()")
            val shouldTake = shouldTakeMedicine(medicine, date, medicines)
            android.util.Log.d("DosageCalculator", "  - shouldTake: $shouldTake")
            
            val isActiveAndShouldTake = isActive && shouldTake
            android.util.Log.d("DosageCalculator", "  - isActiveAndShouldTake: $isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        android.util.Log.d("DosageCalculator", "Активных лекарств для календаря: ${activeMedicines.size}")
        
        // ИСПРАВЛЕНО: Для календаря НЕ фильтруем по takenToday
        // Это позволяет видеть все лекарства, которые должны приниматься в указанную дату
        val medicinesForCalendar = activeMedicines
        
        android.util.Log.d("DosageCalculator", "Результат для календаря: ${medicinesForCalendar.size} лекарств")
        
        // ДОБАВЛЕНО: Подробное логирование для отладки
        activeMedicines.forEach { medicine ->
            android.util.Log.d("DosageCalculator", "🔍 КАЛЕНДАРЬ: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - takenToday: ${medicine.takenToday}")
            android.util.Log.d("DosageCalculator", "  - lastTakenTime: ${medicine.lastTakenTime}")
            android.util.Log.d("DosageCalculator", "  - В списке календаря: ${medicinesForCalendar.contains(medicine)}")
        }
        
        return medicinesForCalendar
    }

    /**
     * Получает активные лекарства для указанной даты
     */
    fun getActiveMedicinesForDate(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        //  ИСПРАВЛЕНО: Убрано избыточное логирование для предотвращения ANR
        android.util.Log.d("DosageCalculator", "Фильтрация лекарств: ${medicines.size} лекарств для даты $date")
        android.util.Log.e("DosageCalculator", "📋 DosageCalculator: Фильтрация ${medicines.size} лекарств для даты $date")
        
        android.util.Log.d("DosageCalculator", "=== ФИЛЬТРАЦИЯ АКТИВНЫХ ЛЕКАРСТВ С ВАЛИДАЦИЕЙ ГРУПП ===")
        val activeMedicines = medicines.filter { medicine ->
            android.util.Log.d("DosageCalculator", "🔍 ФИЛЬТРАЦИЯ: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - groupId: ${medicine.groupId}")
            android.util.Log.d("DosageCalculator", "  - groupName: ${medicine.groupName}")
            android.util.Log.d("DosageCalculator", "  - groupOrder: ${medicine.groupOrder}")
            android.util.Log.d("DosageCalculator", "  - groupStartDate: ${medicine.groupStartDate}")
            android.util.Log.d("DosageCalculator", "  - groupFrequency: ${medicine.groupFrequency}")
            
            val isActive = medicine.isActive
            android.util.Log.d("DosageCalculator", "  - isActive: $isActive")
            
            android.util.Log.d("DosageCalculator", "  - ВЫЗЫВАЕМ shouldTakeMedicine()")
            val shouldTake = shouldTakeMedicine(medicine, date, medicines)
            android.util.Log.d("DosageCalculator", "  - shouldTake: $shouldTake")
            
            val isActiveAndShouldTake = isActive && shouldTake
            android.util.Log.d("DosageCalculator", "  - isActiveAndShouldTake: $isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        android.util.Log.d("DosageCalculator", "Активных лекарств: ${activeMedicines.size}")
        android.util.Log.e("DosageCalculator", "📋 DosageCalculator: Активных лекарств: ${activeMedicines.size}")
        
        // ИСПРАВЛЕНО: Принятые лекарства должны исчезать из списка "на сегодня"
        val medicinesForToday = activeMedicines.filter { medicine ->
            // ИСПРАВЛЕНО: Используем takenToday вместо lastTakenTime для более точной проверки
            !medicine.takenToday
        }
        
        android.util.Log.d("DosageCalculator", "Результат: ${medicinesForToday.size} лекарств на сегодня")
        android.util.Log.e("DosageCalculator", "📋 DosageCalculator: Результат: ${medicinesForToday.size} лекарств на сегодня")
        
        //  ДОБАВЛЕНО: Подробное логирование для отладки
        activeMedicines.forEach { medicine ->
            android.util.Log.d("DosageCalculator", "🔍 ФИЛЬТРАЦИЯ: ${medicine.name}")
            android.util.Log.d("DosageCalculator", "  - takenToday: ${medicine.takenToday}")
            android.util.Log.d("DosageCalculator", "  - lastTakenTime: ${medicine.lastTakenTime}")
            android.util.Log.d("DosageCalculator", "  - В списке 'на сегодня': ${medicinesForToday.contains(medicine)}")
            android.util.Log.d("DosageCalculator", "  - Причина исключения: ${if (!medicinesForToday.contains(medicine)) "takenToday = true" else "включено"}")
        }
        
        // ДОБАВЛЕНО: Логирование всех лекарств, которые не прошли фильтрацию
        medicines.forEach { medicine ->
            if (!activeMedicines.contains(medicine)) {
                android.util.Log.e("DosageCalculator", "❌❌❌ ИСКЛЮЧЕНО: ${medicine.name} ❌❌❌")
                android.util.Log.e("DosageCalculator", "  - isActive: ${medicine.isActive}")
                android.util.Log.e("DosageCalculator", "  - shouldTakeMedicine: ${shouldTakeMedicine(medicine, date, medicines)}")
                android.util.Log.e("DosageCalculator", "  - takenToday: ${medicine.takenToday}")
                android.util.Log.e("DosageCalculator", "  - Причина: ${if (!medicine.isActive) "не активно" else if (!shouldTakeMedicine(medicine, date, medicines)) "не должно приниматься сегодня" else "неизвестно"}")
            }
        }
        
        return medicinesForToday
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
        // Получаем контекст для доступа к ресурсам
        val context = com.medicalnotes.app.MedicalNotesApplication.instance
        return getDosageDescription(medicine, context)
    }
    
    /**
     * Получает описание схемы приема для отображения с указанным контекстом
     */
    fun getDosageDescription(medicine: Medicine, context: Context): String {
        val frequencyText = when (medicine.frequency) {
            DosageFrequency.DAILY -> context.getString(com.medicalnotes.app.R.string.frequency_daily)
            DosageFrequency.EVERY_OTHER_DAY -> context.getString(com.medicalnotes.app.R.string.frequency_every_other_day)
            DosageFrequency.TWICE_A_WEEK -> context.getString(com.medicalnotes.app.R.string.frequency_twice_a_week)
            DosageFrequency.THREE_TIMES_A_WEEK -> context.getString(com.medicalnotes.app.R.string.frequency_three_times_a_week)
            DosageFrequency.WEEKLY -> context.getString(com.medicalnotes.app.R.string.frequency_weekly)
            DosageFrequency.CUSTOM -> context.getString(com.medicalnotes.app.R.string.frequency_custom)
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
     * Единый метод для определения просрочки
     */
    fun isMedicineOverdue(medicine: Medicine, date: LocalDate = LocalDate.now()): Boolean {
        // ИСПРАВЛЕНО: Принятые лекарства не могут быть просроченными
        if (medicine.takenToday) {
            android.util.Log.d("DosageCalculator", "Лекарство ${medicine.name} уже принято сегодня - не просрочено")
            return false
        }
        
        if (!shouldTakeMedicine(medicine, date)) {
            return false
        }
        
        val doseTimes = getDoseTimesForDate(medicine, date)
        val now = java.time.LocalDateTime.now()
        
        return doseTimes.any { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            val timeDiff = java.time.Duration.between(doseDateTime, now)
            
            // ИСПРАВЛЕНО: Используем короткий буфер (5 минут) но ограничиваем максимум 24 часами
            val isOverdue = timeDiff.toMinutes() > OVERDUE_BUFFER_MINUTES && 
                           timeDiff.toHours() <= MAX_OVERDUE_HOURS && 
                           doseDateTime.isBefore(now)
            android.util.Log.d("DosageCalculator", "Время приема: ${doseTime}, прошло ${timeDiff.toMinutes()} минут, просрочено: $isOverdue")
            isOverdue
        }
    }
    
    /**
     * Единый метод для получения статуса
     */
    fun getMedicineStatus(medicine: Medicine, date: LocalDate = LocalDate.now()): MedicineStatus {
        android.util.Log.d("DosageCalculator", "=== ОПРЕДЕЛЕНИЕ СТАТУСА ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "Дата: $date")
        
        if (!shouldTakeMedicine(medicine, date)) {
            android.util.Log.d("DosageCalculator", "Статус: NOT_TODAY (не по расписанию)")
            return MedicineStatus.NOT_TODAY
        }
        
        if (medicine.takenToday) {
            android.util.Log.d("DosageCalculator", "Статус: TAKEN_TODAY (уже принято)")
            return MedicineStatus.TAKEN_TODAY
        }
        
        val doseTimes = getDoseTimesForDate(medicine, date)
        val now = java.time.LocalDateTime.now()
        
        android.util.Log.d("DosageCalculator", "Времена приема: $doseTimes")
        android.util.Log.d("DosageCalculator", "Текущее время: $now")
        
        // Проверяем просроченные приемы
        val overdueDoses = doseTimes.filter { doseTime ->
            val doseDateTime = date.atTime(doseTime)
            val timeDiff = java.time.Duration.between(doseDateTime, now)
            // ИСПРАВЛЕНО: Используем короткий буфер (5 минут) но ограничиваем максимум 24 часами
            val isOverdue = timeDiff.toMinutes() > OVERDUE_BUFFER_MINUTES && 
                           timeDiff.toHours() <= MAX_OVERDUE_HOURS && 
                           doseDateTime.isBefore(now)
            android.util.Log.d("DosageCalculator", "Доза ${doseTime}: время прошло ${timeDiff.toMinutes()} минут, просрочено: $isOverdue")
            isOverdue
        }
        
        android.util.Log.d("DosageCalculator", "Просроченные приемы: $overdueDoses")
        
        return when {
            overdueDoses.isNotEmpty() -> {
                android.util.Log.d("DosageCalculator", "Статус: OVERDUE (просрочено)")
                MedicineStatus.OVERDUE
            }
            doseTimes.any { it.atDate(date).isAfter(now) } -> {
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
     * Проверяет, нужно ли принимать лекарство в группе с валидацией
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>? = null): Boolean {
        android.util.Log.d("DosageCalculator", "=== ГРУППОВАЯ ЛОГИКА С ВАЛИДАЦИЕЙ ===")
        android.util.Log.d("DosageCalculator", "Лекарство: ${medicine.name}")
        android.util.Log.d("DosageCalculator", "  - Группа ID: ${medicine.groupId}")
        android.util.Log.d("DosageCalculator", "  - Группа: ${medicine.groupName}")
        android.util.Log.d("DosageCalculator", "  - Порядок в группе: ${medicine.groupOrder}")
        android.util.Log.d("DosageCalculator", "  - groupStartDate: ${medicine.groupStartDate}")
        android.util.Log.d("DosageCalculator", "  - groupFrequency: ${medicine.groupFrequency}")
        
        // Проверяем валидность группы
        if (!medicine.isValidGroup()) {
            android.util.Log.w("DosageCalculator", "  - Группа невалидна для ${medicine.name}")
            android.util.Log.w("DosageCalculator", "  - isValidGroup() = false")
            return false
        }
        
        // Если переданы все лекарства, проверяем консистентность группы
        if (allMedicines != null) {
            val groupValidationStatus = medicine.getGroupValidationStatus(allMedicines)
            android.util.Log.d("DosageCalculator", "  - Статус валидации группы: $groupValidationStatus")
            
            if (groupValidationStatus != com.medicalnotes.app.models.GroupValidationStatus.VALID) {
                android.util.Log.w("DosageCalculator", "  - Группа невалидна: $groupValidationStatus")
                return false
            }
        }
        
        // ИСПРАВЛЕНО: Используем правильное преобразование groupStartDate
        val startDate = if (medicine.groupStartDate > 0) {
            java.time.Instant.ofEpochMilli(medicine.groupStartDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            // Если groupStartDate не установлен, используем обычный startDate
            java.time.Instant.ofEpochMilli(medicine.startDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
        
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        android.util.Log.d("DosageCalculator", "  - Дата начала группы: $startDate")
        android.util.Log.d("DosageCalculator", "  - Проверяемая дата: $date")
        android.util.Log.d("DosageCalculator", "  - Дней с начала: $daysSinceStart")
        android.util.Log.d("DosageCalculator", "  - Частота группы: ${medicine.groupFrequency}")
        
        // ИСПРАВЛЕНО: Используем индивидуальную частоту лекарства, а не групповую
        // Это критично для правильной работы уведомлений
        val frequencyToUse = medicine.frequency // Используем индивидуальную частоту, не групповую!
        android.util.Log.d("DosageCalculator", "  - Используем индивидуальную частоту: $frequencyToUse (не групповую: ${medicine.groupFrequency})")
        
        if (frequencyToUse == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0, 1, 2, 3...)
            val groupDay = (daysSinceStart % 2).toInt()
            
            // ИСПРАВЛЕНО: Проверяем, было ли лекарство принято вчера
            val yesterday = date.minusDays(1)
            val wasTakenYesterday = if (medicine.lastTakenTime > 0) {
                val lastTakenDate = java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                lastTakenDate == yesterday
            } else {
                false
            }
            
            android.util.Log.d("DosageCalculator", "  - Вчерашняя дата: $yesterday")
            android.util.Log.d("DosageCalculator", "  - Принято вчера: $wasTakenYesterday")
            
            // Для группы "через день":
            // - Лекарство с groupOrder = 1 принимается в дни 0, 2, 4, 6... (четные дни группы)
            // - Лекарство с groupOrder = 2 принимается в дни 1, 3, 5, 7... (нечетные дни группы)
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false  // Неизвестный порядок
                medicine.groupOrder == 1 -> groupDay == 0  // Первое лекарство в четные дни группы
                medicine.groupOrder == 2 -> groupDay == 1  // Второе лекарство в нечетные дни группы
                else -> false  // Неподдерживаемый порядок
            }
            
            // ИСПРАВЛЕНО: Если лекарство было принято вчера и сегодня не должно приниматься по расписанию,
            // то не показываем его как пропущенное
            val finalResult = if (wasTakenYesterday && !shouldTake) {
                android.util.Log.d("DosageCalculator", "  - Лекарство принято вчера и сегодня не по расписанию - не показываем")
                false
            } else if (wasTakenYesterday && shouldTake) {
                // Если лекарство было принято вчера, но сегодня должно приниматься по расписанию,
                // то показываем его (возможно, нужно принять еще раз)
                android.util.Log.d("DosageCalculator", "  - Лекарство принято вчера, но сегодня тоже по расписанию - показываем")
                true
            } else {
                shouldTake
            }
            
            android.util.Log.d("DosageCalculator", "  - День группы: $groupDay")
            android.util.Log.d("DosageCalculator", "  - Порядок лекарства: ${medicine.groupOrder}")
            android.util.Log.d("DosageCalculator", "  - Нужно принимать: $shouldTake")
            android.util.Log.d("DosageCalculator", "  - Итоговый результат: $finalResult")
            android.util.Log.d("DosageCalculator", "  - Логика: groupOrder=${medicine.groupOrder}, groupDay=$groupDay, wasTakenYesterday=$wasTakenYesterday")
            return finalResult
        }
        
        // Для других частот используем обычную логику
        val result = when (medicine.groupFrequency) {
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