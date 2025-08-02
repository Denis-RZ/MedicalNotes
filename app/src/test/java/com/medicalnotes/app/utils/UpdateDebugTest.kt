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
class UpdateDebugTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testUpdateMedicine() {
        println("=== ТЕСТ ОБНОВЛЕНИЯ ЛЕКАРСТВА ===")
        
        // Создаем лекарство
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
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Загружаем созданное лекарство
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
        
        // Проверяем файл перед обновлением
        val medicinesFile = File(context.filesDir, "medicines.json")
        println("Файл перед обновлением: ${medicinesFile.readText()}")
        
        // Обновляем лекарство
        println("=== ОТЛАДКА ОБНОВЛЕНИЯ ===")
        println("ID лекарства для обновления: ${updatedMedicine.id}")
        println("Время лекарства для обновления: ${updatedMedicine.time}")
        
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        println("Результат обновления: $updateSuccess")
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем файл после обновления
        println("Файл после обновления: ${medicinesFile.readText()}")
        
        // Загружаем лекарства снова
        val medicinesAfterUpdate = dataManager.loadMedicines()
        println("Загружено лекарств после обновления: ${medicinesAfterUpdate.size}")
        if (medicinesAfterUpdate.isNotEmpty()) {
            println("Лекарство после обновления: ID=${medicinesAfterUpdate[0].id}, время=${medicinesAfterUpdate[0].time}")
        }
        
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterUpdate.size)
        assertEquals("Время должно быть обновлено", LocalTime.of(16, 0), medicinesAfterUpdate[0].time)
        
        // Дополнительный тест: проверяем saveMedicines напрямую
        println("=== ТЕСТ ПРЯМОГО СОХРАНЕНИЯ ===")
        val directSaveSuccess = dataManager.saveMedicines(listOf(updatedMedicine))
        println("Результат прямого сохранения: $directSaveSuccess")
        
        val medicinesAfterDirectSave = dataManager.loadMedicines()
        println("Лекарства после прямого сохранения: ${medicinesAfterDirectSave.map { "${it.name}(время:${it.time})" }}")
        
        assertEquals("Время должно быть обновлено после прямого сохранения", LocalTime.of(16, 0), medicinesAfterDirectSave[0].time)
    }
} 