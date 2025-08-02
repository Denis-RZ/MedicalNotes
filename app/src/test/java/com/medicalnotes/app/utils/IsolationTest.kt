package com.medicalnotes.app.utils

import android.content.Context
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalTime
import org.junit.Assert.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
class IsolationTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testAddMedicineIsolation() {
        println("=== ТЕСТ ИЗОЛЯЦИИ ДОБАВЛЕНИЯ ===")
        
        // Проверяем начальное состояние
        val medicinesFile = File(context.filesDir, "medicines.json")
        println("Файл лекарств существует: ${medicinesFile.exists()}")
        if (medicinesFile.exists()) {
            println("Начальное содержимое файла: ${medicinesFile.readText()}")
        }
        
        // Загружаем лекарства
        val initialMedicines = dataManager.loadMedicines()
        println("Начальное количество лекарств: ${initialMedicines.size}")
        
        // Создаем простое лекарство
        val medicine = Medicine(
            id = 0,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(14, 30),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        println("Создано лекарство: ${medicine.name}, время: ${medicine.time}")
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        println("Результат добавления: $addSuccess")
        
        // Проверяем файл после добавления
        println("Файл после добавления существует: ${medicinesFile.exists()}")
        if (medicinesFile.exists()) {
            println("Содержимое файла после добавления: ${medicinesFile.readText()}")
        }
        
        // Загружаем лекарства снова
        val medicinesAfterAdd = dataManager.loadMedicines()
        println("Количество лекарств после добавления: ${medicinesAfterAdd.size}")
        
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterAdd.size)
        
        println("✓ Тест пройден: лекарство успешно добавлено")
    }
} 