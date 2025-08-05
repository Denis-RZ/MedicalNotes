package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.GroupValidationStatus

class NewStructureTest {
    
    @Test
    fun testNewStructureValidation() {
        println("=== ТЕСТ НОВОЙ СТРУКТУРЫ ДАННЫХ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с новой структурой
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "",
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "",
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val allMedicines = listOf(lipetor, fubuxicin)
        
        println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУПП ===")
        
        // Проверяем валидацию каждого лекарства
        allMedicines.forEach { medicine ->
            println("Лекарство: ${medicine.name}")
            println("  - isValidGroup: ${medicine.isValidGroup()}")
            println("  - needsGroupValidation: ${medicine.needsGroupValidation()}")
            println("  - groupValidationStatus: ${medicine.getGroupValidationStatus(allMedicines)}")
            println("  - generateGroupValidationHash: ${medicine.generateGroupValidationHash()}")
        }
        
        // Проверяем консистентность группы
        println("\n=== ПРОВЕРКА КОНСИСТЕНТНОСТИ ГРУППЫ ===")
        val isGroupConsistent = lipetor.isGroupConsistent(allMedicines)
        println("Группа консистентна: $isGroupConsistent")
        
        // Исправляем данные группы
        println("\n=== ИСПРАВЛЕНИЕ ДАННЫХ ГРУППЫ ===")
        val fixedLipetor = lipetor.fixGroupData(allMedicines)
        val fixedFubuxicin = fubuxicin.fixGroupData(allMedicines)
        
        println("Липетор после исправления:")
        println("  - groupValidationHash: ${fixedLipetor.groupValidationHash}")
        println("  - isValidGroup: ${fixedLipetor.isValidGroup()}")
        println("  - groupValidationStatus: ${fixedLipetor.getGroupValidationStatus(allMedicines)}")
        
        println("Фубуксусат после исправления:")
        println("  - groupValidationHash: ${fixedFubuxicin.groupValidationHash}")
        println("  - isValidGroup: ${fixedFubuxicin.isValidGroup()}")
        println("  - groupValidationStatus: ${fixedFubuxicin.getGroupValidationStatus(allMedicines)}")
        
        // Проверяем, что исправленные данные валидны
        val fixedMedicines = listOf(fixedLipetor, fixedFubuxicin)
        val isFixedGroupConsistent = fixedLipetor.isGroupConsistent(fixedMedicines)
        println("Исправленная группа консистентна: $isFixedGroupConsistent")
        
        // Проверяем, что только одно лекарство должно приниматься
        println("\n=== ПРОВЕРКА ЛОГИКИ ГРУППИРОВКИ ===")
        val lipetorShouldTake = shouldTakeMedicineInGroup(fixedLipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fixedFubuxicin, today)
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        
        // Проверяем, что исправленные данные работают правильно
        assertTrue("Группа должна быть консистентной после исправления", isFixedGroupConsistent)
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        assertTrue("Липетор должен быть валидным после исправления", fixedLipetor.isValidGroup())
        assertTrue("Фубуксусат должен быть валидным после исправления", fixedFubuxicin.isValidGroup())
        
        println("✅ Тест новой структуры данных прошел успешно!")
    }
    
    @Test
    fun testGroupValidationWithHash() {
        println("=== ТЕСТ ВАЛИДАЦИИ ГРУППЫ С ХЕШЕМ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с правильным хешем
        val lipetor = Medicine(
            id = 1754031172266L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 1,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "1:Тест:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val fubuxicin = Medicine(
            id = 1754284099807L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 2,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "1:Тест:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val allMedicines = listOf(lipetor, fubuxicin)
        
        println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУПП С ХЕШЕМ ===")
        
        allMedicines.forEach { medicine ->
            println("Лекарство: ${medicine.name}")
            println("  - isValidGroup: ${medicine.isValidGroup()}")
            println("  - groupValidationStatus: ${medicine.getGroupValidationStatus(allMedicines)}")
            println("  - generateGroupValidationHash: ${medicine.generateGroupValidationHash()}")
        }
        
        val isGroupConsistent = lipetor.isGroupConsistent(allMedicines)
        println("Группа консистентна: $isGroupConsistent")
        
        // Проверяем логику группировки
        val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        
        // Проверяем, что группа валидна
        assertTrue("Группа должна быть валидной", lipetor.isValidGroup())
        assertTrue("Группа должна быть консистентной", isGroupConsistent)
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        
        println("✅ Тест валидации группы с хешем прошел успешно!")
    }
    
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.groupStartDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        
        if (medicine.groupFrequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            return when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder == 1 -> groupDay == 0
                medicine.groupOrder == 2 -> groupDay == 1
                else -> false
            }
        }
        
        return false
    }
} 