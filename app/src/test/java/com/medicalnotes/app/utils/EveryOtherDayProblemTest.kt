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
class EveryOtherDayProblemTest {

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
    fun testEveryOtherDayProblemReproduction() = runBlocking {
        println("=== ТЕСТ ВОСПРОИЗВЕДЕНИЯ ПРОБЛЕМЫ 'ЧЕРЕЗ ДЕНЬ' ===")
        
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault())
        val startDate = startOfDay.toInstant().toEpochMilli()
        
        println("Сегодня: $today")
        println("startDate: $startDate")
        
        // Создаем лекарство с частотой "через день" (как в логах пользователя)
        val medicine = Medicine(
            id = 1L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(21, 54), // Время из логов пользователя
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
        
        println("Создано лекарство: ${medicine.name}")
        println("Частота: ${medicine.frequency}")
        println("Время: ${medicine.time}")
        println("startDate: ${medicine.startDate}")
        println("takenToday: ${medicine.takenToday}")
        
        // Добавляем лекарство в DataManager
        dataManager.addMedicine(medicine)
        println("Лекарство добавлено в DataManager")
        
        // Проверяем, что лекарство сохранено
        val savedMedicines = dataManager.loadMedicines()
        println("Сохранено лекарств: ${savedMedicines.size}")
        assertEquals("Должно быть 1 лекарство после добавления", 1, savedMedicines.size)
        
        // Проверяем логику DosageCalculator
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
        println("DosageCalculator.shouldTakeMedicine: $shouldTake")
        
        // Проверяем getActiveMedicinesForDate
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(savedMedicines, today)
        println("DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size} лекарств")
        for (med in activeMedicines) {
            println("  - ${med.name}: ${med.frequency}")
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
                println("  - ${med.name}: ${med.frequency}")
            }
        }
        
        // Анализируем результаты
        println("=== АНАЛИЗ РЕЗУЛЬТАТОВ ===")
        println("1. DosageCalculator.shouldTakeMedicine: $shouldTake")
        println("2. DosageCalculator.getActiveMedicinesForDate: ${activeMedicines.size}")
        println("3. MedicineRepository.getAllMedicines: ${repoMedicines.size}")
        println("4. MainViewModel.todayMedicines.value: ${todayMedicines?.size ?: 0}")
        
        // Проверяем, что проблема воспроизводится
        if (shouldTake && activeMedicines.isNotEmpty() && repoMedicines.isNotEmpty() && (todayMedicines?.isEmpty() != false)) {
            println("✅ ПРОБЛЕМА ВОСПРОИЗВЕДЕНА: DosageCalculator работает, но MainViewModel возвращает пустой список")
        } else if (!shouldTake) {
            println("❌ DosageCalculator.shouldTakeMedicine возвращает false - логика расчета неверна")
        } else if (activeMedicines.isEmpty()) {
            println("❌ DosageCalculator.getActiveMedicinesForDate возвращает пустой список")
        } else if (repoMedicines.isEmpty()) {
            println("❌ MedicineRepository.getAllMedicines возвращает пустой список")
        } else {
            println("✅ Все работает корректно")
        }
        
        // Дополнительная проверка: тестируем с DAILY частотой
        println("=== ТЕСТ С ЧАСТОТОЙ 'КАЖДЫЙ ДЕНЬ' ===")
        val dailyMedicine = medicine.copy(
            id = 2L,
            name = "Липетор",
            frequency = DosageFrequency.DAILY,
            time = LocalTime.of(21, 57)
        )
        
        dataManager.addMedicine(dailyMedicine)
        val allMedicines = dataManager.loadMedicines()
        val dailyActiveMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
        println("С DAILY частотой: ${dailyActiveMedicines.size} активных лекарств")
        
        // Очищаем и тестируем только DAILY
        runBlocking {
            withContext(Dispatchers.IO) {
                val medicinesFile = context.getFileStreamPath("medicines.json")
                if (medicinesFile.exists()) {
                    medicinesFile.delete()
                }
            }
        }
        
        dataManager.addMedicine(dailyMedicine)
        mainViewModel.loadTodayMedicines()
        kotlinx.coroutines.delay(1000)
        
        val dailyTodayMedicines = mainViewModel.todayMedicines.value
        println("MainViewModel с DAILY: ${dailyTodayMedicines?.size ?: 0} лекарств")
        
        println("=== ИТОГОВЫЙ АНАЛИЗ ===")
        println("EVERY_OTHER_DAY: ${todayMedicines?.size ?: 0} лекарств в MainViewModel")
        println("DAILY: ${dailyTodayMedicines?.size ?: 0} лекарств в MainViewModel")
        
        if ((todayMedicines?.isEmpty() != false) && (dailyTodayMedicines?.isNotEmpty() == true)) {
            println("✅ ПРОБЛЕМА ПОДТВЕРЖДЕНА: EVERY_OTHER_DAY не работает, DAILY работает")
        }
    }
} 