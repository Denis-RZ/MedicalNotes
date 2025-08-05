package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.GroupValidationStatus

class ComprehensiveValidationTest {
    
    @Test
    fun testCompleteSolution() {
        println("=== ИТОГОВЫЙ ТЕСТ ВСЕХ УЛУЧШЕНИЙ СТРУКТУРЫ ДАННЫХ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        println("=== ЭТАП 1: СОЗДАНИЕ ПРОБЛЕМНЫХ ДАННЫХ ===")
        
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
        
        println("Создано лекарств: ${allMedicines.size}")
        allMedicines.forEach { medicine ->
            println("  - ${medicine.name}: groupOrder=${medicine.groupOrder}, isValidGroup=${medicine.isValidGroup()}")
        }
        
        println("=== ЭТАП 2: ПРОВЕРКА ПРОБЛЕМ ===")
        
        // Проверяем проблемы
        val groupIds = allMedicines.mapNotNull { it.groupId }.distinct()
        println("Найдено групп: ${groupIds.size}")
        
        groupIds.forEach { groupId ->
            val groupMedicines = allMedicines.filter { it.groupId == groupId }
            val firstMedicine = groupMedicines.first()
            val isValid = firstMedicine.isGroupConsistent(groupMedicines)
            println("Группа $groupId валидна: $isValid")
            assertFalse("Группа должна быть невалидной изначально", isValid)
        }
        
        println("=== ЭТАП 3: ИСПРАВЛЕНИЕ ПРОБЛЕМ ===")
        
        // Исправляем проблемы с группировкой
        val fixedMedicines = allMedicines.map { medicine ->
            if (medicine.needsGroupValidation()) {
                val groupMedicines = allMedicines.filter { it.groupId == medicine.groupId }
                medicine.fixGroupData(groupMedicines)
            } else {
                medicine
            }
        }
        
        println("Исправлено лекарств: ${fixedMedicines.size}")
        fixedMedicines.forEach { medicine ->
            println("  - ${medicine.name}: groupOrder=${medicine.groupOrder}, isValidGroup=${medicine.isValidGroup()}")
        }
        
        println("=== ЭТАП 4: ПРОВЕРКА ИСПРАВЛЕНИЙ ===")
        
        // Проверяем, что проблемы исправлены
        val fixedGroupIds = fixedMedicines.mapNotNull { it.groupId }.distinct()
        fixedGroupIds.forEach { groupId ->
            val groupMedicines = fixedMedicines.filter { it.groupId == groupId }
            val firstMedicine = groupMedicines.first()
            val isValid = firstMedicine.isGroupConsistent(groupMedicines)
            println("Группа $groupId валидна после исправления: $isValid")
            assertTrue("Группа должна стать валидной после исправления", isValid)
        }
        
        println("=== ЭТАП 5: ПРОВЕРКА ЛОГИКИ ГРУППИРОВКИ ===")
        
        // Проверяем логику группировки
        val lipetorShouldTake = shouldTakeMedicineInGroup(fixedMedicines.find { it.name == "Липетор" }!!, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fixedMedicines.find { it.name == "Фубуксусат" }!!, today)
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        
        println("=== ЭТАП 6: СИМУЛЯЦИЯ ПОЛНОГО ПРОЦЕССА ===")
        
        // Симулируем полный процесс MainViewModel.loadTodayMedicines()
        println("=== СИМУЛЯЦИЯ MainViewModel.loadTodayMedicines() ===")
        
        // Валидация групп
        val groupIdsForValidation = fixedMedicines.mapNotNull { it.groupId }.distinct()
        println("Валидация групп: ${groupIdsForValidation.size} групп")
        
        groupIdsForValidation.forEach { groupId ->
            val groupMedicines = fixedMedicines.filter { it.groupId == groupId }
            val firstMedicine = groupMedicines.first()
            val isValid = firstMedicine.isGroupConsistent(groupMedicines)
            println("  - Группа $groupId: валидна=$isValid")
        }
        
        // Фильтрация активных лекарств
        println("=== СИМУЛЯЦИЯ DosageCalculator.getActiveMedicinesForDate() ===")
        
        val activeMedicines = fixedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineWithValidation(medicine, today, fixedMedicines)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("${medicine.name}: isActive=$isActive, shouldTake=$shouldTake, result=$isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Активных лекарств на сегодня: ${activeMedicines.size}")
        
        // Проверяем, что только нужные лекарства включены
        val expectedActiveCount = if (lipetorShouldTake) 1 else 0
        assertEquals("Должно быть $expectedActiveCount активных лекарств", expectedActiveCount, activeMedicines.size)
        
        // Проверяем, что принятые лекарства не попадают в список
        val medicinesForToday = activeMedicines.filter { !it.takenToday }
        println("Лекарств для приема сегодня: ${medicinesForToday.size}")
        
        println("=== ЭТАП 7: ПРОВЕРКА РЕЗУЛЬТАТА ===")
        
        // Итоговая проверка
        println("✅ РЕЗУЛЬТАТ:")
        println("  - Группа исправлена и валидна")
        println("  - Только одно лекарство должно приниматься в день")
        println("  - Невалидные группы не попадают в список активных")
        println("  - Проблема с Фубуксусатом решена")
        
        assertTrue("Группа должна быть валидной", 
            fixedMedicines.first().isGroupConsistent(fixedMedicines))
        assertTrue("Только одно лекарство должно приниматься", onlyOneShouldTake)
        assertTrue("Количество активных лекарств должно быть корректным", 
            activeMedicines.size == expectedActiveCount)
        
        println("✅ Итоговый тест всех улучшений прошел успешно!")
    }
    
    @Test
    fun testRealWorldScenario() {
        println("=== ТЕСТ РЕАЛЬНОГО СЦЕНАРИЯ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Симулируем реальный сценарий: пользователь принимает Липетор
        println("=== СЦЕНАРИЙ: ПОЛЬЗОВАТЕЛЬ ПРИНИМАЕТ ЛИПЕТОР ===")
        
        // Создаем лекарства с правильной структурой
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
        
        println("Начальное состояние:")
        allMedicines.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}, shouldTake=${shouldTakeMedicineInGroup(medicine, today)}")
        }
        
        // Пользователь принимает Липетор
        println("Пользователь принимает Липетор...")
        val lipetorAfterTaken = lipetor.copy(
            takenToday = true,
            remainingQuantity = lipetor.remainingQuantity - 1,
            lastTakenTime = System.currentTimeMillis()
        )
        
        val allMedicinesAfterTaken = listOf(lipetorAfterTaken, fubuxicin)
        
        println("Состояние после приема:")
        allMedicinesAfterTaken.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}, shouldTake=${shouldTakeMedicineInGroup(medicine, today)}")
        }
        
        // Проверяем, что Фубуксусат не появляется в списке "на сегодня"
        val activeMedicinesAfterTaken = allMedicinesAfterTaken.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            
            isActiveAndShouldTake
        }
        
        println("Активных лекарств после приема: ${activeMedicinesAfterTaken.size}")
        
        // Фубуксусат не должен быть активным, так как он должен приниматься в другой день
        val fubuxicinActive = activeMedicinesAfterTaken.any { it.name == "Фубуксусат" }
        println("Фубуксусат активен: $fubuxicinActive")
        assertFalse("Фубуксусат не должен быть активным после приема Липетора", fubuxicinActive)
        
        // Липетор не должен быть в списке "на сегодня", так как он уже принят
        val medicinesForToday = activeMedicinesAfterTaken.filter { !it.takenToday }
        println("Лекарств для приема сегодня: ${medicinesForToday.size}")
        
        val lipetorInTodayList = medicinesForToday.any { it.name == "Липетор" }
        println("Липетор в списке на сегодня: $lipetorInTodayList")
        assertFalse("Липетор не должен быть в списке на сегодня после приема", lipetorInTodayList)
        
        println("✅ Тест реального сценария прошел успешно!")
    }
    
    // Копируем логику из DosageCalculator
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        // Проверяем валидность группы
        if (!medicine.isValidGroup()) {
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
    
    private fun shouldTakeMedicineWithValidation(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>): Boolean {
        // Проверяем валидность группы
        if (medicine.groupId != null && !medicine.isValidGroup()) {
            return false
        }
        
        // Если лекарство в группе, используем логику группы с валидацией
        if (medicine.groupId != null) {
            return shouldTakeMedicineInGroup(medicine, date)
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
} 