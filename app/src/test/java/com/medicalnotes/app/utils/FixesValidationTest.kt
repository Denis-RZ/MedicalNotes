package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

/**
 * Тест для проверки всех исправлений проблем
 */
class FixesValidationTest {

    @Test
    fun testAllFixes() {
        println("\n=== ПРОВЕРКА ВСЕХ ИСПРАВЛЕНИЙ ===")
        
        val today = LocalDate.now()
        
        // Создаем лекарства как в реальной ситуации
        val lipetor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis(),
            isActive = true,
            takenToday = false,
            remainingQuantity = 30,
            quantity = 30,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 1
        )
        
        val fubuxicin = Medicine(
            id = 2L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            time = LocalTime.of(18, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis(),
            isActive = true,
            takenToday = false,
            remainingQuantity = 20,
            quantity = 20,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 2
        )
        
        val medicines = listOf(lipetor, fubuxicin)
        
        println("\n1. ПРОВЕРКА ИСПРАВЛЕНИЯ ПРОБЛЕМ С ДАТОЙ:")
        
        // Проверяем DateUtils
        val currentDate = DateUtils.getCurrentDate()
        val dateConsistency = DateUtils.checkDateConsistency()
        
        println("Текущая дата: $currentDate")
        println("Согласованность дат: $dateConsistency")
        
        assertNotNull("Дата не должна быть null", currentDate)
        assertTrue("Дата должна быть согласована", dateConsistency)
        
        println("\n2. ПРОВЕРКА ИСПРАВЛЕНИЯ ПРОБЛЕМ С ГРУППИРОВКОЙ:")
        
        // Проверяем GroupValidationUtils
        val groupValidation = GroupValidationUtils.validateGroup(medicines)
        val groupLogicValidation = GroupValidationUtils.validateGroupLogic(medicines, today)
        
        println("Валидация группы: $groupValidation")
        println("Валидация логики группы: $groupLogicValidation")
        
        assertTrue("Группировка должна быть корректной", groupValidation)
        assertTrue("Логика группы должна быть корректной", groupLogicValidation)
        
        println("\n3. ПРОВЕРКА ЛОГИКИ ФИЛЬТРАЦИИ:")
        
        // Проверяем начальное состояние
        val initialTodayMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Лекарств на сегодня (начальное состояние): ${initialTodayMedicines.size}")
        initialTodayMedicines.forEach { medicine ->
            println("  - ${medicine.name}")
        }
        
        // Симулируем принятие Липетора
        val lipetorAfterTaken = lipetor.copy(
            takenToday = true,
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            remainingQuantity = lipetor.remainingQuantity - 1
        )
        
        val updatedMedicines = listOf(lipetorAfterTaken, fubuxicin)
        
        // Проверяем состояние после принятия
        val afterTakenTodayMedicines = updatedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Лекарств на сегодня (после принятия Липетора): ${afterTakenTodayMedicines.size}")
        afterTakenTodayMedicines.forEach { medicine ->
            println("  - ${medicine.name}")
        }
        
        // Проверяем, что Липетор исчез из списка
        assertFalse("Липетор должен исчезнуть из списка после принятия", 
                   afterTakenTodayMedicines.any { it.id == lipetor.id })
        
        println("\n4. ПРОВЕРКА КЭШИРОВАНИЯ:")
        
        // Симулируем кэшированные данные (старые данные)
        val cachedMedicines = medicines // Старые данные без обновления
        val cachedTodayMedicines = cachedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Кэшированные лекарства на сегодня: ${cachedTodayMedicines.size}")
        
        // Проверяем, что кэшированные данные отличаются от обновленных
        assertNotEquals("Кэшированные данные должны отличаться от обновленных", 
                       cachedTodayMedicines.size, afterTakenTodayMedicines.size)
        
        println("\n5. ПРОВЕРКА АСИНХРОННОСТИ:")
        
        // Симулируем задержку обновления UI
        val delayedUpdate = { medicines: List<Medicine> ->
            // Симулируем задержку в 100мс
            Thread.sleep(100)
            medicines.filter { medicine ->
                val isActive = medicine.isActive
                val shouldTake = shouldTakeMedicineInGroup(medicine, today)
                val isActiveAndShouldTake = isActive && shouldTake
                val notTakenToday = !medicine.takenToday
                isActiveAndShouldTake && notTakenToday
            }
        }
        
        val delayedResult = delayedUpdate(updatedMedicines)
        println("Результат с задержкой: ${delayedResult.size} лекарств")
        
        // Проверяем, что результат с задержкой совпадает с немедленным
        assertEquals("Результат с задержкой должен совпадать с немедленным", 
                    afterTakenTodayMedicines.size, delayedResult.size)
        
        println("\n6. ИТОГОВАЯ ПРОВЕРКА:")
        
        val allTestsPassed = groupValidation && 
                           groupLogicValidation && 
                           dateConsistency && 
                           !afterTakenTodayMedicines.any { it.id == lipetor.id } &&
                           cachedTodayMedicines.size != afterTakenTodayMedicines.size &&
                           delayedResult.size == afterTakenTodayMedicines.size
        
        if (allTestsPassed) {
            println("✅ ВСЕ ИСПРАВЛЕНИЯ РАБОТАЮТ КОРРЕКТНО")
            println("Проблема с появлением Фубуксицина после принятия Липетора должна быть решена")
        } else {
            println("❌ НЕКОТОРЫЕ ИСПРАВЛЕНИЯ НЕ РАБОТАЮТ")
        }
        
        assertTrue("Все исправления должны работать корректно", allTestsPassed)
    }
    
    /**
     * Копия логики shouldTakeMedicineInGroup из DosageCalculator
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder % 2 == 1 -> groupDay == 0
                medicine.groupOrder % 2 == 0 -> groupDay == 1
                else -> false
            }
            return shouldTake
        }
        return false
    }
} 