package com.medicalnotes.app.test

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.utils.DosageCalculator
import java.time.LocalDate
import java.time.LocalTime

/**
 * Тест логики адаптера и отображения лекарств
 */
fun testAdapterDisplayLogic() {
    println("=== ТЕСТ ЛОГИКИ АДАПТЕРА И ОТОБРАЖЕНИЯ ===")
    
    // Создаем тестовые данные
    val testMedicines = createTestMedicinesFromXML()
    
    // Симулируем работу адаптера
    simulateAdapterWork(testMedicines)
    
    // Тестируем отображение каждого лекарства
    println("\n=== ТЕСТ ОТОБРАЖЕНИЯ КАЖДОГО ЛЕКАРСТВА ===")
    testMedicines.forEach { medicine ->
        testMedicineDisplay(medicine, testMedicines)
    }
}

/**
 * Симулирует работу адаптера
 */
fun simulateAdapterWork(medicines: List<Medicine>) {
    println("=== СИМУЛЯЦИЯ РАБОТЫ АДАПТЕРА ===")
    
    val today = LocalDate.now()
    println("Сегодняшняя дата: $today")
    
    // Получаем лекарства для отображения (как в MainViewModel)
    val todayMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
    println("Лекарств для отображения: ${todayMedicines.size}")
    
    if (todayMedicines.isEmpty()) {
        println("❌ НИ ОДНОГО ЛЕКАРСТВА ДЛЯ ОТОБРАЖЕНИЯ!")
        return
    }
    
    // Симулируем отображение каждого лекарства
    todayMedicines.forEachIndexed { index, medicine ->
        println("\n--- ЭЛЕМЕНТ ${index + 1}: ${medicine.name} ---")
        simulateMedicineItemDisplay(medicine)
    }
}

/**
 * Симулирует отображение элемента лекарства
 */
fun simulateMedicineItemDisplay(medicine: Medicine) {
    println("Название: ${medicine.name}")
    println("Дозировка: ${medicine.dosage}")
    println("Время: ${medicine.time}")
    
    // Симулируем получение описания дозировки
    val dosageDescription = getDosageDescription(medicine)
    println("Описание дозировки: $dosageDescription")
    
    // Симулируем получение статуса
    val status = DosageCalculator.getMedicineStatus(medicine)
    println("Статус: $status")
    
    // Симулируем отображение времени
    val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
        val times = medicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
        times.joinToString(", ")
    } else {
        medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
    println("Отображаемое время: $timeText")
    
    // Симулируем групповую информацию
    val groupInfo = if (medicine.groupName.isNotEmpty()) {
        " (${medicine.groupName}, №${medicine.groupOrder})"
    } else {
        ""
    }
    println("Групповая информация: $groupInfo")
    
    // Симулируем полный текст дозировки
    val fullDosageText = if (medicine.dosage.isNotEmpty()) {
        "$dosageDescription - ${medicine.dosage}$groupInfo"
    } else {
        dosageDescription + groupInfo
    }
    println("Полный текст дозировки: $fullDosageText")
    
    // Проверяем видимость элементов
    val isOverdue = status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE
    val isTaken = status == com.medicalnotes.app.utils.MedicineStatus.TAKEN_TODAY
    
    println("Просрочено: $isOverdue")
    println("Принято: $isTaken")
    println("Видимо: ${!isTaken}")
}

/**
 * Тестирует отображение конкретного лекарства
 */
fun testMedicineDisplay(medicine: Medicine, allMedicines: List<Medicine>) {
    println("\n--- ТЕСТ ОТОБРАЖЕНИЯ: ${medicine.name} ---")
    
    val today = LocalDate.now()
    
    // Проверяем, должно ли отображаться
    val shouldDisplay = shouldDisplayMedicine(medicine, today, allMedicines)
    println("Должно отображаться: $shouldDisplay")
    
    if (!shouldDisplay) {
        println("❌ ПРОБЛЕМА: Лекарство не должно отображаться")
        return
    }
    
    // Проверяем корректность данных для отображения
    val displayData = getDisplayData(medicine)
    println("Данные для отображения:")
    displayData.forEach { (key, value) ->
        println("  $key: $value")
    }
    
    // Проверяем возможные проблемы с отображением
    checkDisplayIssues(medicine, displayData)
}

/**
 * Проверяет, должно ли лекарство отображаться
 */
fun shouldDisplayMedicine(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>): Boolean {
    // Проверяем активность
    if (!medicine.isActive) {
        println("  Причина: Лекарство неактивно")
        return false
    }
    
    // Проверяем, должно ли приниматься сегодня
    val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, date, allMedicines)
    if (!shouldTake) {
        println("  Причина: Не должно приниматься сегодня")
        return false
    }
    
    // Проверяем, не принято ли уже
    if (medicine.takenToday) {
        println("  Причина: Уже принято сегодня")
        return false
    }
    
    return true
}

/**
 * Получает данные для отображения
 */
fun getDisplayData(medicine: Medicine): Map<String, String> {
    return mapOf(
        "name" to medicine.name,
        "dosage" to medicine.dosage,
        "time" to medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
        "frequency" to medicine.frequency.name,
        "groupName" to (medicine.groupName.ifEmpty { "Нет группы" }),
        "groupOrder" to medicine.groupOrder.toString(),
        "isActive" to medicine.isActive.toString(),
        "takenToday" to medicine.takenToday.toString(),
        "remainingQuantity" to medicine.remainingQuantity.toString()
    )
}

/**
 * Проверяет возможные проблемы с отображением
 */
fun checkDisplayIssues(medicine: Medicine, displayData: Map<String, String>) {
    val issues = mutableListOf<String>()
    
    // Проверяем пустые поля
    if (medicine.name.isBlank()) {
        issues.add("Пустое название")
    }
    
    if (medicine.dosage.isBlank()) {
        issues.add("Пустая дозировка")
    }
    
    if (medicine.remainingQuantity <= 0) {
        issues.add("Нет остатка")
    }
    
    // Проверяем групповые данные
    if (medicine.groupId != null && medicine.groupName.isBlank()) {
        issues.add("Пустое название группы")
    }
    
    if (medicine.groupId != null && medicine.groupOrder <= 0) {
        issues.add("Некорректный порядок в группе")
    }
    
    if (issues.isNotEmpty()) {
        println("⚠️ ПРОБЛЕМЫ С ДАННЫМИ:")
        issues.forEach { issue ->
            println("  - $issue")
        }
    } else {
        println("✅ Данные корректны")
    }
}

/**
 * Получает описание дозировки (упрощенная версия)
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
    
    val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
        val times = medicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
        "в ${times.joinToString(", ")}"
    } else {
        "в ${medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
    }
    
    return "$frequencyText $timeText"
}

/**
 * Создает тестовые лекарства из XML (копия)
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
 * Запускает тест
 */
fun main() {
    testAdapterDisplayLogic()
} 