package com.medicalnotes.app.utils

import android.content.Context
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.MedicineRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
class EditMedicineTodayTest {
    
    private lateinit var context: Context
    private lateinit var dataManager: DataManager
    private lateinit var medicineRepository: MedicineRepository
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
        medicineRepository = MedicineRepository(context)
        
        // Очищаем данные перед каждым тестом
        clearTestData()
    }
    
    private fun clearTestData() {
        try {
            val medicinesFile = context.getFileStreamPath("medicines.json")
            if (medicinesFile.exists()) {
                medicinesFile.delete()
            }
        } catch (e: Exception) {
            println("Ошибка очистки данных: ${e.message}")
        }
    }
    
    @Test
    fun testMedicineAppearsInTodayListAfterEdit() = runBlocking {
        println("=== ТЕСТ: Лекарство появляется в списке 'на сегодня' после редактирования ===")
        
        // 1. Создаем лекарство для сегодня
        val today = LocalDate.now()
        val medicineTime = LocalTime.of(10, 0) // 10:00
        
        val originalMedicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = medicineTime,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        println("1. Создаем исходное лекарство:")
        println("   - Название: ${originalMedicine.name}")
        println("   - Время: ${originalMedicine.time}")
        println("   - Частота: ${originalMedicine.frequency}")
        println("   - StartDate: ${originalMedicine.startDate}")
        println("   - TakenToday: ${originalMedicine.takenToday}")
        
        // 2. Сохраняем лекарство
        val insertResult = medicineRepository.insertMedicine(originalMedicine)
        println("2. Результат сохранения: $insertResult")
        
        // 3. Проверяем, что лекарство есть в списке "на сегодня"
        val todayMedicinesBeforeEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        println("3. Лекарства на сегодня ДО редактирования: ${todayMedicinesBeforeEdit.size}")
        todayMedicinesBeforeEdit.forEach { medicine ->
            println("   - ${medicine.name} (ID: ${medicine.id})")
        }
        
        val isInTodayListBefore = todayMedicinesBeforeEdit.any { it.id == originalMedicine.id }
        println("   Лекарство в списке 'на сегодня' ДО редактирования: $isInTodayListBefore")
        
        // 4. Редактируем лекарство (меняем только название, не время и частоту)
        val editedMedicine = originalMedicine.copy(
            name = "Отредактированное лекарство",
            updatedAt = System.currentTimeMillis()
        )
        
        println("4. Редактируем лекарство:")
        println("   - Новое название: ${editedMedicine.name}")
        println("   - Время осталось: ${editedMedicine.time}")
        println("   - Частота осталась: ${editedMedicine.frequency}")
        println("   - StartDate остался: ${editedMedicine.startDate}")
        println("   - TakenToday остался: ${editedMedicine.takenToday}")
        
        // 5. Сохраняем отредактированное лекарство
        val updateResult = medicineRepository.updateMedicine(editedMedicine)
        println("5. Результат обновления: $updateResult")
        
        // 6. Проверяем, что лекарство все еще в списке "на сегодня"
        val todayMedicinesAfterEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        println("6. Лекарства на сегодня ПОСЛЕ редактирования: ${todayMedicinesAfterEdit.size}")
        todayMedicinesAfterEdit.forEach { medicine ->
            println("   - ${medicine.name} (ID: ${medicine.id})")
        }
        
        val isInTodayListAfter = todayMedicinesAfterEdit.any { it.id == editedMedicine.id }
        println("   Лекарство в списке 'на сегодня' ПОСЛЕ редактирования: $isInTodayListAfter")
        
        // 7. Проверяем, что лекарство принято и исчезает из списка
        println("7. Принимаем лекарство...")
        val takenMedicine = editedMedicine.copy(
            takenToday = true,
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            remainingQuantity = editedMedicine.remainingQuantity - 1
        )
        
        val takeResult = medicineRepository.updateMedicine(takenMedicine)
        println("   Результат приема: $takeResult")
        
        val todayMedicinesAfterTaking = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        println("8. Лекарства на сегодня ПОСЛЕ приема: ${todayMedicinesAfterTaking.size}")
        todayMedicinesAfterTaking.forEach { medicine ->
            println("   - ${medicine.name} (ID: ${medicine.id})")
        }
        
        val isInTodayListAfterTaking = todayMedicinesAfterTaking.any { it.id == takenMedicine.id }
        println("   Лекарство в списке 'на сегодня' ПОСЛЕ приема: $isInTodayListAfterTaking")
        
        // 8. Проверяем утверждения
        println("=== РЕЗУЛЬТАТЫ ТЕСТА ===")
        println("Лекарство в списке ДО редактирования: $isInTodayListBefore")
        println("Лекарство в списке ПОСЛЕ редактирования: $isInTodayListAfter")
        println("Лекарство в списке ПОСЛЕ приема: $isInTodayListAfterTaking")
        
        // Утверждения
        assert(isInTodayListBefore) { "Лекарство должно быть в списке 'на сегодня' ДО редактирования" }
        assert(isInTodayListAfter) { "Лекарство должно остаться в списке 'на сегодня' ПОСЛЕ редактирования" }
        assert(!isInTodayListAfterTaking) { "Лекарство должно исчезнуть из списка 'на сегодня' ПОСЛЕ приема" }
        
        println("✅ ТЕСТ ПРОЙДЕН: Лекарство правильно отображается в списке 'на сегодня' после редактирования")
    }
    
    @Test
    fun testMedicineDisappearsAfterSecondEdit() = runBlocking {
        println("=== ТЕСТ: Лекарство исчезает после второго редактирования ===")
        
        // 1. Создаем лекарство
        val today = LocalDate.now()
        val medicineTime = LocalTime.of(14, 0) // 14:00
        
        val originalMedicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Лекарство для второго теста",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = medicineTime,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        println("1. Создаем исходное лекарство: ${originalMedicine.name}")
        
        // 2. Сохраняем лекарство
        medicineRepository.insertMedicine(originalMedicine)
        
        // 3. Первое редактирование (меняем только название)
        val firstEdit = originalMedicine.copy(
            name = "Первое редактирование",
            updatedAt = System.currentTimeMillis()
        )
        
        println("2. Первое редактирование: ${firstEdit.name}")
        medicineRepository.updateMedicine(firstEdit)
        
        // 4. Проверяем после первого редактирования
        val todayMedicinesAfterFirstEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListAfterFirstEdit = todayMedicinesAfterFirstEdit.any { it.id == firstEdit.id }
        println("3. Лекарство в списке после первого редактирования: $isInListAfterFirstEdit")
        
        // 5. Второе редактирование (меняем только название снова)
        val secondEdit = firstEdit.copy(
            name = "Второе редактирование",
            updatedAt = System.currentTimeMillis()
        )
        
        println("4. Второе редактирование: ${secondEdit.name}")
        medicineRepository.updateMedicine(secondEdit)
        
        // 6. Проверяем после второго редактирования
        val todayMedicinesAfterSecondEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListAfterSecondEdit = todayMedicinesAfterSecondEdit.any { it.id == secondEdit.id }
        println("5. Лекарство в списке после второго редактирования: $isInListAfterSecondEdit")
        
        // 7. Проверяем утверждения
        println("=== РЕЗУЛЬТАТЫ ВТОРОГО ТЕСТА ===")
        println("После первого редактирования: $isInListAfterFirstEdit")
        println("После второго редактирования: $isInListAfterSecondEdit")
        
        assert(isInListAfterFirstEdit) { "Лекарство должно быть в списке после первого редактирования" }
        assert(isInListAfterSecondEdit) { "Лекарство должно остаться в списке после второго редактирования" }
        
        println("✅ ВТОРОЙ ТЕСТ ПРОЙДЕН: Лекарство остается в списке после второго редактирования")
    }
    
    @Test
    fun testMedicineWithTimeChange() = runBlocking {
        println("=== ТЕСТ: Лекарство с изменением времени ===")
        
        // 1. Создаем лекарство на прошлое время
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(2) // 2 часа назад
        
        val originalMedicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Лекарство с изменением времени",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = pastTime,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        println("1. Создаем лекарство с прошлым временем: ${originalMedicine.time}")
        
        // 2. Сохраняем лекарство
        medicineRepository.insertMedicine(originalMedicine)
        
        // 3. Проверяем, что лекарство в списке "на сегодня"
        val todayMedicinesBeforeEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListBeforeEdit = todayMedicinesBeforeEdit.any { it.id == originalMedicine.id }
        println("2. Лекарство в списке ДО изменения времени: $isInListBeforeEdit")
        
        // 4. Изменяем время на будущее
        val futureTime = LocalTime.now().plusHours(2) // через 2 часа
        val editedMedicine = originalMedicine.copy(
            time = futureTime,
            updatedAt = System.currentTimeMillis()
        )
        
        println("3. Изменяем время на: ${editedMedicine.time}")
        medicineRepository.updateMedicine(editedMedicine)
        
        // 5. Проверяем после изменения времени
        val todayMedicinesAfterEdit = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListAfterEdit = todayMedicinesAfterEdit.any { it.id == editedMedicine.id }
        println("4. Лекарство в списке ПОСЛЕ изменения времени: $isInListAfterEdit")
        
        // 6. Проверяем утверждения
        println("=== РЕЗУЛЬТАТЫ ТЕСТА С ИЗМЕНЕНИЕМ ВРЕМЕНИ ===")
        println("ДО изменения времени: $isInListBeforeEdit")
        println("ПОСЛЕ изменения времени: $isInListAfterEdit")
        
        assert(isInListBeforeEdit) { "Лекарство должно быть в списке ДО изменения времени" }
        assert(isInListAfterEdit) { "Лекарство должно остаться в списке ПОСЛЕ изменения времени" }
        
        println("✅ ТЕСТ С ИЗМЕНЕНИЕМ ВРЕМЕНИ ПРОЙДЕН")
    }
} 