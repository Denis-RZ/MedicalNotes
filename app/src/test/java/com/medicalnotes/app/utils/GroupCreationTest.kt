package com.medicalnotes.app.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.GroupMetadata
import com.medicalnotes.app.models.Medicine
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class GroupCreationTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testCreateMedicineGroup_ValidGroupData() {
        // Создаем два лекарства для группы
        val medicine1 = createTestMedicine("Лекарство 1", DosageFrequency.EVERY_OTHER_DAY)
        val medicine2 = createTestMedicine("Лекарство 2", DosageFrequency.EVERY_OTHER_DAY)
        
        // Сохраняем первое лекарство
        val success1 = dataManager.addMedicine(medicine1)
        assertTrue("Первое лекарство должно быть сохранено", success1)
        
        // Создаем группу
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тестовая группа"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 2,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
        )
        
        // Обновляем первое лекарство с групповыми данными
        val updatedMedicine1 = medicine1.copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        val updateSuccess1 = dataManager.updateMedicine(updatedMedicine1)
        assertTrue("Первое лекарство должно быть обновлено", updateSuccess1)
        
        // Обновляем второе лекарство с групповыми данными
        val updatedMedicine2 = medicine2.copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        val updateSuccess2 = dataManager.updateMedicine(updatedMedicine2)
        assertTrue("Второе лекарство должно быть обновлено", updateSuccess2)
        
        // Проверяем, что группа создана правильно
        val allMedicines = dataManager.loadMedicines()
        val groupMedicines = allMedicines.filter { it.groupId == groupId }
        
        assertEquals("В группе должно быть 2 лекарства", 2, groupMedicines.size)
        
        // Проверяем первое лекарство
        val savedMedicine1 = groupMedicines.find { it.groupOrder == 1 }
        assertNotNull("Лекарство с порядком 1 должно существовать", savedMedicine1)
        assertEquals(groupId, savedMedicine1!!.groupId)
        assertEquals(groupName, savedMedicine1.groupName)
        assertEquals(1, savedMedicine1.groupOrder)
        assertEquals(groupStartDate, savedMedicine1.groupStartDate)
        assertEquals(groupFrequency, savedMedicine1.groupFrequency)
        assertTrue("Первое лекарство должно быть валидной группой", savedMedicine1.isValidGroup())
        
        // Проверяем второе лекарство
        val savedMedicine2 = groupMedicines.find { it.groupOrder == 2 }
        assertNotNull("Лекарство с порядком 2 должно существовать", savedMedicine2)
        assertEquals(groupId, savedMedicine2!!.groupId)
        assertEquals(groupName, savedMedicine2.groupName)
        assertEquals(2, savedMedicine2.groupOrder)
        assertEquals(groupStartDate, savedMedicine2.groupStartDate)
        assertEquals(groupFrequency, savedMedicine2.groupFrequency)
        assertTrue("Второе лекарство должно быть валидной группой", savedMedicine2.isValidGroup())
        
        // Проверяем консистентность группы
        assertTrue("Группа должна быть консистентной", savedMedicine1.isGroupConsistent(groupMedicines))
        assertTrue("Группа должна быть консистентной", savedMedicine2.isGroupConsistent(groupMedicines))
    }
    
    @Test
    fun testCreateMedicineGroup_InvalidGroupData() {
        // Создаем лекарство с неполными групповыми данными
        val medicine = createTestMedicine("Лекарство", DosageFrequency.EVERY_OTHER_DAY)
        
        // Сохраняем лекарство
        val success = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть сохранено", success)
        
        // Проверяем, что лекарство не в группе
        assertFalse("Лекарство не должно быть валидной группой", medicine.isValidGroup())
        assertEquals(null, medicine.groupId)
        assertEquals("", medicine.groupName)
        assertEquals(0, medicine.groupOrder)
        assertEquals(0L, medicine.groupStartDate)
    }
    
    @Test
    fun testGroupValidation_ConsistentGroup() {
        // Создаем группу с правильными данными
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тестовая группа"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 2,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
        )
        
        val medicine1 = createTestMedicine("Лекарство 1", DosageFrequency.EVERY_OTHER_DAY).copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        val medicine2 = createTestMedicine("Лекарство 2", DosageFrequency.EVERY_OTHER_DAY).copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        val allMedicines = listOf(medicine1, medicine2)
        
        // Проверяем валидацию
        assertTrue("Первое лекарство должно быть валидной группой", medicine1.isValidGroup())
        assertTrue("Второе лекарство должно быть валидной группой", medicine2.isValidGroup())
        assertTrue("Группа должна быть консистентной", medicine1.isGroupConsistent(allMedicines))
        assertTrue("Группа должна быть консистентной", medicine2.isGroupConsistent(allMedicines))
    }
    
    @Test
    fun testGroupValidation_InconsistentGroup() {
        // Создаем группу с неправильными данными
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тестовая группа"
        
        val medicine1 = createTestMedicine("Лекарство 1", DosageFrequency.EVERY_OTHER_DAY).copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        )
        
        val medicine2 = createTestMedicine("Лекарство 2", DosageFrequency.DAILY).copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2,
            groupStartDate = groupStartDate,
            groupFrequency = DosageFrequency.DAILY // Разная частота!
        )
        
        val allMedicines = listOf(medicine1, medicine2)
        
        // Проверяем, что группа не консистентна
        assertFalse("Группа не должна быть консистентной из-за разной частоты", medicine1.isGroupConsistent(allMedicines))
        assertFalse("Группа не должна быть консистентной из-за разной частоты", medicine2.isGroupConsistent(allMedicines))
    }
    
    private fun createTestMedicine(name: String, frequency: DosageFrequency): Medicine {
        return Medicine(
            id = 0L,
            name = name,
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство",
            isActive = true,
            frequency = frequency,
            startDate = System.currentTimeMillis()
        )
    }
} 