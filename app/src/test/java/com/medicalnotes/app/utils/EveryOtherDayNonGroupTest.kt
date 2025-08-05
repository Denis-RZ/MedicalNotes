package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.MedicineRepository
import com.medicalnotes.app.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class EveryOtherDayNonGroupTest {

    private lateinit var context: android.content.Context
    private lateinit var dataManager: DataManager
    private lateinit var medicineRepository: MedicineRepository
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
        medicineRepository = MedicineRepository(context)
        mainViewModel = MainViewModel(context.applicationContext as android.app.Application)
        
        // Очищаем данные перед каждым тестом
        runBlocking {
            withContext(Dispatchers.IO) {
                val medicinesFile = context.getFileStreamPath("medicines.json")
                if (medicinesFile.exists()) {
                    medicinesFile.delete()
                }
            }
        }
    }

    @Test
    fun testEveryOtherDayNonGroupProblem() = runBlocking {
        println("=== ТЕСТ ПРОБЛЕМЫ 'ЧЕРЕЗ ДЕНЬ' НЕ В ГРУППЕ ===")
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        
        println("Сегодня: $today")
        println("startDate: $startDate")
        
        // Создаем два лекарства НЕ в группе с частотой "через день"
        val medicine1 = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "20 мг",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(17, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null, // НЕ в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            groupMetadata = null,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        val medicine2 = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "0.5 таблетки",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate, // ОДИНАКОВАЯ дата начала!
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = null, // НЕ в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            groupMetadata = null,
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("Созданы лекарства НЕ в группе:")
        println("  - ${medicine1.name}: ${medicine1.frequency} (startDate: ${medicine1.startDate})")
        println("  - ${medicine2.name}: ${medicine2.frequency} (startDate: ${medicine2.startDate})")
        println("  - Оба НЕ в группе (groupId = null)")
        println("  - Одинаковая дата начала: $startDate")
        
        // Добавляем лекарства в DataManager
        dataManager.addMedicine(medicine1)
        dataManager.addMedicine(medicine2)
        println("Лекарства добавлены в DataManager")
        
        // Проверяем, что лекарства сохранены
        val savedMedicines = dataManager.loadMedicines()
        println("Сохранено лекарств: ${savedMedicines.size}")
        assertEquals("Должно быть 2 лекарства после добавления", 2, savedMedicines.size)
        
        // Проверяем логику DosageCalculator для каждого лекарства
        println("=== ПРОВЕРКА ЛОГИКИ DOSAGECALCULATOR ===")
        
        val shouldTake1 = DosageCalculator.shouldTakeMedicine(medicine1, today, savedMedicines)
        val shouldTake2 = DosageCalculator.shouldTakeMedicine(medicine2, today, savedMedicines)
        
        println("DosageCalculator.shouldTakeMedicine для ${medicine1.name}: $shouldTake1")
        println("DosageCalculator.shouldTakeMedicine для ${medicine2.name}: $shouldTake2")
        
        // Проверяем getActiveMedicinesForDate
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(savedMedicines, today)
        println("DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size} лекарств")
        for (med in activeMedicines) {
            println("  - ${med.name}: ${med.frequency} (groupId: ${med.groupId})")
        }
        
        // Проверяем MedicineRepository
        val repoMedicines = medicineRepository.getAllMedicines()
        println("MedicineRepository.getAllMedicines: ${repoMedicines.size} лекарств")
        
        // Тестируем MainViewModel
        println("=== ТЕСТИРОВАНИЕ MainViewModel ===")
        mainViewModel.loadTodayMedicines()
        
        // Ждем немного для выполнения корутины
        kotlinx.coroutines.delay(1000)
        
        // Проверяем LiveData
        val todayMedicines = mainViewModel.todayMedicines.value
        println("MainViewModel.todayMedicines.value: ${todayMedicines?.size ?: 0} лекарств")
        
        if (todayMedicines != null) {
            for (med in todayMedicines) {
                println("  - ${med.name}: ${med.frequency} (groupId: ${med.groupId})")
            }
        }
        
        // Анализируем результаты
        println("=== АНАЛИЗ РЕЗУЛЬТАТОВ ===")
        println("1. DosageCalculator.shouldTakeMedicine для ${medicine1.name}: $shouldTake1")
        println("2. DosageCalculator.shouldTakeMedicine для ${medicine2.name}: $shouldTake2")
        println("3. DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size}")
        println("4. MedicineRepository.getAllMedicines: ${repoMedicines.size}")
        println("5. MainViewModel.todayMedicines.value: ${todayMedicines?.size ?: 0}")
        
        // Проверяем проблему
        if (shouldTake1 && shouldTake2) {
            println("❌ ПРОБЛЕМА ОБНАРУЖЕНА: Оба лекарства должны приниматься сегодня одновременно")
            println("   Это происходит потому что у них одинаковая дата начала и частота 'через день'")
            println("   Оба лекарства используют одинаковую логику расчета: daysSinceStart % 2 == 0")
        } else if (shouldTake1 && !shouldTake2) {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Только ${medicine1.name} должен приниматься сегодня")
        } else if (!shouldTake1 && shouldTake2) {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Только ${medicine2.name} должен приниматься сегодня")
        } else {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Ни одно лекарство не должно приниматься сегодня")
        }
        
        // Дополнительная проверка: тестируем с разными датами начала
        println("=== ТЕСТ С РАЗНЫМИ ДАТАМИ НАЧАЛА ===")
        
        // Создаем лекарство с другой датой начала
        val medicine3 = medicine2.copy(
            id = 3L,
            name = "Фубуксусат (смещенная дата)",
            startDate = startDate + (24 * 60 * 60 * 1000) // +1 день
        )
        
        dataManager.addMedicine(medicine3)
        val allMedicinesWithOffset = dataManager.loadMedicines()
        
        val shouldTake1Offset = DosageCalculator.shouldTakeMedicine(medicine1, today, allMedicinesWithOffset)
        val shouldTake3Offset = DosageCalculator.shouldTakeMedicine(medicine3, today, allMedicinesWithOffset)
        
        println("С разными датами начала:")
        println("  - ${medicine1.name}: $shouldTake1Offset (startDate: ${medicine1.startDate})")
        println("  - ${medicine3.name}: $shouldTake3Offset (startDate: ${medicine3.startDate})")
        
        if (shouldTake1Offset && shouldTake3Offset) {
            println("❌ ПРОБЛЕМА: Оба лекарства все еще принимаются одновременно")
        } else {
            println("✅ РЕШЕНИЕ: С разными датами начала лекарства чередуются")
        }
        
        println("=== ИТОГОВЫЙ АНАЛИЗ ===")
        println("Проблема: Лекарства 'через день' с одинаковой датой начала появляются одновременно")
        println("Причина: Оба используют одинаковую логику расчета daysSinceStart % 2 == 0")
        println("Решение: Использовать разные даты начала или группировку")
    }
} 