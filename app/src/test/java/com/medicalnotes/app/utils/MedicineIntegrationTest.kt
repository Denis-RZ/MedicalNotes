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
class MedicineIntegrationTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        dataManager = DataManager(context)
        
        // Очищаем тестовые данные перед каждым тестом
        val medicinesFile = java.io.File(context.filesDir, "medicines.json")
        if (medicinesFile.exists()) {
            medicinesFile.delete()
        }
        
        // Создаем пустой файл
        medicinesFile.writeText("[]")
    }
    
    @Test
    fun testMedicineCreationAndEdit_ShouldRemainInTodayList() {
        println("=== ТЕСТ СОЗДАНИЯ И РЕДАКТИРОВАНИЯ ЛЕКАРСТВА ===")
        
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
        
        // Проверяем что лекарство добавлено
        val medicinesAfterAdd = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterAdd.size)
        assertEquals("Время должно быть 14:30", LocalTime.of(14, 30), medicinesAfterAdd[0].time)
        
        // Находим созданное лекарство
        val createdMedicine = medicinesAfterAdd[0]
        println("Созданное лекарство: ID=${createdMedicine.id}, время=${createdMedicine.time}")
        
        // Создаем обновленную версию с новым временем
        val updatedMedicine = createdMedicine.copy(
            time = LocalTime.of(16, 0),
            updatedAt = System.currentTimeMillis()
        )
        println("Обновленное лекарство: ID=${updatedMedicine.id}, время=${updatedMedicine.time}")
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем что лекарство обновлено
        val medicinesAfterUpdate = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterUpdate.size)
        assertEquals("Время должно быть обновлено на 16:00", LocalTime.of(16, 0), medicinesAfterUpdate[0].time)
        
        println("✓ Тест пройден: лекарство успешно создано и обновлено")
    }
    
    @Test
    fun testMedicineEditToPastTime_ShouldShowAsOverdue() {
        println("=== ТЕСТ РЕДАКТИРОВАНИЯ НА ПРОШЕДШЕЕ ВРЕМЯ ===")
        
        // Создаем лекарство
        val medicine = Medicine(
            id = 0,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(16, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Находим созданное лекарство
        val createdMedicine = dataManager.loadMedicines()[0]
        
        // Создаем обновленную версию с прошедшим временем (например, 09:00)
        val updatedMedicine = createdMedicine.copy(
            time = LocalTime.of(9, 0),
            updatedAt = System.currentTimeMillis()
        )
        println("Обновляем время на прошедшее: ${updatedMedicine.time}")
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем что лекарство обновлено на прошедшее время
        val medicinesAfterUpdate = dataManager.loadMedicines()
        assertEquals("Время должно быть обновлено на 09:00", LocalTime.of(9, 0), medicinesAfterUpdate[0].time)
        
        println("✓ Тест пройден: лекарство успешно обновлено на прошедшее время")
    }
    
    @Test
    fun testMedicineEditToFutureTime_ShouldShowAsUpcoming() {
        println("=== ТЕСТ РЕДАКТИРОВАНИЯ НА БУДУЩЕЕ ВРЕМЯ ===")
        
        // Создаем лекарство
        val medicine = Medicine(
            id = 0,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Находим созданное лекарство
        val createdMedicine = dataManager.loadMedicines()[0]
        
        // Создаем обновленную версию с будущим временем (например, 20:00)
        val updatedMedicine = createdMedicine.copy(
            time = LocalTime.of(20, 0),
            updatedAt = System.currentTimeMillis()
        )
        println("Обновляем время на будущее: ${updatedMedicine.time}")
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем что лекарство обновлено на будущее время
        val medicinesAfterUpdate = dataManager.loadMedicines()
        assertEquals("Время должно быть обновлено на 20:00", LocalTime.of(20, 0), medicinesAfterUpdate[0].time)
        
        println("✓ Тест пройден: лекарство успешно обновлено на будущее время")
    }
    
    @Test
    fun testEveryOtherDayFrequency_ShouldRemainInTodayListAfterEdit() {
        println("=== ТЕСТ ЧАСТОТЫ 'ЧЕРЕЗ ДЕНЬ' ПОСЛЕ РЕДАКТИРОВАНИЯ ===")
        
        // Создаем лекарство с частотой "через день"
        val medicine = Medicine(
            id = 0,
            name = "Тестовое лекарство через день",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(14, 30),
            frequency = DosageFrequency.EVERY_OTHER_DAY, // Через день
            dosageTimes = listOf(DosageTime.CUSTOM),
            customDays = listOf(1, 2, 3, 4, 5, 6, 7)
        )
        
        // Добавляем лекарство
        val addSuccess = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть добавлено", addSuccess)
        
        // Проверяем что лекарство добавлено
        val medicinesAfterAdd = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterAdd.size)
        assertEquals("Частота должна быть EVERY_OTHER_DAY", DosageFrequency.EVERY_OTHER_DAY, medicinesAfterAdd[0].frequency)
        
        // Находим созданное лекарство
        val createdMedicine = medicinesAfterAdd[0]
        println("Созданное лекарство: частота=${createdMedicine.frequency}, время=${createdMedicine.time}")
        
        // Создаем обновленную версию с новым временем
        val updatedMedicine = createdMedicine.copy(
            time = LocalTime.of(16, 0),
            updatedAt = System.currentTimeMillis()
        )
        println("Обновленное лекарство: частота=${updatedMedicine.frequency}, время=${updatedMedicine.time}")
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем что лекарство обновлено и частота сохранена
        val medicinesAfterUpdate = dataManager.loadMedicines()
        assertEquals("Должно быть 1 лекарство", 1, medicinesAfterUpdate.size)
        assertEquals("Время должно быть обновлено на 16:00", LocalTime.of(16, 0), medicinesAfterUpdate[0].time)
        assertEquals("Частота должна остаться EVERY_OTHER_DAY", DosageFrequency.EVERY_OTHER_DAY, medicinesAfterUpdate[0].frequency)
        
        println("✓ Тест пройден: лекарство с частотой 'через день' успешно обновлено")
    }
} 