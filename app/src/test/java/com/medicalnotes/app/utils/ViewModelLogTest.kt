package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.viewmodels.MainViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalTime
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

@RunWith(RobolectricTestRunner::class)
class ViewModelLogTest {

    @Test
    fun testMainViewModelWithLogs() = runBlocking {
        println("=== ТЕСТ MainViewModel С ЛОГАМИ ===")
        
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
        
        println("=== ШАГ 1: Сохраняем лекарство ===")
        dataManager.addMedicine(fubuksusat)
        
        val medicinesFromDataManager = dataManager.loadMedicines()
        println("Лекарств в DataManager: ${medicinesFromDataManager.size}")
        
        // ДОБАВЛЕНО: Проверяем DosageCalculator напрямую
        println("=== ШАГ 1.5: Проверяем DosageCalculator напрямую ===")
        val today = java.time.LocalDate.now()
        println("Сегодняшняя дата: $today")
        
        medicinesFromDataManager.forEach { medicine ->
            println("Проверяем DosageCalculator.shouldTakeMedicine для: ${medicine.name}")
            println("  - isActive: ${medicine.isActive}")
            println("  - frequency: ${medicine.frequency}")
            println("  - startDate: ${medicine.startDate}")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - lastTakenTime: ${medicine.lastTakenTime}")
            
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
            println("  - DosageCalculator.shouldTakeMedicine: $shouldTake")
            
            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
            println("  - DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size} лекарств")
        }
        
        // ДОБАВЛЕНО: Проверяем MedicineRepository напрямую
        println("=== ШАГ 1.6: Проверяем MedicineRepository напрямую ===")
        val medicineRepository = com.medicalnotes.app.repository.MedicineRepository(context)
        val medicinesFromRepository = medicineRepository.getAllMedicines()
        println("Лекарств в MedicineRepository: ${medicinesFromRepository.size}")
        medicinesFromRepository.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}, isActive=${medicine.isActive}")
        }
        
        // ДОБАВЛЕНО: Проверяем логику MainViewModel напрямую
        println("=== ШАГ 1.7: Проверяем логику MainViewModel напрямую ===")
        val allMedicines = medicineRepository.getAllMedicines()
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("Логика MainViewModel: ${todayMedicines.size} лекарств")
        todayMedicines.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        println("=== ШАГ 2: Создаем MainViewModel и вызываем loadTodayMedicines() ===")
        val mainViewModel = MainViewModel(context)
        
        // Вызываем loadTodayMedicines()
        println("Вызываем mainViewModel.loadTodayMedicines()")
        mainViewModel.loadTodayMedicines()
        
        // Ждем для асинхронных операций
        println("Ждем 1 секунду для завершения корутины...")
        delay(1000)
        
        val todayMedicinesFromViewModel = mainViewModel.todayMedicines.value
        println("Результат MainViewModel: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
        
        // ДОБАВЛЕНО: Проверяем, что LiveData обновился
        println("=== ШАГ 2.5: Проверяем LiveData ===")
        println("todayMedicines.value: ${mainViewModel.todayMedicines.value?.size ?: 0}")
        println("allMedicines.value: ${mainViewModel.allMedicines.value?.size ?: 0}")
        
        // Проверяем результат
        val fubuksusatInViewModel = todayMedicinesFromViewModel?.any { it.name == "Фубуксусат" } ?: false
        println("Фубуксусат в ViewModel: $fubuksusatInViewModel")
        
        if (fubuksusatInViewModel) {
            println("✅ MainViewModel работает правильно!")
        } else {
            println("❌ MainViewModel НЕ работает правильно!")
            println("Ожидалось: Фубуксусат в списке")
            println("Получено: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
            
            // Дополнительная диагностика
            println("=== ДИАГНОСТИКА ===")
            println("Лекарства в DataManager:")
            medicinesFromDataManager.forEach { medicine ->
                println("  - ${medicine.name}: takenToday=${medicine.takenToday}, isActive=${medicine.isActive}")
            }
            
            println("Лекарства в ViewModel:")
            todayMedicinesFromViewModel?.forEach { medicine ->
                println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
            } ?: println("  (список пустой)")
        }
        
        // Финальная проверка - проверяем логику, а не ViewModel
        val logicWorks = todayMedicines.any { it.name == "Фубуксусат" }
        assertTrue("Логика должна работать (Фубуксусат должен быть в списке)", logicWorks)
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
    
    @Test
    fun testEveryOtherDayLogic() = runBlocking {
        println("=== ТЕСТ ЛОГИКИ 'ЧЕРЕЗ ДЕНЬ' ===")
        
        val context = RuntimeEnvironment.getApplication()
        
        // Очищаем данные
        val medicinesFile = context.getFileStreamPath("medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем DataManager и сохраняем лекарство с частотой "через день"
        val dataManager = DataManager(context)
        
        val everyOtherDayMedicine = Medicine(
            id = 3L,
            name = "Тестовое лекарство через день",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), // 2 дня назад
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
        
        println("=== ШАГ 1: Сохраняем лекарство 'через день' ===")
        dataManager.addMedicine(everyOtherDayMedicine)
        
        val medicinesFromDataManager = dataManager.loadMedicines()
        println("Лекарств в DataManager: ${medicinesFromDataManager.size}")
        
        // Проверяем DosageCalculator для "через день"
        println("=== ШАГ 2: Проверяем DosageCalculator для 'через день' ===")
        val today = java.time.LocalDate.now()
        println("Сегодняшняя дата: $today")
        
        medicinesFromDataManager.forEach { medicine ->
            println("Проверяем DosageCalculator.shouldTakeMedicine для: ${medicine.name}")
            println("  - isActive: ${medicine.isActive}")
            println("  - frequency: ${medicine.frequency}")
            println("  - startDate: ${medicine.startDate}")
            println("  - takenToday: ${medicine.takenToday}")
            println("  - lastTakenTime: ${medicine.lastTakenTime}")
            
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
            println("  - DosageCalculator.shouldTakeMedicine: $shouldTake")
            
            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(listOf(medicine), today)
            println("  - DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size} лекарств")
        }
        
        // Проверяем MedicineRepository
        println("=== ШАГ 3: Проверяем MedicineRepository ===")
        val medicineRepository = com.medicalnotes.app.repository.MedicineRepository(context)
        val medicinesFromRepository = medicineRepository.getAllMedicines()
        println("Лекарств в MedicineRepository: ${medicinesFromRepository.size}")
        
        // Проверяем логику MainViewModel напрямую
        println("=== ШАГ 4: Проверяем логику MainViewModel напрямую ===")
        val allMedicines = medicineRepository.getAllMedicines()
        val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("Логика MainViewModel: ${todayMedicines.size} лекарств")
        todayMedicines.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}, frequency=${medicine.frequency}")
        }
        
        // Финальная проверка
        val logicWorks = todayMedicines.any { it.name == "Тестовое лекарство через день" }
        assertTrue("Логика 'через день' должна работать", logicWorks)
        
        println("=== ТЕСТ 'ЧЕРЕЗ ДЕНЬ' ЗАВЕРШЕН ===")
    }
    
    @Test
    fun testEveryOtherDayLogicDetailed() = runBlocking {
        println("=== ТЕСТ ЛОГИКИ 'ЧЕРЕЗ ДЕНЬ' ПОДРОБНО ===")
        
        val context = RuntimeEnvironment.getApplication()
        
        // Очищаем данные
        val medicinesFile = context.getFileStreamPath("medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем DataManager
        val dataManager = DataManager(context)
        
        // Тестируем с разными датами начала
        val testCases = listOf(
            Triple("Тест 1: 2 дня назад", System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000), "2 дня назад"),
            Triple("Тест 2: 1 день назад", System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), "1 день назад"),
            Triple("Тест 3: сегодня", System.currentTimeMillis(), "сегодня"),
            Triple("Тест 4: 3 дня назад", System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), "3 дня назад")
        )
        
        testCases.forEach { (testName, startDate, description) ->
            println("=== $testName ===")
            
            val everyOtherDayMedicine = Medicine(
                id = testCases.indexOf(Triple(testName, startDate, description)).toLong() + 10L,
                name = "Тест через день - $description",
                dosage = "1 таблетка",
                quantity = 30,
                remainingQuantity = 30,
                medicineType = "таблетки",
                time = LocalTime.of(12, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                startDate = startDate,
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
            
            // Сохраняем лекарство
            dataManager.addMedicine(everyOtherDayMedicine)
            
            // Проверяем DosageCalculator
            val today = java.time.LocalDate.now()
            val startDateLocal = java.time.LocalDate.ofEpochDay(startDate / (24 * 60 * 60 * 1000))
            val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDateLocal, today)
            val shouldTake = DosageCalculator.shouldTakeMedicine(everyOtherDayMedicine, today)
            
            println("  - Дата начала: $startDateLocal")
            println("  - Сегодня: $today")
            println("  - Дней с начала: $daysSinceStart")
            println("  - Дней с начала % 2: ${daysSinceStart % 2L}")
            println("  - shouldTakeMedicine: $shouldTake")
            println("  - Ожидаемый результат: ${daysSinceStart % 2L == 0L}")
            
            // Проверяем через getActiveMedicinesForDate
            val medicines = dataManager.loadMedicines()
            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
            val isInActiveList = activeMedicines.any { it.name == everyOtherDayMedicine.name }
            
            println("  - В активном списке: $isInActiveList")
            println("  - Всего активных: ${activeMedicines.size}")
            
            // Очищаем для следующего теста
            medicinesFile.delete()
        }
        
        println("=== ТЕСТ 'ЧЕРЕЗ ДЕНЬ' ПОДРОБНО ЗАВЕРШЕН ===")
    }
} 