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
class FullChainTest {

    @Test
    fun testFullChainFromDataManagerToUI() = runBlocking {
        println("=== ПОЛНЫЙ ТЕСТ ЦЕПОЧКИ ОТ DATAMANAGER ДО UI ===")
        
        val context = RuntimeEnvironment.getApplication()
        val dataManager = DataManager(context)
        
        // Очищаем данные - удаляем файл и создаем заново
        val medicinesFile = context.getFileStreamPath("medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем лекарство "Фубуксусат" точно как в логе
        val fubuksusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "50 таблеток",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = LocalTime.of(18, 54), // 18:54 из лога
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = false, // КЛЮЧЕВОЕ: не принято сегодня
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
        
        // Создаем лекарство "Липетор" точно как в логе
        val lipetor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "30 таблеток",
            quantity = 30,
            remainingQuantity = 29,
            medicineType = "таблетки",
            time = LocalTime.of(18, 57), // 18:57 из лога
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000),
            isActive = true,
            takenToday = true, // принято сегодня
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
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
        
        println("=== ШАГ 1: Сохраняем лекарства в DataManager ===")
        dataManager.addMedicine(fubuksusat)
        dataManager.addMedicine(lipetor)
        
        val allMedicinesFromDataManager = dataManager.loadMedicines()
        println("Всего лекарств в DataManager: ${allMedicinesFromDataManager.size}")
        allMedicinesFromDataManager.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}, isActive=${medicine.isActive}")
        }
        
        println("=== ШАГ 2: Проверяем DosageCalculator.shouldTakeMedicine ===")
        val today = LocalDate.now()
        val fubuksusatShouldTake = DosageCalculator.shouldTakeMedicine(fubuksusat, today)
        val lipetorShouldTake = DosageCalculator.shouldTakeMedicine(lipetor, today)
        println("Фубуксусат должен приниматься сегодня: $fubuksusatShouldTake")
        println("Липетор должен приниматься сегодня: $lipetorShouldTake")
        
        println("=== ШАГ 3: Проверяем DosageCalculator.getActiveMedicinesForDate ===")
        val todayMedicinesFromCalculator = DosageCalculator.getActiveMedicinesForDate(allMedicinesFromDataManager, today)
        println("Результат DosageCalculator: ${todayMedicinesFromCalculator.size} лекарств")
        todayMedicinesFromCalculator.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        println("=== ШАГ 4: Проверяем MedicineRepository ===")
        val medicineRepository = MedicineRepository(context)
        val allMedicinesFromRepository = withContext(Dispatchers.IO) {
            medicineRepository.getAllMedicines()
        }
        println("Всего лекарств из Repository: ${allMedicinesFromRepository.size}")
        
        println("=== ШАГ 5: Проверяем MainViewModel ===")
        val mainViewModel = MainViewModel(context)
        mainViewModel.loadTodayMedicines()
        
        // Ждем немного для асинхронных операций
        kotlinx.coroutines.delay(100)
        
        val todayMedicinesFromViewModel = mainViewModel.todayMedicines.value
        println("Результат MainViewModel: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
        todayMedicinesFromViewModel?.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        println("=== АНАЛИЗ РЕЗУЛЬТАТОВ ===")
        
        // Проверяем, что лекарства правильно сохранены
        assertTrue("Фубуксусат должен быть в DataManager", allMedicinesFromDataManager.any { it.name == "Фубуксусат" })
        assertTrue("Липетор должен быть в DataManager", allMedicinesFromDataManager.any { it.name == "Липетор" })
        
        // Проверяем shouldTakeMedicine
        assertTrue("Фубуксусат должен приниматься сегодня", fubuksusatShouldTake)
        assertTrue("Липетор должен приниматься сегодня", lipetorShouldTake)
        
        // КЛЮЧЕВАЯ ПРОВЕРКА: Фубуксусат должен быть в списке "на сегодня"
        val fubuksusatInCalculatorList = todayMedicinesFromCalculator.any { it.name == "Фубуксусат" }
        println("Фубуксусат в списке DosageCalculator: $fubuksusatInCalculatorList")
        assertTrue("Фубуксусат должен быть в списке DosageCalculator", fubuksusatInCalculatorList)
        
        // Липетор НЕ должен быть в списке (принят сегодня)
        val lipetorInCalculatorList = todayMedicinesFromCalculator.any { it.name == "Липетор" }
        println("Липетор в списке DosageCalculator: $lipetorInCalculatorList")
        assertTrue("Липетор НЕ должен быть в списке DosageCalculator", !lipetorInCalculatorList)
        
        // Проверяем ViewModel
        val fubuksusatInViewModelList = todayMedicinesFromViewModel?.any { it.name == "Фубуксусат" } ?: false
        println("Фубуксусат в списке ViewModel: $fubuksusatInViewModelList")
        
        if (!fubuksusatInViewModelList) {
            println("❌ ПРОБЛЕМА: Фубуксусат НЕ попал в ViewModel!")
            println("Это означает, что проблема в цепочке: DataManager -> Repository -> ViewModel")
        } else {
            println("✅ Фубуксусат попал в ViewModel")
        }
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
} 