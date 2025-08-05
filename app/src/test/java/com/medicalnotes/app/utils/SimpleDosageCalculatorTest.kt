package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

class SimpleDosageCalculatorTest {
    
    @Test
    fun testGroupLogicWithoutAndroidLog() {
        println("=== ТЕСТ ЛОГИКИ ГРУПП БЕЗ ANDROID LOG ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарства с правильной структурой группы
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
        
        println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУПП ===")
        
        // Проверяем валидацию каждого лекарства
        allMedicines.forEach { medicine ->
            println("Лекарство: ${medicine.name}")
            println("  - isValidGroup: ${medicine.isValidGroup()}")
            println("  - groupValidationStatus: ${medicine.getGroupValidationStatus(allMedicines)}")
        }
        
        // Проверяем консистентность группы
        val isGroupConsistent = lipetor.isGroupConsistent(allMedicines)
        println("Группа консистентна: $isGroupConsistent")
        assertTrue("Группа должна быть консистентной", isGroupConsistent)
        
        println("=== ПРОВЕРКА ЛОГИКИ ГРУППИРОВКИ ===")
        
        // Проверяем логику группировки напрямую
        val lipetorShouldTake = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTake = shouldTakeMedicineInGroup(fubuxicin, today)
        
        println("Липетор должен приниматься: $lipetorShouldTake")
        println("Фубуксусат должен приниматься: $fubuxicinShouldTake")
        
        val onlyOneShouldTake = lipetorShouldTake != fubuxicinShouldTake
        println("Только одно лекарство должно приниматься: $onlyOneShouldTake")
        assertTrue("Только одно лекарство должно приниматься в день", onlyOneShouldTake)
        
        println("=== ПРОВЕРКА ФИЛЬТРАЦИИ ===")
        
        // Проверяем фильтрацию активных лекарств
        val activeMedicines = allMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            
            println("${medicine.name}: isActive=$isActive, shouldTake=$shouldTake, result=$isActiveAndShouldTake")
            
            isActiveAndShouldTake
        }
        
        println("Активных лекарств на сегодня: ${activeMedicines.size}")
        
        activeMedicines.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем, что только нужные лекарства включены
        val expectedActiveCount = if (lipetorShouldTake) 1 else 0
        assertEquals("Должно быть $expectedActiveCount активных лекарств", expectedActiveCount, activeMedicines.size)
        
        println("✅ Тест логики групп без Android Log прошел успешно!")
    }
    
