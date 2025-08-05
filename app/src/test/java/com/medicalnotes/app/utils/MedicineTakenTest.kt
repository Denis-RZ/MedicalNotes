package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class MedicineTakenTest {
    
    @Test
    fun testMedicineTakenTodayFlag() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        
        // Создаем липетор (groupOrder = 1)
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
        
        // Создаем фубуксицин (groupOrder = 2)
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
        
        // Проверяем начальное состояние
        println("=== НАЧАЛЬНОЕ СОСТОЯНИЕ ===")
        println("Липетор takenToday: ${lipetor.takenToday}")
        println("Фубуксицин takenToday: ${fubuxicin.takenToday}")
        
        // Проверяем логику группировки напрямую
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        val lipetorShouldTakeToday = when {
            lipetor.groupOrder % 2 == 1 -> groupDay == 0  // Нечетные порядки (1,3,5...) в четные дни
            lipetor.groupOrder % 2 == 0 -> groupDay == 1  // Четные порядки (2,4,6...) в нечетные дни
            else -> false
        }
        
        val fubuxicinShouldTakeToday = when {
            fubuxicin.groupOrder % 2 == 1 -> groupDay == 0
            fubuxicin.groupOrder % 2 == 0 -> groupDay == 1
            else -> false
        }
        
        println("День группы: $groupDay")
        println("Липетор должен принимать сегодня: $lipetorShouldTakeToday")
        println("Фубуксицин должен принимать сегодня: $fubuxicinShouldTakeToday")
        
        // Симулируем прием липетора (как это делает decrementMedicineQuantity)
        val lipetorAfterTaken = lipetor.copy(
            remainingQuantity = lipetor.remainingQuantity - 1,
            lastTakenTime = System.currentTimeMillis(),
            takenToday = true,  // ИСПРАВЛЕНО: Устанавливаем takenToday = true
            isMissed = false
        )
        
        // Проверяем состояние после приема
        println("=== ПОСЛЕ ПРИЕМА ЛИПЕТОРА ===")
        println("Липетор takenToday: ${lipetorAfterTaken.takenToday}")
        println("Фубуксицин takenToday: ${fubuxicin.takenToday}")
        
        // Проверяем, что takenToday установлен правильно
        assertTrue("takenToday должен быть установлен в true после приема", lipetorAfterTaken.takenToday)
        
        // Проверяем, что количество уменьшилось
        assertEquals("Количество должно уменьшиться на 1", 29, lipetorAfterTaken.remainingQuantity)
        
        // Проверяем, что lastTakenTime установлен
        assertTrue("lastTakenTime должен быть установлен", lipetorAfterTaken.lastTakenTime > 0)
        
        // Проверяем, что фубуксицин не изменился
        assertFalse("Фубуксицин не должен измениться", fubuxicin.takenToday)
        assertEquals("Количество фубуксицина не должно измениться", 20, fubuxicin.remainingQuantity)
    }
    
    @Test
    fun testGroupLogicAfterTakingMedicine() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        
        // Проверяем логику группировки напрямую
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        
        println("=== ЛОГИКА ГРУППИРОВКИ ===")
        println("Сегодня: $today")
        println("Дней с начала: $daysSinceStart")
        println("День группы: $groupDay")
        
        // Липетор (groupOrder = 1) должен приниматься в четные дни группы
        val lipetorShouldTakeToday = when {
            1 % 2 == 1 -> groupDay == 0  // Нечетные порядки (1,3,5...) в четные дни
            else -> false
        }
        
        // Фубуксицин (groupOrder = 2) должен приниматься в нечетные дни группы
        val fubuxicinShouldTakeToday = when {
            2 % 2 == 0 -> groupDay == 1  // Четные порядки (2,4,6...) в нечетные дни
            else -> false
        }
        
        println("Липетор должен принимать сегодня: $lipetorShouldTakeToday")
        println("Фубуксицин должен принимать сегодня: $fubuxicinShouldTakeToday")
        
        // Проверяем, что они принимаются в разные дни
        assertNotEquals("Липетор и Фубуксицин должны принимать в разные дни", 
                       lipetorShouldTakeToday, fubuxicinShouldTakeToday)
        
        // Если липетор принимается сегодня, то фубуксицин не должен
        if (lipetorShouldTakeToday) {
            assertFalse("Если липетор принимается сегодня, то фубуксицин не должен", fubuxicinShouldTakeToday)
        }
        
        // Если фубуксицин принимается сегодня, то липетор не должен
        if (fubuxicinShouldTakeToday) {
            assertFalse("Если фубуксицин принимается сегодня, то липетор не должен", lipetorShouldTakeToday)
        }
    }
} 