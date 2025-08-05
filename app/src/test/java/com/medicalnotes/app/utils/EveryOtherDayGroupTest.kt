package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.GroupMetadata
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
class EveryOtherDayGroupTest {

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
    fun testEveryOtherDayGroupProblem() = runBlocking {
        println("=== ТЕСТ ПРОБЛЕМЫ ГРУППЫ 'ЧЕРЕЗ ДЕНЬ' ===")
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        
        println("Сегодня: $today")
        println("startDate: $startDate")
        
        // Создаем группу лекарств "через день"
        val groupId = 1L
        val groupName = "Тест группа"
        val groupStartDate = startDate
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        // Лекарство 1 - должно приниматься в четные дни группы (groupOrder = 1)
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
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1, // Первое лекарство в группе
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString(),
            groupMetadata = GroupMetadata(
                groupId = groupId,
                groupName = groupName,
                groupSize = 2,
                groupFrequency = groupFrequency,
                groupStartDate = groupStartDate,
                groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
            ),
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Лекарство 2 - должно приниматься в нечетные дни группы (groupOrder = 2)
        val medicine2 = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "0.5 таблетки",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(22, 54),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            isActive = true,
            takenToday = false,
            lastTakenTime = 0L,
            takenAt = 0L,
            isMissed = false,
            missedCount = 0,
            isOverdue = false,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2, // Второе лекарство в группе
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString(),
            groupMetadata = GroupMetadata(
                groupId = groupId,
                groupName = groupName,
                groupSize = 2,
                groupFrequency = groupFrequency,
                groupStartDate = groupStartDate,
                groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
            ),
            multipleDoses = false,
            doseTimes = emptyList(),
            customDays = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("Создана группа лекарств:")
        println("  - Группа ID: $groupId")
        println("  - Название группы: $groupName")
        println("  - Частота группы: $groupFrequency")
        println("  - Дата начала группы: $groupStartDate")
        println("  - Лекарство 1: ${medicine1.name} (порядок: ${medicine1.groupOrder})")
        println("  - Лекарство 2: ${medicine2.name} (порядок: ${medicine2.groupOrder})")
        
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
            println("  - ${med.name}: ${med.frequency} (порядок в группе: ${med.groupOrder})")
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
                println("  - ${med.name}: ${med.frequency} (порядок в группе: ${med.groupOrder})")
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
            println("   Это неправильно для группы 'через день' - они должны чередоваться")
        } else if (shouldTake1 && !shouldTake2) {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Только ${medicine1.name} должен приниматься сегодня")
        } else if (!shouldTake1 && shouldTake2) {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Только ${medicine2.name} должен приниматься сегодня")
        } else {
            println("✅ ЛОГИКА РАБОТАЕТ ПРАВИЛЬНО: Ни одно лекарство не должно приниматься сегодня")
        }
        
        // Дополнительная проверка: тестируем на следующий день
        println("=== ТЕСТ НА СЛЕДУЮЩИЙ ДЕНЬ ===")
        val tomorrow = today.plusDays(1)
        
        val shouldTake1Tomorrow = DosageCalculator.shouldTakeMedicine(medicine1, tomorrow, savedMedicines)
        val shouldTake2Tomorrow = DosageCalculator.shouldTakeMedicine(medicine2, tomorrow, savedMedicines)
        
        println("Завтра: $tomorrow")
        println("DosageCalculator.shouldTakeMedicine для ${medicine1.name} завтра: $shouldTake1Tomorrow")
        println("DosageCalculator.shouldTakeMedicine для ${medicine2.name} завтра: $shouldTake2Tomorrow")
        
        // Проверяем чередование
        if ((shouldTake1 && shouldTake2Tomorrow) || (shouldTake2 && shouldTake1Tomorrow)) {
            println("✅ ЧЕРЕДОВАНИЕ РАБОТАЕТ ПРАВИЛЬНО: Лекарства чередуются по дням")
        } else {
            println("❌ ПРОБЛЕМА С ЧЕРЕДОВАНИЕМ: Лекарства не чередуются правильно")
        }
        
        println("=== ИТОГОВЫЙ АНАЛИЗ ===")
        println("Сегодня: ${todayMedicines?.size ?: 0} лекарств в MainViewModel")
        println("Проблема: Лекарства 'через день' появляются одновременно вместо чередования")
    }
} 