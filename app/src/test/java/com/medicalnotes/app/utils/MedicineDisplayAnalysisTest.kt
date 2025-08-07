package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Тест для анализа проблемы с отображением лекарств на сегодня
 * Использует данные из предоставленного XML
 */
class MedicineDisplayAnalysisTest {
    
    @Test
    fun testMedicineDisplayIssue() {
        println("=== ТЕСТ АНАЛИЗА ПРОБЛЕМЫ ОТОБРАЖЕНИЯ ЛЕКАРСТВ ===")
        
        // Создаем тестовые данные из XML
        val testMedicines = createTestMedicinesFromXML()
        
        // Получаем сегодняшнюю дату
        val today = LocalDate.now()
        println("Сегодняшняя дата: $today")
        
        // Анализируем каждое лекарство
        testMedicines.forEach { medicine ->
            println("\n--- АНАЛИЗ ЛЕКАРСТВА: ${medicine.name} ---")
            analyzeMedicine(medicine, today, testMedicines)
        }
        
        // Тестируем логику DosageCalculator
        println("\n=== ТЕСТИРОВАНИЕ DOSAGECALCULATOR ===")
        testDosageCalculatorLogic(testMedicines, today)
        
        // Генерируем отчет
        generateReport(testMedicines)
    }
    
    /**
     * Создает тестовые лекарства из предоставленного XML
     */
    private fun createTestMedicinesFromXML(): List<Medicine> {
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
     * Анализирует отдельное лекарство
     */
    private fun analyzeMedicine(medicine: Medicine, date: LocalDate, allMedicines: List<Medicine>) {
        println("ID: ${medicine.id}")
        println("Название: ${medicine.name}")
        println("Дозировка: ${medicine.dosage}")
        println("Время приема: ${medicine.time}")
        println("Частота: ${medicine.frequency}")
        println("Активно: ${medicine.isActive}")
        println("Принято сегодня: ${medicine.takenToday}")
        println("Последний прием: ${formatTimestamp(medicine.lastTakenTime)}")
        
        // Групповая информация
        if (medicine.groupId != null) {
            println("Группа ID: ${medicine.groupId}")
            println("Название группы: ${medicine.groupName}")
            println("Порядок в группе: ${medicine.groupOrder}")
            println("Дата начала группы: ${formatTimestamp(medicine.groupStartDate)}")
            println("Частота группы: ${medicine.groupFrequency}")
        }
        
        // Анализ дат
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        println("Дата начала приема: $startDate")
        
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        println("Дней с начала приема: $daysSinceStart")
        
        // Проверяем логику DosageCalculator
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, date, allMedicines)
        println("Должно приниматься сегодня: $shouldTake")
        
        // Проверяем статус
        val status = DosageCalculator.getMedicineStatus(medicine, date)
        println("Статус: $status")
        
        // Проверяем, будет ли показано в списке "на сегодня"
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, date)
        val isInTodayList = activeMedicines.any { it.id == medicine.id }
        println("В списке 'на сегодня': $isInTodayList")
        
        // Анализ проблем
        if (!isInTodayList) {
            println("❌ ПРОБЛЕМА: Лекарство не отображается в списке 'на сегодня'")
            if (!medicine.isActive) {
                println("   Причина: Лекарство неактивно")
            } else if (!shouldTake) {
                println("   Причина: Не должно приниматься сегодня по расписанию")
            } else if (medicine.takenToday) {
                println("   Причина: Уже принято сегодня")
            } else {
                println("   Причина: Неизвестная проблема")
            }
        } else {
            println("✅ Лекарство корректно отображается в списке 'на сегодня'")
        }
    }
    
    /**
     * Тестирует логику DosageCalculator
     */
    private fun testDosageCalculatorLogic(medicines: List<Medicine>, date: LocalDate) {
        println("\n--- ТЕСТ ЛОГИКИ DOSAGECALCULATOR ---")
        
        medicines.forEach { medicine ->
            println("\nТестируем: ${medicine.name}")
            
            // Тест shouldTakeMedicine
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, date, medicines)
            println("  shouldTakeMedicine: $shouldTake")
            
            // Тест getMedicineStatus
            val status = DosageCalculator.getMedicineStatus(medicine, date)
            println("  getMedicineStatus: $status")
            
            // Тест getActiveMedicinesForDate
            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, date)
            val isActive = activeMedicines.any { it.id == medicine.id }
            println("  getActiveMedicinesForDate: $isActive")
            
            // Детальный анализ групповой логики
            if (medicine.groupId != null) {
                println("  Групповая логика:")
                val groupStartDate = if (medicine.groupStartDate > 0) {
                    java.time.Instant.ofEpochMilli(medicine.groupStartDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                } else {
                    java.time.Instant.ofEpochMilli(medicine.startDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }
                
                val daysSinceGroupStart = java.time.temporal.ChronoUnit.DAYS.between(groupStartDate, date)
                val groupDay = (daysSinceGroupStart % 2).toInt()
                
                println("    Дата начала группы: $groupStartDate")
                println("    Дней с начала группы: $daysSinceGroupStart")
                println("    День группы (0/1): $groupDay")
                println("    Порядок лекарства: ${medicine.groupOrder}")
                
                val expectedTake = when {
                    medicine.groupOrder == 1 -> groupDay == 0
                    medicine.groupOrder == 2 -> groupDay == 1
                    else -> false
                }
                println("    Ожидаемый результат: $expectedTake")
                println("    Фактический результат: $shouldTake")
                
                if (expectedTake != shouldTake) {
                    println("    ⚠️ РАСХОЖДЕНИЕ В ЛОГИКЕ!")
                }
            }
        }
    }
    
    /**
     * Генерирует отчет
     */
    private fun generateReport(medicines: List<Medicine>) {
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
     * Форматирует timestamp в читаемый вид
     */
    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Не установлено"
        
        val date = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }
} 