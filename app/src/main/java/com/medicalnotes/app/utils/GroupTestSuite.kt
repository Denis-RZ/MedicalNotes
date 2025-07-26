package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import java.time.LocalTime

/**
 * Автоматический тестовый набор для функционала групп
 */
object GroupTestSuite {
    
    private val testResults = mutableListOf<String>()
    
    /**
     * Запускает все тесты групп
     */
    fun runAllTests(): List<String> {
        testResults.clear()
        addLog("=== АВТОМАТИЧЕСКОЕ ТЕСТИРОВАНИЕ ГРУПП ===")
        
        try {
            testGroupDataIntegrity()
            testGroupOrderingLogic()
            testGroupDisplayLogic()
            testGroupTimeConflicts()
            testGroupValidation()
            testGroupEdgeCases()
            
            addLog("=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===")
            addLog("Всего тестов: ${testResults.size}")
            
        } catch (e: Exception) {
            addLog("КРИТИЧЕСКАЯ ОШИБКА: ${e.message}")
            e.printStackTrace()
        }
        
        return testResults.toList()
    }
    
    /**
     * Тест 1: Целостность данных групп
     */
    private fun testGroupDataIntegrity() {
        addLog("1. ТЕСТ ЦЕЛОСТНОСТИ ДАННЫХ ГРУПП")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        
        testMedicines.forEach { medicine ->
            // Проверка 1: Если есть группа, должен быть порядок
            if (medicine.groupName.isNotEmpty() && medicine.groupOrder <= 0) {
                addLog("❌ ОШИБКА: Лекарство '${medicine.name}' имеет группу '${medicine.groupName}' но порядок ${medicine.groupOrder}")
            }
            
            // Проверка 2: Если нет группы, порядок должен быть 0
            if (medicine.groupName.isEmpty() && medicine.groupOrder > 0) {
                addLog("❌ ОШИБКА: Лекарство '${medicine.name}' не имеет группы но порядок ${medicine.groupOrder}")
            }
            
            // Проверка 3: Порядок должен быть положительным
            if (medicine.groupOrder < 0) {
                addLog("❌ ОШИБКА: Лекарство '${medicine.name}' имеет отрицательный порядок ${medicine.groupOrder}")
            }
        }
        
        addLog("✅ Тест целостности данных завершен")
    }
    
    /**
     * Тест 2: Логика упорядочивания групп
     */
    private fun testGroupOrderingLogic() {
        addLog("2. ТЕСТ ЛОГИКИ УПОРЯДОЧИВАНИЯ ГРУПП")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val groupedMedicines = testMedicines.groupBy { it.groupName }
        
        groupedMedicines.forEach { (groupName, medicines) ->
            if (groupName.isNotEmpty()) {
                addLog("Проверяем группу: '$groupName'")
                
                // Проверка 1: Последовательность порядка
                val orders = medicines.map { it.groupOrder }.sorted()
                val expectedOrders = (1..medicines.size).toList()
                
                if (orders != expectedOrders) {
                    addLog("❌ ОШИБКА: Нарушена последовательность в группе '$groupName'")
                    addLog("   Ожидалось: $expectedOrders")
                    addLog("   Фактически: $orders")
                }
                
                // Проверка 2: Дубликаты порядка
                val orderCounts = medicines.groupBy { it.groupOrder }
                val duplicates = orderCounts.filter { it.value.size > 1 }
                
                if (duplicates.isNotEmpty()) {
                    addLog("❌ ОШИБКА: Дубликаты порядка в группе '$groupName': ${duplicates.keys}")
                }
                
                // Проверка 3: Максимальный порядок не превышает количество лекарств
                val maxOrder = medicines.maxOfOrNull { it.groupOrder } ?: 0
                if (maxOrder > medicines.size) {
                    addLog("❌ ОШИБКА: Максимальный порядок $maxOrder превышает количество лекарств ${medicines.size}")
                }
            }
        }
        
        addLog("✅ Тест логики упорядочивания завершен")
    }
    
    /**
     * Тест 3: Логика отображения групп
     */
    private fun testGroupDisplayLogic() {
        addLog("3. ТЕСТ ЛОГИКИ ОТОБРАЖЕНИЯ ГРУПП")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        
        testMedicines.forEach { medicine ->
            // Проверка 1: Корректность отображения названия группы
            val displayName = if (medicine.groupName.isNotEmpty()) {
                "${medicine.groupName}, №${medicine.groupOrder}"
            } else {
                ""
            }
            
            if (medicine.groupName.isNotEmpty() && displayName.isEmpty()) {
                addLog("❌ ОШИБКА: Неправильное отображение группы для '${medicine.name}'")
            }
            
            // Проверка 2: Корректность отображения времени
            val timeDisplay = medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            if (timeDisplay.isEmpty()) {
                addLog("❌ ОШИБКА: Пустое отображение времени для '${medicine.name}'")
            }
        }
        
        addLog("✅ Тест логики отображения завершен")
    }
    
