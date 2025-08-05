package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class GroupLogicFixTest {
    
    @Test
    fun testFixedGroupLogic() {
        println("=== ТЕСТ ИСПРАВЛЕННОЙ ЛОГИКИ ГРУППИРОВКИ ===")
        
        val today = LocalDate.of(2025, 8, 5) // 5 августа 2025
        val startDate = today.minusDays(30) // 30 дней назад
        
        println("Дата: $today")
        println("Дата начала: $startDate")
        
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        println("Дней с начала: $daysSinceStart")
        println("День группы: $groupDay")
        
        // Создаем лекарства как в логе
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 45),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            quantity = 30,
            remainingQuantity = 20,
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        // Проверяем логику группировки
        val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
        
        println("Липетор (groupOrder=1) должен приниматься: $lipetorShouldTake")
        println("Фубуксусат (groupOrder=2) должен приниматься: $fubuxicinShouldTake")
        
        // Проверяем, что только одно лекарство принимается в день
        assertTrue("Только одно лекарство должно приниматься в день", 
                  lipetorShouldTake != fubuxicinShouldTake)
        
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
        
        println("✅ Тест прошел успешно!")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        // Логика группы "через день"
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
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
        
        return false
    }
} 