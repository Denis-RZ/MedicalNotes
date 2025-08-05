package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.GroupValidationStatus

class SimpleDataManagerTest {
    
    @Test
    fun testGroupValidationLogic() {
        println("=== ТЕСТ ЛОГИКИ ВАЛИДАЦИИ ГРУПП ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с проблемами в группировке
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
            groupStartDate = 0, // Проблема: не установлена
            groupFrequency = DosageFrequency.DAILY, // Проблема: неправильная частота
            groupValidationHash = "", // Проблема: пустой хеш
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
            groupOrder = 0, // Проблема: неправильный порядок
            groupStartDate = 0, // Проблема: не установлена
            groupFrequency = DosageFrequency.DAILY, // Проблема: неправильная частота
            groupValidationHash = "", // Проблема: пустой хеш
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
        assertFalse("Группа должна быть неконсистентной до исправления", isGroupConsistent)
        
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
        
        println("✅ Тест логики валидации групп прошел успешно!")
    }
    
    @Test
    fun testGroupValidationStatus() {
        println("=== ТЕСТ СТАТУСОВ ВАЛИДАЦИИ ГРУПП ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Тест 1: Лекарство не в группе
        val nonGroupMedicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = startDateMillis,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val status1 = nonGroupMedicine.getGroupValidationStatus(listOf(nonGroupMedicine))
        println("Лекарство не в группе: $status1")
        assertEquals("Статус должен быть NOT_IN_GROUP", GroupValidationStatus.NOT_IN_GROUP, status1)
        
        // Тест 2: Лекарство с неверными данными группы
        val invalidGroupMedicine = Medicine(
            id = 2L,
            name = "Неверная группа",
            dosage = "1 таблетка",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 0, // Неверный порядок
            groupStartDate = 0, // Не установлена
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "", // Пустой хеш
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val status2 = invalidGroupMedicine.getGroupValidationStatus(listOf(invalidGroupMedicine))
        println("Лекарство с неверными данными группы: $status2")
        assertEquals("Статус должен быть INVALID_GROUP_DATA", GroupValidationStatus.INVALID_GROUP_DATA, status2)
        
        // Тест 3: Валидная группа
        val validMedicine1 = Medicine(
            id = 3L,
            name = "Валидное 1",
            dosage = "1 таблетка",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 2L,
            groupName = "Валидная группа",
            groupOrder = 1,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "2:Валидная группа:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val validMedicine2 = Medicine(
            id = 4L,
            name = "Валидное 2",
            dosage = "1 таблетка",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 2L,
            groupName = "Валидная группа",
            groupOrder = 2,
            groupStartDate = startDateMillis,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY,
            groupValidationHash = "2:Валидная группа:$startDateMillis:EVERY_OTHER_DAY".hashCode().toString(),
            quantity = 30,
            remainingQuantity = 30,
            takenToday = false
        )
        
        val validGroup = listOf(validMedicine1, validMedicine2)
        val status3 = validMedicine1.getGroupValidationStatus(validGroup)
        println("Валидная группа: $status3")
        assertEquals("Статус должен быть VALID", GroupValidationStatus.VALID, status3)
        
        println("✅ Тест статусов валидации групп прошел успешно!")
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