package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant

class RealDataTest {

    @Test
    fun testLipitorAndFubuxusat() {
        println("=== ТЕСТ РЕАЛЬНЫХ ДАННЫХ ЛИПЕТОР И ФУБУКСУСАТ ===")
        
        // Создаем лекарства на основе логов пользователя
        val lipitor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(21, 52), // Время из логов: 21:52
            frequency = DosageFrequency.DAILY, // Предполагаем ежедневный прием
            startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli(), // 7 дней назад
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = null, // Не в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0L,
            groupFrequency = DosageFrequency.DAILY
        )
        
        val fubuxusat = Medicine(
            id = 2L,
            name = "Фубуксусат",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(19, 15), // Время из логов: 19:15
            frequency = DosageFrequency.DAILY, // Предполагаем ежедневный прием
            startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli(), // 7 дней назад
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = null, // Не в группе
            groupName = "",
            groupOrder = 0,
            groupStartDate = 0L,
            groupFrequency = DosageFrequency.DAILY
        )
        
        val today = LocalDate.now()
        
        println("Сегодняшняя дата: $today")
        println()
        
        // Тестируем Липетор
        println("=== ТЕСТ ЛИПЕТОР ===")
        val lipitorShouldTake = MedicineStatusHelper.shouldTakeToday(lipitor)
        val lipitorStatus = MedicineStatusHelper.getMedicineStatus(lipitor)
        
        println("Липетор должен принимать сегодня: $lipitorShouldTake")
        println("Статус Липетора: $lipitorStatus")
        println("Время приема: ${lipitor.time}")
        println("Частота: ${lipitor.frequency}")
        println("Активно: ${lipitor.isActive}")
        println("Дата начала: ${lipitor.startDate}")
        println()
        
        // Тестируем Фубуксусат
        println("=== ТЕСТ ФУБУКСУСАТ ===")
        val fubuxusatShouldTake = MedicineStatusHelper.shouldTakeToday(fubuxusat)
        val fubuxusatStatus = MedicineStatusHelper.getMedicineStatus(fubuxusat)
        
        println("Фубуксусат должен принимать сегодня: $fubuxusatShouldTake")
        println("Статус Фубуксусата: $fubuxusatStatus")
        println("Время приема: ${fubuxusat.time}")
        println("Частота: ${fubuxusat.frequency}")
        println("Активно: ${fubuxusat.isActive}")
        println("Дата начала: ${fubuxusat.startDate}")
        println()
        
        // Проверяем ожидаемое поведение
        assertTrue("Липетор должен принимать сегодня (ежедневно)", lipitorShouldTake)
        assertTrue("Фубуксусат должен принимать сегодня (ежедневно)", fubuxusatShouldTake)
        
        // Статус должен быть UPCOMING или OVERDUE, но не NOT_TODAY
        assertNotEquals("Липетор не должен иметь статус NOT_TODAY", MedicineStatus.NOT_TODAY, lipitorStatus)
        assertNotEquals("Фубуксусат не должен иметь статус NOT_TODAY", MedicineStatus.NOT_TODAY, fubuxusatStatus)
        
