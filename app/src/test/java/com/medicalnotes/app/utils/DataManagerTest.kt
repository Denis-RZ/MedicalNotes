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
class DataManagerTest {
    
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
    fun testUpdateMedicine_ShouldUpdateTime() {
        // Создаем лекарство
        val medicine = Medicine(
            id = 0L, // 0 означает что ID будет сгенерирован автоматически
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
        
        // Сохраняем лекарство
        val success = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть успешно добавлено", success)
        
        // Проверяем что лекарство сохранено
        val medicinesAfterCreation = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterCreation.size)
        assertEquals("Время должно быть 14:30", LocalTime.of(14, 30), medicinesAfterCreation[0].time)
        
        // Находим созданное лекарство по имени
        val createdMedicine = medicinesAfterCreation.find { it.name == "Тестовое лекарство" }
        assertNotNull("Созданное лекарство должно быть найдено", createdMedicine)
        
        // Создаем обновленную версию с новым временем
        val updatedMedicine = createdMedicine!!.copy(
            time = LocalTime.of(16, 0), // 16:00
            updatedAt = System.currentTimeMillis()
        )
        
        println("DEBUG: Созданное лекарство время: ${createdMedicine.time}")
        println("DEBUG: Обновленное лекарство время: ${updatedMedicine.time}")
        println("DEBUG: Обновленное лекарство ID: ${updatedMedicine.id}")
        println("DEBUG: Тип ID обновленного лекарства: ${updatedMedicine.id::class.java.simpleName}")
        
        // Обновляем лекарство
        println("DEBUG: Обновляем лекарство с ID: ${updatedMedicine.id}")
        println("DEBUG: Новое время: ${updatedMedicine.time}")
        println("DEBUG: Все лекарства перед обновлением: ${medicinesAfterCreation.map { "${it.name}(ID:${it.id}, тип:${it.id::class.java.simpleName})" }}")
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        println("DEBUG: Результат обновления: $updateSuccess")
        assertTrue("Лекарство должно быть успешно обновлено", updateSuccess)
        
        // Проверяем содержимое JSON файла до обновления
        if (testFile.exists()) {
            val jsonContentBefore = testFile.readText()
            println("DEBUG: JSON содержимое ДО обновления: $jsonContentBefore")
        }
        
        // Проверяем содержимое JSON файла после обновления
        if (testFile.exists()) {
            val jsonContentAfter = testFile.readText()
            println("DEBUG: JSON содержимое ПОСЛЕ обновления: $jsonContentAfter")
        }
        
        // Проверяем, что лекарство действительно найдено в списке
        val medicinesAfterUpdate = dataManager.loadMedicines()
        val foundMedicine = medicinesAfterUpdate.find { it.id == updatedMedicine.id }
        println("DEBUG: Найдено лекарство после обновления: ${foundMedicine != null}")
        if (foundMedicine != null) {
            println("DEBUG: Время найденного лекарства: ${foundMedicine.time}")
        }
        
        // Проверяем что время обновилось
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterUpdate.size)
        
        // Логируем для отладки
        println("DEBUG: Время после обновления: ${medicinesAfterUpdate[0].time}")
        println("DEBUG: Ожидаемое время: ${LocalTime.of(16, 0)}")
        println("DEBUG: ID лекарства: ${medicinesAfterUpdate[0].id}")
        println("DEBUG: ID обновленного лекарства: ${updatedMedicine.id}")
        
        assertEquals("Время должно быть обновлено на 16:00", LocalTime.of(16, 0), medicinesAfterUpdate[0].time)
        
        println("✓ Тест пройден: время лекарства успешно обновлено")
    }
} 