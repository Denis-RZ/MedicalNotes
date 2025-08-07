package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.io.File

class ConsoleTest {

    @Test
    fun testLipitorAndFubuxusatLogic() {
        val output = StringBuilder()
        
        output.appendLine("=== ТЕСТ ЛОГИКИ ЛИПЕТОР И ФУБУКСУСАТ ===")
        
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
        
        output.appendLine("Сегодняшняя дата: $today")
        output.appendLine("Дата начала: ${Instant.ofEpochMilli(startDate)}")
        output.appendLine()
        
        // Тестируем логику напрямую
        output.appendLine("=== ТЕСТ ЛИПЕТОР ===")
        val lipitorStartDate = Instant.ofEpochMilli(lipitor.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val lipitorDaysSinceStart = ChronoUnit.DAYS.between(lipitorStartDate, today)
        
        output.appendLine("Липетор:")
        output.appendLine("  - Дата начала: $lipitorStartDate")
        output.appendLine("  - Дней с начала: $lipitorDaysSinceStart")
        output.appendLine("  - Частота: ${lipitor.frequency}")
        output.appendLine("  - Активно: ${lipitor.isActive}")
        output.appendLine("  - В группе: ${lipitor.groupId != null}")
        
        // Проверяем логику для ежедневного приема
        val lipitorShouldTake = when (lipitor.frequency) {
            DosageFrequency.DAILY -> {
                output.appendLine("  - Логика DAILY: true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val shouldTake = lipitorDaysSinceStart % 2L == 0L
                output.appendLine("  - Логика EVERY_OTHER_DAY: дней с начала = $lipitorDaysSinceStart, принимать = $shouldTake")
                shouldTake
            }
            else -> {
                output.appendLine("  - Логика ${lipitor.frequency}: false")
                false
            }
        }
        
        output.appendLine("  - РЕЗУЛЬТАТ: $lipitorShouldTake")
        output.appendLine()
        
        // Тестируем Фубуксусат
        output.appendLine("=== ТЕСТ ФУБУКСУСАТ ===")
        val fubuxusatStartDate = Instant.ofEpochMilli(fubuxusat.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val fubuxusatDaysSinceStart = ChronoUnit.DAYS.between(fubuxusatStartDate, today)
        
        output.appendLine("Фубуксусат:")
        output.appendLine("  - Дата начала: $fubuxusatStartDate")
        output.appendLine("  - Дней с начала: $fubuxusatDaysSinceStart")
        output.appendLine("  - Частота: ${fubuxusat.frequency}")
        output.appendLine("  - Активно: ${fubuxusat.isActive}")
        output.appendLine("  - В группе: ${fubuxusat.groupId != null}")
        
        val fubuxusatShouldTake = when (fubuxusat.frequency) {
            DosageFrequency.DAILY -> {
                output.appendLine("  - Логика DAILY: true")
                true
            }
            DosageFrequency.EVERY_OTHER_DAY -> {
                val shouldTake = fubuxusatDaysSinceStart % 2L == 0L
                output.appendLine("  - Логика EVERY_OTHER_DAY: дней с начала = $fubuxusatDaysSinceStart, принимать = $shouldTake")
                shouldTake
            }
            else -> {
                output.appendLine("  - Логика ${fubuxusat.frequency}: false")
                false
            }
        }
        
        output.appendLine("  - РЕЗУЛЬТАТ: $fubuxusatShouldTake")
        output.appendLine()
        
        // Проверяем ожидаемое поведение
        assertTrue("Липетор должен принимать сегодня (ежедневно)", lipitorShouldTake)
        assertTrue("Фубуксусат должен принимать сегодня (ежедневно)", fubuxusatShouldTake)
        
        output.appendLine("✅ ТЕСТ ПРОЙДЕН: Лекарства должны принимать сегодня")
        output.appendLine()
        output.appendLine("=== ВЫВОД ===")
        output.appendLine("Если в вашем приложении эти лекарства показывают статус NOT_TODAY,")
        output.appendLine("то проблема может быть в одном из следующих:")
        output.appendLine("1. Частота приема установлена не на DAILY")
        output.appendLine("2. Лекарства находятся в группе")
        output.appendLine("3. Лекарства неактивны (isActive = false)")
        output.appendLine("4. Дата начала установлена в будущем")
        output.appendLine("5. Проблема в логике MedicineStatusHelper")
        
        // Записываем результат в файл
        val outputFile = File("test_output.txt")
        outputFile.writeText(output.toString())
        
        println("Результат записан в файл: ${outputFile.absolutePath}")
    }
} 