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

@RunWith(RobolectricTestRunner::class)
class SimpleEditTest {
    
    private lateinit var context: Context
    private lateinit var dataManager: DataManager
    private lateinit var medicineRepository: MedicineRepository
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
        medicineRepository = MedicineRepository(context)
        
        // Очищаем данные
        try {
            val medicinesFile = context.getFileStreamPath("medicines.json")
            if (medicinesFile.exists()) {
                medicinesFile.delete()
            }
        } catch (e: Exception) {
            println("Ошибка очистки: ${e.message}")
        }
    }
    
    @Test
    fun testFixWorks() = runBlocking {
        println("\n=== ПРОВЕРКА ИСПРАВЛЕНИЯ ===")
        
        // 1. Создаем лекарство
        val today = LocalDate.now()
        val medicineTime = LocalTime.of(15, 0)
        
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
        
        println("1. Создаем лекарство: ${originalMedicine.name}")
        println("   - Время: ${originalMedicine.time}")
        println("   - StartDate: ${originalMedicine.startDate}")
        
        // 2. Сохраняем
        val insertResult = medicineRepository.insertMedicine(originalMedicine)
        println("2. Сохранение: $insertResult")
        
        // 3. Проверяем в списке "на сегодня"
        val todayMedicinesBefore = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListBefore = todayMedicinesBefore.any { it.id == originalMedicine.id }
        println("3. В списке 'на сегодня' ДО редактирования: $isInListBefore")
        
        // 4. Редактируем (меняем только название)
        val editedMedicine = originalMedicine.copy(
            name = "Отредактированное лекарство",
            updatedAt = System.currentTimeMillis()
        )
        
        println("4. Редактируем лекарство: ${editedMedicine.name}")
        println("   - StartDate остался: ${editedMedicine.startDate}")
        
        val updateResult = medicineRepository.updateMedicine(editedMedicine)
        println("5. Обновление: $updateResult")
        
        // 5. Проверяем после редактирования
        val todayMedicinesAfter = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListAfter = todayMedicinesAfter.any { it.id == editedMedicine.id }
        println("6. В списке 'на сегодня' ПОСЛЕ редактирования: $isInListAfter")
        
        // 6. Принимаем лекарство
        val takenMedicine = editedMedicine.copy(
            takenToday = true,
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            remainingQuantity = editedMedicine.remainingQuantity - 1
        )
        
        println("7. Принимаем лекарство...")
        val takeResult = medicineRepository.updateMedicine(takenMedicine)
        println("8. Результат приема: $takeResult")
        
        val todayMedicinesAfterTaking = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            today
        )
        val isInListAfterTaking = todayMedicinesAfterTaking.any { it.id == takenMedicine.id }
        println("9. В списке 'на сегодня' ПОСЛЕ приема: $isInListAfterTaking")
        
        // 7. Результаты
        println("\n=== РЕЗУЛЬТАТЫ ===")
        println("ДО редактирования: $isInListBefore")
        println("ПОСЛЕ редактирования: $isInListAfter")
        println("ПОСЛЕ приема: $isInListAfterTaking")
        
        // 8. Проверяем, что исправление работает
        assert(isInListBefore) { "Лекарство должно быть в списке ДО редактирования" }
        assert(isInListAfter) { "Лекарство должно остаться в списке ПОСЛЕ редактирования" }
        assert(!isInListAfterTaking) { "Лекарство должно исчезнуть ПОСЛЕ приема" }
        
        println("\n✅ ИСПРАВЛЕНИЕ РАБОТАЕТ! Лекарство правильно отображается в списке 'на сегодня'")
        println("   - Проблема с startDate была исправлена")
        println("   - Лекарство остается в списке после редактирования")
        println("   - Лекарство исчезает после приема")
    }
} 