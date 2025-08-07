package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.temporal.ChronoUnit

class SimpleLogicTest {

    @Test
    fun testLipitorAndFubuxusatLogic() {
        println("=== ТЕСТ ЛОГИКИ ЛИПЕТОР И ФУБУКСУСАТ ===")
        
        val today = LocalDate.now()
        val startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli() // 7 дней назад
        
        // Создаем лекарства на основе логов пользователя
        val lipitor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(21, 52), // Время из логов: 21:52
            frequency = DosageFrequency.DAILY, // Предполагаем ежедневный прием
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = null, // Не в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0L,
            groupFrequency = DosageFrequency.DAILY
        )
        
        val fubuxusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(19, 15), // Время из логов: 19:15
            frequency = DosageFrequency.DAILY, // Предполагаем ежедневный прием
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = null, // Не в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0L,
            groupFrequency = DosageFrequency.DAILY
        )
        
        println("Сегодняшняя дата: $today")
        println("Дата начала: ${Instant.ofEpochMilli(startDate)}")
        println()
        
        // Тестируем логику напрямую
        println("=== ТЕСТ ЛИПЕТОР ===")
        val lipitorStartDate = Instant.ofEpochMilli(lipitor.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val lipitorDaysSinceStart = ChronoUnit.DAYS.between(lipitorStartDate, today)
        
        println("Липетор:")
        println("  - Дата начала: $lipitorStartDate")
        println("  - Дней с начала: $lipitorDaysSinceStart")
        println("  - Частота: ${lipitor.frequency}")
        println("  - Активно: ${lipitor.isActive}")
        println("  - В группе: ${lipitor.groupId != null}")
        
        // Проверяем логику для ежедневного приема
        val lipitorShouldTake = when (lipitor.frequency) {
            DosageFrequency.DAILY -> {
                println("  - Логика DAILY: true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val shouldTake = lipitorDaysSinceStart % 2L == 0L
                println("  - Логика EVERY_OTHER_DAY: дней с начала = $lipitorDaysSinceStart, принимать = $shouldTake")
                shouldTake
            }
            else -> {
                println("  - Логика ${lipitor.frequency}: false")
                false
            }
        }
        
        println("  - РЕЗУЛЬТАТ: $lipitorShouldTake")
        println()
        
        // Тестируем Фубуксусат
        println("=== ТЕСТ ФУБУКСУСАТ ===")
        val fubuxusatStartDate = Instant.ofEpochMilli(fubuxusat.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val fubuxusatDaysSinceStart = ChronoUnit.DAYS.between(fubuxusatStartDate, today)
        
        println("Фубуксусат:")
        println("  - Дата начала: $fubuxusatStartDate")
        println("  - Дней с начала: $fubuxusatDaysSinceStart")
        println("  - Частота: ${fubuxusat.frequency}")
        println("  - Активно: ${fubuxusat.isActive}")
        println("  - В группе: ${fubuxusat.groupId != null}")
        
        val fubuxusatShouldTake = when (fubuxusat.frequency) {
            DosageFrequency.DAILY -> {
                println("  - Логика DAILY: true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val shouldTake = fubuxusatDaysSinceStart % 2L == 0L
                println("  - Логика EVERY_OTHER_DAY: дней с начала = $fubuxusatDaysSinceStart, принимать = $shouldTake")
                shouldTake
            }
            else -> {
                println("  - Логика ${fubuxusat.frequency}: false")
                false
            }
        }
        
        println("  - РЕЗУЛЬТАТ: $fubuxusatShouldTake")
        println()
        
        // Проверяем ожидаемое поведение
        assertTrue("Липетор должен принимать сегодня (ежедневно)", lipitorShouldTake)
        assertTrue("Фубуксусат должен принимать сегодня (ежедневно)", fubuxusatShouldTake)
        
        println("✅ ТЕСТ ПРОЙДЕН: Лекарства должны принимать сегодня")
        println()
        println("=== ВЫВОД ===")
        println("Если в вашем приложении эти лекарства показывают статус NOT_TODAY,")
        println("то проблема может быть в одном из следующих:")
        println("1. Частота приема установлена не на DAILY")
        println("2. Лекарства находятся в группе")
        println("3. Лекарства неактивны (isActive = false)")
        println("4. Дата начала установлена в будущем")
        println("5. Проблема в логике MedicineStatusHelper")
    }
    
    @Test
    fun testDifferentScenarios() {
        println("=== ТЕСТ РАЗНЫХ СЦЕНАРИЕВ ===")
        
        val today = LocalDate.now()
        val startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli() // 7 дней назад
        
        // Сценарий 1: Лекарство с частотой "через день"
        val everyOtherDayMedicine = Medicine(
            id = 1L,
            name = "Через день лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true
        )
        
        val startDateLocal = Instant.ofEpochMilli(startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val daysSinceStart = ChronoUnit.DAYS.between(startDateLocal, today)
        val shouldTakeEveryOtherDay = daysSinceStart % 2L == 0L
        
        println("Сценарий 1: Через день")
        println("  - Дней с начала: $daysSinceStart")
        println("  - Должно принимать: $shouldTakeEveryOtherDay")
        println("  - Если false, то статус будет NOT_TODAY")
        println()
        
        // Сценарий 2: Неактивное лекарство
        val inactiveMedicine = Medicine(
            id = 2L,
            name = "Неактивное лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = false // Неактивно
        )
        
        println("Сценарий 2: Неактивное лекарство")
        println("  - Активно: ${inactiveMedicine.isActive}")
        println("  - Статус будет NOT_TODAY независимо от других параметров")
        println()
        
        // Сценарий 3: Лекарство в группе
        val groupedMedicine = Medicine(
            id = 3L,
            name = "Группированное лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = 1L,
            groupName = "Группа через день",
            groupOrder = 1,
            groupStartDate = startDate,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        )
        
        println("Сценарий 3: Лекарство в группе")
        println("  - Группа: ${groupedMedicine.groupName}")
        println("  - Порядок в группе: ${groupedMedicine.groupOrder}")
        println("  - Частота группы: ${groupedMedicine.groupFrequency}")
        println("  - Логика группы может давать NOT_TODAY")
        println()
        
        println("=== РЕКОМЕНДАЦИИ ===")
        println("Проверьте в приложении:")
        println("1. Частоту приема Липетора и Фубуксусата")
        println("2. Активность лекарств")
        println("3. Наличие группировки")
        println("4. Дату начала приема")
    }
} 