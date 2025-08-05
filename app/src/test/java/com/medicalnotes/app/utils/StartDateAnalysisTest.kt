package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class StartDateAnalysisTest {
    
    @Test
    fun testStartDateAnalysis() {
        println("=== АНАЛИЗ ПРОБЛЕМЫ С START_DATE ===")
        
        val today = LocalDate.of(2025, 8, 5)
        println("Сегодня: $today")
        
        // Тестируем разные варианты startDate
        val startDates = listOf(
            today.minusDays(1),   // Вчера
            today.minusDays(2),   // 2 дня назад
            today.minusDays(3),   // 3 дня назад
            today.minusDays(7),   // Неделю назад
            today.minusDays(14),  // 2 недели назад
            today.minusDays(30),  // Месяц назад
            today.minusDays(60),  // 2 месяца назад
            today.minusDays(100), // 100 дней назад
            today.minusDays(200), // 200 дней назад
            today.minusDays(365)  // Год назад
        )
        
        startDates.forEach { startDate ->
            println("\n=== ТЕСТ С START_DATE: $startDate ===")
            
            val lipetor = Medicine(
                id = 1754031172266L,
                name = "Липетор",
                dosage = "1 таблетка",
                time = LocalTime.of(17, 54),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
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
                startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000L,
                groupId = 1L,
                groupName = "Тест",
                groupOrder = 2,
                quantity = 30,
                remainingQuantity = 30,
                takenToday = false
            )
            
            val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
            val groupDay = (daysSinceStart % 2).toInt()
            
            val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
            val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
            
            println("Дней с начала: $daysSinceStart")
            println("День группы: $groupDay")
            println("Липетор должен приниматься: $lipetorShouldTake")
            println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
            
            // Проверяем, есть ли проблема
            if (lipetorShouldTake && fubuxicinShouldTake) {
                println("❌ ПРОБЛЕМА НАЙДЕНА: Оба лекарства должны приниматься!")
                println("Это объясняет проблему на скриншоте")
                println("startDate: $startDate")
                println("daysSinceStart: $daysSinceStart")
                println("groupDay: $groupDay")
                return
            } else if (!lipetorShouldTake && !fubuxicinShouldTake) {
                println("❌ ПРОБЛЕМА НАЙДЕНА: Ни одно лекарство не должно приниматься!")
                println("startDate: $startDate")
                println("daysSinceStart: $daysSinceStart")
                println("groupDay: $groupDay")
                return
            } else {
                println("✅ Логика работает правильно для этой даты")
            }
        }
        
        println("\n=== ПРОВЕРКА РЕАЛЬНЫХ ДАННЫХ С УСТРОЙСТВА ===")
        
        // Проверим реальные startDate из лога
        val realStartDates = listOf(
            1751472000000L, // Примерная дата из теста
            1751385600000L, // На день раньше
            1751558400000L, // На день позже
            1751299200000L, // На 2 дня раньше
            1751644800000L  // На 2 дня позже
        )
        
        realStartDates.forEach { startDateMillis ->
            val startDate = LocalDate.ofEpochDay(startDateMillis / (24 * 60 * 60 * 1000))
            println("\nПроверяем startDate: $startDate (${startDateMillis}L)")
            
            val lipetor = Medicine(
                id = 1754031172266L,
                name = "Липетор",
                dosage = "1 таблетка",
                time = LocalTime.of(17, 54),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                startDate = startDateMillis,
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
                startDate = startDateMillis,
                groupId = 1L,
                groupName = "Тест",
                groupOrder = 2,
                quantity = 30,
                remainingQuantity = 30,
                takenToday = false
            )
            
            val daysSinceStart = ChronoUnit.DAYS.between(startDate, today)
            val groupDay = (daysSinceStart % 2).toInt()
            
            val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
            val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
            
            println("Дней с начала: $daysSinceStart")
            println("День группы: $groupDay")
            println("Липетор: $lipetorShouldTake")
            println("Фубуксусат: $fubuxicinShouldTake")
            
            if (lipetorShouldTake && fubuxicinShouldTake) {
                println("❌ ПРОБЛЕМА: Оба лекарства должны приниматься!")
                println("startDateMillis: $startDateMillis")
                println("startDate: $startDate")
                println("daysSinceStart: $daysSinceStart")
                println("groupDay: $groupDay")
                println("Это может быть причиной проблемы на скриншоте")
            }
        }
        
        println("\n✅ Анализ завершен")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            return when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder == 1 -> groupDay == 0
                medicine.groupOrder == 2 -> groupDay == 1
                else -> false
            }
        }
        
        return false
    }
} 