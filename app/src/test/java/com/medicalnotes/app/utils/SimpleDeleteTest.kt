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
class SimpleDeleteTest {
    
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
    fun testDeleteMedicine_BasicFunctionality() {
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
        
        // Сохраняем лекарство напрямую
        val saveSuccess = dataManager.saveMedicines(listOf(medicine))
        assertTrue("Сохранение должно быть успешным", saveSuccess)
        
        // Проверяем, что лекарство сохранено
        val medicinesAfterSave = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство после сохранения", 1, medicinesAfterSave.size)
        assertEquals("Имя лекарства должно совпадать", "Аспирин", medicinesAfterSave[0].name)
        
        // Удаляем лекарство
        val deleteSuccess = dataManager.deleteMedicine(medicine.id)
        assertTrue("Удаление должно быть успешным", deleteSuccess)
        
        // Проверяем, что лекарство удалено
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
    fun testDeleteMedicine_EmptyList() {
        // Проверяем удаление из пустого списка
        val deleteSuccess = dataManager.deleteMedicine(1L)
        assertFalse("Удаление из пустого списка должно вернуть false", deleteSuccess)
    }
} 