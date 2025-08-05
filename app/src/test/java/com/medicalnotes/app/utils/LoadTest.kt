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
class LoadTest {
    
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
    fun testLoadMedicines_EmptyFile() {
        // Проверяем загрузку из пустого файла
        val medicines = dataManager.loadMedicines()
        assertEquals("Список должен быть пустым", 0, medicines.size)
    }
    
    @Test
    fun testLoadMedicines_NonExistentFile() {
        // Удаляем файл если он существует
        if (testFile.exists()) {
            testFile.delete()
        }
        
        // Проверяем загрузку из несуществующего файла
        val medicines = dataManager.loadMedicines()
        assertEquals("Список должен быть пустым", 0, medicines.size)
    }
    
    @Test
    fun testLoadMedicines_EmptyArray() {
        // Создаем файл с пустым массивом
        testFile.writeText("[]")
        
        // Проверяем загрузку
        val medicines = dataManager.loadMedicines()
        assertEquals("Список должен быть пустым", 0, medicines.size)
    }
    
    @Test
    fun testLoadMedicines_InvalidJson() {
        // Создаем файл с неверным JSON
        testFile.writeText("invalid json")
        
        // Проверяем загрузку
        val medicines = dataManager.loadMedicines()
        assertEquals("Список должен быть пустым при неверном JSON", 0, medicines.size)
    }
    
    @Test
    fun testLoadMedicines_ValidJson() {
        // Создаем файл с валидным JSON
        val json = """[
            {
                "id": 1,
                "name": "Аспирин",
                "dosage": "1 таблетка",
                "quantity": 10,
                "remainingQuantity": 5,
                "time": "09:00",
                "frequency": "DAILY",
                "dosageTimes": ["MORNING"],
                "startDate": 1234567890,
                "isActive": true
            }
        ]"""
        testFile.writeText(json)
        
        // Проверяем загрузку
        val medicines = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicines.size)
        assertEquals("Имя лекарства должно совпадать", "Аспирин", medicines[0].name)
    }
} 