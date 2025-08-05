package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class GroupFilteringIssueTest {

    @Test
    fun testGroupFilteringIssue() {
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
        
        println("=== ТЕСТ ПРОБЛЕМЫ ГРУППИРОВКИ ===")
        println("Сегодня: $today")
        println("Дней с начала: ${java.time.temporal.ChronoUnit.DAYS.between(startDate, today)}")
        
        // Проверяем, какие лекарства должны приниматься сегодня
        val activeMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("Лекарство: ${medicine.name}")
            println("  - isActive: $isActive")
            println("  - shouldTake: $shouldTake")
            println("  - isActiveAndShouldTake: $isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Активных лекарств: ${activeMedicines.size}")
        
        // Симулируем нажатие "принял" для Липетора
        val updatedMedicines = medicines.map { medicine ->
            if (medicine.id == 1L) { // Липетор
                medicine.copy(
                    takenToday = true,
                    remainingQuantity = medicine.remainingQuantity - 1,
                    lastTakenTime = System.currentTimeMillis()
                )
            } else {
                medicine
            }
        }
        
        println("=== ПОСЛЕ НАЖАТИЯ 'ПРИНЯЛ' ДЛЯ ЛИПЕТОРА ===")
        
        // Проверяем, какие лекарства должны показываться в списке "на сегодня"
        val medicinesForToday = updatedMedicines.filter { medicine ->
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
        
        println("Лекарств в списке 'на сегодня': ${medicinesForToday.size}")
        
        // Проверяем, что Фубуксицин НЕ должен быть в списке "на сегодня"
        val fubuxicinInTodayList = medicinesForToday.any { it.id == 2L }
        println("Фубуксицин в списке 'на сегодня': $fubuxicinInTodayList")
        
        // Фубуксицин не должен быть в списке "на сегодня", так как он должен приниматься завтра
        assertFalse("Фубуксицин не должен быть в списке 'на сегодня'", fubuxicinInTodayList)
        
        // Проверяем, что Липетор не должен быть в списке "на сегодня" (так как принят)
        val lipetorInTodayList = medicinesForToday.any { it.id == 1L }
        println("Липетор в списке 'на сегодня': $lipetorInTodayList")
        assertFalse("Липетор не должен быть в списке 'на сегодня' (принят)", lipetorInTodayList)
        
        // Список "на сегодня" должен быть пустым
        assertTrue("Список 'на сегодня' должен быть пустым", medicinesForToday.isEmpty())
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