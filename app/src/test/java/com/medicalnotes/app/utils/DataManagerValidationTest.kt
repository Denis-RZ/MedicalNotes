package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.time.LocalDate
import java.time.LocalTime
import java.io.File
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import android.content.Context
import org.robolectric.RuntimeEnvironment

class DataManagerValidationTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    private lateinit var testMedicinesFile: File
    
    @Before
    fun setUp() {
        try {
            context = RuntimeEnvironment.getApplication()
            dataManager = DataManager(context)
            testMedicinesFile = File(context.filesDir, "medicines.json")
            
            // Очищаем тестовые данные
            if (testMedicinesFile.exists()) {
                testMedicinesFile.delete()
            }
            
            // Создаем пустой файл
            testMedicinesFile.parentFile?.mkdirs()
            testMedicinesFile.createNewFile()
            testMedicinesFile.writeText("[]")
            
        } catch (e: Exception) {
            println("Ошибка инициализации теста: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @After
    fun tearDown() {
        try {
            // Очищаем тестовые данные
            if (::testMedicinesFile.isInitialized && testMedicinesFile.exists()) {
                testMedicinesFile.delete()
            }
        } catch (e: Exception) {
            println("Ошибка очистки теста: ${e.message}")
        }
    }
    
    @Test
    fun testGroupValidationMethods() {
        println("=== ТЕСТ МЕТОДОВ ВАЛИДАЦИИ ГРУПП В DATAMANAGER ===")
        
        try {
            val today = LocalDate.of(2025, 8, 5)
            val startDate = today.minusDays(30)
            val startDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000L
            
            // Создаем тестовые лекарства с проблемами в группировке
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
            
            val testMedicines = listOf(lipetor, fubuxicin)
            
            println("=== СОХРАНЕНИЕ ТЕСТОВЫХ ДАННЫХ ===")
            val saveSuccess = dataManager.saveMedicines(testMedicines)
            println("Сохранение данных: $saveSuccess")
            assertTrue("Данные должны сохраниться", saveSuccess)
            
            println("=== ПРОВЕРКА ВАЛИДАЦИИ ГРУППЫ ===")
            val isValidBefore = dataManager.validateGroup(1L)
            println("Группа валидна до исправления: $isValidBefore")
            assertFalse("Группа должна быть невалидной до исправления", isValidBefore)
            
            println("=== ПОЛУЧЕНИЕ ЛЕКАРСТВ ГРУППЫ ===")
            val groupMedicines = dataManager.getGroupMedicines(1L)
            println("Лекарств в группе: ${groupMedicines.size}")
            assertEquals("Должно быть 2 лекарства в группе", 2, groupMedicines.size)
            
            groupMedicines.forEach { medicine ->
                println("  - ${medicine.name}: groupOrder=${medicine.groupOrder}, isValidGroup=${medicine.isValidGroup()}")
            }
            
            println("=== ИСПРАВЛЕНИЕ ПРОБЛЕМ С ГРУППОЙ ===")
            val fixedMedicines = dataManager.fixGroupInconsistencies(1L)
            println("Исправлено лекарств: ${fixedMedicines.size}")
            
            val fixedGroupMedicines = fixedMedicines.filter { it.groupId == 1L }
            fixedGroupMedicines.forEach { medicine ->
                println("  - ${medicine.name}: groupOrder=${medicine.groupOrder}, groupStartDate=${medicine.groupStartDate}")
                println("    isValidGroup: ${medicine.isValidGroup()}")
            }
            
            println("=== ПРОВЕРКА ВАЛИДАЦИИ ПОСЛЕ ИСПРАВЛЕНИЯ ===")
            val isValidAfter = dataManager.validateGroup(1L)
            println("Группа валидна после исправления: $isValidAfter")
            assertTrue("Группа должна быть валидной после исправления", isValidAfter)
            
            println("=== ПРОВЕРКА ВАЛИДНОСТИ ДЛЯ ДАТЫ ===")
            val isValidForDate = dataManager.isGroupValidForDate(1L, today)
            println("Группа валидна для даты $today: $isValidForDate")
            assertTrue("Группа должна быть валидной для даты", isValidForDate)
            
            println("=== ПРОВЕРКА ВАЛИДАЦИИ ВСЕХ ГРУПП ===")
            val allGroupsValid = dataManager.validateAndFixAllGroups()
            println("Все группы валидны: $allGroupsValid")
            assertTrue("Все группы должны быть валидными", allGroupsValid)
            
            println("✅ Тест методов валидации групп прошел успешно!")
            
        } catch (e: Exception) {
            println("Ошибка в тесте: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    @Test
    fun testGroupValidationWithRealData() {
        println("=== ТЕСТ ВАЛИДАЦИИ ГРУПП С РЕАЛЬНЫМИ ДАННЫМИ ===")
        
        try {
            // Загружаем реальные данные
            val realMedicines = dataManager.loadMedicines()
            println("Загружено реальных лекарств: ${realMedicines.size}")
            
            if (realMedicines.isNotEmpty()) {
                val groupIds = realMedicines.mapNotNull { it.groupId }.distinct()
                println("Найдено групп в реальных данных: ${groupIds.size}")
                
                groupIds.forEach { groupId ->
                    println("Проверяем группу $groupId:")
                    val isValid = dataManager.validateGroup(groupId)
                    println("  - Валидна: $isValid")
                    
                    if (!isValid) {
                        println("  - Исправляем группу...")
                        dataManager.fixGroupInconsistencies(groupId)
                        val isValidAfter = dataManager.validateGroup(groupId)
                        println("  - Валидна после исправления: $isValidAfter")
                    }
                }
                
                // Проверяем все группы
                val allValid = dataManager.validateAndFixAllGroups()
                println("Все группы валидны: $allValid")
                
                println("✅ Тест с реальными данными завершен!")
            } else {
                println("Нет реальных данных для тестирования")
            }
            
        } catch (e: Exception) {
            println("Ошибка в тесте с реальными данными: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
} 