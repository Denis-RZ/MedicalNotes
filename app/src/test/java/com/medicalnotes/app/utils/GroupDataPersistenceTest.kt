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
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class GroupDataPersistenceTest {
    
    private lateinit var dataManager: DataManager
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataManager = DataManager(context)
    }
    
    @Test
    fun testGroupDataPersistence_EveryOtherDay() {
        // Создаем группу из двух лекарств "через день"
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тест"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 2,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:2".hashCode().toString()
        )
        
        // Создаем первое лекарство (порядок 1)
        val medicine1 = Medicine(
            id = 1L,
            name = "Лекарство 1",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство 1",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            takenToday = false,
            lastTakenTime = 0L,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Создаем второе лекарство (порядок 2)
        val medicine2 = Medicine(
            id = 2L,
            name = "Лекарство 2",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство 2",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            takenToday = false,
            lastTakenTime = 0L,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 2,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Сохраняем лекарства в XML
        val success1 = dataManager.addMedicine(medicine1)
        assertTrue("Первое лекарство должно быть сохранено", success1)
        
        val success2 = dataManager.addMedicine(medicine2)
        assertTrue("Второе лекарство должно быть сохранено", success2)
        
        // Проверяем, что данные сохранились в XML
        val savedMedicines = dataManager.loadMedicines()
        assertEquals("Должно быть 2 лекарства в XML", 2, savedMedicines.size)
        
        // Проверяем групповые данные первого лекарства
        val savedMedicine1 = savedMedicines.find { it.id == 1L }
        assertNotNull("Первое лекарство должно быть найдено", savedMedicine1)
        assertEquals("Group ID должен совпадать", groupId, savedMedicine1!!.groupId)
        assertEquals("Group Name должен совпадать", groupName, savedMedicine1.groupName)
        assertEquals("Group Order должен быть 1", 1, savedMedicine1.groupOrder)
        assertEquals("Group Start Date должен совпадать", groupStartDate, savedMedicine1.groupStartDate)
        assertEquals("Group Frequency должен совпадать", groupFrequency, savedMedicine1.groupFrequency)
        assertTrue("Первое лекарство должно быть валидной группой", savedMedicine1.isValidGroup())
        
        // Проверяем групповые данные второго лекарства
        val savedMedicine2 = savedMedicines.find { it.id == 2L }
        assertNotNull("Второе лекарство должно быть найдено", savedMedicine2)
        assertEquals("Group ID должен совпадать", groupId, savedMedicine2!!.groupId)
        assertEquals("Group Name должен совпадать", groupName, savedMedicine2.groupName)
        assertEquals("Group Order должен быть 2", 2, savedMedicine2.groupOrder)
        assertEquals("Group Start Date должен совпадать", groupStartDate, savedMedicine2.groupStartDate)
        assertEquals("Group Frequency должен совпадать", groupFrequency, savedMedicine2.groupFrequency)
        assertTrue("Второе лекарство должно быть валидной группой", savedMedicine2.isValidGroup())
        
        // Проверяем консистентность группы
        assertTrue("Группа должна быть консистентной", savedMedicine1.isGroupConsistent(savedMedicines))
        assertTrue("Группа должна быть консистентной", savedMedicine2.isGroupConsistent(savedMedicines))
        
        // Тестируем логику "через день" для группы
        val today = LocalDate.now()
        
        // Проверяем, что только одно лекарство должно приниматься сегодня
        val shouldTake1 = DosageCalculator.shouldTakeMedicine(savedMedicine1!!, today)
        val shouldTake2 = DosageCalculator.shouldTakeMedicine(savedMedicine2!!, today)
        
        // В группе "через день" только одно лекарство должно приниматься в день
        assertTrue("Только одно лекарство должно приниматься сегодня", shouldTake1 != shouldTake2)
        
        // Проверяем логику DosageCalculator для группы
        val activeMedicines = DosageCalculator.getActiveMedicinesForDate(savedMedicines, today)
        val groupMedicines = activeMedicines.filter { it.groupId == groupId }
        
        assertEquals("Только одно лекарство из группы должно быть активным сегодня", 1, groupMedicines.size)
        
        // Проверяем, что это правильное лекарство (с правильным порядком)
        val activeMedicine = groupMedicines.first()
        val expectedOrder = if (shouldTake1) 1 else 2
        assertEquals("Активное лекарство должно иметь правильный порядок", expectedOrder, activeMedicine.groupOrder)
        
        // Проверяем логику для завтрашнего дня
        val tomorrow = today.plusDays(1)
        val shouldTake1Tomorrow = DosageCalculator.shouldTakeMedicine(savedMedicine1!!, tomorrow)
        val shouldTake2Tomorrow = DosageCalculator.shouldTakeMedicine(savedMedicine2!!, tomorrow)
        
        // Завтра должно приниматься другое лекарство
        assertTrue("Завтра должно приниматься другое лекарство", shouldTake1Tomorrow != shouldTake1)
        assertTrue("Завтра должно приниматься другое лекарство", shouldTake2Tomorrow != shouldTake2)
        assertTrue("Только одно лекарство должно приниматься завтра", shouldTake1Tomorrow != shouldTake2Tomorrow)
        
        // Проверяем активные лекарства на завтра
        val activeMedicinesTomorrow = DosageCalculator.getActiveMedicinesForDate(savedMedicines, tomorrow)
        val groupMedicinesTomorrow = activeMedicinesTomorrow.filter { it.groupId == groupId }
        
        assertEquals("Только одно лекарство из группы должно быть активным завтра", 1, groupMedicinesTomorrow.size)
        
        val activeMedicineTomorrow = groupMedicinesTomorrow.first()
        val expectedOrderTomorrow = if (shouldTake1Tomorrow) 1 else 2
        assertEquals("Активное лекарство завтра должно иметь правильный порядок", expectedOrderTomorrow, activeMedicineTomorrow.groupOrder)
        
        // Проверяем, что это другое лекарство
        assertTrue("Активные лекарства в разные дни должны быть разными", activeMedicine.id != activeMedicineTomorrow.id)
    }
    
    @Test
    fun testGroupDataPersistence_EditMedicine() {
        // Создаем лекарство без группы
        val medicine = Medicine(
            id = 1L,
            name = "Лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis(),
            takenToday = false,
            lastTakenTime = 0L
        )
        
        // Сохраняем лекарство
        val success = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть сохранено", success)
        
        // Проверяем, что лекарство не в группе
        val savedMedicine = dataManager.loadMedicines().first()
        assertFalse("Лекарство не должно быть в группе", savedMedicine.isValidGroup())
        
        // Теперь добавляем лекарство в группу через редактирование
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Новая группа"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 1,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:1".hashCode().toString()
        )
        
        val updatedMedicine = savedMedicine.copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(updatedMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем, что групповые данные сохранились
        val reloadedMedicine = dataManager.loadMedicines().first()
        assertTrue("Лекарство должно быть в группе после редактирования", reloadedMedicine.isValidGroup())
        assertEquals("Group ID должен совпадать", groupId, reloadedMedicine.groupId)
        assertEquals("Group Name должен совпадать", groupName, reloadedMedicine.groupName)
        assertEquals("Group Order должен быть 1", 1, reloadedMedicine.groupOrder)
        assertEquals("Group Start Date должен совпадать", groupStartDate, reloadedMedicine.groupStartDate)
        assertEquals("Group Frequency должен совпадать", groupFrequency, reloadedMedicine.groupFrequency)
    }
    
    @Test
    fun testGroupDataPersistence_RemoveFromGroup() {
        // Создаем лекарство в группе
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupName = "Тестовая группа"
        val groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = 1,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:1".hashCode().toString()
        )
        
        val medicine = Medicine(
            id = 1L,
            name = "Лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = LocalTime.of(8, 0),
            notes = "Тестовое лекарство",
            isActive = true,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            takenToday = false,
            lastTakenTime = 0L,
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        
        // Сохраняем лекарство
        val success = dataManager.addMedicine(medicine)
        assertTrue("Лекарство должно быть сохранено", success)
        
        // Проверяем, что лекарство в группе
        val savedMedicine = dataManager.loadMedicines().first()
        assertTrue("Лекарство должно быть в группе", savedMedicine.isValidGroup())
        
        // Убираем лекарство из группы
        val removedFromGroupMedicine = savedMedicine.copy(
            groupId = null,
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0L,
            groupFrequency = DosageFrequency.DAILY,
            groupValidationHash = "",
            groupMetadata = null
        )
        
        // Обновляем лекарство
        val updateSuccess = dataManager.updateMedicine(removedFromGroupMedicine)
        assertTrue("Лекарство должно быть обновлено", updateSuccess)
        
        // Проверяем, что групповые данные удалены
        val reloadedMedicine = dataManager.loadMedicines().first()
        assertFalse("Лекарство не должно быть в группе после удаления", reloadedMedicine.isValidGroup())
        assertEquals("Group ID должен быть null", null, reloadedMedicine.groupId)
        assertEquals("Group Name должен быть пустым", "", reloadedMedicine.groupName)
        assertEquals("Group Order должен быть 0", 0, reloadedMedicine.groupOrder)
        assertEquals("Group Start Date должен быть 0", 0L, reloadedMedicine.groupStartDate)
    }
} 