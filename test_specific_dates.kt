package com.medicalnotes.app.test

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.utils.DosageCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Тест для анализа конкретных дат из XML данных
 */
fun main() {
    println("=== АНАЛИЗ КОНКРЕТНЫХ ДАТ ИЗ XML ===")
    println()
    
    // Анализируем даты из XML
    analyzeXMLDates()
    
    // Тестируем логику для конкретных дат
    testSpecificDateLogic()
    
    // Генерируем отчет по датам
    generateDateReport()
}

/**
 * Анализирует даты из XML
 */
fun analyzeXMLDates() {
    println("=== АНАЛИЗ ДАТ ИЗ XML ===")
    
    // Даты из XML
    val lipetorStartDate = 1754381301006L
    val lipetorLastTaken = 1754473507174L
    val lipetorGroupStart = 1754451744031L
    
    val fubuksusatStartDate = 1754381353472L
    val fubuksusatLastTaken = 1754471876018L
    val fubuksusatGroupStart = 1754451755574L
    
    println("Липетор:")
    println("  Дата начала приема: ${formatTimestamp(lipetorStartDate)}")
    println("  Последний прием: ${formatTimestamp(lipetorLastTaken)}")
    println("  Дата начала группы: ${formatTimestamp(lipetorGroupStart)}")
    
    println("Фубуксусат:")
    println("  Дата начала приема: ${formatTimestamp(fubuksusatStartDate)}")
    println("  Последний прием: ${formatTimestamp(fubuksusatLastTaken)}")
    println("  Дата начала группы: ${formatTimestamp(fubuksusatGroupStart)}")
    
    // Анализируем разницы во времени
    val now = System.currentTimeMillis()
    
    println("\nРазницы во времени (от текущего момента):")
    println("Липетор:")
    println("  От начала приема: ${formatDuration(now - lipetorStartDate)}")
    println("  От последнего приема: ${formatDuration(now - lipetorLastTaken)}")
    println("  От начала группы: ${formatDuration(now - lipetorGroupStart)}")
    
    println("Фубуксусат:")
    println("  От начала приема: ${formatDuration(now - fubuksusatStartDate)}")
    println("  От последнего приема: ${formatDuration(now - fubuksusatLastTaken)}")
    println("  От начала группы: ${formatDuration(now - fubuksusatGroupStart)}")
}

/**
 * Тестирует логику для конкретных дат
 */
fun testSpecificDateLogic() {
    println("\n=== ТЕСТ ЛОГИКИ ДЛЯ КОНКРЕТНЫХ ДАТ ===")
    
    val medicines = createTestMedicinesFromXML()
    val today = LocalDate.now()
    
    medicines.forEach { medicine ->
        println("\n--- ${medicine.name} ---")
        
        // Анализируем даты
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        
        val groupStartDate = if (medicine.groupStartDate > 0) {
            java.time.Instant.ofEpochMilli(medicine.groupStartDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            startDate
        }
        
        val lastTakenDate = if (medicine.lastTakenTime > 0) {
            java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            null
        }
        
        println("Дата начала приема: $startDate")
        println("Дата начала группы: $groupStartDate")
        println("Последний прием: $lastTakenDate")
        println("Сегодня: $today")
        
        // Анализируем дни
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val daysSinceGroupStart = java.time.temporal.ChronoUnit.DAYS.between(groupStartDate, today)
        val daysSinceLastTaken = lastTakenDate?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, today) 
        }
        
        println("Дней с начала приема: $daysSinceStart")
        println("Дней с начала группы: $daysSinceGroupStart")
        println("Дней с последнего приема: $daysSinceLastTaken")
        
        // Анализируем групповую логику
        if (medicine.groupId != null) {
            val groupDay = (daysSinceGroupStart % 2).toInt()
            println("День группы (0/1): $groupDay")
            println("Порядок в группе: ${medicine.groupOrder}")
            
            val expectedTake = when {
                medicine.groupOrder == 1 -> groupDay == 0
                medicine.groupOrder == 2 -> groupDay == 1
                else -> false
            }
            
            println("Ожидаемый результат: $expectedTake")
            
            // Проверяем, было ли принято вчера
            val yesterday = today.minusDays(1)
            val wasTakenYesterday = lastTakenDate == yesterday
            println("Принято вчера: $wasTakenYesterday")
            
            // Финальная логика
            val finalResult = if (wasTakenYesterday && !expectedTake) {
                false
            } else {
                expectedTake
            }
            
            println("Финальный результат: $finalResult")
            
            // Проверяем фактический результат
            val actualResult = DosageCalculator.shouldTakeMedicine(medicine, today, medicines)
            println("Фактический результат: $actualResult")
            
            if (finalResult != actualResult) {
                println("⚠️ РАСХОЖДЕНИЕ В ЛОГИКЕ!")
            }
        }
    }
}

