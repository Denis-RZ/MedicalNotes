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
class ComprehensiveEditTest {
    
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
    fun testAllMedicineStatusCombinations() = runBlocking {
        println("\n=== КОМПЛЕКСНАЯ ПРОВЕРКА ВСЕХ СТАТУСОВ ===")
        
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(3) // 3 часа назад (просрочено)
        val futureTime = LocalTime.now().plusHours(2) // через 2 часа (будущее)
        
        // ТЕСТ 1: Лекарство просрочено (не принято, время прошло)
        println("\n--- ТЕСТ 1: Просроченное лекарство ---")
        val overdueMedicine = createMedicine(
            name = "Просроченное лекарство",
            time = pastTime,
            takenToday = false,
            lastTakenTime = 0
        )
        
        testMedicineScenario(overdueMedicine, "Просроченное", today)
        
        // ТЕСТ 2: Лекарство принято сегодня
        println("\n--- ТЕСТ 2: Принятое лекарство ---")
        val takenMedicine = createMedicine(
            name = "Принятое лекарство",
            time = pastTime,
            takenToday = true,
            lastTakenTime = System.currentTimeMillis()
        )
        
        testMedicineScenario(takenMedicine, "Принятое", today)
        
        // ТЕСТ 3: Лекарство на будущее время
        println("\n--- ТЕСТ 3: Будущее лекарство ---")
        val futureMedicine = createMedicine(
            name = "Будущее лекарство",
            time = futureTime,
            takenToday = false,
            lastTakenTime = 0
        )
        
        testMedicineScenario(futureMedicine, "Будущее", today)
        
        // ТЕСТ 4: Лекарство принято, но время еще не пришло
        println("\n--- ТЕСТ 4: Принятое будущее лекарство ---")
        val takenFutureMedicine = createMedicine(
            name = "Принятое будущее лекарство",
            time = futureTime,
            takenToday = true,
            lastTakenTime = System.currentTimeMillis()
        )
        
        testMedicineScenario(takenFutureMedicine, "Принятое будущее", today)
        
        // ТЕСТ 5: Лекарство с пропущенным приемом
        println("\n--- ТЕСТ 5: Пропущенное лекарство ---")
        val missedMedicine = createMedicine(
            name = "Пропущенное лекарство",
            time = pastTime,
            takenToday = false,
            lastTakenTime = 0,
            isMissed = true,
            missedCount = 1
        )
        
        testMedicineScenario(missedMedicine, "Пропущенное", today)
        
        println("\n=== РЕЗУЛЬТАТЫ КОМПЛЕКСНОЙ ПРОВЕРКИ ===")
        println("Все тесты завершены. Проверьте логи выше для детального анализа.")
    }
    
    private fun createMedicine(
        name: String,
        time: LocalTime,
        takenToday: Boolean,
        lastTakenTime: Long,
        isMissed: Boolean = false,
        missedCount: Int = 0
    ): Medicine {
        val today = LocalDate.now()
        return Medicine(
            id = System.currentTimeMillis(),
            name = name,
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = time,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = takenToday,
            lastTakenTime = lastTakenTime,
            isMissed = isMissed,
            missedCount = missedCount
        )
    }
    
    private suspend fun testMedicineScenario(
        originalMedicine: Medicine,
        scenarioName: String,
        date: LocalDate
    ) {
        println("Сценарий: $scenarioName")
        println("  - Название: ${originalMedicine.name}")
        println("  - Время: ${originalMedicine.time}")
        println("  - TakenToday: ${originalMedicine.takenToday}")
        println("  - LastTakenTime: ${originalMedicine.lastTakenTime}")
        println("  - IsMissed: ${originalMedicine.isMissed}")
        println("  - MissedCount: ${originalMedicine.missedCount}")
        
        // 1. Сохраняем исходное лекарство
        val insertResult = medicineRepository.insertMedicine(originalMedicine)
        println("  1. Сохранение: $insertResult")
        
        // 2. Проверяем в списке "на сегодня" ДО редактирования
        val todayMedicinesBefore = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            date
        )
        val isInListBefore = todayMedicinesBefore.any { it.id == originalMedicine.id }
        println("  2. В списке 'на сегодня' ДО редактирования: $isInListBefore")
        
