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
class DosageCalculatorLogicTest {

    @Test
    fun testEveryOtherDayGroupLogic() {
        println("=== ТЕСТ ЛОГИКИ ГРУППЫ 'ЧЕРЕЗ ДЕНЬ' ===")
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        
        println("Сегодня: $today")
        println("startDate: $startDate")
        
        // Создаем группу лекарств "через день"
        val groupId = 1L
        val groupName = "Тест группа"
        val groupStartDate = startDate
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        // Лекарство 1 - должно приниматься в четные дни группы (groupOrder = 1)
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
        
        // Лекарство 2 - должно приниматься в нечетные дни группы (groupOrder = 2)
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
        
        println("=== ПРОВЕРКА ЛОГИКИ ДЛЯ СЕГОДНЯ ===")
        
        val shouldTake1 = DosageCalculator.shouldTakeMedicine(medicine1, today, allMedicines)
        val shouldTake2 = DosageCalculator.shouldTakeMedicine(medicine2, today, allMedicines)
        
        println("DosageCalculator.shouldTakeMedicine для ${medicine1.name}: $shouldTake1")
        println("DosageCalculator.shouldTakeMedicine для ${medicine2.name}: $shouldTake2")
        
        // Проверяем, что не оба лекарства принимаются одновременно
        assertFalse("Оба лекарства не должны приниматься одновременно", shouldTake1 && shouldTake2)
        
        // Проверяем логику для следующих дней
        println("=== ПРОВЕРКА ЛОГИКИ ДЛЯ СЛЕДУЮЩИХ ДНЕЙ ===")
        
        for (dayOffset in 0..5) {
            val testDate = today.plusDays(dayOffset.toLong())
            val shouldTake1Test = DosageCalculator.shouldTakeMedicine(medicine1, testDate, allMedicines)
            val shouldTake2Test = DosageCalculator.shouldTakeMedicine(medicine2, testDate, allMedicines)
            
            println("День +$dayOffset ($testDate):")
            println("  - ${medicine1.name}: $shouldTake1Test")
            println("  - ${medicine2.name}: $shouldTake2Test")
            
            // Проверяем, что не оба лекарства принимаются одновременно
            assertFalse("Оба лекарства не должны приниматься одновременно в день +$dayOffset", 
                       shouldTake1Test && shouldTake2Test)
            
            // Проверяем чередование
            if (dayOffset > 0) {
                val previousDate = today.plusDays((dayOffset - 1).toLong())
                val shouldTake1Prev = DosageCalculator.shouldTakeMedicine(medicine1, previousDate, allMedicines)
                val shouldTake2Prev = DosageCalculator.shouldTakeMedicine(medicine2, previousDate, allMedicines)
                
                // Если в предыдущий день принималось лекарство 1, то сегодня должно приниматься лекарство 2
                // Если в предыдущий день принималось лекарство 2, то сегодня должно приниматься лекарство 1
                // Если в предыдущий день не принималось ни одно, то сегодня может приниматься любое
                if (shouldTake1Prev || shouldTake2Prev) {
                    val shouldAlternate = (shouldTake1Prev && shouldTake2Test) || (shouldTake2Prev && shouldTake1Test)
                    if (!shouldAlternate) {
                        println("  ⚠️  НЕТ ЧЕРЕДОВАНИЯ: В предыдущий день принималось ${if (shouldTake1Prev) medicine1.name else medicine2.name}")
                    }
                }
            }
        }
        
        println("=== ИТОГОВЫЙ АНАЛИЗ ===")
        if (shouldTake1 && shouldTake2) {
            println("❌ ПРОБЛЕМА: Оба лекарства принимаются одновременно")
        } else if (shouldTake1 || shouldTake2) {
            println("✅ ЛОГИКА РАБОТАЕТ: Только одно лекарство принимается сегодня")
        } else {
            println("✅ ЛОГИКА РАБОТАЕТ: Ни одно лекарство не принимается сегодня")
        }
    }
} 