package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class MainActivityScenarioTest {

    @Test
    fun testMainActivityScenario() {
        val today = LocalDate.now()
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
        
        println("=== ТЕСТ СЦЕНАРИЯ MainActivity ===")
        println("Сегодня: $today")
        println("Дней с начала: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, today)}")
        
        // Шаг 1: Начальное состояние (как в loadTodayMedicines)
        println("\n=== ШАГ 1: НАЧАЛЬНОЕ СОСТОЯНИЕ (loadTodayMedicines) ===")
        val initialTodayMedicines = getActiveMedicinesForDate(medicines, today)
        println("Начальный список 'на сегодня': ${initialTodayMedicines.map { it.name }}")
        
        // Шаг 2: Симулируем нажатие "принял" для Липетора (как в takeMedicine)
        println("\n=== ШАГ 2: НАЖАТИЕ 'ПРИНЯЛ' ДЛЯ ЛИПЕТОРА (takeMedicine) ===")
        val updatedMedicines = medicines.map { medicine ->
            if (medicine.id == 1L) { // Липетор
                val updated = medicine.copy(
                    takenToday = true,
                    isMissed = false,
                    lastTakenTime = System.currentTimeMillis(),
                    takenAt = System.currentTimeMillis(),
                    remainingQuantity = medicine.remainingQuantity - 1
                )
                println("Обновлен Липетор:")
                println("  - takenToday: ${updated.takenToday}")
                println("  - remainingQuantity: ${updated.remainingQuantity}")
                println("  - lastTakenTime: ${updated.lastTakenTime}")
                updated
            } else {
                medicine
            }
        }
        
        // Шаг 3: Перезагружаем список (как в loadTodayMedicines после takeMedicine)
        println("\n=== ШАГ 3: ПЕРЕЗАГРУЗКА СПИСКА (loadTodayMedicines после takeMedicine) ===")
        val finalTodayMedicines = getActiveMedicinesForDate(updatedMedicines, today)
        println("Финальный список 'на сегодня': ${finalTodayMedicines.map { it.name }}")
        
        // Проверяем результаты
        val lipetorInInitial = initialTodayMedicines.any { it.id == 1L }
        val fubuxicinInInitial = initialTodayMedicines.any { it.id == 2L }
        val lipetorInFinal = finalTodayMedicines.any { it.id == 1L }
        val fubuxicinInFinal = finalTodayMedicines.any { it.id == 2L }
        
        println("\n=== РЕЗУЛЬТАТЫ ===")
        println("Липетор в начальном списке: $lipetorInInitial")
        println("Фубуксицин в начальном списке: $fubuxicinInInitial")
        println("Липетор в финальном списке: $lipetorInFinal")
        println("Фубуксицин в финальном списке: $fubuxicinInFinal")
        
        // Определяем, какой день группы сегодня
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        println("День группы: $groupDay (${if (groupDay == 0) "четный" else "нечетный"})")
        
        // Проверяем ожидаемое поведение
        if (groupDay == 0) {
            // Четный день - должен быть Липетор
            assertTrue("Липетор должен быть в начальном списке в четный день", lipetorInInitial)
            assertFalse("Фубуксицин не должен быть в начальном списке в четный день", fubuxicinInInitial)
            assertFalse("Липетор не должен быть в финальном списке (принят)", lipetorInFinal)
            assertFalse("Фубуксицин не должен быть в финальном списке", fubuxicinInFinal)
        } else {
            // Нечетный день - должен быть Фубуксицин
            assertFalse("Липетор не должен быть в начальном списке в нечетный день", lipetorInInitial)
            assertTrue("Фубуксицин должен быть в начальном списке в нечетный день", fubuxicinInInitial)
            assertFalse("Липетор не должен быть в финальном списке", lipetorInFinal)
            assertFalse("Фубуксицин не должен быть в финальном списке (принят)", fubuxicinInFinal)
        }
        
        // В любом случае, после принятия лекарства список должен быть пустым
        assertTrue("Список 'на сегодня' должен быть пустым после принятия лекарства", finalTodayMedicines.isEmpty())
        
        // Проверяем, что Фубуксицин НЕ появился в списке после принятия Липетора
        assertFalse("Фубуксицин не должен появиться в списке после принятия Липетора", fubuxicinInFinal)
    }
    
    /**
     * Симулирует функцию getActiveMedicinesForDate из DosageCalculator
     */
    private fun getActiveMedicinesForDate(medicines: List<Medicine>, date: LocalDate): List<Medicine> {
        println("=== ФИЛЬТРАЦИЯ АКТИВНЫХ ЛЕКАРСТВ ===")
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
        
        println("Результат: ${medicinesForToday.size} лекарств на сегодня")
        
        // Подробное логирование для отладки
        activeMedicines.forEach { medicine ->
            println("🔍 ФИЛЬТРАЦИЯ: ${medicine.name}")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - lastTakenTime: ${medicine.lastTakenTime}")
            println("  - В списке 'на сегодня': ${medicinesForToday.contains(medicine)}")
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