package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class DayChangeTest {

    @Test
    fun testDayChangeScenario() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val startDate = today.minusDays(10)
        
        // Создаем группу лекарств "через день"
        val lipetor = Medicine(
            id = 1,
            name = "Липетор",
            dosage = "1 таблетка",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(18, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            isActive = true,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 1, // Принимается в четные дни (0, 2, 4...)
            takenToday = false,
            quantity = 30,
            remainingQuantity = 30
        )
        
        val fubuxicin = Medicine(
            id = 2,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            time = LocalTime.of(18, 0),
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            isActive = true,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 2, // Принимается в нечетные дни (1, 3, 5...)
            takenToday = false,
            quantity = 30,
            remainingQuantity = 30
        )
        
        val medicines = listOf(lipetor, fubuxicin)
        
        println("=== ТЕСТ СМЕНЫ ДНЯ ===")
        println("Сегодня: $today")
        println("Завтра: $tomorrow")
        println("Дней с начала до сегодня: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, today)}")
        println("Дней с начала до завтра: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, tomorrow)}")
        
        // Шаг 1: Проверяем, какие лекарства должны приниматься сегодня
        println("\n=== ШАГ 1: ЛЕКАРСТВА НА СЕГОДНЯ ===")
        val todayMedicines = getActiveMedicinesForDate(medicines, today)
        println("Лекарства на сегодня: ${todayMedicines.map { it.name }}")
        
        // Шаг 2: Симулируем принятие лекарства сегодня
        println("\n=== ШАГ 2: ПРИНЯТИЕ ЛЕКАРСТВА СЕГОДНЯ ===")
        val takenMedicines = medicines.map { medicine ->
            if (todayMedicines.any { it.id == medicine.id }) {
                // Если лекарство должно приниматься сегодня, помечаем как принятое
                val taken = medicine.copy(
                    takenToday = true,
                    isMissed = false,
                    lastTakenTime = System.currentTimeMillis(),
                    takenAt = System.currentTimeMillis(),
                    remainingQuantity = medicine.remainingQuantity - 1
                )
                println("Принято сегодня: ${taken.name}")
                println("  - takenToday: ${taken.takenToday}")
                println("  - remainingQuantity: ${taken.remainingQuantity}")
                taken
            } else {
                medicine
            }
        }
        
        // Шаг 3: Симулируем смену дня (DailyResetService)
        println("\n=== ШАГ 3: СМЕНА ДНЯ (DailyResetService) ===")
        val resetMedicines = takenMedicines.map { medicine ->
            val reset = medicine.copy(
                takenToday = false,
                takenAt = 0,
                isOverdue = false
            )
            println("Сброшен статус: ${reset.name}")
            println("  - takenToday: ${reset.takenToday}")
            println("  - takenAt: ${reset.takenAt}")
            reset
        }
        
        // Шаг 4: Проверяем, какие лекарства должны приниматься завтра
        println("\n=== ШАГ 4: ЛЕКАРСТВА НА ЗАВТРА ===")
        val tomorrowMedicines = getActiveMedicinesForDate(resetMedicines, tomorrow)
        println("Лекарства на завтра: ${tomorrowMedicines.map { it.name }}")
        
        // Проверяем результаты
        val lipetorInToday = todayMedicines.any { it.id == 1L }
        val fubuxicinInToday = todayMedicines.any { it.id == 2L }
        val lipetorInTomorrow = tomorrowMedicines.any { it.id == 1L }
        val fubuxicinInTomorrow = tomorrowMedicines.any { it.id == 2L }
        
        println("\n=== РЕЗУЛЬТАТЫ ===")
        println("Липетор на сегодня: $lipetorInToday")
        println("Фубуксицин на сегодня: $fubuxicinInToday")
        println("Липетор на завтра: $lipetorInTomorrow")
        println("Фубуксицин на завтра: $fubuxicinInTomorrow")
        
        // Определяем, какой день группы сегодня и завтра
        val daysSinceStartToday = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val daysSinceStartTomorrow = java.time.temporal.ChronoUnit.DAYS.between(startDate, tomorrow)
        val groupDayToday = (daysSinceStartToday % 2).toInt()
        val groupDayTomorrow = (daysSinceStartTomorrow % 2).toInt()
        
        println("День группы сегодня: $groupDayToday (${if (groupDayToday == 0) "четный" else "нечетный"})")
        println("День группы завтра: $groupDayTomorrow (${if (groupDayTomorrow == 0) "четный" else "нечетный"})")
        
        // Проверяем логику "через день"
        if (groupDayToday == 0) {
            // Сегодня четный день - должен быть Липетор
            assertTrue("Липетор должен быть в списке на сегодня (четный день)", lipetorInToday)
            assertFalse("Фубуксицин не должен быть в списке на сегодня (четный день)", fubuxicinInToday)
            
            // Завтра нечетный день - должен быть Фубуксицин
            assertFalse("Липетор не должен быть в списке на завтра (нечетный день)", lipetorInTomorrow)
            assertTrue("Фубуксицин должен быть в списке на завтра (нечетный день)", fubuxicinInTomorrow)
        } else {
            // Сегодня нечетный день - должен быть Фубуксицин
            assertFalse("Липетор не должен быть в списке на сегодня (нечетный день)", lipetorInToday)
            assertTrue("Фубуксицин должен быть в списке на сегодня (нечетный день)", fubuxicinInToday)
            
            // Завтра четный день - должен быть Липетор
            assertTrue("Липетор должен быть в списке на завтра (четный день)", lipetorInTomorrow)
            assertFalse("Фубуксицин не должен быть в списке на завтра (четный день)", fubuxicinInTomorrow)
        }
        
        // Проверяем, что после смены дня статус сбросился
        val resetLipetor = resetMedicines.find { it.id == 1L }
        val resetFubuxicin = resetMedicines.find { it.id == 2L }
        
        assertNotNull("Липетор должен быть найден после сброса", resetLipetor)
        assertNotNull("Фубуксицин должен быть найден после сброса", resetFubuxicin)
        assertFalse("Статус takenToday должен быть сброшен для Липетора", resetLipetor!!.takenToday)
        assertFalse("Статус takenToday должен быть сброшен для Фубуксицина", resetFubuxicin!!.takenToday)
        assertEquals("takenAt должен быть сброшен для Липетора", 0, resetLipetor.takenAt)
        assertEquals("takenAt должен быть сброшен для Фубуксицина", 0, resetFubuxicin.takenAt)
    }
    
    /**
     * Симулирует функцию getActiveMedicinesForDate из DosageCalculator
     */
    private fun getActiveMedicinesForDate(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        println("=== ФИЛЬТРАЦИЯ АКТИВНЫХ ЛЕКАРСТВ ДЛЯ ДАТЫ: $date ===")
        val activeMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, date)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("Лекарство: ${medicine.name}")
            println("  - isActive: $isActive")
            println("  - shouldTake: $shouldTake")
            println("  - isActiveAndShouldTake: $isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Активных лекарств: ${activeMedicines.size}")
        
        // Принятые лекарства должны исчезать из списка "на сегодня"
        val medicinesForToday = activeMedicines.filter { medicine ->
            !medicine.takenToday
        }
        
        println("Результат: ${medicinesForToday.size} лекарств на дату $date")
        
        // Подробное логирование для отладки
        activeMedicines.forEach { medicine ->
            println("🔍 ФИЛЬТРАЦИЯ: ${medicine.name}")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - lastTakenTime: ${medicine.lastTakenTime}")
            println("  - В списке на $date: ${medicinesForToday.contains(medicine)}")
            println("  - Причина исключения: ${if (!medicinesForToday.contains(medicine)) "takenToday = true" else "включено"}")
        }
        
        return medicinesForToday
    }
    
    /**
     * Логика группировки лекарств (скопирована из DosageCalculator)
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        
        // Логика группы "через день"
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0, 1, 2, 3...)
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false  // Неизвестный порядок
                medicine.groupOrder % 2 == 1 -> groupDay == 0  // Нечетные порядки (1,3,5...) в четные дни
                medicine.groupOrder % 2 == 0 -> groupDay == 1  // Четные порядки (2,4,6...) в нечетные дни
                else -> false
            }
            
            return shouldTake
        }
        
        return false
    }
} 