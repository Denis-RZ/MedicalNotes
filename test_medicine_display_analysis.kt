package com.medicalnotes.app.test

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.utils.DosageCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Полный анализ проблемы с отображением лекарств на сегодня
 * Использует данные из предоставленного XML
 */
fun main() {
    println("=== ПОЛНЫЙ АНАЛИЗ ПРОБЛЕМЫ ОТОБРАЖЕНИЯ ЛЕКАРСТВ ===")
    println("Дата анализа: ${LocalDate.now()}")
    println("Время анализа: ${java.time.LocalTime.now()}")
    println()
    
    // Создаем тестовые данные из XML
    val testMedicines = createTestMedicinesFromXML()
    
    // Запускаем все тесты
    runAllTests(testMedicines)
    
    // Генерируем отчет
    generateReport(testMedicines)
}

/**
 * Запускает все тесты
 */
fun runAllTests(medicines: List<Medicine>) {
    println("=== ЗАПУСК ВСЕХ ТЕСТОВ ===")
    
    // Тест 1: Анализ данных из XML
    println("\n1. АНАЛИЗ ДАННЫХ ИЗ XML")
    analyzeXMLData(medicines)
    
    // Тест 2: Логика DosageCalculator
    println("\n2. ТЕСТ ЛОГИКИ DOSAGECALCULATOR")
    testDosageCalculatorLogic(medicines)
    
    // Тест 3: Логика MainViewModel
    println("\n3. ТЕСТ ЛОГИКИ MAINVIEWMODEL")
    testMainViewModelLogic(medicines)
    
    // Тест 4: Логика адаптера
    println("\n4. ТЕСТ ЛОГИКИ АДАПТЕРА")
    testAdapterLogic(medicines)
    
    // Тест 5: Групповая логика
    println("\n5. ТЕСТ ГРУППОВОЙ ЛОГИКИ")
    testGroupLogic(medicines)
}

/**
 * Анализирует данные из XML
 */
fun analyzeXMLData(medicines: List<Medicine>) {
    println("Количество лекарств: ${medicines.size}")
    
    medicines.forEach { medicine ->
        println("\n--- ${medicine.name} ---")
        println("ID: ${medicine.id}")
        println("Дозировка: ${medicine.dosage}")
        println("Время: ${medicine.time}")
        println("Частота: ${medicine.frequency}")
        println("Активно: ${medicine.isActive}")
        println("Принято сегодня: ${medicine.takenToday}")
        println("Последний прием: ${formatTimestamp(medicine.lastTakenTime)}")
        
        if (medicine.groupId != null) {
            println("Группа: ${medicine.groupName} (ID: ${medicine.groupId})")
            println("Порядок в группе: ${medicine.groupOrder}")
            println("Дата начала группы: ${formatTimestamp(medicine.groupStartDate)}")
            println("Частота группы: ${medicine.groupFrequency}")
        }
        
        // Анализ дат
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        
        println("Дата начала: $startDate")
        println("Дней с начала: $daysSinceStart")
    }
}

/**
 * Тестирует логику DosageCalculator
 */
fun testDosageCalculatorLogic(medicines: List<Medicine>) {
    val today = LocalDate.now()
    
    medicines.forEach { medicine ->
        println("\n--- ${medicine.name} ---")
        
        // Тест shouldTakeMedicine
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today, medicines)
        println("shouldTakeMedicine: $shouldTake")
        
        // Тест getMedicineStatus
        val status = DosageCalculator.getMedicineStatus(medicine, today)
        println("getMedicineStatus: $status")
        
        // Тест getActiveMedicinesForDate
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
        val isActive = activeMedicines.any { it.id == medicine.id }
        println("getActiveMedicinesForDate: $isActive")
        
        // Анализ проблем
        if (!isActive) {
            println("❌ ПРОБЛЕМА: Лекарство не в списке активных")
            if (!shouldTake) {
                println("   Причина: Не должно приниматься сегодня")
            } else if (medicine.takenToday) {
                println("   Причина: Уже принято сегодня")
            } else {
                println("   Причина: Неизвестная проблема")
            }
        } else {
            println("✅ Лекарство в списке активных")
        }
    }
}

