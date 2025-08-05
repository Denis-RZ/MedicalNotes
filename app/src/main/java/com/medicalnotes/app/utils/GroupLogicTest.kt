package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.GroupMetadata
import com.medicalnotes.app.models.Medicine
import java.time.LocalDate
import java.time.LocalTime

/**
 * Простой тест для проверки логики группировки лекарств "через день"
 * Этот класс можно использовать для ручного тестирования
 */
class GroupLogicTest(private val context: Context) {
    
    private val dataManager = DataManager(context)
    
    /**
     * Запускает полный тест логики группировки
     */
    fun runFullTest() {
        Log.d("GroupLogicTest", "=== НАЧАЛО ТЕСТА ГРУППОВОЙ ЛОГИКИ ===")
        
        try {
            // Очищаем данные перед тестом
            clearTestData()
            
            // Тест 1: Создание группы из двух лекарств "через день"
            testCreateGroup()
            
            // Тест 2: Проверка логики "через день" для группы
            testEveryOtherDayLogic()
            
            // Тест 3: Проверка сохранения и загрузки данных
            testDataPersistence()
            
            Log.d("GroupLogicTest", "=== ТЕСТ УСПЕШНО ЗАВЕРШЕН ===")
            
        } catch (e: Exception) {
            Log.e("GroupLogicTest", "Ошибка в тесте", e)
        }
    }
    
    private fun clearTestData() {
        Log.d("GroupLogicTest", "Очистка тестовых данных...")
        val allMedicines = dataManager.loadMedicines()
        allMedicines.forEach { medicine ->
            if (medicine.name.startsWith("Тест")) {
                dataManager.deleteMedicine(medicine.id)
            }
        }
    }
    
    private fun testCreateGroup() {
        Log.d("GroupLogicTest", "--- Тест 1: Создание группы ---")
        
        // Создаем группу из двух лекарств "через день"
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тест"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 2,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
        )
        
        // Создаем первое лекарство (порядок 1)
        val medicine1 = Medicine(
            id = 0, // Будет назначен автоматически
            name = "Тест Лекарство 1",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство 1",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            takenToday = false,
            lastTakenTime = 0L,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Создаем второе лекарство (порядок 2)
        val medicine2 = Medicine(
            id = 0, // Будет назначен автоматически
            name = "Тест Лекарство 2",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство 2",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            takenToday = false,
            lastTakenTime = 0L,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Сохраняем лекарства
        val success1 = dataManager.addMedicine(medicine1)
        val success2 = dataManager.addMedicine(medicine2)
        
        if (success1 && success2) {
            Log.d("GroupLogicTest", "✓ Группа создана успешно")
        } else {
            Log.e("GroupLogicTest", "✗ Ошибка создания группы")
        }
    }
    
    private fun testEveryOtherDayLogic() {
        Log.d("GroupLogicTest", "--- Тест 2: Логика 'через день' ---")
        
        val today = LocalDate.now()
        val savedMedicines = dataManager.loadMedicines()
        val testMedicines = savedMedicines.filter { it.name.startsWith("Тест") }
        
        if (testMedicines.size != 2) {
            Log.e("GroupLogicTest", "✗ Не найдено 2 тестовых лекарства")
            return
        }
        
        val medicine1 = testMedicines.find { it.groupOrder == 1 }
        val medicine2 = testMedicines.find { it.groupOrder == 2 }
        
        if (medicine1 == null || medicine2 == null) {
            Log.e("GroupLogicTest", "✗ Не найдены лекарства с правильным порядком")
            return
        }
        
        // Проверяем логику "через день"
        val shouldTake1 = DosageCalculator.shouldTakeMedicine(medicine1, today)
        val shouldTake2 = DosageCalculator.shouldTakeMedicine(medicine2, today)
        
        Log.d("GroupLogicTest", "Сегодня: $today")
        Log.d("GroupLogicTest", "Лекарство 1 должно приниматься: $shouldTake1")
        Log.d("GroupLogicTest", "Лекарство 2 должно приниматься: $shouldTake2")
        
        if (shouldTake1 != shouldTake2) {
            Log.d("GroupLogicTest", "✓ Логика 'через день' работает правильно")
        } else {
            Log.e("GroupLogicTest", "✗ Ошибка логики 'через день' - оба лекарства должны приниматься в разные дни")
        }
        
        // Проверяем активные лекарства
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(testMedicines, today)
        val groupMedicines = activeMedicines.filter { it.groupId == medicine1.groupId }
        
        Log.d("GroupLogicTest", "Активных лекарств из группы: ${groupMedicines.size}")
        
        if (groupMedicines.size == 1) {
            Log.d("GroupLogicTest", "✓ Только одно лекарство из группы активно сегодня")
        } else {
            Log.e("GroupLogicTest", "✗ Неправильное количество активных лекарств: ${groupMedicines.size}")
        }
    }
    
    private fun testDataPersistence() {
        Log.d("GroupLogicTest", "--- Тест 3: Сохранение и загрузка данных ---")
        
        val savedMedicines = dataManager.loadMedicines()
        val testMedicines = savedMedicines.filter { it.name.startsWith("Тест") }
        
        if (testMedicines.isEmpty()) {
            Log.e("GroupLogicTest", "✗ Не найдены тестовые лекарства")
            return
        }
        
        val firstMedicine = testMedicines.first()
        
        // Проверяем групповые данные
        if (firstMedicine.groupId != null && 
            firstMedicine.groupName.isNotEmpty() && 
            firstMedicine.groupOrder > 0 && 
            firstMedicine.groupStartDate > 0L &&
            firstMedicine.isValidGroup()) {
            
            Log.d("GroupLogicTest", "✓ Групповые данные сохранены правильно")
            Log.d("GroupLogicTest", "  - Group ID: ${firstMedicine.groupId}")
            Log.d("GroupLogicTest", "  - Group Name: ${firstMedicine.groupName}")
            Log.d("GroupLogicTest", "  - Group Order: ${firstMedicine.groupOrder}")
            Log.d("GroupLogicTest", "  - Group Start Date: ${firstMedicine.groupStartDate}")
            Log.d("GroupLogicTest", "  - Group Frequency: ${firstMedicine.groupFrequency}")
            Log.d("GroupLogicTest", "  - Valid Group: ${firstMedicine.isValidGroup()}")
            
        } else {
            Log.e("GroupLogicTest", "✗ Групповые данные не сохранены правильно")
            Log.e("GroupLogicTest", "  - Group ID: ${firstMedicine.groupId}")
            Log.e("GroupLogicTest", "  - Group Name: ${firstMedicine.groupName}")
            Log.e("GroupLogicTest", "  - Group Order: ${firstMedicine.groupOrder}")
            Log.e("GroupLogicTest", "  - Group Start Date: ${firstMedicine.groupStartDate}")
            Log.e("GroupLogicTest", "  - Valid Group: ${firstMedicine.isValidGroup()}")
        }
    }
} 