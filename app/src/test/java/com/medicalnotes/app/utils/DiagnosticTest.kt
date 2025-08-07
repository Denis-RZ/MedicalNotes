package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId

class DiagnosticTest {

    @Test
    fun testLipitorAndFubuxusatDiagnostic() {
        println("=== ДИАГНОСТИКА ЛИПЕТОР И ФУБУКСУСАТ ===")
        
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
        
        // Тестируем Липетор
        println("=== ДИАГНОСТИКА ЛИПЕТОР ===")
        val lipitorStartDate = Instant.ofEpochMilli(lipitor.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val lipitorDaysSinceStart = ChronoUnit.DAYS.between(lipitorStartDate, today)
        
        println("Липетор:")
        println("  - Дата начала: $lipitorStartDate")
        println("  - Дней с начала: $lipitorDaysSinceStart")
        println("  - Частота: ${lipitor.frequency}")
        println("  - Активно: ${lipitor.isActive}")
        println("  - В группе: ${lipitor.groupId != null}")
        println("  - Принято сегодня: ${lipitor.takenToday}")
        
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
        
        println("  - РЕЗУЛЬТАТ shouldTakeToday: $lipitorShouldTake")
        
        // Симулируем статус
        val lipitorStatus = when {
            !lipitor.isActive -> "NOT_TODAY (не активно)"
            lipitor.takenToday -> "TAKEN_TODAY (уже принято)"
            lipitorShouldTake -> "UPCOMING/OVERDUE (по расписанию)"
            else -> "NOT_TODAY (не по расписанию)"
        }
        println("  - СТАТУС: $lipitorStatus")
        println()
        
        // Тестируем Фубуксусат
        println("=== ДИАГНОСТИКА ФУБУКСУСАТ ===")
        val fubuxusatStartDate = Instant.ofEpochMilli(fubuxusat.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val fubuxusatDaysSinceStart = ChronoUnit.DAYS.between(fubuxusatStartDate, today)
        
        println("Фубуксусат:")
        println("  - Дата начала: $fubuxusatStartDate")
        println("  - Дней с начала: $fubuxusatDaysSinceStart")
        println("  - Частота: ${fubuxusat.frequency}")
        println("  - Активно: ${fubuxusat.isActive}")
        println("  - В группе: ${fubuxusat.groupId != null}")
        println("  - Принято сегодня: ${fubuxusat.takenToday}")
        
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
        
        println("  - РЕЗУЛЬТАТ shouldTakeToday: $fubuxusatShouldTake")
        
        // Симулируем статус
        val fubuxusatStatus = when {
            !fubuxusat.isActive -> "NOT_TODAY (не активно)"
            fubuxusat.takenToday -> "TAKEN_TODAY (уже принято)"
            fubuxusatShouldTake -> "UPCOMING/OVERDUE (по расписанию)"
            else -> "NOT_TODAY (не по расписанию)"
        }
        println("  - СТАТУС: $fubuxusatStatus")
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
        println()
        println("=== РЕКОМЕНДАЦИИ ===")
        println("Проверьте в приложении:")
        println("1. Частоту приема Липетора и Фубуксусата")
        println("2. Активность лекарств")
        println("3. Наличие группировки")
        println("4. Дату начала приема")
    }
} 