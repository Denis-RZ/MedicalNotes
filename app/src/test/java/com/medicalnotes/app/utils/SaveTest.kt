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
class SaveTest {
    
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
    fun testSaveMedicines_BasicFunctionality() {
        // Создаем простое лекарство
        val medicine = Medicine(
            id = 1L,
            name = "Аспирин",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 5,
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        // Сохраняем лекарство
        val saveSuccess = dataManager.saveMedicines(listOf(medicine))
        assertTrue("Сохранение должно быть успешным", saveSuccess)
        
        // Проверяем, что файл создан
        assertTrue("Файл должен существовать", testFile.exists())
        
        // Проверяем содержимое файла
        val fileContent = testFile.readText()
        assertTrue("Файл не должен быть пустым", fileContent.isNotEmpty())
        assertTrue("Файл должен содержать имя лекарства", fileContent.contains("Аспирин"))
        
        // Загружаем лекарства
        val medicines = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicines.size)
        assertEquals("Имя лекарства должно совпадать", "Аспирин", medicines[0].name)
    }
    
    @Test
    fun testSaveMedicines_EmptyList() {
        // Сохраняем пустой список
        val saveSuccess = dataManager.saveMedicines(emptyList())
        assertTrue("Сохранение пустого списка должно быть успешным", saveSuccess)
        
        // Проверяем, что файл создан
        assertTrue("Файл должен существовать", testFile.exists())
        
        // Проверяем содержимое файла
        val fileContent = testFile.readText()
        assertEquals("Файл должен содержать пустой массив", "[]", fileContent.trim())
        
        // Загружаем лекарства
        val medicines = dataManager.loadMedicines()
        assertEquals("Список должен быть пустым", 0, medicines.size)
    }
    
    @Test
    fun testSaveMedicines_MultipleMedicines() {
        // Создаем несколько лекарств
        val medicine1 = Medicine(
            id = 1L,
            name = "Аспирин",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 5,
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        val medicine2 = Medicine(
            id = 2L,
            name = "Парацетамол",
            dosage = "2 таблетки",
            quantity = 20,
            remainingQuantity = 10,
            time = LocalTime.of(18, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.EVENING),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        // Сохраняем лекарства
        val saveSuccess = dataManager.saveMedicines(listOf(medicine1, medicine2))
        assertTrue("Сохранение должно быть успешным", saveSuccess)
        
        // Загружаем лекарства
        val medicines = dataManager.loadMedicines()
        assertEquals("Должно быть 2 лекарства", 2, medicines.size)
        
        val names = medicines.map { it.name }.sorted()
        assertEquals("Должны быть правильные имена", listOf("Аспирин", "Парацетамол"), names)
    }
} 