package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class ConsoleOutputTest {
    
    @Test
    fun testConsoleOutput() {
        System.out.println("=== ТЕСТ ВЫВОДА В КОНСОЛЬ ===")
        System.out.println("Этот тест должен показать вывод в консоль")
        
        val today = LocalDate.of(2025, 8, 5)
        System.out.println("Сегодня: $today")
        
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = 1751472000000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
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
            startDate = 1751472000000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
        
        System.out.println("Липетор должен приниматься: $lipetorShouldTake")
        System.out.println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        if (lipetorShouldTake && fubuxicinShouldTake) {
            System.out.println("❌ ПРОБЛЕМА: Оба лекарства должны приниматься!")
            System.out.println("Это объясняет проблему на скриншоте")
        } else if (!lipetorShouldTake && !fubuxicinShouldTake) {
            System.out.println("❌ ПРОБЛЕМА: Ни одно лекарство не должно приниматься!")
        } else {
            System.out.println("✅ Логика работает правильно")
        }
        
        System.out.println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        System.out.println("=== ГРУППОВАЯ ЛОГИКА ===")
        System.out.println("Лекарство: ${medicine.name}")
        System.out.println("  - Группа: ${medicine.groupName}")
        System.out.println("  - Порядок в группе: ${medicine.groupOrder}")
        System.out.println("  - Дата начала: $startDate")
        System.out.println("  - Проверяемая дата: $date")
        System.out.println("  - Дней с начала: $daysSinceStart")
        
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder == 1 -> groupDay == 0
                medicine.groupOrder == 2 -> groupDay == 1
                else -> false
            }
            
            System.out.println("  - День группы: $groupDay")
            System.out.println("  - Порядок лекарства: ${medicine.groupOrder}")
            System.out.println("  - Нужно принимать: $shouldTake")
            return shouldTake
        }
        
        return false
    }
} 