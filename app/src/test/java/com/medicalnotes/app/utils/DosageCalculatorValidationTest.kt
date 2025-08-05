package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class DosageCalculatorValidationTest {
    
    @Test
    fun testDosageCalculatorWithGroupValidation() {
        println("=== ТЕСТ DOSAGECALCULATOR С ВАЛИДАЦИЕЙ ГРУПП ===")
        
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
        
        println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУПП ===")
        
        // Проверяем валидацию каждого лекарства
        allMedicines.forEach { medicine ->
            println("Лекарство: ${medicine.name}")
            println("  - isValidGroup: ${medicine.isValidGroup()}")
            println("  - groupValidationStatus: ${medicine.getGroupValidationStatus(allMedicines)}")
        }
        
        // Проверяем консистентность группы
        val isGroupConsistent = lipetor.isGroupConsistent(allMedicines)
        println("Группа консистентна: $isGroupConsistent")
        assertTrue("Группа должна быть консистентной", isGroupConsistent)
        
        println("=== ПРОВЕРКА DOSAGECALCULATOR ===")
        
        // Проверяем shouldTakeMedicine для каждого лекарства
        allMedicines.forEach { medicine ->
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today, allMedicines)
            println("${medicine.name} должен приниматься: $shouldTake")
        }
        
        // Проверяем, что только одно лекарство должно приниматься
        val lipetorShouldTake = DosageCalculator.shouldTakeMedicine(lipetor, today, allMedicines)
        val fubuxicinShouldTake = DosageCalculator.shouldTakeMedicine(fubuxicin, today, allMedicines)
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        
        println("=== ПРОВЕРКА getActiveMedicinesForDate ===")
        
        // Проверяем фильтрацию активных лекарств
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("Активных лекарств на сегодня: ${activeMedicines.size}")
        
        activeMedicines.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем, что только нужные лекарства включены
        val expectedActiveCount = if (lipetorShouldTake) 1 else 0
        assertEquals("Должно быть $expectedActiveCount активных лекарств", expectedActiveCount, activeMedicines.size)
        
        println("✅ Тест DosageCalculator с валидацией групп прошел успешно!")
    }
    
    @Test
    fun testDosageCalculatorWithInvalidGroup() {
        println("=== ТЕСТ DOSAGECALCULATOR С НЕВАЛИДНОЙ ГРУППОЙ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарство с невалидной группой
        val invalidMedicine = Medicine(
            id = 1754031172266L,
            name = "Невалидное лекарство",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 0, // Невалидный порядок
            groupStartDate = 0, // Не установлена
            groupFrequency = DosageFrequency.DAILY, // Неправильная частота
            groupValidationHash = "", // Пустой хеш
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val allMedicines = listOf(invalidMedicine)
        
        println("=== ПРОВЕРКА НЕВАЛИДНОЙ ГРУППЫ ===")
        
        println("Лекарство: ${invalidMedicine.name}")
        println("  - isValidGroup: ${invalidMedicine.isValidGroup()}")
        println("  - groupValidationStatus: ${invalidMedicine.getGroupValidationStatus(allMedicines)}")
        
        assertFalse("Группа должна быть невалидной", invalidMedicine.isValidGroup())
        
        println("=== ПРОВЕРКА DOSAGECALCULATOR С НЕВАЛИДНОЙ ГРУППОЙ ===")
        
        // Проверяем shouldTakeMedicine для невалидной группы
        val shouldTake = DosageCalculator.shouldTakeMedicine(invalidMedicine, today, allMedicines)
        println("Невалидное лекарство должно приниматься: $shouldTake")
        
        // Невалидная группа не должна приниматься
        assertFalse("Невалидная группа не должна приниматься", shouldTake)
        
        println("=== ПРОВЕРКА getActiveMedicinesForDate С НЕВАЛИДНОЙ ГРУППОЙ ===")
        
        // Проверяем фильтрацию активных лекарств
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("Активных лекарств на сегодня: ${activeMedicines.size}")
        
        // Невалидные лекарства не должны быть активными
        assertEquals("Невалидные лекарства не должны быть активными", 0, activeMedicines.size)
        
        println("✅ Тест DosageCalculator с невалидной группой прошел успешно!")
    }
    
    @Test
    fun testDosageCalculatorWithoutGroup() {
        println("=== ТЕСТ DOSAGECALCULATOR БЕЗ ГРУППЫ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарство без группы
        val nonGroupMedicine = Medicine(
            id = 1754031172266L,
            name = "Обычное лекарство",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.DAILY,
            startDate = startDateMillis,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val allMedicines = listOf(nonGroupMedicine)
        
        println("=== ПРОВЕРКА ЛЕКАРСТВА БЕЗ ГРУППЫ ===")
        
        println("Лекарство: ${nonGroupMedicine.name}")
        println("  - groupId: ${nonGroupMedicine.groupId}")
        println("  - needsGroupValidation: ${nonGroupMedicine.needsGroupValidation()}")
        
        assertFalse("Лекарство не должно нуждаться в валидации группы", nonGroupMedicine.needsGroupValidation())
        
        println("=== ПРОВЕРКА DOSAGECALCULATOR БЕЗ ГРУППЫ ===")
        
        // Проверяем shouldTakeMedicine для лекарства без группы
        val shouldTake = DosageCalculator.shouldTakeMedicine(nonGroupMedicine, today, allMedicines)
        println("Лекарство без группы должно приниматься: $shouldTake")
        
        // Ежедневное лекарство должно приниматься
        assertTrue("Ежедневное лекарство должно приниматься", shouldTake)
        
        println("=== ПРОВЕРКА getActiveMedicinesForDate БЕЗ ГРУППЫ ===")
        
        // Проверяем фильтрацию активных лекарств
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("Активных лекарств на сегодня: ${activeMedicines.size}")
        
        // Лекарство без группы должно быть активным
        assertEquals("Лекарство без группы должно быть активным", 1, activeMedicines.size)
        
        println("✅ Тест DosageCalculator без группы прошел успешно!")
    }
} 