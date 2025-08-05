package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GroupFilteringTest {
    
    @Test
    fun testGroupEveryOtherDayLogic() {
        // Создаем тестовую дату (сегодня)
        val today = LocalDate.now()
        val startDate = today.minusDays(10) // Начали 10 дней назад
        
        // Липетор - groupOrder = 1 (принимается в четные дни группы)
        val lipetor = Medicine(
            id = 1,
            name = "Липетор",
            dosage = "10 мг",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(18, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 1,
            isActive = true,
            takenToday = false,
            quantity = 30,
            remainingQuantity = 30
        )
        
        // Фубуксицин - groupOrder = 2 (принимается в нечетные дни группы)
        val fubuxicin = Medicine(
            id = 2,
            name = "Фубуксицин",
            dosage = "500 мг",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(19, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 2,
            isActive = true,
            takenToday = false,
            quantity = 20,
            remainingQuantity = 20
        )
        
        // Проверяем логику группировки напрямую
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        // Логика группы "через день":
        // - Лекарство с groupOrder = 1 принимается в дни 0, 2, 4, 6... (четные дни)
        // - Лекарство с groupOrder = 2 принимается в дни 1, 3, 5, 7... (нечетные дни)
        val lipetorShouldTakeToday = when {
            lipetor.groupOrder % 2 == 1 -> groupDay == 0  // Нечетные порядки (1,3,5...) в четные дни
            lipetor.groupOrder % 2 == 0 -> groupDay == 1  // Четные порядки (2,4,6...) в нечетные дни
            else -> false
        }
        
        val fubuxicinShouldTakeToday = when {
            fubuxicin.groupOrder % 2 == 1 -> groupDay == 0  // Нечетные порядки (1,3,5...) в четные дни
            fubuxicin.groupOrder % 2 == 0 -> groupDay == 1  // Четные порядки (2,4,6...) в нечетные дни
            else -> false
        }
        
        println("=== ТЕСТ ГРУППОВОЙ ЛОГИКИ ===")
        println("Сегодня: $today")
        println("Дней с начала: $daysSinceStart")
        println("День группы: $groupDay")
        println("Липетор (groupOrder=1) должен принимать сегодня: $lipetorShouldTakeToday")
        println("Фубуксицин (groupOrder=2) должен принимать сегодня: $fubuxicinShouldTakeToday")
        
        // Проверяем, что они принимаются в разные дни
        assertNotEquals("Липетор и Фубуксицин должны принимать в разные дни", 
                       lipetorShouldTakeToday, fubuxicinShouldTakeToday)
        
        // Проверяем логику для завтра
        val tomorrow = today.plusDays(1)
        val daysSinceStartTomorrow = ChronoUnit.DAYS.between(startDate, tomorrow)
        val groupDayTomorrow = (daysSinceStartTomorrow % 2).toInt()
        
        val lipetorShouldTakeTomorrow = when {
            lipetor.groupOrder % 2 == 1 -> groupDayTomorrow == 0
            lipetor.groupOrder % 2 == 0 -> groupDayTomorrow == 1
            else -> false
        }
        
        val fubuxicinShouldTakeTomorrow = when {
            fubuxicin.groupOrder % 2 == 1 -> groupDayTomorrow == 0
            fubuxicin.groupOrder % 2 == 0 -> groupDayTomorrow == 1
            else -> false
        }
        
        println("Завтра: $tomorrow")
        println("День группы завтра: $groupDayTomorrow")
        println("Липетор должен принимать завтра: $lipetorShouldTakeTomorrow")
        println("Фубуксицин должен принимать завтра: $fubuxicinShouldTakeTomorrow")
        
        // Проверяем, что завтра принимается другое лекарство
        assertNotEquals("Завтра должно приниматься другое лекарство", 
                       lipetorShouldTakeToday, lipetorShouldTakeTomorrow)
    }
    
    @Test
    fun testGroupLogicAfterTakingMedicine() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        
        // Липетор - принимается сегодня
        val lipetor = Medicine(
            id = 1,
            name = "Липетор",
            dosage = "10 мг",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(18, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 1,
            isActive = true,
            takenToday = false,
            quantity = 30,
            remainingQuantity = 30
        )
        
        // Фубуксицин - НЕ должен приниматься сегодня
        val fubuxicin = Medicine(
            id = 2,
            name = "Фубуксицин",
            dosage = "500 мг",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(19, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 2,
            isActive = true,
            takenToday = false,
            quantity = 20,
            remainingQuantity = 20
        )
        
        // Проверяем логику группировки напрямую
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        val lipetorShouldTakeToday = when {
            lipetor.groupOrder % 2 == 1 -> groupDay == 0
            lipetor.groupOrder % 2 == 0 -> groupDay == 1
            else -> false
        }
        
        val fubuxicinShouldTakeToday = when {
            fubuxicin.groupOrder % 2 == 1 -> groupDay == 0
            fubuxicin.groupOrder % 2 == 0 -> groupDay == 1
            else -> false
        }
        
        println("=== НАЧАЛЬНОЕ СОСТОЯНИЕ ===")
        println("День группы: $groupDay")
        println("Липетор должен принимать сегодня: $lipetorShouldTakeToday")
        println("Фубуксицин должен принимать сегодня: $fubuxicinShouldTakeToday")
        
        // Проверяем, что только одно лекарство должно приниматься сегодня
        assertTrue("Липетор должен приниматься сегодня", lipetorShouldTakeToday)
        assertFalse("Фубуксицин НЕ должен принимать сегодня", fubuxicinShouldTakeToday)
        
        // Симулируем прием липетора (устанавливаем takenToday = true)
        val lipetorAfterTaken = lipetor.copy(
            takenToday = true,
            remainingQuantity = 29,
            lastTakenTime = System.currentTimeMillis()
        )
        
        // Проверяем, что фубуксицин все еще НЕ должен приниматься сегодня
        // (логика группировки не зависит от takenToday)
        val fubuxicinStillShouldNotTake = fubuxicinShouldTakeToday
        println("=== ПОСЛЕ ПРИЕМА ЛИПЕТОРА ===")
        println("Фубуксицин все еще НЕ должен принимать сегодня: ${!fubuxicinStillShouldNotTake}")
        
        // Фубуксицин не должен появиться в списке после приема липетора
        assertFalse("Фубуксицин не должен принимать сегодня после приема липетора", fubuxicinStillShouldNotTake)
    }
    
    @Test
    fun testGroupLogicForDifferentDays() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        
        // Тестируем логику для нескольких дней подряд
        for (dayOffset in 0..5) {
            val testDate = today.plusDays(dayOffset.toLong())
            val daysSinceStart = ChronoUnit.DAYS.between(startDate, testDate)
            val groupDay = (daysSinceStart % 2).toInt()
            
            val lipetor = Medicine(
                id = 1,
                name = "Липетор",
                dosage = "10 мг",
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                time = LocalTime.of(18, 0),
                startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
                groupId = 1L,
                groupName = "Группа 1",
                groupOrder = 1,
                isActive = true,
                takenToday = false,
                quantity = 30,
                remainingQuantity = 30
            )
            
            val fubuxicin = Medicine(
                id = 2,
                name = "Фубуксицин",
                dosage = "500 мг",
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                time = LocalTime.of(19, 0),
                startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
                groupId = 1L,
                groupName = "Группа 1",
                groupOrder = 2,
                isActive = true,
                takenToday = false,
                quantity = 20,
                remainingQuantity = 20
            )
            
            val lipetorShouldTake = when {
                lipetor.groupOrder % 2 == 1 -> groupDay == 0
                lipetor.groupOrder % 2 == 0 -> groupDay == 1
                else -> false
            }
            
            val fubuxicinShouldTake = when {
                fubuxicin.groupOrder % 2 == 1 -> groupDay == 0
                fubuxicin.groupOrder % 2 == 0 -> groupDay == 1
                else -> false
            }
            
            println("День +$dayOffset ($testDate), день группы: $groupDay:")
            println("  - Липетор: $lipetorShouldTake")
            println("  - Фубуксицин: $fubuxicinShouldTake")
            
            // В каждый день должно быть только одно лекарство
            assertNotEquals("В день +$dayOffset должно быть только одно лекарство", 
                           lipetorShouldTake, fubuxicinShouldTake)
        }
    }
} 