/**
 * Генерирует отчет по датам
 */
fun generateDateReport() {
    println("\n=== ОТЧЕТ ПО ДАТАМ ===")
    
    val medicines = createTestMedicinesFromXML()
    val today = LocalDate.now()
    
    println("Текущая дата: $today")
    println()
    
    medicines.forEach { medicine ->
        println("--- ${medicine.name} ---")
        
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        
        val groupStartDate = if (medicine.groupStartDate > 0) {
            java.time.Instant.ofEpochMilli(medicine.groupStartDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            startDate
        }
        
        val lastTakenDate = if (medicine.lastTakenTime > 0) {
            java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            null
        }
        
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val daysSinceGroupStart = java.time.temporal.ChronoUnit.DAYS.between(groupStartDate, today)
        val daysSinceLastTaken = lastTakenDate?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, today) 
        }
        
        println("Дата начала: $startDate (${daysSinceStart} дней назад)")
        println("Дата начала группы: $groupStartDate (${daysSinceGroupStart} дней назад)")
        if (lastTakenDate != null) {
            println("Последний прием: $lastTakenDate (${daysSinceLastTaken} дней назад)")
        } else {
            println("Последний прием: Не установлен")
        }
        
        // Анализ проблемы
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today, medicines)
        val status = DosageCalculator.getMedicineStatus(medicine, today)
        
        println("Должно приниматься сегодня: $shouldTake")
        println("Статус: $status")
        
        if (!shouldTake) {
            println("❌ ПРОБЛЕМА: Не должно приниматься сегодня")
            
            if (medicine.groupId != null) {
                val groupDay = (daysSinceGroupStart % 2).toInt()
                println("  Причина: День группы $groupDay, порядок ${medicine.groupOrder}")
            } else {
                println("  Причина: Не по расписанию (${medicine.frequency})")
            }
        } else {
            println("✅ Должно приниматься сегодня")
        }
        
        println()
    }
}

/**
 * Форматирует timestamp
 */
fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "Не установлено"
    
    val date = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
}

/**
 * Форматирует длительность
 */
fun formatDuration(millis: Long): String {
    val days = millis / (24 * 60 * 60 * 1000)
    val hours = (millis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
    val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)
    
    return "${days}д ${hours}ч ${minutes}м"
}

/**
 * Создает тестовые лекарства из XML
 */
fun createTestMedicinesFromXML(): List<Medicine> {
    return listOf(
        Medicine(
            id = 1754381301015,
            name = "Липетор",
            dosage = "20",
            quantity = 44,
            remainingQuantity = 44,
            medicineType = "Tablets",
            time = LocalTime.of(17, 41),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = 1754381301006,
            isActive = true,
            takenToday = false,
            lastTakenTime = 1754473507174,
            takenAt = 0,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = 1754451744031,
            groupName = "Тестер",
            groupOrder = 1,
            groupStartDate = 1754451744031,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            multipleDoses = false,
            doseTimes = listOf(LocalTime.of(17, 41)),
            customDays = emptyList(),
            updatedAt = 1754540857591
        ),
        Medicine(
            id = 1754381353482,
            name = "Фубуксусат",
            dosage = "Полтоблетки",
            quantity = 34,
            remainingQuantity = 34,
            medicineType = "Tablets",
            time = LocalTime.of(16, 15),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = 1754381353472,
            isActive = true,
            takenToday = false,
            lastTakenTime = 1754471876018,
            takenAt = 0,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = 1754451755574,
            groupName = "Тестер",
            groupOrder = 2,
            groupStartDate = 1754451755574,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            multipleDoses = false,
            doseTimes = listOf(LocalTime.of(16, 15)),
            customDays = emptyList(),
            updatedAt = 1754540857591
        )
    )
} 