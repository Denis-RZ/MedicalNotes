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
class DebugEditTest {
    
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
            Log.e("DebugEditTest", "Ошибка очистки: ${e.message}")
        }
    }
    
    @Test
    fun debugEditProblem() = runBlocking {
        Log.d("DebugEditTest", "=== ОТЛАДКА ПРОБЛЕМЫ РЕДАКТИРОВАНИЯ ===")
        
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(2) // 2 часа назад
        
        // Создаем лекарство
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
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        Log.d("DebugEditTest", "1. Создаем лекарство: ${originalMedicine.name}")
        Log.d("DebugEditTest", "   - Время: ${originalMedicine.time}")
        Log.d("DebugEditTest", "   - StartDate: ${originalMedicine.startDate}")
        Log.d("DebugEditTest", "   - TakenToday: ${originalMedicine.takenToday}")
        
        // Сохраняем
        val insertResult = medicineRepository.insertMedicine(originalMedicine)
        Log.d("DebugEditTest", "2. Сохранение: $insertResult")
        
        // Проверяем в списке "на сегодня" ДО редактирования
        val allMedicinesBefore = medicineRepository.getAllMedicines()
        Log.d("DebugEditTest", "3. Всего лекарств в базе ДО редактирования: ${allMedicinesBefore.size}")
        
        val todayMedicinesBefore = DosageCalculator.getActiveMedicinesForDate(allMedicinesBefore, today)
        Log.d("DebugEditTest", "4. Лекарств на сегодня ДО редактирования: ${todayMedicinesBefore.size}")
        
        val isInListBefore = todayMedicinesBefore.any { it.id == originalMedicine.id }
        Log.d("DebugEditTest", "5. В списке 'на сегодня' ДО редактирования: $isInListBefore")
        
        // Редактируем лекарство
        val editedMedicine = originalMedicine.copy(
            name = "Отредактированное лекарство",
            updatedAt = System.currentTimeMillis()
        )
        
        Log.d("DebugEditTest", "6. Редактируем лекарство: ${editedMedicine.name}")
        Log.d("DebugEditTest", "   - StartDate остался: ${editedMedicine.startDate}")
        Log.d("DebugEditTest", "   - TakenToday остался: ${editedMedicine.takenToday}")
        
        val updateResult = medicineRepository.updateMedicine(editedMedicine)
        Log.d("DebugEditTest", "7. Результат обновления: $updateResult")
        
        // Проверяем в списке "на сегодня" ПОСЛЕ редактирования
        val allMedicinesAfter = medicineRepository.getAllMedicines()
        Log.d("DebugEditTest", "8. Всего лекарств в базе ПОСЛЕ редактирования: ${allMedicinesAfter.size}")
        
        val todayMedicinesAfter = DosageCalculator.getActiveMedicinesForDate(allMedicinesAfter, today)
        Log.d("DebugEditTest", "9. Лекарств на сегодня ПОСЛЕ редактирования: ${todayMedicinesAfter.size}")
        
        val isInListAfter = todayMedicinesAfter.any { it.id == editedMedicine.id }
        Log.d("DebugEditTest", "10. В списке 'на сегодня' ПОСЛЕ редактирования: $isInListAfter")
        
        // Проверяем статус лекарства
        val medicineStatus = MedicineStatusHelper.getMedicineStatus(editedMedicine)
        Log.d("DebugEditTest", "11. Статус лекарства: $medicineStatus")
        
        // Анализируем логику DosageCalculator
        Log.d("DebugEditTest", "12. Анализ логики DosageCalculator:")
        
        val shouldTake = DosageCalculator.shouldTakeMedicine(editedMedicine, today)
        Log.d("DebugEditTest", "    - shouldTakeMedicine: $shouldTake")
        
        val isActive = editedMedicine.isActive
        Log.d("DebugEditTest", "    - isActive: $isActive")
        
        val lastTakenDate = if (editedMedicine.lastTakenTime > 0) {
            LocalDate.ofEpochDay(editedMedicine.lastTakenTime / (24 * 60 * 60 * 1000))
        } else {
            LocalDate.MIN
        }
        Log.d("DebugEditTest", "    - lastTakenDate: $lastTakenDate")
        Log.d("DebugEditTest", "    - today: $today")
        Log.d("DebugEditTest", "    - lastTakenDate != today: ${lastTakenDate != today}")
        
        Log.d("DebugEditTest", "13. Итоговая логика:")
        Log.d("DebugEditTest", "    - isActive && shouldTake: ${isActive && shouldTake}")
        Log.d("DebugEditTest", "    - lastTakenDate != today: ${lastTakenDate != today}")
        Log.d("DebugEditTest", "    - Результат: ${(isActive && shouldTake) && (lastTakenDate != today)}")
        
        // Результаты
        Log.d("DebugEditTest", "=== РЕЗУЛЬТАТЫ ===")
        Log.d("DebugEditTest", "ДО редактирования: $isInListBefore")
        Log.d("DebugEditTest", "ПОСЛЕ редактирования: $isInListAfter")
        Log.d("DebugEditTest", "Статус: $medicineStatus")
        
        if (isInListBefore != isInListAfter) {
            Log.e("DebugEditTest", "⚠️  ПРОБЛЕМА: Статус в списке изменился после редактирования!")
        } else {
            Log.d("DebugEditTest", "✅ ОК: Статус в списке не изменился")
        }
        
        // Проверяем утверждения
        assert(isInListBefore) { "Лекарство должно быть в списке ДО редактирования" }
        assert(isInListAfter) { "Лекарство должно остаться в списке ПОСЛЕ редактирования" }
        
        Log.d("DebugEditTest", "✅ ТЕСТ ПРОЙДЕН")
    }
} 