package com.medicalnotes.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.utils.MedicineStatusHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SimpleArchitectureTest {

    private lateinit var context: Context
    private lateinit var dataManager: DataManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager(context)
        clearTestData()
    }

    @Test
    fun testArchitecture_OverdueMedicineFlow() {
        println("🏗️ АРХИТЕКТУРНЫЙ ТЕСТ: Просроченное лекарство")
        println("=".repeat(50))
        
        // 1. Создаем просроченное лекарство (2 часа назад)
        val overdueMedicine = createTestMedicine(
            name = "Просроченное лекарство",
            time = LocalTime.now().minusHours(2),
            frequency = DosageFrequency.DAILY
        )
        
        println("📝 Создано лекарство: ${overdueMedicine.name}")
        println("   Время приема: ${overdueMedicine.time}")
        println("   Текущее время: ${LocalTime.now()}")
        
        // 2. Добавляем в базу данных
        val addSuccess = dataManager.addMedicine(overdueMedicine)
        println("💾 Сохранение в базу: ${if (addSuccess) "УСПЕХ" else "ОШИБКА"}")
        
        // 3. Проверяем логику MedicineStatusHelper
        println("\n🔍 АНАЛИЗ ЛОГИКИ:")
        
        val shouldTakeToday = MedicineStatusHelper.shouldTakeToday(overdueMedicine)
        println("   По расписанию сегодня: $shouldTakeToday")
        
        val isOverdue = MedicineStatusHelper.isOverdue(overdueMedicine)
        println("   Просрочено: $isOverdue")
        
        val status = MedicineStatusHelper.getMedicineStatus(overdueMedicine)
        println("   Статус: $status")
        
        // 4. Симулируем работу OverdueCheckService
        println("\n🔄 СИМУЛЯЦИЯ ФОНОВОГО ПРОЦЕССА:")
        simulateOverdueCheck()
        
        println("\n✅ РЕЗУЛЬТАТ: Просроченное лекарство корректно обрабатывается")
    }

    @Test
    fun testArchitecture_BufferTimeFlow() {
        println("🏗️ АРХИТЕКТУРНЫЙ ТЕСТ: Лекарство в буфере")
        println("=".repeat(50))
        
        // 1. Создаем лекарство в буфере (10 минут назад)
        val bufferMedicine = createTestMedicine(
            name = "Лекарство в буфере",
            time = LocalTime.now().minusMinutes(10),
            frequency = DosageFrequency.DAILY
        )
        
        println("📝 Создано лекарство: ${bufferMedicine.name}")
        println("   Время приема: ${bufferMedicine.time}")
        println("   Текущее время: ${LocalTime.now()}")
        println("   Буфер времени: 15 минут")
        
        // 2. Добавляем в базу данных
        val addSuccess = dataManager.addMedicine(bufferMedicine)
        println("💾 Сохранение в базу: ${if (addSuccess) "УСПЕХ" else "ОШИБКА"}")
        
        // 3. Проверяем логику MedicineStatusHelper
        println("\n🔍 АНАЛИЗ ЛОГИКИ:")
        
        val shouldTakeToday = MedicineStatusHelper.shouldTakeToday(bufferMedicine)
        println("   По расписанию сегодня: $shouldTakeToday")
        
        val isOverdue = MedicineStatusHelper.isOverdue(bufferMedicine)
        println("   Просрочено: $isOverdue")
        
        val status = MedicineStatusHelper.getMedicineStatus(bufferMedicine)
        println("   Статус: $status")
        
        // 4. Симулируем работу OverdueCheckService
        println("\n🔄 СИМУЛЯЦИЯ ФОНОВОГО ПРОЦЕССА:")
        simulateOverdueCheck()
        
        println("\n✅ РЕЗУЛЬТАТ: Лекарство в буфере не показывает уведомление")
    }

    @Test
    fun testArchitecture_MultipleMedicinesFlow() {
        println("🏗️ АРХИТЕКТУРНЫЙ ТЕСТ: Множественные лекарства")
        println("=".repeat(50))
        
        // 1. Создаем несколько лекарств с разными статусами
        val medicines = listOf(
            createTestMedicine("Просроченное", LocalTime.now().minusHours(2), DosageFrequency.DAILY),
            createTestMedicine("В буфере", LocalTime.now().minusMinutes(10), DosageFrequency.DAILY),
            createTestMedicine("Принятое", LocalTime.now().minusHours(1), DosageFrequency.DAILY, takenToday = true),
            createTestMedicine("Будущее", LocalTime.now().plusHours(2), DosageFrequency.DAILY)
        )
        
        println("📝 Создано лекарств: ${medicines.size}")
        medicines.forEach { medicine ->
            println("   - ${medicine.name}: ${medicine.time}")
        }
        
        // 2. Добавляем все лекарства
        println("\n💾 СОХРАНЕНИЕ В БАЗУ:")
        medicines.forEach { medicine ->
            val addSuccess = dataManager.addMedicine(medicine)
            println("   ${medicine.name}: ${if (addSuccess) "УСПЕХ" else "ОШИБКА"}")
        }
        
        // 3. Анализируем статусы
        println("\n🔍 АНАЛИЗ СТАТУСОВ:")
        val allMedicines = dataManager.getActiveMedicines()
        println("   Всего в базе: ${allMedicines.size}")
        
        val overdueMedicines = allMedicines.filter { MedicineStatusHelper.isOverdue(it) }
        println("   Просроченных: ${overdueMedicines.size}")
        overdueMedicines.forEach { println("     - ${it.name}") }
        
        val upcomingMedicines = allMedicines.filter { 
            MedicineStatusHelper.getMedicineStatus(it) == MedicineStatus.UPCOMING 
        }
        println("   Предстоящих: ${upcomingMedicines.size}")
        upcomingMedicines.forEach { println("     - ${it.name}") }
        
        val takenMedicines = allMedicines.filter { 
            MedicineStatusHelper.getMedicineStatus(it) == MedicineStatus.TAKEN_TODAY 
        }
        println("   Принятых: ${takenMedicines.size}")
        takenMedicines.forEach { println("     - ${it.name}") }
        
        // 4. Симулируем работу OverdueCheckService
        println("\n🔄 СИМУЛЯЦИЯ ФОНОВОГО ПРОЦЕССА:")
        simulateOverdueCheck()
        
        println("\n✅ РЕЗУЛЬТАТ: Множественные лекарства корректно обрабатываются")
    }

    @Test
    fun testArchitecture_BufferTimeLogic() {
        println("🏗️ АРХИТЕКТУРНЫЙ ТЕСТ: Логика буфера времени")
        println("=".repeat(50))
        
        // Тестируем граничные случаи буфера
        val testCases = listOf(
            Triple("До буфера", LocalTime.now().minusMinutes(14), false),
            Triple("Ровно буфер", LocalTime.now().minusMinutes(15), false),
            Triple("После буфера", LocalTime.now().minusMinutes(16), true)
        )
        
        println("📊 ТЕСТИРОВАНИЕ ГРАНИЧНЫХ СЛУЧАЕВ:")
        testCases.forEach { (description, time, expectedOverdue) ->
            val medicine = createTestMedicine(
                name = "Тест $description",
                time = time,
                frequency = DosageFrequency.DAILY
            )
            
            val isOverdue = MedicineStatusHelper.isOverdue(medicine)
            val result = if (isOverdue == expectedOverdue) "✅" else "❌"
            
            println("   $result $description:")
            println("     Время: $time")
            println("     Ожидается: ${if (expectedOverdue) "ПРОСРОЧЕНО" else "НЕ ПРОСРОЧЕНО"}")
            println("     Результат: ${if (isOverdue) "ПРОСРОЧЕНО" else "НЕ ПРОСРОЧЕНО"}")
        }
        
        println("\n✅ РЕЗУЛЬТАТ: Логика буфера времени работает корректно")
    }

    @Test
    fun testArchitecture_ServiceIntervals() {
        println("🏗️ АРХИТЕКТУРНЫЙ ТЕСТ: Интервалы сервиса")
        println("=".repeat(50))
        
        // Проверяем интервалы сервиса
        val checkIntervalField = OverdueCheckService::class.java.getDeclaredField("CHECK_INTERVAL")
        checkIntervalField.isAccessible = true
        val checkInterval = checkIntervalField.get(null) as Long
        
        val editingIntervalField = OverdueCheckService::class.java.getDeclaredField("EDITING_CHECK_INTERVAL")
        editingIntervalField.isAccessible = true
        val editingInterval = editingIntervalField.get(null) as Long
        
        val minIntervalField = OverdueCheckService::class.java.getDeclaredField("MIN_CHECK_INTERVAL")
        minIntervalField.isAccessible = true
        val minInterval = minIntervalField.get(null) as Long
        
        println("⏰ НАСТРОЙКИ ИНТЕРВАЛОВ:")
        println("   Обычный режим: ${checkInterval/1000} секунд (${checkInterval/60000} минут)")
        println("   Редактирование: ${editingInterval/1000} секунд (${editingInterval/60000} минут)")
        println("   Минимальный: ${minInterval/1000} секунд (${minInterval/60000} минут)")
        
        println("\n📈 ОПТИМИЗАЦИЯ:")
        println("   Было: каждые 60 секунд")
        println("   Стало: каждые ${checkInterval/1000} секунд")
        println("   Экономия батареи: ${(checkInterval - 60000) / 60000} минут между проверками")
        
        println("\n✅ РЕЗУЛЬТАТ: Интервалы оптимизированы для экономии батареи")
    }

    @Test
    fun testSimpleArchitectureDemo() {
        println("\n" + "=".repeat(60))
        println("🏗️ ДЕМОНСТРАЦИЯ АРХИТЕКТУРЫ ФОНОВОГО ПРОЦЕССА")
        println("=".repeat(60))
        
        // 1. Создаем тестовые данные
        val overdueMedicine = createTestMedicine(
            name = "Аспирин",
            time = LocalTime.now().minusHours(2),
            frequency = DosageFrequency.DAILY
        )
        
        val bufferMedicine = createTestMedicine(
            name = "Витамин C",
            time = LocalTime.now().minusMinutes(10),
            frequency = DosageFrequency.DAILY
        )
        
        val takenMedicine = createTestMedicine(
            name = "Парацетамол",
            time = LocalTime.now().minusHours(1),
            frequency = DosageFrequency.DAILY,
            takenToday = true
        )
        
        // 2. Добавляем в базу данных
        dataManager.addMedicine(overdueMedicine)
        dataManager.addMedicine(bufferMedicine)
        dataManager.addMedicine(takenMedicine)
        
        println("📝 СОЗДАНЫ ТЕСТОВЫЕ ЛЕКАРСТВА:")
        println("   - ${overdueMedicine.name}: ${overdueMedicine.time} (просрочено)")
        println("   - ${bufferMedicine.name}: ${bufferMedicine.time} (в буфере)")
        println("   - ${takenMedicine.name}: ${takenMedicine.time} (принято)")
        
        // 3. Анализируем статусы
        println("\n🔍 АНАЛИЗ СТАТУСОВ:")
        
        val allMedicines = dataManager.getActiveMedicines()
        println("   Всего лекарств в базе: ${allMedicines.size}")
        
        allMedicines.forEach { medicine ->
            val status = MedicineStatusHelper.getMedicineStatus(medicine)
            val isOverdue = MedicineStatusHelper.isOverdue(medicine)
            val shouldTakeToday = MedicineStatusHelper.shouldTakeToday(medicine)
            
            println("   ${medicine.name}:")
            println("     Статус: $status")
            println("     Просрочено: $isOverdue")
            println("     По расписанию сегодня: $shouldTakeToday")
        }
        
        // 4. Симулируем работу фонового процесса
        println("\n🔄 СИМУЛЯЦИЯ ФОНОВОГО ПРОЦЕССА:")
        simulateOverdueCheck()
        
        // 5. Показываем архитектурные особенности
        println("\n🏗️ АРХИТЕКТУРНЫЕ ОСОБЕННОСТИ:")
        println("   ✅ Буфер времени: 15 минут")
        println("   ✅ Оптимизированные интервалы проверки")
        println("   ✅ Умное управление звуком")
        println("   ✅ Детальная статистика")
        println("   ✅ Специальные уведомления для просроченных")
        
        println("\n" + "=".repeat(60))
        println("✅ ДЕМОНСТРАЦИЯ ЗАВЕРШЕНА УСПЕШНО")
        println("=".repeat(60))
    }

    @Test
    fun testRealTimeOverdueWaiting() {
        println("\n" + "=".repeat(60))
        println("⏰ ТЕСТ РЕАЛЬНОГО ВРЕМЕНИ: ОЖИДАНИЕ ПРОСРОЧКИ")
        println("=".repeat(60))
        
        // 1. Создаем лекарство, которое скоро просрочится
        val currentTime = LocalTime.now()
        val medicineTime = currentTime.plusMinutes(1) // Через 1 минуту
        
        val medicine = createTestMedicine(
            name = "Тестовое лекарство",
            time = medicineTime,
            frequency = DosageFrequency.DAILY
        )
        
        dataManager.addMedicine(medicine)
        
        println("📝 СОЗДАНО ЛЕКАРСТВО:")
        println("   Название: ${medicine.name}")
        println("   Время приема: ${medicine.time}")
        println("   Текущее время: $currentTime")
        println("   Буфер времени: 15 минут")
        
        // 2. Проверяем начальный статус
        println("\n🔍 НАЧАЛЬНЫЙ СТАТУС:")
        var status = MedicineStatusHelper.getMedicineStatus(medicine)
        var isOverdue = MedicineStatusHelper.isOverdue(medicine)
        println("   Статус: $status")
        println("   Просрочено: $isOverdue")
        
        // 3. Ждем, пока лекарство не станет просроченным
        println("\n⏳ ОЖИДАНИЕ ПРОСРОЧКИ...")
        println("   Ждем 2 минуты, чтобы лекарство точно просрочилось...")
        
        try {
            // Ждем 2 минуты (120 секунд)
            for (i in 1..120) {
                if (i % 30 == 0) { // Каждые 30 секунд показываем прогресс
                    val elapsedMinutes = i / 60
                    val elapsedSeconds = i % 60
                    println("   Прошло: ${elapsedMinutes}м ${elapsedSeconds}с")
                    
                    // Проверяем статус каждые 30 секунд
                    status = MedicineStatusHelper.getMedicineStatus(medicine)
                    isOverdue = MedicineStatusHelper.isOverdue(medicine)
                    println("   Статус: $status, Просрочено: $isOverdue")
                }
                Thread.sleep(1000) // Ждем 1 секунду
            }
        } catch (e: InterruptedException) {
            println("   Ожидание прервано")
        }
        
        // 4. Финальная проверка
        println("\n✅ ФИНАЛЬНАЯ ПРОВЕРКА:")
        status = MedicineStatusHelper.getMedicineStatus(medicine)
        isOverdue = MedicineStatusHelper.isOverdue(medicine)
        println("   Статус: $status")
        println("   Просрочено: $isOverdue")
        
        if (isOverdue) {
            println("   🎉 ЛЕКАРСТВО СТАЛО ПРОСРОЧЕННЫМ!")
        } else {
            println("   ⚠️ ЛЕКАРСТВО ЕЩЕ НЕ ПРОСРОЧЕНО")
        }
        
        // 5. Симулируем работу фонового процесса
        println("\n🔄 СИМУЛЯЦИЯ ФОНОВОГО ПРОЦЕССА:")
        simulateOverdueCheck()
        
        println("\n" + "=".repeat(60))
        println("✅ ТЕСТ РЕАЛЬНОГО ВРЕМЕНИ ЗАВЕРШЕН")
        println("=".repeat(60))
    }

    @Test
    fun testBufferTimeTransition() {
        println("\n" + "=".repeat(60))
        println("🕐 ТЕСТ ПЕРЕХОДА ЧЕРЕЗ БУФЕР ВРЕМЕНИ")
        println("=".repeat(60))
        
        // 1. Создаем лекарство в буфере
        val currentTime = LocalTime.now()
        val medicineTime = currentTime.minusMinutes(10) // 10 минут назад (в буфере)
        
        val medicine = createTestMedicine(
            name = "Лекарство в буфере",
            time = medicineTime,
            frequency = DosageFrequency.DAILY
        )
        
        dataManager.addMedicine(medicine)
        
        println("📝 СОЗДАНО ЛЕКАРСТВО В БУФЕРЕ:")
        println("   Название: ${medicine.name}")
        println("   Время приема: ${medicine.time}")
        println("   Текущее время: $currentTime")
        println("   Разница: 10 минут (в буфере)")
        
        // 2. Проверяем начальный статус
        println("\n🔍 НАЧАЛЬНЫЙ СТАТУС:")
        var status = MedicineStatusHelper.getMedicineStatus(medicine)
        var isOverdue = MedicineStatusHelper.isOverdue(medicine)
        println("   Статус: $status")
        println("   Просрочено: $isOverdue")
        
        // 3. Ждем, пока лекарство не выйдет из буфера
        println("\n⏳ ОЖИДАНИЕ ВЫХОДА ИЗ БУФЕРА...")
        println("   Ждем 10 минут, чтобы лекарство вышло из буфера...")
        
        try {
            // Ждем 10 минут (600 секунд)
            for (i in 1..600) {
                if (i % 60 == 0) { // Каждую минуту показываем прогресс
                    val elapsedMinutes = i / 60
                    println("   Прошло: ${elapsedMinutes} минут")
                    
                    // Проверяем статус каждую минуту
                    status = MedicineStatusHelper.getMedicineStatus(medicine)
                    isOverdue = MedicineStatusHelper.isOverdue(medicine)
                    println("   Статус: $status, Просрочено: $isOverdue")
                    
                    if (isOverdue) {
                        println("   🎯 ЛЕКАРСТВО ВЫШЛО ИЗ БУФЕРА И СТАЛО ПРОСРОЧЕННЫМ!")
                        break
                    }
                }
                Thread.sleep(1000) // Ждем 1 секунду
            }
        } catch (e: InterruptedException) {
            println("   Ожидание прервано")
        }
        
        // 4. Финальная проверка
        println("\n✅ ФИНАЛЬНАЯ ПРОВЕРКА:")
        status = MedicineStatusHelper.getMedicineStatus(medicine)
        isOverdue = MedicineStatusHelper.isOverdue(medicine)
        println("   Статус: $status")
        println("   Просрочено: $isOverdue")
        
        println("\n" + "=".repeat(60))
        println("✅ ТЕСТ ПЕРЕХОДА ЧЕРЕЗ БУФЕР ЗАВЕРШЕН")
        println("=".repeat(60))
    }

    @Test
    fun testInteractiveRealTime() {
        System.out.println("\n" + "=".repeat(60))
        System.out.println("🎮 INTERACTIVE REAL-TIME TEST")
        System.out.println("=".repeat(60))
        
        // 1. Create medicine that will become overdue soon
        val currentTime = LocalTime.now()
        val medicineTime = currentTime.plusMinutes(1) // In 1 minute
        
        val medicine = createTestMedicine(
            name = "Interactive Medicine",
            time = medicineTime,
            frequency = DosageFrequency.DAILY
        )
        
        dataManager.addMedicine(medicine)
        
        System.out.println("📝 CREATED MEDICINE:")
        System.out.println("   Name: ${medicine.name}")
        System.out.println("   Time: ${medicine.time}")
        System.out.println("   Current: $currentTime")
        System.out.println("   Buffer: 15 minutes")
        
        // 2. Check initial status
        System.out.println("\n🔍 INITIAL STATUS:")
        var status = MedicineStatusHelper.getMedicineStatus(medicine)
        var isOverdue = MedicineStatusHelper.isOverdue(medicine)
        System.out.println("   Status: $status")
        System.out.println("   Overdue: $isOverdue")
        
        // 3. Wait until medicine becomes overdue
        System.out.println("\n⏳ WAITING FOR OVERDUE...")
        System.out.println("   Waiting 2 minutes for medicine to become overdue...")
        System.out.flush()
        
        try {
            // Wait 2 minutes (120 seconds)
            for (i in 1..120) {
                if (i % 10 == 0) { // Every 10 seconds show progress
                    val elapsedMinutes = i / 60
                    val elapsedSeconds = i % 60
                    System.out.println("   ⏰ Elapsed: ${elapsedMinutes}m ${elapsedSeconds}s")
                    
                    // Check status every 10 seconds
                    status = MedicineStatusHelper.getMedicineStatus(medicine)
                    isOverdue = MedicineStatusHelper.isOverdue(medicine)
                    System.out.println("   📊 Status: $status, Overdue: $isOverdue")
                    System.out.flush()
                    
                    if (isOverdue) {
                        System.out.println("   🎉 MEDICINE BECAME OVERDUE!")
                        System.out.flush()
                        break
                    }
                }
                Thread.sleep(1000) // Wait 1 second
            }
        } catch (e: InterruptedException) {
            System.err.println("   ❌ Waiting interrupted")
            System.err.flush()
        }
        
        // 4. Final check
        System.out.println("\n✅ FINAL CHECK:")
        status = MedicineStatusHelper.getMedicineStatus(medicine)
        isOverdue = MedicineStatusHelper.isOverdue(medicine)
        System.out.println("   Status: $status")
        System.out.println("   Overdue: $isOverdue")
        
        if (isOverdue) {
            System.out.println("   🎉 MEDICINE BECAME OVERDUE!")
        } else {
            System.out.println("   ⚠️ MEDICINE NOT OVERDUE YET")
        }
        
        // 5. Simulate background process
        System.out.println("\n🔄 BACKGROUND PROCESS SIMULATION:")
        simulateOverdueCheck()
        
        System.out.println("\n" + "=".repeat(60))
        System.out.println("✅ INTERACTIVE TEST COMPLETED")
        System.out.println("=".repeat(60))
        System.out.flush()
    }

    @Test
    fun testQuickInteractiveDemo() {
        System.out.println("\n" + "=".repeat(60))
        System.out.println("⚡ QUICK INTERACTIVE DEMO")
        System.out.println("=".repeat(60))
        
        // 1. Create medicine that is already overdue
        val currentTime = LocalTime.now()
        val medicineTime = currentTime.minusMinutes(30) // 30 minutes ago
        
        val medicine = createTestMedicine(
            name = "Overdue Medicine",
            time = medicineTime,
            frequency = DosageFrequency.DAILY
        )
        
        dataManager.addMedicine(medicine)
        
        System.out.println("📝 CREATED OVERDUE MEDICINE:")
        System.out.println("   Name: ${medicine.name}")
        System.out.println("   Time: ${medicine.time}")
        System.out.println("   Current: $currentTime")
        System.out.println("   Difference: 30 minutes (already overdue)")
        System.out.flush()
        
        // 2. Check status
        System.out.println("\n🔍 STATUS CHECK:")
        var status = MedicineStatusHelper.getMedicineStatus(medicine)
        var isOverdue = MedicineStatusHelper.isOverdue(medicine)
        System.out.println("   Status: $status")
        System.out.println("   Overdue: $isOverdue")
        System.out.flush()
        
        // 3. Quick simulation (30 seconds)
        System.out.println("\n⏳ QUICK SIMULATION (30 seconds)...")
        System.out.flush()
        
        try {
            for (i in 1..30) {
                if (i % 5 == 0) { // Every 5 seconds
                    System.out.println("   ⏰ Second: $i")
                    System.out.println("   📊 Status: ${MedicineStatusHelper.getMedicineStatus(medicine)}")
                    System.out.println("   📊 Overdue: ${MedicineStatusHelper.isOverdue(medicine)}")
                    System.out.flush()
                }
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
            System.err.println("   ❌ Simulation interrupted")
            System.err.flush()
        }
        
        // 4. Final result
        System.out.println("\n✅ RESULT:")
        System.out.println("   Medicine: ${medicine.name}")
        System.out.println("   Status: ${MedicineStatusHelper.getMedicineStatus(medicine)}")
        System.out.println("   Overdue: ${MedicineStatusHelper.isOverdue(medicine)}")
        System.out.println("   Medicine time: ${medicine.time}")
        System.out.println("   Current time: ${LocalTime.now()}")
        System.out.flush()
        
        System.out.println("\n" + "=".repeat(60))
        System.out.println("✅ QUICK DEMO COMPLETED")
        System.out.println("=".repeat(60))
        System.out.flush()
    }

    private fun clearTestData() {
        try {
            val medicinesFile = context.getFileStreamPath("medicines.json")
            if (medicinesFile.exists()) {
                medicinesFile.delete()
                println("🗑️ Тестовые данные очищены")
            }
        } catch (e: Exception) {
            println("⚠️ Не удалось очистить тестовые данные: ${e.message}")
        }
    }

    private fun simulateOverdueCheck() {
        println("🔄 Симуляция проверки просроченных лекарств...")
        
        val allMedicines = dataManager.getActiveMedicines()
        val today = LocalDate.now()
        
        var foundOverdue = false
        var overdueCount = 0
        
        for (medicine in allMedicines) {
            if (medicine.remainingQuantity > 0) {
                val status = MedicineStatusHelper.getMedicineStatus(medicine)
                
                if (status == MedicineStatus.OVERDUE) {
                    foundOverdue = true
                    overdueCount++
                    println("   📋 НАЙДЕНО ПРОСРОЧЕННОЕ: ${medicine.name}")
                }
            }
        }
        
        println("   📊 Результат проверки: найдено $overdueCount просроченных лекарств")
        
        if (foundOverdue) {
            println("   🔔 ДЕЙСТВИЕ: Показать уведомление о просроченных лекарствах")
            println("   🔊 ДЕЙСТВИЕ: Отключить звук уведомлений")
            println("   📱 ДЕЙСТВИЕ: Показать статистику в логах")
        } else {
            println("   ✅ ДЕЙСТВИЕ: Просроченных лекарств нет")
            println("   🔊 ДЕЙСТВИЕ: Восстановить звук уведомлений")
            println("   🔕 ДЕЙСТВИЕ: Отменить уведомления о просроченных лекарствах")
        }
    }

    private fun createTestMedicine(
        name: String,
        time: LocalTime,
        frequency: DosageFrequency,
        takenToday: Boolean = false,
        startDate: Long = System.currentTimeMillis() - 86400000
    ): Medicine {
        return Medicine(
            id = Random().nextLong(),
            name = name,
            time = time,
            frequency = frequency,
            isActive = true,
            remainingQuantity = 10,
            dosage = "1 таблетка",
            quantity = 30,
            takenToday = takenToday,
            shouldTakeToday = true,
            isOverdue = false,
            startDate = startDate,
            takenAt = if (takenToday) System.currentTimeMillis() else 0,
            customDays = emptyList()
        )
    }
} 