/**
 * Тестирует логику MainViewModel
 */
fun testMainViewModelLogic(medicines: List<Medicine>) {
    val today = LocalDate.now()
    
    // Симулируем loadTodayMedicines
    println("Симуляция loadTodayMedicines:")
    println("Всего лекарств: ${medicines.size}")
    
    // Валидация групп
    val groupIds = medicines.mapNotNull { it.groupId }.distinct()
    println("Групп найдено: ${groupIds.size}")
    
    groupIds.forEach { groupId ->
        val groupMedicines = medicines.filter { it.groupId == groupId }
        println("Группа $groupId: ${groupMedicines.size} лекарств")
        
        // Проверяем валидность группы
        val firstMedicine = groupMedicines.first()
        val isValid = firstMedicine.isGroupConsistent(groupMedicines)
        println("  Валидна: $isValid")
    }
    
    // Получаем активные лекарства
    val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
    println("Активных лекарств: ${activeMedicines.size}")
    
    if (activeMedicines.isEmpty()) {
        println("❌ КРИТИЧЕСКАЯ ПРОБЛЕМА: Ни одного активного лекарства!")
    } else {
        activeMedicines.forEach { medicine ->
            println("✅ ${medicine.name} - ${medicine.time}")
        }
    }
}

/**
 * Тестирует логику адаптера
 */
fun testAdapterLogic(medicines: List<Medicine>) {
    val today = LocalDate.now()
    val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
    
    println("Лекарств для отображения: ${activeMedicines.size}")
    
    if (activeMedicines.isEmpty()) {
        println("❌ АДАПТЕР: Нет лекарств для отображения")
        return
    }
    
    activeMedicines.forEachIndexed { index, medicine ->
        println("\n--- Элемент ${index + 1}: ${medicine.name} ---")
        
        // Симулируем отображение
        val dosageDescription = getDosageDescription(medicine)
        val groupInfo = if (medicine.groupName.isNotEmpty()) {
            " (${medicine.groupName}, №${medicine.groupOrder})"
        } else {
            ""
        }
        val fullDosageText = "$dosageDescription - ${medicine.dosage}$groupInfo"
        
        println("Название: ${medicine.name}")
        println("Полный текст: $fullDosageText")
        println("Время: ${medicine.time}")
        println("Статус: ${DosageCalculator.getMedicineStatus(medicine)}")
        
        // Проверяем данные
        val issues = checkMedicineData(medicine)
        if (issues.isNotEmpty()) {
            println("⚠️ Проблемы с данными:")
            issues.forEach { issue ->
                println("  - $issue")
            }
        } else {
            println("✅ Данные корректны")
        }
    }
}

/**
 * Тестирует групповую логику
 */
