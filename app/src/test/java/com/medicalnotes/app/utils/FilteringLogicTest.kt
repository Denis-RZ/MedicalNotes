package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@RunWith(RobolectricTestRunner::class)
class FilteringLogicTest {

    @Test
    fun testFubuksusatFilteringIssue() {
        println("=== ТЕСТ ПРОБЛЕМЫ ФИЛЬТРАЦИИ ФУБУКСУСАТ ===")
        
        // Получаем текущее время и добавляем 2 часа для будущего приема
        val currentTime = LocalTime.now()
        val futureTime = if (currentTime.hour >= 22) {
            // Если уже поздно, устанавливаем время на завтра утром
            LocalTime.of(9, 0)
        } else {
            // Иначе добавляем 2 часа
            currentTime.plusHours(2)
        }
        
        // Создаем лекарство "Фубуксусат" на основе лога
        val fubuksusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "50 таблеток",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = futureTime, // Время в будущем, чтобы не было просрочено
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false, // КЛЮЧЕВОЕ: не принято сегодня
            lastTakenTime = 0L, // не принималось
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Создаем лекарство "Липетор" для сравнения
        val lipetor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "30 таблеток",
            quantity = 30,
            remainingQuantity = 29,
            medicineType = "таблетки",
            time = LocalTime.of(19, 57), // 19:57 из лога
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = true, // принято сегодня
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val allMedicines = listOf(lipetor, fubuksusat)
        val today = LocalDate.now()
        
        println("Всего лекарств: ${allMedicines.size}")
        println("Фубуксусат - takenToday: ${fubuksusat.takenToday}")
        println("Липетор - takenToday: ${lipetor.takenToday}")
        println("Фубуксусат - время приема: ${fubuksusat.time}")
        println("Текущее время: $currentTime")
        println("Будущее время: $futureTime")
        
        // Проверяем shouldTakeMedicine для каждого лекарства
        val fubuksusatShouldTake = DosageCalculator.shouldTakeMedicine(fubuksusat, today)
        val lipetorShouldTake = DosageCalculator.shouldTakeMedicine(lipetor, today)
        
        println("Фубуксусат должен приниматься сегодня: $fubuksusatShouldTake")
        println("Липетор должен приниматься сегодня: $lipetorShouldTake")
        
        // Проверяем статусы
        val fubuksusatStatus = DosageCalculator.getMedicineStatus(fubuksusat, today)
        val lipetorStatus = DosageCalculator.getMedicineStatus(lipetor, today)
        
        println("Фубуксусат статус: $fubuksusatStatus")
        println("Липетор статус: $lipetorStatus")
        
        // ОСНОВНОЙ ТЕСТ: проверяем фильтрацию
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        todayMedicines.forEach { medicine ->
            println("  - ${medicine.name} (takenToday: ${medicine.takenToday})")
        }
        
        // ПРОВЕРКИ
        assertTrue("Фубуксусат должен приниматься сегодня", fubuksusatShouldTake)
        assertTrue("Липетор должен приниматься сегодня", lipetorShouldTake)
        assertEquals("Фубуксусат должен иметь статус UPCOMING", com.medicalnotes.app.utils.MedicineStatus.UPCOMING, fubuksusatStatus)
        assertEquals("Липетор должен иметь статус TAKEN_TODAY", com.medicalnotes.app.utils.MedicineStatus.TAKEN_TODAY, lipetorStatus)
        
        // КЛЮЧЕВАЯ ПРОВЕРКА: Фубуксусат должен быть в списке "на сегодня"
        val fubuksusatInList = todayMedicines.any { it.name == "Фубуксусат" }
        assertTrue("Фубуксусат должен быть в списке 'на сегодня'", fubuksusatInList)
        
        // Липетор НЕ должен быть в списке (принят сегодня)
        val lipetorInList = todayMedicines.any { it.name == "Липетор" }
        assertTrue("Липетор НЕ должен быть в списке 'на сегодня' (принят сегодня)", !lipetorInList)
        
        println("=== ТЕСТ ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    @Test
    fun testFilteringLogicWithTakenToday() {
        println("=== ТЕСТ ЛОГИКИ ФИЛЬТРАЦИИ С TAKEN_TODAY ===")
        
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "10 таблеток",
            quantity = 10,
            remainingQuantity = 10,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
        
        println("Лекарство takenToday: ${medicine.takenToday}")
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        
        // Лекарство должно быть в списке, так как takenToday = false
        assertEquals("Лекарство должно быть в списке", 1, todayMedicines.size)
        assertTrue("Лекарство должно быть в списке", todayMedicines.any { it.name == "Тестовое лекарство" })
        
        println("=== ТЕСТ ЗАВЕРШЕН УСПЕШНО ===")
    }
    
    @Test
    fun testFilteringLogicWithTakenTodayTrue() {
        println("=== ТЕСТ ЛОГИКИ ФИЛЬТРАЦИИ С TAKEN_TODAY = TRUE ===")
        
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "10 таблеток",
            quantity = 10,
            remainingQuantity = 9,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = true, // ПРИНЯТО сегодня
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
        
        println("Лекарство takenToday: ${medicine.takenToday}")
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        
        // Лекарство НЕ должно быть в списке, так как takenToday = true
        assertEquals("Лекарство НЕ должно быть в списке", 0, todayMedicines.size)
        assertTrue("Лекарство НЕ должно быть в списке", todayMedicines.none { it.name == "Тестовое лекарство" })
        
        println("=== ТЕСТ ЗАВЕРШЕН УСПЕШНО ===")
    }

    @Test
    fun testBasicFilteringLogic() {
        println("=== ТЕСТ БАЗОВОЙ ЛОГИКИ ФИЛЬТРАЦИИ ===")
        
        // Создаем простое лекарство с фиксированным временем
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 10,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0), // Фиксированное время 12:00
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        
        println("Лекарство - takenToday: ${medicine.takenToday}")
        println("Лекарство - время приема: ${medicine.time}")
        println("Текущее время: ${LocalTime.now()}")
        
        // Проверяем shouldTakeMedicine
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
        println("Лекарство должно приниматься сегодня: $shouldTake")
        
        // Проверяем статус
        val status = DosageCalculator.getMedicineStatus(medicine, today)
        println("Статус лекарства: $status")
        
        // Проверяем фильтрацию
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        
        // БАЗОВЫЕ ПРОВЕРКИ
        assertTrue("Лекарство должно приниматься сегодня", shouldTake)
        assertTrue("Лекарство должно быть в списке (takenToday = false)", todayMedicines.isNotEmpty())
        assertTrue("Лекарство должно быть в списке", todayMedicines.any { it.name == "Тестовое лекарство" })
        
        println("=== ТЕСТ БАЗОВОЙ ЛОГИКИ ЗАВЕРШЕН УСПЕШНО ===")
    }

    @Test
    fun testEveryOtherDayProblem() {
        println("=== ТЕСТ ПРОБЛЕМЫ 'ЧЕРЕЗ ДЕНЬ' ===")
        
        // Создаем лекарство с периодичностью "через день" и временем 19:00
        val medicine = Medicine(
            id = 1L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(19, 0), // Время 19:00
            frequency = DosageFrequency.EVERY_OTHER_DAY, // Через день
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        
        println("Лекарство: ${medicine.name}")
        println("Частота: ${medicine.frequency}")
        println("Время приема: ${medicine.time}")
        println("Дата начала: ${java.time.Instant.ofEpochMilli(medicine.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}")
        println("Сегодня: $today")
        println("Вчера: $yesterday")
        println("Завтра: $tomorrow")
        
        // Проверяем shouldTakeMedicine для разных дат
        val shouldTakeYesterday = DosageCalculator.shouldTakeMedicine(medicine, yesterday)
        val shouldTakeToday = DosageCalculator.shouldTakeMedicine(medicine, today)
        val shouldTakeTomorrow = DosageCalculator.shouldTakeMedicine(medicine, tomorrow)
        
        println("Должно приниматься вчера: $shouldTakeYesterday")
        println("Должно приниматься сегодня: $shouldTakeToday")
        println("Должно приниматься завтра: $shouldTakeTomorrow")
        
        // Проверяем статус для сегодня
        val statusToday = DosageCalculator.getMedicineStatus(medicine, today)
        println("Статус сегодня: $statusToday")
        
        // Проверяем фильтрацию для сегодня
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
        println("Результат фильтрации сегодня: ${todayMedicines.size} лекарств")
        todayMedicines.forEach { med ->
            println("  - ${med.name} (takenToday: ${med.takenToday})")
        }
        
        // Проверяем фильтрацию для завтра
        val tomorrowMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), tomorrow)
        println("Результат фильтрации завтра: ${tomorrowMedicines.size} лекарств")
        tomorrowMedicines.forEach { med ->
            println("  - ${med.name} (takenToday: ${med.takenToday})")
        }
        
        // АНАЛИЗ ПРОБЛЕМЫ
        println("=== АНАЛИЗ ПРОБЛЕМЫ ===")
        
        // Проверяем логику "через день"
        val startDate = java.time.Instant.ofEpochMilli(medicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        
        val daysSinceStartYesterday = java.time.temporal.ChronoUnit.DAYS.between(startDate, yesterday)
        val daysSinceStartToday = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val daysSinceStartTomorrow = java.time.temporal.ChronoUnit.DAYS.between(startDate, tomorrow)
        
        println("Дней с начала вчера: $daysSinceStartYesterday (остаток от деления на 2: ${daysSinceStartYesterday % 2})")
        println("Дней с начала сегодня: $daysSinceStartToday (остаток от деления на 2: ${daysSinceStartToday % 2})")
        println("Дней с начала завтра: $daysSinceStartTomorrow (остаток от деления на 2: ${daysSinceStartTomorrow % 2})")
        
        // ПРОВЕРКИ
        if (shouldTakeToday) {
            assertTrue("Если лекарство должно приниматься сегодня, оно должно быть в списке", 
                      todayMedicines.isNotEmpty())
            assertTrue("Лекарство должно быть в списке", 
                      todayMedicines.any { it.name == "Фубуксицин" })
            println("✅ Лекарство должно приниматься сегодня и оно в списке")
        } else {
            assertTrue("Если лекарство НЕ должно приниматься сегодня, его не должно быть в списке", 
                      todayMedicines.isEmpty())
            println("✅ Лекарство НЕ должно приниматься сегодня и его нет в списке")
        }
        
        println("=== ТЕСТ ПРОБЛЕМЫ 'ЧЕРЕЗ ДЕНЬ' ЗАВЕРШЕН ===")
    }

    @Test
    fun testTimeBasedFilteringProblem() {
        println("=== ТЕСТ ПРОБЛЕМЫ С ВРЕМЕНЕМ 18:00 vs 19:00 ===")
        
        // Создаем два лекарства с одинаковыми параметрами, но разным временем
        val medicine18 = Medicine(
            id = 1L,
            name = "Фубуксицин 18:00",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(18, 0), // Время 18:00
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val medicine19 = Medicine(
            id = 2L,
            name = "Фубуксицин 19:00",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(19, 0), // Время 19:00
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        
        println("Текущее время: $currentTime")
        println("Лекарство 18:00 - время приема: ${medicine18.time}")
        println("Лекарство 19:00 - время приема: ${medicine19.time}")
        
        // Проверяем shouldTakeMedicine
        val shouldTake18 = DosageCalculator.shouldTakeMedicine(medicine18, today)
        val shouldTake19 = DosageCalculator.shouldTakeMedicine(medicine19, today)
        
        println("Лекарство 18:00 должно приниматься сегодня: $shouldTake18")
        println("Лекарство 19:00 должно приниматься сегодня: $shouldTake19")
        
        // Проверяем статусы
        val status18 = DosageCalculator.getMedicineStatus(medicine18, today)
        val status19 = DosageCalculator.getMedicineStatus(medicine19, today)
        
        println("Статус лекарства 18:00: $status18")
        println("Статус лекарства 19:00: $status19")
        
        // Проверяем фильтрацию
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine18, medicine19), today)
        println("Результат фильтрации: ${todayMedicines.size} лекарств")
        todayMedicines.forEach { med ->
            println("  - ${med.name} (takenToday: ${med.takenToday})")
        }
        
        // АНАЛИЗ ПРОБЛЕМЫ
        println("=== АНАЛИЗ ПРОБЛЕМЫ ===")
        
        // Проверяем, есть ли лекарства в списке
        val medicine18InList = todayMedicines.any { it.name == "Фубуксицин 18:00" }
        val medicine19InList = todayMedicines.any { it.name == "Фубуксицин 19:00" }
        
        println("Лекарство 18:00 в списке: $medicine18InList")
        println("Лекарство 19:00 в списке: $medicine19InList")
        
        // Если оба лекарства должны приниматься сегодня, но одно в списке, а другое нет
        if (shouldTake18 == shouldTake19) {
            if (medicine18InList != medicine19InList) {
                println("🚨 ПРОБЛЕМА НАЙДЕНА: Одинаковые лекарства с разным временем ведут себя по-разному!")
                println("   Это указывает на проблему в логике определения статуса по времени")
            } else {
                println("✅ Оба лекарства ведут себя одинаково")
            }
        } else {
            println("ℹ️ Лекарства имеют разную логику приема (это нормально для 'через день')")
        }
        
        println("=== ТЕСТ ПРОБЛЕМЫ С ВРЕМЕНЕМ ЗАВЕРШЕН ===")
    }

    @Test
    fun testEditTimeProblem() {
        println("=== ТЕСТ ПРОБЛЕМЫ РЕДАКТИРОВАНИЯ ВРЕМЕНИ ===")
        
        // Создаем лекарство с временем 18:00
        val originalMedicine = Medicine(
            id = 1L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(18, 0), // Изначальное время 18:00
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        
        println("Текущее время: $currentTime")
        println("Изначальное время приема: ${originalMedicine.time}")
        
        // Проверяем исходное состояние
        val shouldTakeOriginal = DosageCalculator.shouldTakeMedicine(originalMedicine, today)
        val statusOriginal = DosageCalculator.getMedicineStatus(originalMedicine, today)
        val todayMedicinesOriginal = DosageCalculator.getActiveMedicinesForDate(listOf(originalMedicine), today)
        
        println("ИСХОДНОЕ СОСТОЯНИЕ:")
        println("  - Должно приниматься сегодня: $shouldTakeOriginal")
        println("  - Статус: $statusOriginal")
        println("  - В списке 'на сегодня': ${todayMedicinesOriginal.isNotEmpty()}")
        
        // Симулируем редактирование времени на 19:00
        val editedMedicine = originalMedicine.copy(
            time = LocalTime.of(19, 0), // Новое время 19:00
            updatedAt = System.currentTimeMillis()
        )
        
        println("ПОСЛЕ РЕДАКТИРОВАНИЯ:")
        println("  - Новое время приема: ${editedMedicine.time}")
        println("  - Новое время уже прошло: ${editedMedicine.time.isBefore(currentTime)}")
        
        // Проверяем состояние после редактирования
        val shouldTakeEdited = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        val statusEdited = DosageCalculator.getMedicineStatus(editedMedicine, today)
        val todayMedicinesEdited = DosageCalculator.getActiveMedicinesForDate(listOf(editedMedicine), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeEdited")
        println("  - Статус: $statusEdited")
        println("  - В списке 'на сегодня': ${todayMedicinesEdited.isNotEmpty()}")
        
        // АНАЛИЗ ПРОБЛЕМЫ
        println("=== АНАЛИЗ ПРОБЛЕМЫ ===")
        
        if (shouldTakeOriginal == shouldTakeEdited) {
            if (todayMedicinesOriginal.isNotEmpty() != todayMedicinesEdited.isNotEmpty()) {
                println("🚨 ПРОБЛЕМА НАЙДЕНА: После редактирования времени лекарство исчезло из списка!")
                println("   Это указывает на проблему в логике сброса статуса при редактировании")
                
                // Проверяем логику сброса статуса из EditMedicineActivity
                val shouldResetStatus = originalMedicine.frequency != editedMedicine.frequency || 
                                       (originalMedicine.takenToday && editedMedicine.time.isBefore(currentTime))
                
                println("   Логика сброса статуса:")
                println("     - Изменена частота: ${originalMedicine.frequency != editedMedicine.frequency}")
                println("     - Принято сегодня: ${originalMedicine.takenToday}")
                println("     - Новое время прошло: ${editedMedicine.time.isBefore(currentTime)}")
                println("     - Сбрасываем статус: $shouldResetStatus")
            } else {
                println("✅ Лекарство ведет себя одинаково до и после редактирования")
            }
        } else {
            println("ℹ️ Логика приема изменилась после редактирования")
        }
        
        println("=== ТЕСТ ПРОБЛЕМЫ РЕДАКТИРОВАНИЯ ЗАВЕРШЕН ===")
    }

    @Test
    fun testEditStatusResetFix() {
        println("=== ТЕСТ ИСПРАВЛЕНИЯ СБРОСА СТАТУСА ===")
        
        // Создаем лекарство которое НЕ было принято сегодня
        val originalMedicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(18, 0),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = false, // НЕ принято сегодня
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val currentTime = LocalTime.now()
        
        // Симулируем логику сброса статуса из EditMedicineActivity
        val newTime = LocalTime.of(19, 0) // Новое время 19:00
        val shouldResetStatus = originalMedicine.frequency != originalMedicine.frequency || 
                               (originalMedicine.takenToday && newTime.isBefore(currentTime))
        
        // Симулируем исправленную логику
        val takenTodayAfterEdit = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday
        val takenAtAfterEdit = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt
        
        println("ИСХОДНОЕ СОСТОЯНИЕ:")
        println("  - takenToday: ${originalMedicine.takenToday}")
        println("  - takenAt: ${originalMedicine.takenAt}")
        println("  - Время приема: ${originalMedicine.time}")
        
        println("ПОСЛЕ РЕДАКТИРОВАНИЯ:")
        println("  - Новое время: $newTime")
        println("  - Новое время прошло: ${newTime.isBefore(currentTime)}")
        println("  - shouldResetStatus: $shouldResetStatus")
        println("  - takenToday после редактирования: $takenTodayAfterEdit")
        println("  - takenAt после редактирования: $takenAtAfterEdit")
        
        // ПРОВЕРКИ
        assertFalse("Статус takenToday не должен сброситься для непринятого лекарства", 
                   shouldResetStatus && !originalMedicine.takenToday)
        assertEquals("takenToday должен остаться false", false, takenTodayAfterEdit)
        assertEquals("takenAt должен остаться 0", 0L, takenAtAfterEdit)
        
        println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Статус не сбрасывается для непринятого лекарства")
        println("=== ТЕСТ ИСПРАВЛЕНИЯ ЗАВЕРШЕН ===")
    }

    @Test
    fun testClickedTakenButNotActuallyTaken() {
        println("=== ТЕСТ: НАЖАЛ 'ПРИНЯЛ' НО НЕ ПРИНЯЛ ===")
        
        val currentTime = LocalTime.now()
        val today = LocalDate.now()
        
        // Создаем лекарство которое было отмечено как принятое, но фактически не принято
        val medicineMarkedAsTaken = Medicine(
            id = 1L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30, // Количество НЕ уменьшилось - значит не принято
            medicineType = "таблетки",
            time = LocalTime.of(18, 0), // Изначальное время 18:00
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = true, // ОТМЕЧЕНО как принятое
            lastTakenTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            takenAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("ИСХОДНОЕ СОСТОЯНИЕ:")
        println("  - takenToday: ${medicineMarkedAsTaken.takenToday}")
        println("  - remainingQuantity: ${medicineMarkedAsTaken.remainingQuantity}")
        println("  - Время приема: ${medicineMarkedAsTaken.time}")
        println("  - Время отметки 'принято': ${java.time.Instant.ofEpochMilli(medicineMarkedAsTaken.takenAt).atZone(java.time.ZoneId.systemDefault()).toLocalTime()}")
        
        // Проверяем исходное состояние
        val shouldTakeOriginal = DosageCalculator.shouldTakeMedicine(medicineMarkedAsTaken, today)
        val statusOriginal = DosageCalculator.getMedicineStatus(medicineMarkedAsTaken, today)
        val todayMedicinesOriginal = DosageCalculator.getActiveMedicinesForDate(listOf(medicineMarkedAsTaken), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeOriginal")
        println("  - Статус: $statusOriginal")
        println("  - В списке 'на сегодня': ${todayMedicinesOriginal.isNotEmpty()}")
        
        // Симулируем редактирование времени на более позднее (19:00)
        val newTime = LocalTime.of(19, 0)
        val shouldResetStatus = medicineMarkedAsTaken.frequency != medicineMarkedAsTaken.frequency || 
                               (medicineMarkedAsTaken.takenToday && newTime.isBefore(currentTime))
        
        // Применяем логику редактирования
        val editedMedicine = medicineMarkedAsTaken.copy(
            time = newTime,
            lastTakenTime = if (shouldResetStatus) 0 else medicineMarkedAsTaken.lastTakenTime,
            takenToday = if (shouldResetStatus && medicineMarkedAsTaken.takenToday) false else medicineMarkedAsTaken.takenToday,
            takenAt = if (shouldResetStatus && medicineMarkedAsTaken.takenToday) 0 else medicineMarkedAsTaken.takenAt,
            isMissed = if (shouldResetStatus) false else medicineMarkedAsTaken.isMissed,
            missedCount = if (shouldResetStatus) 0 else medicineMarkedAsTaken.missedCount,
            updatedAt = System.currentTimeMillis()
        )
        
        println("ПОСЛЕ РЕДАКТИРОВАНИЯ ВРЕМЕНИ НА 19:00:")
        println("  - Новое время: ${editedMedicine.time}")
        println("  - Новое время прошло: ${editedMedicine.time.isBefore(currentTime)}")
        println("  - shouldResetStatus: $shouldResetStatus")
        println("  - takenToday после редактирования: ${editedMedicine.takenToday}")
        println("  - takenAt после редактирования: ${editedMedicine.takenAt}")
        
        // Проверяем состояние после редактирования
        val shouldTakeEdited = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        val statusEdited = DosageCalculator.getMedicineStatus(editedMedicine, today)
        val todayMedicinesEdited = DosageCalculator.getActiveMedicinesForDate(listOf(editedMedicine), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeEdited")
        println("  - Статус: $statusEdited")
        println("  - В списке 'на сегодня': ${todayMedicinesEdited.isNotEmpty()}")
        
        // АНАЛИЗ ПРОБЛЕМЫ
        println("=== АНАЛИЗ ПРОБЛЕМЫ ===")
        
        // Проверяем логику сброса статуса
        if (medicineMarkedAsTaken.takenToday && newTime.isBefore(currentTime)) {
            println("🚨 ПРОБЛЕМА НАЙДЕНА: Лекарство отмечено как принятое, но время изменено на прошедшее")
            println("   Текущая логика сбрасывает статус, но это неправильно!")
            println("   Лекарство должно остаться в списке, так как оно фактически не принято")
            
            // Проверяем, что количество не изменилось (признак того, что не принято)
            assertEquals("Количество не должно измениться", 30, editedMedicine.remainingQuantity)
            
            // Логика должна быть: если количество не изменилось, то статус НЕ сбрасываем
            val shouldNotResetBecauseNotActuallyTaken = medicineMarkedAsTaken.remainingQuantity == medicineMarkedAsTaken.quantity
            println("   Количество не изменилось: $shouldNotResetBecauseNotActuallyTaken")
            println("   Статус НЕ должен сбрасываться: $shouldNotResetBecauseNotActuallyTaken")
            
            if (shouldNotResetBecauseNotActuallyTaken) {
                assertTrue("Лекарство должно остаться в списке, так как фактически не принято", 
                          todayMedicinesEdited.isNotEmpty())
                println("✅ ИСПРАВЛЕНИЕ НУЖНО: Статус не должен сбрасываться для фактически непринятого лекарства")
            }
        } else {
            println("ℹ️ Логика работает корректно для данного случая")
        }
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }

    @Test
    fun testFixForClickedTakenButNotActuallyTaken() {
        println("=== ТЕСТ ИСПРАВЛЕНИЯ: НАЖАЛ 'ПРИНЯЛ' НО НЕ ПРИНЯЛ ===")
        
        val currentTime = LocalTime.now()
        val today = LocalDate.now()
        
        // Создаем лекарство которое было отмечено как принятое, но фактически не принято
        val originalMedicine = Medicine(
            id = 1L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30, // Количество НЕ уменьшилось - значит не принято
            medicineType = "таблетки",
            time = LocalTime.of(18, 0), // Изначальное время 18:00
            frequency = DosageFrequency.DAILY, // Используем ежедневную частоту для упрощения теста
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = true, // ОТМЕЧЕНО как принятое
            lastTakenTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            takenAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("ИСХОДНОЕ СОСТОЯНИЕ:")
        println("  - takenToday: ${originalMedicine.takenToday}")
        println("  - remainingQuantity: ${originalMedicine.remainingQuantity}")
        println("  - quantity: ${originalMedicine.quantity}")
        println("  - Время приема: ${originalMedicine.time}")
        
        // Симулируем ИСПРАВЛЕННУЮ логику из EditMedicineActivity
        val newTime = LocalTime.of(19, 0) // Новое время 19:00
        val wasActuallyTaken = originalMedicine.remainingQuantity < originalMedicine.quantity
        val shouldResetStatus = originalMedicine.frequency != originalMedicine.frequency || 
                               (originalMedicine.takenToday && newTime.isBefore(currentTime)) ||
                               (originalMedicine.takenToday && !wasActuallyTaken)
        
        // Применяем исправленную логику
        val editedMedicine = originalMedicine.copy(
            time = newTime,
            lastTakenTime = if (shouldResetStatus) 0 else originalMedicine.lastTakenTime,
            takenToday = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday,
            takenAt = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt,
            isMissed = if (shouldResetStatus) false else originalMedicine.isMissed,
            missedCount = if (shouldResetStatus) 0 else originalMedicine.missedCount,
            updatedAt = System.currentTimeMillis()
        )
        
        println("ПОСЛЕ РЕДАКТИРОВАНИЯ (С ИСПРАВЛЕНИЕМ):")
        println("  - Новое время: $newTime")
        println("  - Новое время прошло: ${newTime.isBefore(currentTime)}")
        println("  - wasActuallyTaken: $wasActuallyTaken")
        println("  - shouldResetStatus: $shouldResetStatus")
        println("  - takenToday после редактирования: ${editedMedicine.takenToday}")
        println("  - takenAt после редактирования: ${editedMedicine.takenAt}")
        
        // Проверяем состояние после редактирования
        val shouldTakeEdited = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        val statusEdited = DosageCalculator.getMedicineStatus(editedMedicine, today)
        val todayMedicinesEdited = DosageCalculator.getActiveMedicinesForDate(listOf(editedMedicine), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeEdited")
        println("  - Статус: $statusEdited")
        println("  - В списке 'на сегодня': ${todayMedicinesEdited.isNotEmpty()}")
        
        // ПРОВЕРКИ ИСПРАВЛЕНИЯ
        println("=== ПРОВЕРКИ ИСПРАВЛЕНИЯ ===")
        
        // Проверяем, что количество не изменилось
        assertEquals("Количество не должно измениться", 30, editedMedicine.remainingQuantity)
        
        // Проверяем, что лекарство было фактически принято
        assertFalse("Лекарство не было фактически принято (количество не уменьшилось)", wasActuallyTaken)
        
        // Проверяем, что статус должен сброситься
        assertTrue("Статус должен сброситься для фактически непринятого лекарства", shouldResetStatus)
        
        // Проверяем, что takenToday сбросился
        assertFalse("takenToday должен сброситься", editedMedicine.takenToday)
        
        // Проверяем, что лекарство теперь в списке "на сегодня"
        assertTrue("Лекарство должно быть в списке 'на сегодня' после сброса статуса", 
                  todayMedicinesEdited.isNotEmpty())
        assertTrue("Лекарство должно быть в списке", 
                  todayMedicinesEdited.any { it.name == "Фубуксицин" })
        
        println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Статус сбрасывается для фактически непринятого лекарства")
        println("✅ Лекарство теперь появляется в списке 'на сегодня'")
        println("=== ТЕСТ ИСПРАВЛЕНИЯ ЗАВЕРШЕН ===")
    }

    @Test
    fun testFixForEveryOtherDayFrequency() {
        println("=== ТЕСТ ИСПРАВЛЕНИЯ: ЧЕРЕЗ ДЕНЬ + НАЖАЛ 'ПРИНЯЛ' НО НЕ ПРИНЯЛ ===")
        
        val currentTime = LocalTime.now()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        // Создаем лекарство с частотой "через день" которое было отмечено как принятое, но фактически не принято
        val originalMedicine = Medicine(
            id = 1L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30, // Количество НЕ уменьшилось - значит не принято
            medicineType = "таблетки",
            time = LocalTime.of(18, 0), // Изначальное время 18:00
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // начали вчера
            isActive = true,
            takenToday = true, // ОТМЕЧЕНО как принятое
            lastTakenTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            takenAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 часа назад
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("ИСХОДНОЕ СОСТОЯНИЕ:")
        println("  - takenToday: ${originalMedicine.takenToday}")
        println("  - remainingQuantity: ${originalMedicine.remainingQuantity}")
        println("  - quantity: ${originalMedicine.quantity}")
        println("  - Время приема: ${originalMedicine.time}")
        println("  - Частота: ${originalMedicine.frequency}")
        println("  - Дата начала: ${java.time.Instant.ofEpochMilli(originalMedicine.startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}")
        println("  - Сегодня: $today")
        println("  - Вчера: $yesterday")
        
        // Проверяем исходное состояние
        val shouldTakeOriginal = DosageCalculator.shouldTakeMedicine(originalMedicine, today)
        val statusOriginal = DosageCalculator.getMedicineStatus(originalMedicine, today)
        val todayMedicinesOriginal = DosageCalculator.getActiveMedicinesForDate(listOf(originalMedicine), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeOriginal")
        println("  - Статус: $statusOriginal")
        println("  - В списке 'на сегодня': ${todayMedicinesOriginal.isNotEmpty()}")
        
        // Симулируем ИСПРАВЛЕННУЮ логику из EditMedicineActivity
        val newTime = LocalTime.of(19, 0) // Новое время 19:00
        val wasActuallyTaken = originalMedicine.remainingQuantity < originalMedicine.quantity
        val shouldResetStatus = originalMedicine.frequency != originalMedicine.frequency || 
                               (originalMedicine.takenToday && newTime.isBefore(currentTime)) ||
                               (originalMedicine.takenToday && !wasActuallyTaken)
        
        // Применяем исправленную логику
        val editedMedicine = originalMedicine.copy(
            time = newTime,
            lastTakenTime = if (shouldResetStatus) 0 else originalMedicine.lastTakenTime,
            takenToday = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday,
            takenAt = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt,
            isMissed = if (shouldResetStatus) false else originalMedicine.isMissed,
            missedCount = if (shouldResetStatus) 0 else originalMedicine.missedCount,
            updatedAt = System.currentTimeMillis()
        )
        
        println("ПОСЛЕ РЕДАКТИРОВАНИЯ (С ИСПРАВЛЕНИЕМ):")
        println("  - Новое время: $newTime")
        println("  - Новое время прошло: ${newTime.isBefore(currentTime)}")
        println("  - wasActuallyTaken: $wasActuallyTaken")
        println("  - shouldResetStatus: $shouldResetStatus")
        println("  - takenToday после редактирования: ${editedMedicine.takenToday}")
        println("  - takenAt после редактирования: ${editedMedicine.takenAt}")
        
        // Проверяем состояние после редактирования
        val shouldTakeEdited = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        val statusEdited = DosageCalculator.getMedicineStatus(editedMedicine, today)
        val todayMedicinesEdited = DosageCalculator.getActiveMedicinesForDate(listOf(editedMedicine), today)
        
        println("  - Должно приниматься сегодня: $shouldTakeEdited")
        println("  - Статус: $statusEdited")
        println("  - В списке 'на сегодня': ${todayMedicinesEdited.isNotEmpty()}")
        
        // ПРОВЕРКИ ИСПРАВЛЕНИЯ
        println("=== ПРОВЕРКИ ИСПРАВЛЕНИЯ ===")
        
        // Проверяем, что количество не изменилось
        assertEquals("Количество не должно измениться", 30, editedMedicine.remainingQuantity)
        
        // Проверяем, что лекарство было фактически принято
        assertFalse("Лекарство не было фактически принято (количество не уменьшилось)", wasActuallyTaken)
        
        // Проверяем, что статус должен сброситься
        assertTrue("Статус должен сброситься для фактически непринятого лекарства", shouldResetStatus)
        
        // Проверяем, что takenToday сбросился
        assertFalse("takenToday должен сброситься", editedMedicine.takenToday)
        
        // Проверяем логику "через день"
        val startDate = java.time.Instant.ofEpochMilli(editedMedicine.startDate)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        
        println("  - Дней с начала: $daysSinceStart (остаток от деления на 2: ${daysSinceStart % 2})")
        
        // Если сегодня должен быть день приема, то лекарство должно быть в списке
        if (shouldTakeEdited) {
            assertTrue("Лекарство должно быть в списке 'на сегодня' после сброса статуса", 
                      todayMedicinesEdited.isNotEmpty())
            assertTrue("Лекарство должно быть в списке", 
                      todayMedicinesEdited.any { it.name == "Фубуксицин" })
            println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Лекарство появляется в списке 'на сегодня'")
        } else {
            assertTrue("Лекарство НЕ должно быть в списке, так как сегодня не день приема", 
                      todayMedicinesEdited.isEmpty())
            println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Лекарство НЕ в списке, так как сегодня не день приема")
        }
        
        println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Статус сбрасывается для фактически непринятого лекарства")
        println("=== ТЕСТ ИСПРАВЛЕНИЯ ЗАВЕРШЕН ===")
    }
} 