        // 3. Редактируем лекарство (меняем только название)
        val editedMedicine = originalMedicine.copy(
            name = "Отредактированное ${originalMedicine.name}",
            updatedAt = System.currentTimeMillis()
        )
        
        println("  3. Редактируем лекарство: ${editedMedicine.name}")
        println("     - StartDate остался: ${editedMedicine.startDate}")
        println("     - TakenToday остался: ${editedMedicine.takenToday}")
        
        val updateResult = medicineRepository.updateMedicine(editedMedicine)
        println("  4. Результат обновления: $updateResult")
        
        // 4. Проверяем в списке "на сегодня" ПОСЛЕ редактирования
        val todayMedicinesAfter = DosageCalculator.getActiveMedicinesForDate(
            medicineRepository.getAllMedicines(),
            date
        )
        val isInListAfter = todayMedicinesAfter.any { it.id == editedMedicine.id }
        println("  5. В списке 'на сегодня' ПОСЛЕ редактирования: $isInListAfter")
        
        // 5. Проверяем статус лекарства
        val medicineStatus = MedicineStatusHelper.getMedicineStatus(editedMedicine)
        println("  6. Статус лекарства: $medicineStatus")
        
        // 6. Анализируем результат
        println("  РЕЗУЛЬТАТ для $scenarioName:")
        println("    ДО редактирования: $isInListBefore")
        println("    ПОСЛЕ редактирования: $isInListAfter")
        println("    Статус: $medicineStatus")
        
        if (isInListBefore != isInListAfter) {
            println("    ⚠️  ПРОБЛЕМА: Статус в списке изменился после редактирования!")
        } else {
            println("    ✅ ОК: Статус в списке не изменился")
        }
        
        // Очищаем для следующего теста
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
    fun testDosageCalculatorLogic() = runBlocking {
        println("\n=== ПРОВЕРКА ЛОГИКИ DOSAGECALCULATOR ===")
        
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(2)
        
        // Создаем лекарство
        val medicine = createMedicine(
            name = "Тестовое лекарство",
            time = pastTime,
            takenToday = false,
            lastTakenTime = 0
        )
        
        println("1. Создаем лекарство:")
        println("   - Время: ${medicine.time}")
        println("   - TakenToday: ${medicine.takenToday}")
        println("   - LastTakenTime: ${medicine.lastTakenTime}")
        println("   - StartDate: ${medicine.startDate}")
        
        // Сохраняем
        medicineRepository.insertMedicine(medicine)
        
        // Получаем все лекарства
        val allMedicines = medicineRepository.getAllMedicines()
        println("2. Всего лекарств в базе: ${allMedicines.size}")
        
        // Проверяем логику DosageCalculator
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("3. Активных лекарств для сегодня: ${activeMedicines.size}")
        
        activeMedicines.forEach { med ->
            println("   - ${med.name}: takenToday=${med.takenToday}, lastTakenTime=${med.lastTakenTime}")
        }
        
        val isInActiveList = activeMedicines.any { it.id == medicine.id }
        println("4. Наше лекарство в активном списке: $isInActiveList")
        
        // Проверяем логику фильтрации
        println("5. Анализ логики фильтрации:")
        
        // Проверяем shouldTakeMedicine
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
        println("   - shouldTakeMedicine: $shouldTake")
        
        // Проверяем isActive
        val isActive = medicine.isActive
        println("   - isActive: $isActive")
        
        // Проверяем lastTakenDate
        val lastTakenDate = if (medicine.lastTakenTime > 0) {
            LocalDate.ofEpochDay(medicine.lastTakenTime / (24 * 60 * 60 * 1000))
        } else {
            LocalDate.MIN
        }
        println("   - lastTakenDate: $lastTakenDate")
        println("   - today: $today")
        println("   - lastTakenDate != today: ${lastTakenDate != today}")
        
        // Проверяем takenToday
        println("   - takenToday: ${medicine.takenToday}")
        
        println("6. Итоговая логика:")
        println("   - isActive && shouldTake: ${isActive && shouldTake}")
        println("   - lastTakenDate != today: ${lastTakenDate != today}")
        println("   - Результат: ${(isActive && shouldTake) && (lastTakenDate != today)}")
        
        assert(isInActiveList) { "Лекарство должно быть в активном списке" }
        println("✅ Логика DosageCalculator работает правильно")
    }
} 