package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class RealWorldGroupTest {

    @Test
    fun testRealWorldScenario() {
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
        
        println("=== РЕАЛЬНЫЙ СЦЕНАРИЙ ===")
        println("Сегодня: $today")
        println("Дней с начала: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, today)}")
        
        // Шаг 1: Проверяем начальное состояние
        println("\n=== ШАГ 1: НАЧАЛЬНОЕ СОСТОЯНИЕ ===")
        val initialActiveMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("Лекарство: ${medicine.name}")
            println("  - isActive: $isActive")
            println("  - shouldTake: $shouldTake")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - В списке 'на сегодня': ${isActiveAndShouldTake && !medicine.takenToday}")
            
            isActiveAndShouldTake && !medicine.takenToday
        }
        
        println("Начальный список 'на сегодня': ${initialActiveMedicines.map { it.name }}")
        
        // Шаг 2: Симулируем нажатие "принял" для Липетора
        println("\n=== ШАГ 2: НАЖАТИЕ 'ПРИНЯЛ' ДЛЯ ЛИПЕТОРА ===")
        val updatedMedicines = medicines.map { medicine ->
            if (medicine.id == 1L) { // Липетор
                val updated = medicine.copy(
                    takenToday = true,
                    remainingQuantity = medicine.remainingQuantity - 1,
                    lastTakenTime = System.currentTimeMillis()
                )
                println("Обновлен Липетор:")
                println("  - takenToday: ${updated.takenToday}")
                println("  - remainingQuantity: ${updated.remainingQuantity}")
                updated
            } else {
                medicine
            }
        }
        
        // Шаг 3: Проверяем состояние после обновления
        println("\n=== ШАГ 3: СОСТОЯНИЕ ПОСЛЕ ОБНОВЛЕНИЯ ===")
        val finalActiveMedicines = updatedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            
            println("Лекарство: ${medicine.name}")
            println("  - isActive: $isActive")
            println("  - shouldTake: $shouldTake")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - notTakenToday: $notTakenToday")
            println("  - В списке 'на сегодня': ${isActiveAndShouldTake && notTakenToday}")
            
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Финальный список 'на сегодня': ${finalActiveMedicines.map { it.name }}")
        
        // Проверяем результаты
        val lipetorShouldBeInInitial = initialActiveMedicines.any { it.id == 1L }
        val fubuxicinShouldBeInInitial = initialActiveMedicines.any { it.id == 2L }
        val lipetorShouldBeInFinal = finalActiveMedicines.any { it.id == 1L }
        val fubuxicinShouldBeInFinal = finalActiveMedicines.any { it.id == 2L }
        
        println("\n=== РЕЗУЛЬТАТЫ ===")
        println("Липетор в начальном списке: $lipetorShouldBeInInitial")
        println("Фубуксицин в начальном списке: $fubuxicinShouldBeInInitial")
        println("Липетор в финальном списке: $lipetorShouldBeInFinal")
        println("Фубуксицин в финальном списке: $fubuxicinShouldBeInFinal")
        
        // Определяем, какой день группы сегодня
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        val groupDay = (daysSinceStart % 2).toInt()
        println("День группы: $groupDay (${if (groupDay == 0) "четный" else "нечетный"})")
        
        // Проверяем ожидаемое поведение
        if (groupDay == 0) {
            // Четный день - должен быть Липетор
            assertTrue("Липетор должен быть в начальном списке в четный день", lipetorShouldBeInInitial)
            assertFalse("Фубуксицин не должен быть в начальном списке в четный день", fubuxicinShouldBeInInitial)
            assertFalse("Липетор не должен быть в финальном списке (принят)", lipetorShouldBeInFinal)
            assertFalse("Фубуксицин не должен быть в финальном списке", fubuxicinShouldBeInFinal)
        } else {
            // Нечетный день - должен быть Фубуксицин
            assertFalse("Липетор не должен быть в начальном списке в нечетный день", lipetorShouldBeInInitial)
            assertTrue("Фубуксицин должен быть в начальном списке в нечетный день", fubuxicinShouldBeInInitial)
            assertFalse("Липетор не должен быть в финальном списке", lipetorShouldBeInFinal)
            assertFalse("Фубуксицин не должен быть в финальном списке (принят)", fubuxicinShouldBeInFinal)
        }
        
        // В любом случае, после принятия лекарства список должен быть пустым
        assertTrue("Список 'на сегодня' должен быть пустым после принятия лекарства", finalActiveMedicines.isEmpty())
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