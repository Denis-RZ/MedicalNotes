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
class DebugTest {

    @Test
    fun testDebugIssue() {
        println("=== ДЕБАГ ПРОБЛЕМЫ ===")
        
        // Создаем лекарство точно как в логе пользователя
        val fubuksusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "50 таблеток",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = LocalTime.of(18, 54), // 18:54 из лога
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
        
        println("Лекарство: ${fubuksusat.name}")
        println("  - takenToday: ${fubuksusat.takenToday}")
        println("  - isActive: ${fubuksusat.isActive}")
        println("  - frequency: ${fubuksusat.frequency}")
        println("  - startDate: ${fubuksusat.startDate}")
        println("  - time: ${fubuksusat.time}")
        
        // Проверяем shouldTakeMedicine
        val shouldTake = DosageCalculator.shouldTakeMedicine(fubuksusat, today)
        println("shouldTakeMedicine: $shouldTake")
        
        // Проверяем статус
        val status = DosageCalculator.getMedicineStatus(fubuksusat, today)
        println("Статус: $status")
        
        // Проверяем фильтрацию
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(fubuksusat), today)
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        
        if (todayMedicines.isNotEmpty()) {
            println("✅ Лекарство попало в список!")
            todayMedicines.forEach { medicine ->
                println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
            }
        } else {
            println("❌ Лекарство НЕ попало в список!")
        }
        
        // Проверяем, что логика работает правильно
        assertTrue("Лекарство должно приниматься сегодня", shouldTake)
        assertEquals("Статус должен быть UPCOMING", com.medicalnotes.app.utils.MedicineStatus.UPCOMING, status)
        assertTrue("Лекарство должно быть в списке", todayMedicines.isNotEmpty())
        
        println("=== ДЕБАГ ЗАВЕРШЕН ===")
    }
} 