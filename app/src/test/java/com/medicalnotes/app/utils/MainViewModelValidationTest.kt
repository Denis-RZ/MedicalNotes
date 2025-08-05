package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class MainViewModelValidationTest {
    
    @Test
    fun testMainViewModelGroupValidation() {
        println("=== ТЕСТ MAINVIEWMODEL С ВАЛИДАЦИЕЙ ГРУПП ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с проблемами в группировке (как в реальных данных)
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            groupStartDate = 0, // Проблема: не установлена
            groupFrequency = DosageFrequency.DAILY, // Проблема: неправильная частота
            groupValidationHash = "", // Проблема: пустой хеш
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 0, // Проблема: неправильный порядок
            groupStartDate = 0, // Проблема: не установлена
            groupFrequency = DosageFrequency.DAILY, // Проблема: неправильная частота
            groupValidationHash = "", // Проблема: пустой хеш
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val allMedicines = listOf(lipetor, fubuxicin)
        
        println("=== СИМУЛЯЦИЯ loadTodayMedicines() ===")
        
        // Симулируем логику MainViewModel.loadTodayMedicines()
        println("Всего лекарств в базе: ${allMedicines.size}")
        println("Сегодняшняя дата: $today")
        
        // ВАЛИДАЦИЯ И ИСПРАВЛЕНИЕ ГРУПП
        println("=== ВАЛИДАЦИЯ ГРУПП ===")
        val groupIds = allMedicines.mapNotNull { it.groupId }.distinct()
        println("Найдено групп: ${groupIds.size}")
        
        groupIds.forEach { groupId ->
            println("Проверяем группу $groupId")
            val groupMedicines = allMedicines.filter { it.groupId == groupId }
            println("  - Лекарств в группе: ${groupMedicines.size}")
            
            // Проверяем валидность группы
            val firstMedicine = groupMedicines.first()
            val isValid = firstMedicine.isGroupConsistent(groupMedicines)
            println("  - Группа валидна: $isValid")
            
            if (!isValid) {
                println("  - Группа $groupId невалидна, исправляем...")
                
                // Исправляем данные группы
                val fixedMedicines = groupMedicines.map { medicine ->
                    medicine.fixGroupData(groupMedicines)
                }
                
                println("  - Исправлено лекарств: ${fixedMedicines.size}")
                fixedMedicines.forEach { medicine ->
                    println("    - ${medicine.name}: groupOrder=${medicine.groupOrder}, isValidGroup=${medicine.isValidGroup()}")
                }
                
                // Проверяем, что группа стала валидной
                val fixedFirstMedicine = fixedMedicines.first()
                val isFixedValid = fixedFirstMedicine.isGroupConsistent(fixedMedicines)
                println("  - Группа валидна после исправления: $isFixedValid")
                assertTrue("Группа должна стать валидной после исправления", isFixedValid)
            }
        }
        
        println("=== СИМУЛЯЦИЯ getActiveMedicinesForDate ===")
        
        // Симулируем вызов DosageCalculator.getActiveMedicinesForDate
        val todayMedicines = allMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineWithValidation(medicine, today, allMedicines)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("${medicine.name}: isActive=$isActive, shouldTake=$shouldTake, result=$isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Лекарств на сегодня: ${todayMedicines.size}")
        
        // Проверяем, что невалидные группы не попадают в список
        assertEquals("Невалидные группы не должны попадать в список", 0, todayMedicines.size)
        
        println("✅ Тест MainViewModel с валидацией групп прошел успешно!")
    }
    
    @Test
    fun testMainViewModelWithValidGroup() {
        println("=== ТЕСТ MAINVIEWMODEL С ВАЛИДНОЙ ГРУППОЙ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с правильной структурой группы
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "1:Тест:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "1:Тест:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val allMedicines = listOf(lipetor, fubuxicin)
        
        println("=== СИМУЛЯЦИЯ loadTodayMedicines() С ВАЛИДНОЙ ГРУППОЙ ===")
        
        // Симулируем логику MainViewModel.loadTodayMedicines()
        println("Всего лекарств в базе: ${allMedicines.size}")
        println("Сегодняшняя дата: $today")
        
        // ВАЛИДАЦИЯ ГРУПП
        println("=== ВАЛИДАЦИЯ ГРУПП ===")
        val groupIds = allMedicines.mapNotNull { it.groupId }.distinct()
        println("Найдено групп: ${groupIds.size}")
        
        groupIds.forEach { groupId ->
            println("Проверяем группу $groupId")
            val groupMedicines = allMedicines.filter { it.groupId == groupId }
            println("  - Лекарств в группе: ${groupMedicines.size}")
            
            // Проверяем валидность группы
            val firstMedicine = groupMedicines.first()
            val isValid = firstMedicine.isGroupConsistent(groupMedicines)
            println("  - Группа валидна: $isValid")
            assertTrue("Группа должна быть валидной", isValid)
        }
        
        println("=== СИМУЛЯЦИЯ getActiveMedicinesForDate С ВАЛИДНОЙ ГРУППОЙ ===")
        
        // Симулируем вызов DosageCalculator.getActiveMedicinesForDate
        val todayMedicines = allMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineWithValidation(medicine, today, allMedicines)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("${medicine.name}: isActive=$isActive, shouldTake=$shouldTake, result=$isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Лекарств на сегодня: ${todayMedicines.size}")
        
        // Проверяем, что только одно лекарство попадает в список
        assertEquals("Должно быть только одно лекарство в списке", 1, todayMedicines.size)
        
        println("✅ Тест MainViewModel с валидной группой прошел успешно!")
    }
    
    // Копируем логику из DosageCalculator с валидацией
    private fun shouldTakeMedicineWithValidation(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>): Boolean {
        // Проверяем валидность группы
        if (medicine.groupId != null && !medicine.isValidGroup()) {
            return false
        }
        
        // Если лекарство в группе, используем логику группы с валидацией
        if (medicine.groupId != null) {
            return shouldTakeMedicineInGroup(medicine, date, allMedicines)
        }
        
        // Обычная логика для лекарств не в группе
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        // Если дата раньше начала приема
        if (date.isBefore(startDate)) {
            return false
        }
        
        return when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.WEEKLY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 7L == 0L
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
        }
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>): Boolean {
        // Проверяем валидность группы
        if (!medicine.isValidGroup()) {
            return false
        }
        
        // Если переданы все лекарства, проверяем консистентность группы
        val groupValidationStatus = medicine.getGroupValidationStatus(allMedicines)
        if (groupValidationStatus != com.medicalnotes.app.models.GroupValidationStatus.VALID) {
            return false
        }
        
        // Используем groupStartDate вместо startDate для групповых лекарств
        val startDate = LocalDate.ofEpochDay(medicine.groupStartDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        
        // Логика группы "через день"
        if (medicine.groupFrequency == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0, 1, 2, 3...)
            val groupDay = (daysSinceStart % 2).toInt()
            
            // Для группы "через день":
            // - Лекарство с groupOrder = 1 принимается в дни 0, 2, 4, 6... (четные дни группы)
            // - Лекарство с groupOrder = 2 принимается в дни 1, 3, 5, 7... (нечетные дни группы)
            
            return when {
                medicine.groupOrder <= 0 -> false  // Неизвестный порядок
                medicine.groupOrder == 1 -> groupDay == 0  // Первое лекарство в четные дни группы
                medicine.groupOrder == 2 -> groupDay == 1  // Второе лекарство в нечетные дни группы
                else -> false  // Неподдерживаемый порядок
            }
        }
        
        // Для других частот используем обычную логику
        return when (medicine.groupFrequency) {
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
    }
} 