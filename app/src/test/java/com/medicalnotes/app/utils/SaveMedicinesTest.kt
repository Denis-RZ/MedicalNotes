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
class SaveMedicinesTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testSaveMedicines() {
        println("=== ТЕСТ СОХРАНЕНИЯ ЛЕКАРСТВ ===")
        
        // Создаем лекарство с временем 16:00
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(16, 0), // 16:00
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        println("Создано лекарство: время=${medicine.time}")
        
        // Сохраняем лекарство
        val saveSuccess = dataManager.saveMedicines(listOf(medicine))
        println("Результат сохранения: $saveSuccess")
        assertTrue("Лекарство должно быть сохранено", saveSuccess)
        
        // Загружаем лекарство
        val loadedMedicines = dataManager.loadMedicines()
        println("Загружено лекарств: ${loadedMedicines.size}")
        
        if (loadedMedicines.isNotEmpty()) {
            println("Загруженное лекарство: время=${loadedMedicines[0].time}")
            assertEquals("Время должно быть 16:00", LocalTime.of(16, 0), loadedMedicines[0].time)
        }
        
        assertEquals("Должно быть 1 лекарство", 1, loadedMedicines.size)
        println("✓ Тест пройден: лекарство успешно сохранено и загружено")
    }
} 