        println("✅ ТЕСТ ПРОЙДЕН: Лекарства должны принимать сегодня")
    }
    
    @Test
    fun testDifferentFrequencies() {
        println("=== ТЕСТ РАЗНЫХ ЧАСТОТ ПРИЕМА ===")
        
        val today = LocalDate.now()
        val startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli() // 7 дней назад
        
        // Тест 1: Ежедневный прием
        val dailyMedicine = Medicine(
            id = 1L,
            name = "Ежедневное лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true
        )
        
        val dailyShouldTake = MedicineStatusHelper.shouldTakeToday(dailyMedicine)
        println("Ежедневное лекарство должно принимать: $dailyShouldTake")
        assertTrue("Ежедневное лекарство должно принимать сегодня", dailyShouldTake)
        
        // Тест 2: Через день
        val everyOtherDayMedicine = Medicine(
            id = 2L,
            name = "Через день лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true
        )
        
        val everyOtherDayShouldTake = MedicineStatusHelper.shouldTakeToday(everyOtherDayMedicine)
        println("Через день лекарство должно принимать: $everyOtherDayShouldTake")
        // Может быть true или false в зависимости от дня
        
        // Тест 3: Неактивное лекарство
        val inactiveMedicine = Medicine(
            id = 3L,
            name = "Неактивное лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = startDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = false // Неактивно
        )
        
        val inactiveShouldTake = MedicineStatusHelper.shouldTakeToday(inactiveMedicine)
        val inactiveStatus = MedicineStatusHelper.getMedicineStatus(inactiveMedicine)
        println("Неактивное лекарство должно принимать: $inactiveShouldTake")
        println("Статус неактивного лекарства: $inactiveStatus")
        
        // Неактивное лекарство не должно принимать
        assertFalse("Неактивное лекарство не должно принимать", inactiveShouldTake)
        assertEquals("Неактивное лекарство должно иметь статус NOT_TODAY", MedicineStatus.NOT_TODAY, inactiveStatus)
        
        println("✅ ТЕСТ РАЗНЫХ ЧАСТОТ ПРОЙДЕН")
    }
    
    @Test
    fun testGroupedMedicines() {
        println("=== ТЕСТ ГРУППИРОВАННЫХ ЛЕКАРСТВ ===")
        
        val today = LocalDate.now()
        val groupId = 1L
        val groupStartDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli() // 7 дней назад
        
        // Лекарство в группе "через день"
        val groupedMedicine = Medicine(
            id = 1L,
            name = "Группированное лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = groupStartDate,
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true,
            groupId = groupId,
            groupName = "Группа через день",
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = DosageFrequency.EVERY_OTHER_DAY
        )
        
        val groupedShouldTake = MedicineStatusHelper.shouldTakeToday(groupedMedicine)
        val groupedStatus = MedicineStatusHelper.getMedicineStatus(groupedMedicine)
        
        println("Группированное лекарство должно принимать: $groupedShouldTake")
        println("Статус группированного лекарства: $groupedStatus")
        println("Группа: ${groupedMedicine.groupName}")
        println("Порядок в группе: ${groupedMedicine.groupOrder}")
        println("Частота группы: ${groupedMedicine.groupFrequency}")
        
        // Проверяем, что логика группы работает
        if (groupedShouldTake) {
            assertNotEquals("Если должно принимать, то не должно быть NOT_TODAY", MedicineStatus.NOT_TODAY, groupedStatus)
        }
        
        println("✅ ТЕСТ ГРУППИРОВАННЫХ ЛЕКАРСТВ ПРОЙДЕН")
    }
    
    @Test
    fun testFutureStartDate() {
        println("=== ТЕСТ БУДУЩЕЙ ДАТЫ НАЧАЛА ===")
        
        val today = LocalDate.now()
        val futureStartDate = Instant.now().plusSeconds(86400 * 7).toEpochMilli() // 7 дней в будущем
        
        val futureMedicine = Medicine(
            id = 1L,
            name = "Будущее лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            time = LocalTime.of(8, 0),
            frequency = DosageFrequency.DAILY,
            startDate = futureStartDate, // Будущая дата
            lastTakenTime = 0L,
            takenToday = false,
            isActive = true
        )
        
        val futureShouldTake = MedicineStatusHelper.shouldTakeToday(futureMedicine)
        val futureStatus = MedicineStatusHelper.getMedicineStatus(futureMedicine)
        
        println("Лекарство с будущей датой начала должно принимать: $futureShouldTake")
        println("Статус лекарства с будущей датой: $futureStatus")
        
        // Лекарство с будущей датой начала не должно принимать сегодня
        assertFalse("Лекарство с будущей датой не должно принимать сегодня", futureShouldTake)
        assertEquals("Лекарство с будущей датой должно иметь статус NOT_TODAY", MedicineStatus.NOT_TODAY, futureStatus)
        
        println("✅ ТЕСТ БУДУЩЕЙ ДАТЫ НАЧАЛА ПРОЙДЕН")
    }
} 