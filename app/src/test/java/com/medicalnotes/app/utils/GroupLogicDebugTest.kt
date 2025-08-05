package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.GroupMetadata
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class GroupLogicDebugTest {

    @Test
    fun testGroupLogicDebug() {
        println("=== ОТЛАДКА ЛОГИКИ ГРУППЫ 'ТЕСТ' ===")
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        
        println("Сегодня: $today")
        println("startDate: $startDate")
        
        // Создаем группу "Тест" как в реальном приложении
        val groupId = 1L
        val groupName = "Тест"
        val groupStartDate = startDate
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        // Лекарство 1 - порядок 1
        val medicine1 = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "20 мг",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1, // Первое лекарство в группе
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString(),
            groupMetadata = GroupMetadata(
                groupId = groupId,
                groupName = groupName,
                groupSize = 2,
                groupFrequency = groupFrequency,
                groupStartDate = groupStartDate,
                groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
            ),
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Лекарство 2 - порядок 2
        val medicine2 = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "0.5 таблетки",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2, // Второе лекарство в группе
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString(),
            groupMetadata = GroupMetadata(
                groupId = groupId,
                groupName = groupName,
                groupSize = 2,
                groupFrequency = groupFrequency,
                groupStartDate = groupStartDate,
                groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
            ),
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val allMedicines = listOf(medicine1, medicine2)
        
        println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУППЫ ===")
        
        // Проверяем валидность каждого лекарства
        println("Лекарство 1 (${medicine1.name}):")
        println("  - isValidGroup(): ${medicine1.isValidGroup()}")
        println("  - isGroupConsistent(): ${medicine1.isGroupConsistent(allMedicines)}")
        println("  - getGroupValidationStatus(): ${medicine1.getGroupValidationStatus(allMedicines)}")
        
        println("Лекарство 2 (${medicine2.name}):")
        println("  - isValidGroup(): ${medicine2.isValidGroup()}")
        println("  - isGroupConsistent(): ${medicine2.isGroupConsistent(allMedicines)}")
        println("  - getGroupValidationStatus(): ${medicine2.getGroupValidationStatus(allMedicines)}")
        
        // Проверяем логику DosageCalculator
        println("=== ПРОВЕРКА ЛОГИКИ DOSAGECALCULATOR ===")
        
        val shouldTake1 = DosageCalculator.shouldTakeMedicine(medicine1, today, allMedicines)
        val shouldTake2 = DosageCalculator.shouldTakeMedicine(medicine2, today, allMedicines)
        
        println("DosageCalculator.shouldTakeMedicine для ${medicine1.name}: $shouldTake1")
        println("DosageCalculator.shouldTakeMedicine для ${medicine2.name}: $shouldTake2")
        
        // Проверяем getActiveMedicinesForDate
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size} лекарств")
        for (med in activeMedicines) {
            println("  - ${med.name}: ${med.frequency} (порядок в группе: ${med.groupOrder})")
        }
        
        // Проверяем логику для следующих дней
        println("=== ПРОВЕРКА ЛОГИКИ ДЛЯ СЛЕДУЮЩИХ ДНЕЙ ===")
        
        for (dayOffset in 0..3) {
            val testDate = today.plusDays(dayOffset.toLong())
            val shouldTake1Test = DosageCalculator.shouldTakeMedicine(medicine1, testDate, allMedicines)
            val shouldTake2Test = DosageCalculator.shouldTakeMedicine(medicine2, testDate, allMedicines)
            
            println("День +$dayOffset ($testDate):")
            println("  - ${medicine1.name}: $shouldTake1Test")
            println("  - ${medicine2.name}: $shouldTake2Test")
            
            // Проверяем, что не оба лекарства принимаются одновременно
            if (shouldTake1Test && shouldTake2Test) {
                println("  ❌ ПРОБЛЕМА: Оба лекарства принимаются одновременно!")
            } else if (shouldTake1Test || shouldTake2Test) {
                println("  ✅ ОК: Только одно лекарство принимается")
            } else {
                println("  ✅ ОК: Ни одно лекарство не принимается")
            }
        }
        
        // Проверяем проблему с валидацией
        println("=== АНАЛИЗ ПРОБЛЕМЫ ===")
        
        if (!medicine1.isValidGroup() || !medicine2.isValidGroup()) {
            println("❌ ПРОБЛЕМА: Группа невалидна")
            println("  - medicine1.isValidGroup(): ${medicine1.isValidGroup()}")
            println("  - medicine2.isValidGroup(): ${medicine2.isValidGroup()}")
        } else if (medicine1.getGroupValidationStatus(allMedicines) != com.medicalnotes.app.models.GroupValidationStatus.VALID ||
                   medicine2.getGroupValidationStatus(allMedicines) != com.medicalnotes.app.models.GroupValidationStatus.VALID) {
            println("❌ ПРОБЛЕМА: Статус валидации группы не VALID")
            println("  - medicine1.getGroupValidationStatus(): ${medicine1.getGroupValidationStatus(allMedicines)}")
            println("  - medicine2.getGroupValidationStatus(): ${medicine2.getGroupValidationStatus(allMedicines)}")
        } else if (shouldTake1 && shouldTake2) {
            println("❌ ПРОБЛЕМА: Оба лекарства принимаются одновременно")
            println("  - Это происходит потому что логика группировки не работает правильно")
        } else {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО")
        }
        
        // Дополнительная проверка: тестируем без валидации группы
        println("=== ТЕСТ БЕЗ ВАЛИДАЦИИ ГРУППЫ ===")
        
        val shouldTake1NoValidation = DosageCalculator.shouldTakeMedicine(medicine1, today, null)
        val shouldTake2NoValidation = DosageCalculator.shouldTakeMedicine(medicine2, today, null)
        
        println("Без валидации группы:")
        println("  - ${medicine1.name}: $shouldTake1NoValidation")
        println("  - ${medicine2.name}: $shouldTake2NoValidation")
        
        if (shouldTake1NoValidation && shouldTake2NoValidation) {
            println("❌ ПРОБЛЕМА: Даже без валидации оба лекарства принимаются одновременно")
        } else {
            println("✅ Без валидации логика работает правильно")
        }
    }
} 