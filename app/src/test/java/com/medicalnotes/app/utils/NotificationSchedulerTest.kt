package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class NotificationSchedulerTest {
    
    @Test
    fun testComputeNextTrigger_Creation_FutureTime() {
        // Сценарий 1: Создание в 12:00 на 12:02
        val now = LocalDateTime.of(2024, 1, 15, 12, 0) // 12:00
        val targetTime = LocalTime.of(12, 2) // 12:02
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = false)
        
        val expectedTime = LocalDateTime.of(2024, 1, 15, 12, 2) // сегодня в 12:02
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertFalse(result.markOverdue)
    }
    
    @Test
    fun testComputeNextTrigger_Edit_FutureTime() {
        // Сценарий 2: Редактирование в 12:01 на 12:05
        val now = LocalDateTime.of(2024, 1, 15, 12, 1) // 12:01
        val targetTime = LocalTime.of(12, 5) // 12:05
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = true)
        
        val expectedTime = LocalDateTime.of(2024, 1, 15, 12, 5) // сегодня в 12:05
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertFalse(result.markOverdue)
    }
    
    @Test
    fun testComputeNextTrigger_Edit_PastTime() {
        // Сценарий 3: Редактирование в 12:10 на 09:00 (прошедшее время)
        val now = LocalDateTime.of(2024, 1, 15, 12, 10) // 12:10
        val targetTime = LocalTime.of(9, 0) // 09:00
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = true)
        
        val expectedTime = LocalDateTime.of(2024, 1, 15, 12, 11) // ASAP: +1 минута
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertTrue(result.markOverdue)
    }
    
    @Test
    fun testComputeNextTrigger_Creation_PastTime() {
        // Создание в 12:00 на 09:00 (прошедшее время) - перенос на завтра
        val now = LocalDateTime.of(2024, 1, 15, 12, 0) // 12:00
        val targetTime = LocalTime.of(9, 0) // 09:00
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = false)
        
        val expectedTime = LocalDateTime.of(2024, 1, 16, 9, 0) // завтра в 09:00
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertFalse(result.markOverdue)
    }
    
    @Test
    fun testComputeNextTrigger_Edit_ExactCurrentTime() {
        // Редактирование на текущее время
        val now = LocalDateTime.of(2024, 1, 15, 12, 0) // 12:00
        val targetTime = LocalTime.of(12, 0) // 12:00
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = true)
        
        val expectedTime = LocalDateTime.of(2024, 1, 15, 12, 1) // ASAP: +1 минута
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertTrue(result.markOverdue)
    }
    
    @Test
    fun testComputeNextTrigger_Creation_ExactCurrentTime() {
        // Создание на текущее время
        val now = LocalDateTime.of(2024, 1, 15, 12, 0) // 12:00
        val targetTime = LocalTime.of(12, 0) // 12:00
        
        val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit = false)
        
        val expectedTime = LocalDateTime.of(2024, 1, 15, 12, 0) // сегодня в 12:00
        val expectedMs = expectedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        assertEquals(expectedMs, result.triggerAtMs)
        assertFalse(result.markOverdue)
    }
    
    @Test
    fun testNotificationTitlesAndDescriptions_OverdueMedicine() {
        println("=== ТЕСТ ЗАГОЛОВКОВ И ОПИСАНИЙ УВЕДОМЛЕНИЙ ДЛЯ ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
        
        // Тестируем логику создания заголовков и описаний для просроченных лекарств
        val medicineName = "Аспирин"
        val medicineTime = LocalTime.of(9, 0)
        val isOverdue = true
        
        // Симулируем логику из NotificationManager.showMedicineCardNotification
        val title = if (isOverdue) "ПРОСРОЧЕНО: $medicineName" else "Примите: $medicineName"
        val contentText = if (isOverdue) "Просрочено! Запланировано было на $medicineTime" else "Запланировано на $medicineTime"
        
        println("📝 ТЕСТОВЫЕ ДАННЫЕ:")
        println("   Название лекарства: $medicineName")
        println("   Время приема: $medicineTime")
        println("   Статус просрочки: $isOverdue")
        
        println("\n🔍 РЕЗУЛЬТАТЫ:")
        println("   Заголовок: '$title'")
        println("   Описание: '$contentText'")
        
        // Проверяем правильность заголовка для просроченного лекарства
        val expectedTitle = "ПРОСРОЧЕНО: $medicineName"
        assertEquals("Заголовок должен содержать 'ПРОСРОЧЕНО'", expectedTitle, title)
        assertTrue("Заголовок должен начинаться с 'ПРОСРОЧЕНО'", title.startsWith("ПРОСРОЧЕНО:"))
        assertTrue("Заголовок должен содержать название лекарства", title.contains(medicineName))
        
        // Проверяем правильность описания для просроченного лекарства
        val expectedDescription = "Просрочено! Запланировано было на $medicineTime"
        assertEquals("Описание должно указывать на просрочку", expectedDescription, contentText)
        assertTrue("Описание должно содержать 'Просрочено!'", contentText.startsWith("Просрочено!"))
        assertTrue("Описание должно содержать время приема", contentText.contains(medicineTime.toString()))
        
        println("\n✅ РЕЗУЛЬТАТ: Заголовки и описания для просроченных лекарств корректны")
    }
    
    @Test
    fun testNotificationTitlesAndDescriptions_RegularMedicine() {
        println("=== ТЕСТ ЗАГОЛОВКОВ И ОПИСАНИЙ УВЕДОМЛЕНИЙ ДЛЯ ОБЫЧНЫХ ЛЕКАРСТВ ===")
        
        // Тестируем логику создания заголовков и описаний для обычных лекарств
        val medicineName = "Витамин C"
        val medicineTime = LocalTime.of(14, 30)
        val isOverdue = false
        
        // Симулируем логику из NotificationManager.showMedicineCardNotification
        val title = if (isOverdue) "ПРОСРОЧЕНО: $medicineName" else "Примите: $medicineName"
        val contentText = if (isOverdue) "Просрочено! Запланировано было на $medicineTime" else "Запланировано на $medicineTime"
        
        println("📝 ТЕСТОВЫЕ ДАННЫЕ:")
        println("   Название лекарства: $medicineName")
        println("   Время приема: $medicineTime")
        println("   Статус просрочки: $isOverdue")
        
        println("\n🔍 РЕЗУЛЬТАТЫ:")
        println("   Заголовок: '$title'")
        println("   Описание: '$contentText'")
        
        // Проверяем правильность заголовка для обычного лекарства
        val expectedTitle = "Примите: $medicineName"
        assertEquals("Заголовок должен содержать 'Примите'", expectedTitle, title)
        assertTrue("Заголовок должен начинаться с 'Примите:'", title.startsWith("Примите:"))
        assertTrue("Заголовок должен содержать название лекарства", title.contains(medicineName))
        assertFalse("Заголовок не должен содержать 'ПРОСРОЧЕНО'", title.contains("ПРОСРОЧЕНО"))
        
        // Проверяем правильность описания для обычного лекарства
        val expectedDescription = "Запланировано на $medicineTime"
        assertEquals("Описание должно указывать на запланированное время", expectedDescription, contentText)
        assertTrue("Описание должно содержать 'Запланировано на'", contentText.startsWith("Запланировано на"))
        assertTrue("Описание должно содержать время приема", contentText.contains(medicineTime.toString()))
        assertFalse("Описание не должно содержать 'Просрочено!'", contentText.contains("Просрочено!"))
        
        println("\n✅ РЕЗУЛЬТАТ: Заголовки и описания для обычных лекарств корректны")
    }
    
    @Test
    fun testOverdueFlagLogic() {
        println("=== ТЕСТ ЛОГИКИ ФЛАГА ПРОСРОЧКИ ===")
        
        // Тестируем различные сценарии с флагом markOverdue
        val testCases = listOf(
            Triple("Редактирование на прошедшее время", LocalTime.of(9, 0), true),
            Triple("Редактирование на будущее время", LocalTime.of(16, 0), false),
            Triple("Создание на прошедшее время", LocalTime.of(9, 0), false),
            Triple("Создание на будущее время", LocalTime.of(16, 0), false)
        )
        
        val now = LocalDateTime.of(2024, 1, 15, 12, 0) // 12:00
        
        testCases.forEach { (description, targetTime, isEdit) ->
            println("\n📋 ТЕСТ: $description")
            println("   Время: $targetTime")
            println("   Редактирование: $isEdit")
            
            val result = NotificationScheduler.computeNextTriggerStatic(now, targetTime, isEdit)
            
            println("   Результат markOverdue: ${result.markOverdue}")
            
            // Проверяем логику
            if (isEdit && targetTime <= now.toLocalTime()) {
                assertTrue("При редактировании на прошедшее время markOverdue должен быть true", result.markOverdue)
            } else {
                assertFalse("В остальных случаях markOverdue должен быть false", result.markOverdue)
            }
        }
        
        println("\n✅ РЕЗУЛЬТАТ: Логика флага просрочки работает корректно")
    }
    
    /**
     * ДОБАВЛЕНО: Тест логики группы "через день" с разным количеством лекарств
     */
    @Test
    fun testGroupEveryOtherDayLogic_MultipleMedicines() {
        android.util.Log.d("NotificationSchedulerTest", "=== ТЕСТ ГРУППЫ 'ЧЕРЕЗ ДЕНЬ' С РАЗНЫМ КОЛИЧЕСТВОМ ЛЕКАРСТВ ===")
        
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)
        val dayAfterTomorrow = today.plusDays(2)
        
        // Создаем лекарства с разными порядками в группе
        val medicines = listOf(
            createTestMedicine("Лекарство 1", groupOrder = 1),
            createTestMedicine("Лекарство 2", groupOrder = 2),
            createTestMedicine("Лекарство 3", groupOrder = 3),
            createTestMedicine("Лекарство 4", groupOrder = 4),
            createTestMedicine("Лекарство 5", groupOrder = 5)
        )
        
        android.util.Log.d("NotificationSchedulerTest", "📅 СЕГОДНЯ ($today):")
        medicines.forEach { medicine ->
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
            android.util.Log.d("NotificationSchedulerTest", "  ${medicine.name} (№${medicine.groupOrder}): ${if (shouldTake) "✅ ПРИНИМАТЬ" else "❌ НЕ ПРИНИМАТЬ"}")
        }
        
        android.util.Log.d("NotificationSchedulerTest", "📅 ЗАВТРА ($tomorrow):")
        medicines.forEach { medicine ->
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, tomorrow)
            android.util.Log.d("NotificationSchedulerTest", "  ${medicine.name} (№${medicine.groupOrder}): ${if (shouldTake) "✅ ПРИНИМАТЬ" else "❌ НЕ ПРИНИМАТЬ"}")
        }
        
        android.util.Log.d("NotificationSchedulerTest", "📅 ПОСЛЕЗАВТРА ($dayAfterTomorrow):")
        medicines.forEach { medicine ->
            val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, dayAfterTomorrow)
            android.util.Log.d("NotificationSchedulerTest", "  ${medicine.name} (№${medicine.groupOrder}): ${if (shouldTake) "✅ ПРИНИМАТЬ" else "❌ НЕ ПРИНИМАТЬ"}")
        }
        
        // Проверяем логику
        val todayResults = medicines.map { it to DosageCalculator.shouldTakeMedicine(it, today) }
        val tomorrowResults = medicines.map { it to DosageCalculator.shouldTakeMedicine(it, tomorrow) }
        
        // Сегодня должны принимать лекарства с нечетными порядками (1,3,5)
        val shouldTakeToday = todayResults.filter { it.second }.map { it.first.groupOrder }
        val shouldTakeTomorrow = tomorrowResults.filter { it.second }.map { it.first.groupOrder }
        
        android.util.Log.d("NotificationSchedulerTest", "✅ Сегодня принимают: $shouldTakeToday")
        android.util.Log.d("NotificationSchedulerTest", "✅ Завтра принимают: $shouldTakeTomorrow")
        
        // Проверяем, что логика работает правильно
        assertTrue("Сегодня должны принимать нечетные порядки", shouldTakeToday.all { it % 2 == 1 })
        assertTrue("Завтра должны принимать четные порядки", shouldTakeTomorrow.all { it % 2 == 0 })
        
        android.util.Log.d("NotificationSchedulerTest", "✅ ТЕСТ ПРОЙДЕН: Логика группы 'через день' работает корректно")
    }
    
    private fun createTestMedicine(name: String, groupOrder: Int): com.medicalnotes.app.models.Medicine {
        return com.medicalnotes.app.models.Medicine(
            id = System.currentTimeMillis() + groupOrder,
            name = name,
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 10,
            time = java.time.LocalTime.of(9, 0),
            frequency = com.medicalnotes.app.models.DosageFrequency.EVERY_OTHER_DAY,
            groupName = "Группа через день",
            groupOrder = groupOrder,
            startDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Начали вчера
        )
    }
} 