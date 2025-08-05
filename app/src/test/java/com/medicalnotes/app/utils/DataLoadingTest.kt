package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class DataLoadingTest {

    @Test
    fun testDataLoadingScenario() {
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
        
        println("=== ТЕСТ ЗАГРУЗКИ ДАННЫХ ===")
        println("Сегодня: $today")
        println("Дней с начала: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, today)}")
        
        // Шаг 1: Начальное состояние
        println("\n=== ШАГ 1: НАЧАЛЬНОЕ СОСТОЯНИЕ ===")
        val initialTodayMedicines = getActiveMedicinesForDate(medicines, today)
        println("Начальный список 'на сегодня': ${initialTodayMedicines.map { it.name }}")
        
        // Шаг 2: Симулируем сохранение в базу данных (как в DataManager)
        println("\n=== ШАГ 2: СОХРАНЕНИЕ В БАЗУ ДАННЫХ ===")
        val savedMedicines = medicines.map { medicine ->
            if (medicine.id == 1L) { // Липетор
                val saved = medicine.copy(
                    takenToday = true,
                    isMissed = false,
                    lastTakenTime = System.currentTimeMillis(),
                    takenAt = System.currentTimeMillis(),
                    remainingQuantity = medicine.remainingQuantity - 1,
                    updatedAt = System.currentTimeMillis()
                )
                println("Сохранен Липетор:")
                println("  - takenToday: ${saved.takenToday}")
                println("  - remainingQuantity: ${saved.remainingQuantity}")
                println("  - lastTakenTime: ${saved.lastTakenTime}")
                println("  - updatedAt: ${saved.updatedAt}")
                saved
            } else {
                medicine
            }
        }
        
        // Шаг 3: Симулируем загрузку из базы данных (как в MedicineRepository)
        println("\n=== ШАГ 3: ЗАГРУЗКА ИЗ БАЗЫ ДАННЫХ ===")
        val loadedMedicines = savedMedicines // В реальности это было бы загрузкой из файла/БД
        println("Загружено лекарств из БД: ${loadedMedicines.size}")
        loadedMedicines.forEach { medicine ->
            println("Загружено: ${medicine.name}")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - remainingQuantity: ${medicine.remainingQuantity}")
            println("  - lastTakenTime: ${medicine.lastTakenTime}")
        }
        
        // Шаг 4: Применяем фильтрацию к загруженным данным
        println("\n=== ШАГ 4: ФИЛЬТРАЦИЯ ЗАГРУЖЕННЫХ ДАННЫХ ===")
        val finalTodayMedicines = getActiveMedicinesForDate(loadedMedicines, today)
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
        
        // Проверяем, что данные корректно сохранились и загрузились
        val savedLipetor = savedMedicines.find { it.id == 1L }
        assertNotNull("Липетор должен быть найден в сохраненных данных", savedLipetor)
        assertTrue("Липетор должен быть помечен как принятый сегодня", savedLipetor!!.takenToday)
        assertEquals("Количество Липетора должно уменьшиться на 1", 29, savedLipetor.remainingQuantity)
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