    /**
     * Тест 4: Конфликты времени в группах
     */
    private fun testGroupTimeConflicts() {
        addLog("4. ТЕСТ КОНФЛИКТОВ ВРЕМЕНИ В ГРУППАХ")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val timeGroups = testMedicines.groupBy { it.time }
        
        timeGroups.forEach { (time, medicines) ->
            if (medicines.size > 1) {
                addLog("Время $time: ${medicines.size} лекарств")
                
                // Проверка 1: Все ли лекарства в одной группе
                val groupNames = medicines.map { it.groupName }.distinct()
                
                if (groupNames.size == 1 && groupNames[0].isNotEmpty()) {
                    addLog("⚠️ ВНИМАНИЕ: Все лекарства в $time в одной группе '${groupNames[0]}'")
                }
                
                // Проверка 2: Конфликты порядка в одном времени
                val medicinesInGroups = medicines.filter { it.groupName.isNotEmpty() }
                if (medicinesInGroups.size > 1) {
                    val sameGroup = medicinesInGroups.groupBy { it.groupName }
                    sameGroup.forEach { (groupName, groupMedicines) ->
                        if (groupMedicines.size > 1) {
                            val orders = groupMedicines.map { it.groupOrder }
                            if (orders.distinct().size != orders.size) {
                                addLog("❌ ОШИБКА: Дубликаты порядка в группе '$groupName' в $time")
                            }
                        }
                    }
                }
            }
        }
        
        addLog("✅ Тест конфликтов времени завершен")
    }
    
    /**
     * Тест 5: Валидация групп
     */
    private fun testGroupValidation() {
        addLog("5. ТЕСТ ВАЛИДАЦИИ ГРУПП")
        
        // Тест 1: Пустые названия групп
        val emptyGroupMedicine = Medicine(
            id = 999L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 25,
            medicineType = "Таблетки",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            groupName = "",
            groupOrder = 1
        )
        
        if (emptyGroupMedicine.groupName.isEmpty() && emptyGroupMedicine.groupOrder > 0) {
            addLog("❌ ОШИБКА: Лекарство без группы имеет порядок > 0")
        }
        
        // Тест 2: Некорректные названия групп
        val invalidGroupNames = listOf("   ", "\n", "\t", "   группа   ")
        invalidGroupNames.forEach { invalidName ->
            val medicine = Medicine(
                id = 1000L,
                name = "Тестовое лекарство",
                dosage = "1 таблетка",
                quantity = 30,
                remainingQuantity = 25,
                medicineType = "Таблетки",
                time = LocalTime.of(8, 0),
                frequency = DosageFrequency.DAILY,
                groupName = invalidName,
                groupOrder = 1
            )
            
            if (medicine.groupName.trim().isEmpty() && medicine.groupOrder > 0) {
                addLog("❌ ОШИБКА: Лекарство с пустым названием группы имеет порядок > 0")
            }
        }
        
        // Тест 3: Очень длинные названия групп
        val longGroupName = "A".repeat(100)
        val longGroupMedicine = Medicine(
            id = 1001L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 25,
            medicineType = "Таблетки",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            groupName = longGroupName,
            groupOrder = 1
        )
        
        if (longGroupMedicine.groupName.length > 50) {
            addLog("⚠️ ВНИМАНИЕ: Очень длинное название группы (${longGroupMedicine.groupName.length} символов)")
        }
        
        addLog("✅ Тест валидации завершен")
    }
    
    /**
     * Тест 6: Граничные случаи
     */
    private fun testGroupEdgeCases() {
        addLog("6. ТЕСТ ГРАНИЧНЫХ СЛУЧАЕВ")
        
        // Тест 1: Очень большие порядки
        val largeOrderMedicine = Medicine(
            id = 1002L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 25,
            medicineType = "Таблетки",
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            groupName = "Тестовая группа",
            groupOrder = 999999
        )
        
        if (largeOrderMedicine.groupOrder > 1000) {
            addLog("⚠️ ВНИМАНИЕ: Очень большой порядок в группе: ${largeOrderMedicine.groupOrder}")
        }
        
        // Тест 2: Специальные символы в названиях групп
        val specialChars = listOf("Группа-1", "Группа_2", "Группа 3", "Группа(4)", "Группа@5")
        specialChars.forEach { groupName ->
            val medicine = Medicine(
                id = 1003L,
                name = "Тестовое лекарство",
                dosage = "1 таблетка",
                quantity = 30,
                remainingQuantity = 25,
                medicineType = "Таблетки",
                time = LocalTime.of(8, 0),
                frequency = DosageFrequency.DAILY,
                groupName = groupName,
                groupOrder = 1
            )
            
            if (medicine.groupName != groupName) {
                addLog("❌ ОШИБКА: Название группы изменено: '$groupName' -> '${medicine.groupName}'")
            }
        }
        
        // Тест 3: Лекарства с одинаковыми именами в разных группах
        val sameNameMedicines = listOf(
            Medicine(id = 1004L, name = "Аспирин", dosage = "1 таблетка", quantity = 30, remainingQuantity = 25, medicineType = "Таблетки", time = LocalTime.of(8, 0), frequency = DosageFrequency.DAILY, groupName = "Группа 1", groupOrder = 1),
            Medicine(id = 1005L, name = "Аспирин", dosage = "1 таблетка", quantity = 30, remainingQuantity = 25, medicineType = "Таблетки", time = LocalTime.of(8, 0), frequency = DosageFrequency.DAILY, groupName = "Группа 2", groupOrder = 1)
        )
        
        val groupedByName = sameNameMedicines.groupBy { it.name }
        groupedByName.forEach { (name, medicines) ->
            if (medicines.size > 1) {
                val groups = medicines.map { it.groupName }.distinct()
                if (groups.size > 1) {
                    addLog("⚠️ ВНИМАНИЕ: Лекарство '$name' в разных группах: $groups")
                }
            }
        }
        
        addLog("✅ Тест граничных случаев завершен")
    }
    
    private fun addLog(message: String) {
        testResults.add(message)
        println(message)
    }
} 