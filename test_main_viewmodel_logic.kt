package com.medicalnotes.app.test

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.utils.DosageCalculator
import java.time.LocalDate
import java.time.LocalTime

/**
 * Тест логики MainViewModel для загрузки лекарств на сегодня
 */
fun testMainViewModelLogic() {
    println("=== ТЕСТ ЛОГИКИ MAINVIEWMODEL ===")
    
    // Создаем тестовые данные
    val testMedicines = createTestMedicinesFromXML()
    
    // Симулируем логику loadTodayMedicines из MainViewModel
    val todayMedicines = simulateLoadTodayMedicines(testMedicines)
    
    println("Результат загрузки лекарств на сегодня:")
    if (todayMedicines.isEmpty()) {
        println("❌ НИ ОДНОГО ЛЕКАРСТВА НЕ ЗАГРУЖЕНО!")
    } else {
        todayMedicines.forEach { medicine ->
            println("✅ ${medicine.name} - ${medicine.time}")
        }
    }
    
    // Анализируем каждое лекарство
    println("\n=== ДЕТАЛЬНЫЙ АНАЛИЗ ===")
    testMedicines.forEach { medicine ->
        println("\n--- ${medicine.name} ---")
        analyzeMedicineForViewModel(medicine, testMedicines)
    }
}

/**
 * Симулирует логику loadTodayMedicines из MainViewModel
 */
fun simulateLoadTodayMedicines(allMedicines: List<Medicine>): List<Medicine> {
    println("=== СИМУЛЯЦИЯ LOADTODAYMEDICINES ===")
    println("Всего лекарств в базе: ${allMedicines.size}")
    
    val today = LocalDate.now()
    println("Сегодняшняя дата: $today")
    
    // Валидация групп (как в MainViewModel)
    println("=== ВАЛИДАЦИЯ ГРУПП ===")
    val groupIds = allMedicines.mapNotNull { it.groupId }.distinct()
    println("Найдено групп: ${groupIds.size}")
    
    groupIds.forEach { groupId ->
        println("Проверяем группу $groupId")
        val groupMedicines = allMedicines.filter { it.groupId == groupId }
        println("  - Лекарств в группе: ${groupMedicines.size}")
        
        // Проверяем валидность группы
        val firstMedicine = groupMedicines.first()
        val isValid = firstMedicine.isGroupConsistent(groupMedicines)
        println("  - Группа валидна: $isValid")
        
        if (!isValid) {
            println("  - ⚠️ ГРУППА НЕВАЛИДНА!")
        }
    }
    
    // Используем DosageCalculator для получения активных лекарств
    val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
    println("Активных лекарств: ${activeMedicines.size}")
    
    return activeMedicines
}

/**
 * Анализирует лекарство для ViewModel
 */
fun analyzeMedicineForViewModel(medicine: Medicine, allMedicines: List<Medicine>) {
    val today = LocalDate.now()
    
    println("ID: ${medicine.id}")
    println("Название: ${medicine.name}")
    println("Активно: ${medicine.isActive}")
    println("Принято сегодня: ${medicine.takenToday}")
    
    // Проверяем групповую валидность
    if (medicine.groupId != null) {
        val groupMedicines = allMedicines.filter { it.groupId == medicine.groupId }
        val isGroupConsistent = medicine.isGroupConsistent(groupMedicines)
        println("Группа валидна: $isGroupConsistent")
        
        if (!isGroupConsistent) {
            println("❌ ПРОБЛЕМА: Группа невалидна!")
            return
        }
    }
    
    // Проверяем, должно ли приниматься сегодня
    val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today, allMedicines)
    println("Должно приниматься: $shouldTake")
    
    if (!shouldTake) {
        println("❌ ПРОБЛЕМА: Не должно приниматься сегодня по расписанию")
        return
    }
    
    // Проверяем, не принято ли уже сегодня
    if (medicine.takenToday) {
        println("❌ ПРОБЛЕМА: Уже принято сегодня")
        return
    }
    
    println("✅ Лекарство должно отображаться в списке 'на сегодня'")
}

/**
 * Создает тестовые лекарства из XML (копия из предыдущего файла)
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

/**
 * Тест групповой логики
 */
fun testGroupLogic() {
    println("\n=== ТЕСТ ГРУППОВОЙ ЛОГИКИ ===")
    
    val medicines = createTestMedicinesFromXML()
    val today = LocalDate.now()
    
    medicines.forEach { medicine ->
        println("\n--- ${medicine.name} ---")
        
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
        
        // Финальная логика
        val finalResult = if (wasTakenYesterday && !expectedTake) {
            false // Не показываем, если принято вчера и сегодня не по расписанию
        } else {
            expectedTake
        }
        
        println("Финальный результат: $finalResult")
        
        if (finalResult) {
            println("✅ Должно отображаться сегодня")
        } else {
            println("❌ Не должно отображаться сегодня")
        }
    }
}

/**
 * Запускает все тесты
 */
fun main() {
    testMainViewModelLogic()
    testGroupLogic()
} 