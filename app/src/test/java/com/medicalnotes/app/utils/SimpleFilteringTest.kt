package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class SimpleFilteringTest {

    @Test
    fun testFilteringLogic() {
        println("=== ПРОСТОЙ ТЕСТ ФИЛЬТРАЦИИ ===")
        
        // Создаем лекарство, которое НЕ принято сегодня
        val medicineNotTaken = Medicine(
            id = 1L,
            name = "Лекарство не принято",
            dosage = "10 таблеток",
            quantity = 10,
            remainingQuantity = 10,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicineNotTaken), today)
        
        println("Лекарство takenToday: ${medicineNotTaken.takenToday}")
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        
        // Лекарство должно быть в списке, так как takenToday = false
        assertEquals("Лекарство должно быть в списке", 1, todayMedicines.size)
        assertTrue("Лекарство должно быть в списке", todayMedicines.any { it.name == "Лекарство не принято" })
        
        println("✅ ТЕСТ ПРОЙДЕН: Лекарство с takenToday=false попало в список")
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
} 