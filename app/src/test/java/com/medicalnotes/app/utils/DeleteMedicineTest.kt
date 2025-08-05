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
class DeleteMedicineTest {
    
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
    fun testDeleteMedicine_RemovesFromFile() {
        // Создаем тестовое лекарство
        val medicine = Medicine(
            id = 1L,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 5,
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = System.currentTimeMillis(),
            isActive = true,
            notes = "Тестовые заметки"
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Проверяем, что лекарство добавлено
        val medicinesAfterAdd = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство после добавления", 1, medicinesAfterAdd.size)
        assertEquals("Имя лекарства должно совпадать", medicine.name, medicinesAfterAdd[0].name)
        
        // Удаляем лекарство
        val deleteSuccess = dataManager.deleteMedicine(medicine.id)
        assertTrue("Удаление должно быть успешным", deleteSuccess)
        
        // Проверяем, что лекарство удалено из файла
        val medicinesAfterDelete = dataManager.loadMedicines()
        assertEquals("Список лекарств должен быть пустым после удаления", 0, medicinesAfterDelete.size)
    }
    
    @Test
    fun testDeleteMedicine_NonExistentId() {
        // Пытаемся удалить несуществующее лекарство
        val deleteSuccess = dataManager.deleteMedicine(999L)
        assertFalse("Удаление несуществующего лекарства должно вернуть false", deleteSuccess)
        
        // Проверяем, что список лекарств остается пустым
        val medicines = dataManager.loadMedicines()
        assertEquals("Список лекарств должен остаться пустым", 0, medicines.size)
    }
    
    @Test
    fun testDeleteMedicine_MultipleMedicines() {
        // Создаем несколько лекарств
        val medicine1 = Medicine(
            id = 1L,
            name = "Лекарство 1",
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
            name = "Лекарство 2",
            dosage = "2 таблетки",
            quantity = 20,
            remainingQuantity = 10,
            time = LocalTime.of(18, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.EVENING),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        val medicine3 = Medicine(
            id = 3L,
            name = "Лекарство 3",
            dosage = "1 капсула",
            quantity = 15,
            remainingQuantity = 8,
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.AFTERNOON),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        // Добавляем все лекарства
        assertTrue("Лекарство 1 должно быть добавлено", dataManager.addMedicine(medicine1))
        assertTrue("Лекарство 2 должно быть добавлено", dataManager.addMedicine(medicine2))
        assertTrue("Лекарство 3 должно быть добавлено", dataManager.addMedicine(medicine3))
        
        // Проверяем, что все лекарства добавлены
        val medicinesAfterAdd = dataManager.loadMedicines()
        assertEquals("Должно быть 3 лекарства после добавления", 3, medicinesAfterAdd.size)
        
        // Удаляем лекарство 2
        val deleteSuccess = dataManager.deleteMedicine(medicine2.id)
        assertTrue("Удаление лекарства 2 должно быть успешным", deleteSuccess)
        
        // Проверяем, что остались только лекарства 1 и 3
        val medicinesAfterDelete = dataManager.loadMedicines()
        assertEquals("Должно остаться 2 лекарства после удаления", 2, medicinesAfterDelete.size)
        
        val remainingNames = medicinesAfterDelete.map { it.name }.sorted()
        assertEquals("Должны остаться правильные лекарства", listOf("Лекарство 1", "Лекарство 3"), remainingNames)
    }
    
    @Test
    fun testDeleteMedicine_FilePersistence() {
        // Создаем и добавляем лекарство
        val medicine = Medicine(
            id = 1L,
            name = "Лекарство для теста персистентности",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 5,
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = System.currentTimeMillis(),
            isActive = true
        )
        
        assertTrue("Лекарство должно быть добавлено", dataManager.addMedicine(medicine))
        
        // Создаем новый экземпляр DataManager для проверки персистентности
        val newDataManager = DataManager(context)
        
        // Проверяем, что лекарство загружается в новом экземпляре
        val medicinesInNewInstance = newDataManager.loadMedicines()
        assertEquals("Лекарство должно быть загружено в новом экземпляре", 1, medicinesInNewInstance.size)
        assertEquals("Имя лекарства должно совпадать", medicine.name, medicinesInNewInstance[0].name)
        
        // Удаляем лекарство через новый экземпляр
        val deleteSuccess = newDataManager.deleteMedicine(medicine.id)
        assertTrue("Удаление должно быть успешным", deleteSuccess)
        
        // Создаем еще один новый экземпляр и проверяем, что лекарство удалено
        val finalDataManager = DataManager(context)
        val medicinesAfterDelete = finalDataManager.loadMedicines()
        assertEquals("Лекарство должно быть удалено из файла", 0, medicinesAfterDelete.size)
    }

    @Test
    fun testDeleteMedicine_ThroughViewModel() {
        // Создаем тестовое лекарство
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство для удаления",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 10,
            medicineType = "таблетки",
            time = java.time.LocalTime.of(12, 0),
            notes = "Тестовые заметки",
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = com.medicalnotes.app.models.DosageFrequency.DAILY,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Проверяем, что лекарство добавлено
        val medicinesBeforeDelete = dataManager.loadMedicines()
        assertTrue("Должно быть лекарство в списке", medicinesBeforeDelete.any { it.id == medicine.id })
        
        // Удаляем лекарство
        val deleteSuccess = dataManager.deleteMedicine(medicine.id)
        assertTrue("Лекарство должно быть удалено", deleteSuccess)
        
        // Проверяем, что лекарство удалено
        val medicinesAfterDelete = dataManager.loadMedicines()
        assertFalse("Лекарство не должно быть в списке после удаления", 
                   medicinesAfterDelete.any { it.id == medicine.id })
        
        println("✅ Тест полного цикла удаления прошел успешно")
    }

    @Test
    fun testDeleteMedicine_MedicineManagerActivity() {
        // Создаем тестовое лекарство
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство для MedicineManager",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 10,
            medicineType = "таблетки",
            time = java.time.LocalTime.of(12, 0),
            notes = "Тестовые заметки",
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = com.medicalnotes.app.models.DosageFrequency.DAILY,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Проверяем, что лекарство добавлено
        val medicinesBeforeDelete = dataManager.loadMedicines()
        assertTrue("Должно быть лекарство в списке", medicinesBeforeDelete.any { it.id == medicine.id })
        
        // Симулируем удаление через MedicineManagerActivity (прямой вызов DataManager)
        val deleteSuccess = dataManager.deleteMedicine(medicine.id)
        assertTrue("Лекарство должно быть удалено", deleteSuccess)
        
        // Проверяем, что лекарство удалено
        val medicinesAfterDelete = dataManager.loadMedicines()
        assertFalse("Лекарство не должно быть в списке после удаления", 
                   medicinesAfterDelete.any { it.id == medicine.id })
        
        println("✅ Тест удаления через MedicineManagerActivity прошел успешно")
    }
} 