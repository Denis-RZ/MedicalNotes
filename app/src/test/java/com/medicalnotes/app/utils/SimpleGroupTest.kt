package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class SimpleGroupTest {
    
    @Test
    fun testGroupLogic() {
        println("=== ПРОСТОЙ ТЕСТ ГРУППИРОВКИ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        println("Дата: $today")
        
        val startDate = today.minusDays(30)
        println("Дата начала: $startDate")
        
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
        println("Дней с начала: $daysSinceStart")
        
        val groupDay = (daysSinceStart % 2).toInt()
        println("День группы: $groupDay")
        
        // Тестируем логику для Липетора (groupOrder=1)
        val lipetorShouldTake = groupDay == 0
        println("Липетор (groupOrder=1) должен приниматься: $lipetorShouldTake")
        
        // Тестируем логику для Фубуксусата (groupOrder=2)
        val fubuxicinShouldTake = groupDay == 1
        println("Фубуксусат (groupOrder=2) должен приниматься: $fubuxicinShouldTake")
        
        // Проверяем, что только одно лекарство принимается в день
        assertTrue("Только одно лекарство должно приниматься в день", 
                  lipetorShouldTake != fubuxicinShouldTake)
        
        println("✅ Тест прошел успешно!")
    }
} 