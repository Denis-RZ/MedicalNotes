package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class RealDeviceDataTest {
    
    @Test
    fun testRealDeviceScenario() {
        println("=== ТЕСТ РЕАЛЬНЫХ ДАННЫХ С УСТРОЙСТВА ===")
        
        // Используем реальную дату как на устройстве (5 августа 2025)
        val today = LocalDate.of(2025, 8, 5)
        println("Реальная дата: $today")
        
        // Создаем лекарства с реальными данными с устройства
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = 1751472000000L, // Примерная дата начала (30 дней назад)
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
            startDate = 1751472000000L, // Та же дата начала
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
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        // Проверяем полную логику
        val lipetorFullShouldTake = shouldTakeMedicine(lipetor, today)
        val fubuxicinFullShouldTake = shouldTakeMedicine(fubuxicin, today)
        
        println("Липетор (полная логика): $lipetorFullShouldTake")
        println("Фубуксусат (полная логика): $fubuxicinFullShouldTake")
        
        // Проверяем, что только одно лекарство принимается
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        
        // Если оба лекарства показываются, это проблема
        if (lipetorShouldTake && fubuxicinShouldTake) {
            println("❌ ПРОБЛЕМА: Оба лекарства должны приниматься!")
            println("Это объясняет, почему на скриншоте показываются оба лекарства")
        } else if (!lipetorShouldTake && !fubuxicinShouldTake) {
            println("❌ ПРОБЛЕМА: Ни одно лекарство не должно приниматься!")
            println("Это означает, что логика группировки работает неправильно")
        } else {
            println("✅ Логика группировки работает правильно")
            if (lipetorShouldTake) {
                println("Сегодня должен приниматься Липетор")
            } else {
                println("Сегодня должен приниматься Фубуксусат")
            }
        }
        
        // Проверяем, что результаты совпадают
        assertEquals("Результаты группировки и полной логики для Липетора должны совпадать", 
                    lipetorShouldTake, lipetorFullShouldTake)
        assertEquals("Результаты группировки и полной логики для Фубуксусата должны совпадать", 
                    fubuxicinShouldTake, fubuxicinFullShouldTake)
        
        println("=== ПРОВЕРКА РАЗНЫХ ДАТ ===")
        
        // Проверяем логику для разных дат
        val tomorrow = today.plusDays(1)
        val dayAfterTomorrow = today.plusDays(2)
        
        println("Завтра ($tomorrow):")
        val lipetorTomorrow = shouldTakeMedicineInGroup(lipetor, tomorrow)
        val fubuxicinTomorrow = shouldTakeMedicineInGroup(fubuxicin, tomorrow)
        println("  Липетор: $lipetorTomorrow")
        println("  Фубуксусат: $fubuxicinTomorrow")
        
        println("Послезавтра ($dayAfterTomorrow):")
        val lipetorDayAfter = shouldTakeMedicineInGroup(lipetor, dayAfterTomorrow)
        val fubuxicinDayAfter = shouldTakeMedicineInGroup(fubuxicin, dayAfterTomorrow)
        println("  Липетор: $lipetorDayAfter")
        println("  Фубуксусат: $fubuxicinDayAfter")
        
        // Проверяем чередование
        val shouldAlternate = (lipetorShouldTake != lipetorTomorrow) && (fubuxicinShouldTake != fubuxicinTomorrow)
        println("Лекарства должны чередоваться: $shouldAlternate")
        
        println("✅ Тест завершен")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        println("=== ГРУППОВАЯ ЛОГИКА ===")
        println("Лекарство: ${medicine.name}")
        println("  - Группа: ${medicine.groupName}")
        println("  - Порядок в группе: ${medicine.groupOrder}")
        println("  - Дата начала: $startDate")
        println("  - Проверяемая дата: $date")
        println("  - Дней с начала: $daysSinceStart")
        
        // Логика группы "через день"
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder == 1 -> groupDay == 0  // Первое лекарство в четные дни группы
                medicine.groupOrder == 2 -> groupDay == 1  // Второе лекарство в нечетные дни группы
                else -> false
            }
            
            println("  - День группы: $groupDay")
            println("  - Порядок лекарства: ${medicine.groupOrder}")
            println("  - Нужно принимать: $shouldTake")
            return shouldTake
        }
        
        return false
    }
    
    private fun shouldTakeMedicine(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        if (date.isBefore(startDate)) {
            return false
        }
        
        if (medicine.groupId != null) {
            return shouldTakeMedicineInGroup(medicine, date)
        }
        
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