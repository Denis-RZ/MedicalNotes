package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class DetailedGroupTest {
    
    @Test
    fun testDetailedGroupLogic() {
        println("=== ДЕТАЛЬНЫЙ ТЕСТ ЛОГИКИ ГРУППИРОВКИ ===")
        
        val today = LocalDate.of(2025, 8, 5) // 5 августа 2025
        val startDate = today.minusDays(30) // 30 дней назад
        
        println("Дата: $today")
        println("Дата начала: $startDate")
        
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        println("Дней с начала: $daysSinceStart")
        println("День группы: $groupDay")
        
        // Создаем лекарства как на скриншоте
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54), // 17:54 как на скриншоте
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            quantity = 30,
            remainingQuantity = 19, // 19 таблеток как на скриншоте
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54), // 22:54 как на скриншоте
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        println("=== ПРОВЕРКА ЛОГИКИ ГРУППИРОВКИ ===")
        
        // Проверяем логику группировки
        val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
        
        println("Липетор (groupOrder=1) должен приниматься: $lipetorShouldTake")
        println("Фубуксусат (groupOrder=2) должен приниматься: $fubuxicinShouldTake")
        
        // Проверяем, что только одно лекарство принимается в день
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        
        // Проверяем правильность логики
        if (groupDay == 0) {
            // Четный день группы - должен принимать Липетор (groupOrder=1)
            assertTrue("В четный день группы должен принимать Липетор", lipetorShouldTake)
            assertFalse("В четный день группы не должен принимать Фубуксусат", fubuxicinShouldTake)
            println("✅ Четный день группы - Липетор принимается")
        } else {
            // Нечетный день группы - должен принимать Фубуксусат (groupOrder=2)
            assertFalse("В нечетный день группы не должен принимать Липетор", lipetorShouldTake)
            assertTrue("В нечетный день группы должен принимать Фубуксусат", fubuxicinShouldTake)
            println("✅ Нечетный день группы - Фубуксусат принимается")
        }
        
        println("=== ПРОВЕРКА ПОЛНОЙ ЛОГИКИ ===")
        
        // Проверяем полную логику shouldTakeMedicine
        val lipetorFullShouldTake = shouldTakeMedicine(lipetor, today)
        val fubuxicinFullShouldTake = shouldTakeMedicine(fubuxicin, today)
        
        println("Липетор (полная логика) должен приниматься: $lipetorFullShouldTake")
        println("Фубуксусат (полная логика) должен приниматься: $fubuxicinFullShouldTake")
        
        // Проверяем, что результаты совпадают
        assertEquals("Результаты группировки и полной логики для Липетора должны совпадать", 
                    lipetorShouldTake, lipetorFullShouldTake)
        assertEquals("Результаты группировки и полной логики для Фубуксусата должны совпадать", 
                    fubuxicinShouldTake, fubuxicinFullShouldTake)
        
        println("✅ Тест прошел успешно!")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        println("=== ГРУППОВАЯ ЛОГИКА ===")
        println("Лекарство: ${medicine.name}")
        println("  - Группа ID: ${medicine.groupId}")
        println("  - Группа: ${medicine.groupName}")
        println("  - Порядок в группе: ${medicine.groupOrder}")
        println("  - Частота: ${medicine.frequency}")
        println("  - Дата начала: $startDate")
        println("  - Проверяемая дата: $date")
        println("  - Дней с начала: $daysSinceStart")
        
        // Логика группы "через день"
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0, 1, 2, 3...)
            val groupDay = (daysSinceStart % 2).toInt()
            
            // Для группы "через день":
            // - Лекарство с groupOrder = 1 принимается в дни 0, 2, 4, 6... (четные дни группы)
            // - Лекарство с groupOrder = 2 принимается в дни 1, 3, 5, 7... (нечетные дни группы)
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false  // Неизвестный порядок
                medicine.groupOrder == 1 -> groupDay == 0  // Первое лекарство в четные дни группы
                medicine.groupOrder == 2 -> groupDay == 1  // Второе лекарство в нечетные дни группы
                else -> false  // Неподдерживаемый порядок
            }
            
            println("  - День группы: $groupDay")
            println("  - Порядок лекарства: ${medicine.groupOrder}")
            println("  - Нужно принимать: $shouldTake")
            println("  - Логика: groupOrder=${medicine.groupOrder}, groupDay=$groupDay")
            return shouldTake
        }
        
        return false
    }
    
    private fun shouldTakeMedicine(medicine: Medicine, date: LocalDate): Boolean {
        // Имитируем логику из DosageCalculator.shouldTakeMedicine
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        // Если дата раньше начала приема
        if (date.isBefore(startDate)) {
            return false
        }
        
        // Если лекарство в группе, используем логику группы
        if (medicine.groupId != null) {
            return shouldTakeMedicineInGroup(medicine, date)
        }
        
        // Обычная логика для лекарств не в группе
        return when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            else -> false
        }
    }
} 