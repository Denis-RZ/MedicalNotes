package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class DosageCalculatorDebugTest {

    @Test
    fun testEveryOtherDayLogic() {
        println("=== ТЕСТ ЛОГИКИ EVERY_OTHER_DAY ===")
        
        val today = LocalDate.now()
        println("Сегодня: $today")
        
        // Создаем startDate как сегодня
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        println("startDate (миллисекунды): $startDate")
        
        // Преобразуем обратно в LocalDate для проверки (используем тот же способ, что и в DosageCalculator)
        val startDateLocal = java.time.Instant.ofEpochMilli(startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        println("startDate (LocalDate): $startDateLocal")
        
        // Проверяем, что startDate равен сегодня
        assertEquals("startDate должен быть равен сегодня", today, startDateLocal)
        
        // Создаем лекарство с EVERY_OTHER_DAY
        val medicine = Medicine(
            id = 1L,
            name = "Тест через день",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            isActive = true,
            takenToday = false,
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
        
        println("Лекарство создано:")
        println("  - startDate: ${medicine.startDate}")
        println("  - frequency: ${medicine.frequency}")
        
        // Тестируем shouldTakeMedicine
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
        println("shouldTakeMedicine для сегодня: $shouldTake")
        
        // Проверяем логику вручную
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDateLocal, today)
        println("Дней с начала: $daysSinceStart")
        println("daysSinceStart % 2: ${daysSinceStart % 2L}")
        println("Ожидаемый результат: ${daysSinceStart % 2L == 0L}")
        
        // Проверяем, что логика работает правильно
        assertTrue("Лекарство с startDate = сегодня должно приниматься в первый день", shouldTake)
        
        // Тестируем завтра
        val tomorrow = today.plusDays(1)
        val shouldTakeTomorrow = DosageCalculator.shouldTakeMedicine(medicine, tomorrow)
        println("shouldTakeMedicine для завтра: $shouldTakeTomorrow")
        assertFalse("Лекарство с startDate = сегодня НЕ должно приниматься на следующий день", shouldTakeTomorrow)
        
        // Тестируем послезавтра
        val dayAfterTomorrow = today.plusDays(2)
        val shouldTakeDayAfterTomorrow = DosageCalculator.shouldTakeMedicine(medicine, dayAfterTomorrow)
        println("shouldTakeMedicine для послезавтра: $shouldTakeDayAfterTomorrow")
        assertTrue("Лекарство с startDate = сегодня должно приниматься через день", shouldTakeDayAfterTomorrow)
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
    
    @Test
    fun testStartDateCalculation() {
        println("=== ТЕСТ ВЫЧИСЛЕНИЯ startDate ===")
        
        val today = LocalDate.now()
        println("Сегодня: $today")
        
        // Тестируем разные способы создания startDate
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDateMillis = startOfDay.toInstant().toEpochMilli()
        println("startDate (миллисекунды): $startDateMillis")
        
        // Способ 1: использование Instant (как в DosageCalculator)
        val startDateLocal1 = java.time.Instant.ofEpochMilli(startDateMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        println("Способ 1: $startDateLocal1")
        
        // Способ 2: использование startOfDay
        val startDateLocal2 = startOfDay.toLocalDate()
        println("Способ 2: $startDateLocal2")
        
        assertEquals("Оба способа должны давать одинаковый результат", startDateLocal1, startDateLocal2)
        assertEquals("startDate должен быть равен сегодня", today, startDateLocal1)
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
} 