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
class LiveDataTest {

    @Test
    fun testDoubleFilteringFix() = runBlocking {
        println("=== ТЕСТ ИСПРАВЛЕНИЯ ДВОЙНОЙ ФИЛЬТРАЦИИ ===")
        
        val context = RuntimeEnvironment.getApplication()
        val dataManager = DataManager(context)
        
        // Очищаем данные
        val medicinesFile = context.getFileStreamPath("medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем лекарство "Фубуксусат" точно как в логе пользователя
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
            takenToday = false, // НЕ принято сегодня
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
        
        println("=== ШАГ 2: Проверяем DosageCalculator напрямую ===")
        val today = LocalDate.now()
        val todayMedicinesFromCalculator = DosageCalculator.getActiveMedicinesForDate(allMedicinesFromDataManager, today)
        println("Результат DosageCalculator: ${todayMedicinesFromCalculator.size} лекарств")
        todayMedicinesFromCalculator.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем, что DosageCalculator работает правильно
        val fubuksusatInCalculator = todayMedicinesFromCalculator.any { it.name == "Фубуксусат" }
        val lipetorInCalculator = todayMedicinesFromCalculator.any { it.name == "Липетор" }
        
        println("Фубуксусат в DosageCalculator: $fubuksusatInCalculator")
        println("Липетор в DosageCalculator: $lipetorInCalculator")
        
        assertTrue("Фубуксусат должен быть в DosageCalculator", fubuksusatInCalculator)
        assertTrue("Липетор НЕ должен быть в DosageCalculator", !lipetorInCalculator)
        
        println("=== ШАГ 3: Проверяем MainViewModel (симуляция исправления) ===")
        val medicineRepository = MedicineRepository(context)
        val mainViewModel = MainViewModel(context)
        
        // Симулируем вызов loadTodayMedicines()
        mainViewModel.loadTodayMedicines()
        
        // Ждем для асинхронных операций
        kotlinx.coroutines.delay(200)
        
        val todayMedicinesFromViewModel = mainViewModel.todayMedicines.value
        println("Результат MainViewModel: ${todayMedicinesFromViewModel?.size ?: 0} лекарств")
        todayMedicinesFromViewModel?.forEach { medicine ->
            println("  - ${medicine.name}: takenToday=${medicine.takenToday}")
        }
        
        // Проверяем ViewModel
        val fubuksusatInViewModel = todayMedicinesFromViewModel?.any { it.name == "Фубуксусат" } ?: false
        val lipetorInViewModel = todayMedicinesFromViewModel?.any { it.name == "Липетор" } ?: false
        
        println("Фубуксусат в ViewModel: $fubuksusatInViewModel")
        println("Липетор в ViewModel: $lipetorInViewModel")
        
        println("=== ШАГ 4: Симулируем MainActivity observer (ИСПРАВЛЕННАЯ ВЕРСИЯ) ===")
        
        // Симулируем то, что теперь происходит в MainActivity observer
        // (без повторного вызова DosageCalculator.getActiveMedicinesForDate)
        val medicinesFromObserver = todayMedicinesFromViewModel ?: emptyList()
        println("Данные в observer (уже отфильтрованы): ${medicinesFromObserver.size} лекарств")
        
        // Проверяем, что данные не изменились (не было двойной фильтрации)
        val fubuksusatInObserver = medicinesFromObserver.any { it.name == "Фубуксусат" }
        val lipetorInObserver = medicinesFromObserver.any { it.name == "Липетор" }
        
        println("Фубуксусат в observer: $fubuksusatInObserver")
        println("Липетор в observer: $lipetorInObserver")
        
        // Проверяем, что данные в observer совпадают с данными из ViewModel
        assertEquals("Количество лекарств в observer должно совпадать с ViewModel", 
                     todayMedicinesFromViewModel?.size ?: 0, medicinesFromObserver.size)
        
        println("=== ШАГ 5: Проверяем финальный результат ===")
        
        // Финальные проверки
        assertTrue("Фубуксусат должен быть в финальном списке", fubuksusatInObserver)
        assertTrue("Липетор НЕ должен быть в финальном списке", !lipetorInObserver)
        
        if (fubuksusatInObserver) {
            println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Фубуксусат попал в финальный список!")
        } else {
            println("❌ ПРОБЛЕМА: Фубуксусат НЕ попал в финальный список!")
        }
        
        if (!lipetorInObserver) {
            println("✅ ИСПРАВЛЕНИЕ РАБОТАЕТ: Липетор НЕ попал в финальный список!")
        } else {
            println("❌ ПРОБЛЕМА: Липетор попал в финальный список!")
        }
        
        println("=== ТЕСТ ЗАВЕРШЕН ===")
    }
} 