package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
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
class StartDateProblemTest {
    
    private lateinit var context: Context
    private lateinit var medicineRepository: MedicineRepository
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        medicineRepository = MedicineRepository(context)
        
        // Очищаем данные
        try {
            val medicinesFile = context.getFileStreamPath("medicines.json")
            if (medicinesFile.exists()) {
                medicinesFile.delete()
            }
        } catch (e: Exception) {
            Log.e("StartDateProblemTest", "Ошибка очистки: ${e.message}")
        }
    }
    
    @Test
    fun testStartDateProblem() = runBlocking {
        Log.d("StartDateProblemTest", "=== ТЕСТ ПРОБЛЕМЫ С STARTDATE ===")
        
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val pastTime = LocalTime.now().minusHours(2)
        
        // Создаем лекарство с startDate вчера
        val originalMedicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = pastTime,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = yesterday.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        Log.d("StartDateProblemTest", "1. Создаем лекарство:")
        Log.d("StartDateProblemTest", "   - StartDate: ${originalMedicine.startDate}")
        Log.d("StartDateProblemTest", "   - StartDate как дата: $yesterday")
        Log.d("StartDateProblemTest", "   - Время: ${originalMedicine.time}")
        Log.d("StartDateProblemTest", "   - TakenToday: ${originalMedicine.takenToday}")
        
        // Сохраняем
        medicineRepository.insertMedicine(originalMedicine)
        
        // Проверяем shouldTakeMedicine ДО редактирования
        val shouldTakeBefore = DosageCalculator.shouldTakeMedicine(originalMedicine, today)
        Log.d("StartDateProblemTest", "2. shouldTakeMedicine ДО редактирования: $shouldTakeBefore")
        
        // Проверяем в списке "на сегодня" ДО редактирования
        val allMedicinesBefore = medicineRepository.getAllMedicines()
        val todayMedicinesBefore = DosageCalculator.getActiveMedicinesForDate(allMedicinesBefore, today)
        val isInListBefore = todayMedicinesBefore.any { it.id == originalMedicine.id }
        Log.d("StartDateProblemTest", "3. В списке 'на сегодня' ДО редактирования: $isInListBefore")
        
        // Симулируем редактирование с изменением startDate на сегодня
        val editedMedicine = originalMedicine.copy(
            name = "Отредактированное лекарство",
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            updatedAt = System.currentTimeMillis()
        )
        
        Log.d("StartDateProblemTest", "4. Редактируем лекарство:")
        Log.d("StartDateProblemTest", "   - Новое название: ${editedMedicine.name}")
        Log.d("StartDateProblemTest", "   - Новый StartDate: ${editedMedicine.startDate}")
        Log.d("StartDateProblemTest", "   - Новый StartDate как дата: $today")
        
        // Сохраняем отредактированное лекарство
        medicineRepository.updateMedicine(editedMedicine)
        
        // Проверяем shouldTakeMedicine ПОСЛЕ редактирования
        val shouldTakeAfter = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        Log.d("StartDateProblemTest", "5. shouldTakeMedicine ПОСЛЕ редактирования: $shouldTakeAfter")
        
        // Проверяем в списке "на сегодня" ПОСЛЕ редактирования
        val allMedicinesAfter = medicineRepository.getAllMedicines()
        val todayMedicinesAfter = DosageCalculator.getActiveMedicinesForDate(allMedicinesAfter, today)
        val isInListAfter = todayMedicinesAfter.any { it.id == editedMedicine.id }
        Log.d("StartDateProblemTest", "6. В списке 'на сегодня' ПОСЛЕ редактирования: $isInListAfter")
        
        // Анализируем результат
        Log.d("StartDateProblemTest", "=== АНАЛИЗ РЕЗУЛЬТАТА ===")
        Log.d("StartDateProblemTest", "shouldTakeMedicine изменился: ${shouldTakeBefore != shouldTakeAfter}")
        Log.d("StartDateProblemTest", "Статус в списке изменился: ${isInListBefore != isInListAfter}")
        
        if (shouldTakeBefore != shouldTakeAfter) {
            Log.e("StartDateProblemTest", "⚠️  ПРОБЛЕМА: shouldTakeMedicine изменился после редактирования!")
            Log.e("StartDateProblemTest", "   ДО: $shouldTakeBefore")
            Log.e("StartDateProblemTest", "   ПОСЛЕ: $shouldTakeAfter")
        }
        
        if (isInListBefore != isInListAfter) {
            Log.e("StartDateProblemTest", "⚠️  ПРОБЛЕМА: Статус в списке изменился после редактирования!")
            Log.e("StartDateProblemTest", "   ДО: $isInListBefore")
            Log.e("StartDateProblemTest", "   ПОСЛЕ: $isInListAfter")
        }
        
        // Проверяем утверждения
        assert(isInListBefore) { "Лекарство должно быть в списке ДО редактирования" }
        assert(isInListAfter) { "Лекарство должно остаться в списке ПОСЛЕ редактирования" }
        
        Log.d("StartDateProblemTest", "✅ ТЕСТ ПРОЙДЕН")
    }
} 