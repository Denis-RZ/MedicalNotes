package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.repository.MedicineRepository
import com.medicalnotes.app.viewmodels.MainViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RunWith(RobolectricTestRunner::class)
class ViewModelTest {

    @Test
    fun testMainViewModelLoadTodayMedicines() = runBlocking {
        println("=== ТЕСТ MainViewModel.loadTodayMedicines() ===")
        
        val context = RuntimeEnvironment.getApplication()
        
        // Очищаем данные
        val medicinesFile = context.getFileStreamPath("medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем DataManager и сохраняем лекарство
        val dataManager = DataManager(context)
        
        val fubuksusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "50 таблеток",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = LocalTime.of(18, 54),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null,
            groupName = "",
            groupOrder = 0,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("=== ШАГ 1: Сохраняем лекарство в DataManager ===")
        dataManager.addMedicine(fubuksusat)
        
        val medicinesFromDataManager = dataManager.loadMedicines()
        println("Лекарств в DataManager: ${medicinesFromDataManager.size}")
        medicinesFromDataManager.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        println("=== ШАГ 2: Проверяем DosageCalculator ===")
        val today = LocalDate.now()
        val todayMedicinesFromCalculator = DosageCalculator.getActiveMedicinesForDate(medicinesFromDataManager, today)
        println("Результат DosageCalculator: ${todayMedicinesFromCalculator.size} лекарств")
        
        // Проверяем, что DosageCalculator работает
        assertTrue("Фубуксусат должен быть в DosageCalculator", 
                   todayMedicinesFromCalculator.any { it.name == "Фубуксусат" })
        
        println("=== ШАГ 3: Проверяем MedicineRepository ===")
        val medicineRepository = MedicineRepository(context)
        val medicinesFromRepository = withContext(Dispatchers.IO) {
            medicineRepository.getAllMedicines()
        }
        println("Лекарств из Repository: ${medicinesFromRepository.size}")
        medicinesFromRepository.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем, что Repository возвращает те же данные
        assertEquals("Количество лекарств должно совпадать", 
                     medicinesFromDataManager.size, medicinesFromRepository.size)
        
        println("=== ШАГ 4: Проверяем MainViewModel ===")
        val mainViewModel = MainViewModel(context)
        
        // Вызываем loadTodayMedicines()
        mainViewModel.loadTodayMedicines()
        
        // Ждем для асинхронных операций
        kotlinx.coroutines.delay(300)
        
        val todayMedicinesFromViewModel = mainViewModel.todayMedicines.value
        println("Результат MainViewModel: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
        todayMedicinesFromViewModel?.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем результат
        val fubuksusatInViewModel = todayMedicinesFromViewModel?.any { it.name == "Фубуксусат" } ?: false
        println("Фубуксусат в ViewModel: $fubuksusatInViewModel")
        
        if (fubuksusatInViewModel) {
            println("✅ MainViewModel работает правильно!")
        } else {
            println("❌ MainViewModel НЕ работает правильно!")
            println("Ожидалось: Фубуксусат в списке")
            println("Получено: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
        }
        
        // Финальная проверка
        assertTrue("Фубуксусат должен быть в ViewModel", fubuksusatInViewModel)
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
} 