package com.medicalnotes.app.utils

import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import java.io.File
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SimpleDataManagerTest {
    
    private lateinit var context: Context
    private lateinit var dataManager: DataManager
    private lateinit var testFile: File
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
        
        // Очищаем тестовые данные
        testFile = File(context.filesDir, "medicines.json")
        if (testFile.exists()) {
            testFile.delete()
        }
    }
    
    @After
    fun tearDown() {
        // Очищаем тестовые данные после теста
        if (testFile.exists()) {
            testFile.delete()
        }
    }
    
    @Test
    fun testMedicineCopy_ShouldPreserveTime() {
        // Создаем лекарство
        val originalMedicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "Таблетки",
            time = LocalTime.of(14, 30), // 14:30
            notes = "Тестовое лекарство",
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7),
            customTimes = listOf(),
            doseTimes = listOf(LocalTime.of(14, 30)),
            relatedMedicineIds = listOf(),
            groupId = null,
            groupName = "",
            groupOrder = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis(),
            lastTakenTime = 0,
            takenAt = 0,
            isActive = true,
            isInsulin = false,
            isMissed = false,
            takenToday = false,
            shouldTakeToday = true,
            isOverdue = false,
            multipleDoses = false,
            isPartOfGroup = false,
            missedCount = 0,
            dosesPerDay = 1,
            timeGroupId = null,
            timeGroupName = "",
            timeGroupOrder = 0
        )
        
        // Создаем копию с новым временем
        val updatedMedicine = originalMedicine.copy(
            time = LocalTime.of(16, 0), // 16:00
            updatedAt = System.currentTimeMillis()
        )
        
        // Проверяем что время изменилось
        assertEquals("Время должно быть обновлено", LocalTime.of(16, 0), updatedMedicine.time)
        assertEquals("ID должен остаться тем же", 1L, updatedMedicine.id)
        assertEquals("Название должно остаться тем же", "Тестовое лекарство", updatedMedicine.name)
        
        println("✓ Тест пройден: copy() правильно обновляет время")
    }
    
    @Test
    fun testMedicineListUpdate_ShouldUpdateTime() {
        // Создаем список лекарств
        val medicines = mutableListOf<Medicine>()
        
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "Таблетки",
            time = LocalTime.of(14, 30), // 14:30
            notes = "Тестовое лекарство",
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7),
            customTimes = listOf(),
            doseTimes = listOf(LocalTime.of(14, 30)),
            relatedMedicineIds = listOf(),
            groupId = null,
            groupName = "",
            groupOrder = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis(),
            lastTakenTime = 0,
            takenAt = 0,
            isActive = true,
            isInsulin = false,
            isMissed = false,
            takenToday = false,
            shouldTakeToday = true,
            isOverdue = false,
            multipleDoses = false,
            isPartOfGroup = false,
            missedCount = 0,
            dosesPerDay = 1,
            timeGroupId = null,
            timeGroupName = "",
            timeGroupOrder = 0
        )
        
        medicines.add(medicine)
        
        // Проверяем что лекарство добавлено
        assertEquals("Должно быть 1 лекарство", 1, medicines.size)
        assertEquals("Время должно быть 14:30", LocalTime.of(14, 30), medicines[0].time)
        
        // Обновляем лекарство в списке
        val updatedMedicine = medicine.copy(
            time = LocalTime.of(16, 0), // 16:00
            updatedAt = System.currentTimeMillis()
        )
        
        medicines[0] = updatedMedicine
        
        // Проверяем что время обновилось
        assertEquals("Время должно быть обновлено", LocalTime.of(16, 0), medicines[0].time)
        
        println("✓ Тест пройден: обновление в списке работает правильно")
    }
} 