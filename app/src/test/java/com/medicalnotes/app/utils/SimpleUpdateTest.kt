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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class SimpleUpdateTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testSimpleUpdate() {
        // Создаем простое лекарство
        val medicine = Medicine(
            id = 0, // Будет сгенерирован автоматически
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(14, 30),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Загружаем лекарства
        val medicinesAfterAdd = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterAdd.size)
        
        val createdMedicine = medicinesAfterAdd[0]
        println("Созданное лекарство: ID=${createdMedicine.id}, время=${createdMedicine.time}")
        
        // Создаем обновленную версию
        val updatedMedicine = createdMedicine.copy(
            time = LocalTime.of(16, 0),
            updatedAt = System.currentTimeMillis()
        )
        
        println("Обновленное лекарство: ID=${updatedMedicine.id}, время=${updatedMedicine.time}")
        
        // Обновляем лекарство
        println("=== ПЕРЕД ОБНОВЛЕНИЕМ ===")
        val medicinesBeforeUpdate = dataManager.loadMedicines()
        println("Лекарства перед обновлением: ${medicinesBeforeUpdate.map { "${it.name}(время:${it.time})" }}")
        
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        println("=== ПОСЛЕ ОБНОВЛЕНИЯ ===")
        val medicinesAfterUpdate = dataManager.loadMedicines()
        println("Лекарства после обновления: ${medicinesAfterUpdate.map { "${it.name}(время:${it.time})" }}")
        
        // Отладочная информация
        println("=== ОТЛАДКА ===")
        val debugJson = dataManager.debugSaveMedicines(listOf(updatedMedicine))
        println("Debug JSON: $debugJson")
        
        // Проверяем результат
        val finalMedicines = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, finalMedicines.size)
        
        val finalMedicine = finalMedicines[0]
        println("Финальное лекарство: ID=${finalMedicine.id}, время=${finalMedicine.time}")
        
        assertEquals("Время должно быть обновлено", LocalTime.of(16, 0), finalMedicine.time)
    }
} 