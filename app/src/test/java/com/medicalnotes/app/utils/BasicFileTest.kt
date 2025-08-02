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

@RunWith(RobolectricTestRunner::class)
class BasicFileTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testBasicAddAndLoad() {
        println("=== ТЕСТ БАЗОВОГО ДОБАВЛЕНИЯ ===")
        
        // Проверяем начальное состояние
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
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Проверяем что лекарство добавлено
        val medicinesAfterAdd = dataManager.loadMedicines()
        println("Количество лекарств после добавления: ${medicinesAfterAdd.size}")
        println("Лекарства после добавления: ${medicinesAfterAdd.map { "${it.name}(время:${it.time})" }}")
        
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterAdd.size)
        assertEquals("Время должно быть 14:30", LocalTime.of(14, 30), medicinesAfterAdd[0].time)
        
        println("✓ Тест пройден: лекарство успешно добавлено и загружено")
    }
} 