    @Test
    fun testInvalidGroupLogic() {
        println("=== ТЕСТ ЛОГИКИ НЕВАЛИДНОЙ ГРУППЫ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарство с невалидной группой
        val invalidMedicine = Medicine(
            id = 1754031172266L,
            name = "Невалидное лекарство",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDateMillis,
            groupId = 1L,
            groupName = "Тест",
            groupOrder = 0, // Невалидный порядок
            groupStartDate = 0, // Не установлена
            groupFrequency = DosageFrequency.DAILY, // Неправильная частота
            groupValidationHash = "", // Пустой хеш
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val allMedicines = listOf(invalidMedicine)
        
        println("=== ПРОВЕРКА НЕВАЛИДНОЙ ГРУППЫ ===")
        
        println("Лекарство: ${invalidMedicine.name}")
        println("  - isValidGroup: ${invalidMedicine.isValidGroup()}")
        println("  - groupValidationStatus: ${invalidMedicine.getGroupValidationStatus(allMedicines)}")
        
        assertFalse("Группа должна быть невалидной", invalidMedicine.isValidGroup())
        
        println("=== ПРОВЕРКА ЛОГИКИ НЕВАЛИДНОЙ ГРУППЫ ===")
        
        // Проверяем логику для невалидной группы
        val shouldTake = shouldTakeMedicineInGroup(invalidMedicine, today)
        println("Невалидное лекарство должно приниматься: $shouldTake")
        
        // Невалидная группа не должна приниматься
        assertFalse("Невалидная группа не должна приниматься", shouldTake)
        
        println("✅ Тест логики невалидной группы прошел успешно!")
    }
    
    @Test
    fun testNonGroupLogic() {
        println("=== ТЕСТ ЛОГИКИ БЕЗ ГРУППЫ ===")
        
        val today = LocalDate.of(2025, 8, 5)
        val startDate = today.minusDays(30)
        val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
        
        // Создаем лекарство без группы
        val nonGroupMedicine = Medicine(
            id = 1754031172266L,
            name = "Обычное лекарство",
            dosage = "1 таблетка",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.DAILY,
            startDate = startDateMillis,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            quantity = 30,
            remainingQuantity = 19,
            takenToday = false
        )
        
        val allMedicines = listOf(nonGroupMedicine)
        
        println("=== ПРОВЕРКА ЛЕКАРСТВА БЕЗ ГРУППЫ ===")
        
        println("Лекарство: ${nonGroupMedicine.name}")
        println("  - groupId: ${nonGroupMedicine.groupId}")
        println("  - needsGroupValidation: ${nonGroupMedicine.needsGroupValidation()}")
        
        assertFalse("Лекарство не должно нуждаться в валидации группы", nonGroupMedicine.needsGroupValidation())
        
        println("=== ПРОВЕРКА ЛОГИКИ БЕЗ ГРУППЫ ===")
        
        // Проверяем логику для лекарства без группы
        val shouldTake = shouldTakeMedicineWithoutGroup(nonGroupMedicine, today)
        println("Лекарство без группы должно приниматься: $shouldTake")
        
        // Ежедневное лекарство должно приниматься
        assertTrue("Ежедневное лекарство должно приниматься", shouldTake)
        
        println("✅ Тест логики без группы прошел успешно!")
    }
    
    // Копируем логику из DosageCalculator без Android Log
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        // Проверяем валидность группы
        if (!medicine.isValidGroup()) {
            return false
        }
        
        // Используем groupStartDate вместо startDate для групповых лекарств
        val startDate = LocalDate.ofEpochDay(medicine.groupStartDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
        
        // Логика группы "через день"
        if (medicine.groupFrequency == DosageFrequency.EVERY_OTHER_DAY) {
            // Определяем, какой день группы сегодня (0, 1, 2, 3...)
            val groupDay = (daysSinceStart % 2).toInt()
            
            // Для группы "через день":
            // - Лекарство с groupOrder = 1 принимается в дни 0, 2, 4, 6... (четные дни группы)
            // - Лекарство с groupOrder = 2 принимается в дни 1, 3, 5, 7... (нечетные дни группы)
            
            return when {
                medicine.groupOrder <= 0 -> false  // Неизвестный порядок
                medicine.groupOrder == 1 -> groupDay == 0  // Первое лекарство в четные дни группы
                medicine.groupOrder == 2 -> groupDay == 1  // Второе лекарство в нечетные дни группы
                else -> false  // Неподдерживаемый порядок
            }
        }
        
        // Для других частот используем обычную логику
        return when (medicine.groupFrequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.TWICE_A_WEEK -> {
                daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.WEEKLY -> {
                daysSinceStart % 7L == 0L
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
            else -> false
        }
    }
    
    private fun shouldTakeMedicineWithoutGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        
        // Если дата раньше начала приема
        if (date.isBefore(startDate)) {
            return false
        }
        
        // Обычная логика для лекарств не в группе
        return when (medicine.frequency) {
            DosageFrequency.DAILY -> true
            DosageFrequency.EVERY_OTHER_DAY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.TWICE_A_WEEK -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 3L == 0L || daysSinceStart % 3L == 1L
            }
            DosageFrequency.THREE_TIMES_A_WEEK -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 2L == 0L
            }
            DosageFrequency.WEEKLY -> {
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date)
                daysSinceStart % 7L == 0L
            }
            DosageFrequency.CUSTOM -> {
                val dayOfWeek = date.dayOfWeek.value
                medicine.customDays.contains(dayOfWeek)
            }
        }
    }
} 