fun testGroupLogic(medicines: List<Medicine>) {
    val today = LocalDate.now()
    
    medicines.forEach { medicine ->
        if (medicine.groupId == null) return@forEach
        
        println("\n--- Групповая логика: ${medicine.name} ---")
        
        // Анализируем групповые даты
        val groupStartDate = if (medicine.groupStartDate > 0) {
            java.time.Instant.ofEpochMilli(medicine.groupStartDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        } else {
            java.time.Instant.ofEpochMilli(medicine.startDate)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
        
        val daysSinceGroupStart = java.time.temporal.ChronoUnit.DAYS.between(groupStartDate, today)
        val groupDay = (daysSinceGroupStart % 2).toInt()
        
        println("Дата начала группы: $groupStartDate")
        println("Дней с начала группы: $daysSinceGroupStart")
        println("День группы (0/1): $groupDay")
        println("Порядок в группе: ${medicine.groupOrder}")
        
        // Проверяем логику "через день"
        val expectedTake = when {
            medicine.groupOrder == 1 -> groupDay == 0
            medicine.groupOrder == 2 -> groupDay == 1
            else -> false
        }
        
        println("Ожидаемый результат: $expectedTake")
        
        // Проверяем фактический результат
        val actualTake = DosageCalculator.shouldTakeMedicine(medicine, today, medicines)
        println("Фактический результат: $actualTake")
        
        if (expectedTake != actualTake) {
            println("⚠️ РАСХОЖДЕНИЕ В ЛОГИКЕ!")
        }
        
        // Проверяем, было ли принято вчера
        val yesterday = today.minusDays(1)
        val wasTakenYesterday = if (medicine.lastTakenTime > 0) {
            val lastTakenDate = java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            lastTakenDate == yesterday
        } else {
            false
        }
        
        println("Принято вчера: $wasTakenYesterday")
    }
}

/**
 * Генерирует отчет
 */
fun generateReport(medicines: List<Medicine>) {
    println("\n=== ОТЧЕТ ===")
    
    val today = LocalDate.now()
    val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
    
    println("Всего лекарств: ${medicines.size}")
    println("Активных лекарств: ${activeMedicines.size}")
    
    if (activeMedicines.isEmpty()) {
        println("\n❌ КРИТИЧЕСКАЯ ПРОБЛЕМА: Лекарства не отображаются!")
        println("\nВозможные причины:")
        
        medicines.forEach { medicine ->
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today, medicines)
            val status = DosageCalculator.getMedicineStatus(medicine, today)
            
            println("- ${medicine.name}:")
            println("  * Должно приниматься: $shouldTake")
            println("  * Статус: $status")
            println("  * Принято сегодня: ${medicine.takenToday}")
            
            if (!shouldTake) {
                println("  * Причина: Не по расписанию")
            } else if (medicine.takenToday) {
                println("  * Причина: Уже принято")
            }
        }
    } else {
        println("\n✅ Лекарства отображаются корректно")
        activeMedicines.forEach { medicine ->
            println("- ${medicine.name} (${medicine.time})")
        }
    }
    
    // Рекомендации
    println("\n=== РЕКОМЕНДАЦИИ ===")
    if (activeMedicines.isEmpty()) {
        println("1. Проверьте даты начала приема лекарств")
        println("2. Проверьте групповую логику")
        println("3. Проверьте флаг takenToday")
        println("4. Проверьте логику DosageCalculator.shouldTakeMedicine")
    } else {
        println("1. Лекарства отображаются корректно")
        println("2. Проверьте UI на предмет проблем с отображением")
        println("3. Проверьте адаптер RecyclerView")
    }
}

/**
 * Проверяет данные лекарства
 */
fun checkMedicineData(medicine: Medicine): List<String> {
    val issues = mutableListOf<String>()
    
    if (medicine.name.isBlank()) {
        issues.add("Пустое название")
    }
    
    if (medicine.dosage.isBlank()) {
        issues.add("Пустая дозировка")
    }
    
    if (medicine.remainingQuantity <= 0) {
        issues.add("Нет остатка")
    }
    
    if (medicine.groupId != null && medicine.groupName.isBlank()) {
        issues.add("Пустое название группы")
    }
    
    return issues
}

/**
 * Получает описание дозировки
 */
fun getDosageDescription(medicine: Medicine): String {
    val frequencyText = when (medicine.frequency) {
        DosageFrequency.DAILY -> "Каждый день"
        DosageFrequency.EVERY_OTHER_DAY -> "Через день"
        DosageFrequency.TWICE_A_WEEK -> "2 раза в неделю"
        DosageFrequency.THREE_TIMES_A_WEEK -> "3 раза в неделю"
        DosageFrequency.WEEKLY -> "Раз в неделю"
        DosageFrequency.CUSTOM -> "По расписанию"
    }
    
    val timeText = "в ${medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    return "$frequencyText $timeText"
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