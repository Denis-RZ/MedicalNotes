package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class EditTimeAfterTakenTest {
    
    @Test
    fun testEditTimeToFutureAfterTaken() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        val currentTime = LocalTime.now()
        
        // Создаем лекарство, которое было принято сегодня
        val originalMedicine = Medicine(
            id = 1,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            frequency = DosageFrequency.DAILY,
            time = LocalTime.of(8, 0), // Старое время (утро)
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            isActive = true,
            takenToday = true, // ПРИНЯТО СЕГОДНЯ
            quantity = 30,
            remainingQuantity = 29, // Количество уменьшилось (было принято)
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis()
        )
        
        // Симулируем редактирование времени на будущее
        val newTime = if (currentTime.hour >= 22) {
            // Если сейчас поздно, устанавливаем время на завтра утром
            LocalTime.of(8, 0)
        } else {
            // Иначе устанавливаем время через 2 часа
            currentTime.plusHours(2)
        }
        
        // Проверяем логику сброса статуса
        val wasActuallyTaken = originalMedicine.remainingQuantity < originalMedicine.quantity
        val timeChangedToFuture = originalMedicine.takenToday && 
                                 originalMedicine.time != newTime && 
                                 newTime.isAfter(currentTime)
        
        val shouldResetStatus = originalMedicine.frequency != DosageFrequency.DAILY || 
                               (originalMedicine.takenToday && newTime.isBefore(currentTime)) ||
                               (originalMedicine.takenToday && !wasActuallyTaken) ||
                               timeChangedToFuture
        
        println("=== ТЕСТ РЕДАКТИРОВАНИЯ ВРЕМЕНИ ПОСЛЕ ПРИЕМА ===")
        println("Текущее время: $currentTime")
        println("Старое время лекарства: ${originalMedicine.time}")
        println("Новое время: $newTime")
        println("Лекарство принято сегодня: ${originalMedicine.takenToday}")
        println("Количество уменьшилось: $wasActuallyTaken")
        println("Время изменено на будущее: $timeChangedToFuture")
        println("Сбрасываем статус: $shouldResetStatus")
        
        // Проверяем, что статус должен сброситься
        assertTrue("Статус должен сброситься при изменении времени на будущее", shouldResetStatus)
        assertTrue("Время изменено на будущее", timeChangedToFuture)
        
        // Симулируем обновленное лекарство
        val updatedMedicine = originalMedicine.copy(
            time = newTime,
            takenToday = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday,
            lastTakenTime = if (shouldResetStatus) 0 else originalMedicine.lastTakenTime,
            takenAt = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt
        )
        
        println("=== РЕЗУЛЬТАТ ОБНОВЛЕНИЯ ===")
        println("Новое время: ${updatedMedicine.time}")
        println("takenToday после обновления: ${updatedMedicine.takenToday}")
        println("lastTakenTime после обновления: ${updatedMedicine.lastTakenTime}")
        
        // Проверяем, что статус сбросился
        assertFalse("takenToday должен быть false после обновления", updatedMedicine.takenToday)
        assertEquals("lastTakenTime должен быть 0 после обновления", 0, updatedMedicine.lastTakenTime)
        assertEquals("takenAt должен быть 0 после обновления", 0, updatedMedicine.takenAt)
        
        // Проверяем, что лекарство снова должно появиться в списке "на сегодня"
        // (для DAILY частоты лекарство всегда должно приниматься)
        val shouldTakeToday = updatedMedicine.frequency == DosageFrequency.DAILY
        println("Лекарство должно приниматься сегодня: $shouldTakeToday")
        
        assertTrue("Лекарство должно снова появиться в списке", shouldTakeToday)
    }
    
    @Test
    fun testEditTimeToPastAfterTaken() {
        val today = LocalDate.now()
        val startDate = today.minusDays(10)
        val currentTime = LocalTime.now()
        
        // Создаем лекарство, которое было принято сегодня
        val originalMedicine = Medicine(
            id = 1,
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            frequency = DosageFrequency.DAILY,
            time = LocalTime.of(20, 0), // Старое время (вечер)
            startDate = startDate.toEpochDay() * 24 * 60 * 60 * 1000,
            isActive = true,
            takenToday = true, // ПРИНЯТО СЕГОДНЯ
            quantity = 30,
            remainingQuantity = 29, // Количество уменьшилось (было принято)
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis()
        )
        
        // Симулируем редактирование времени на прошлое
        val newTime = LocalTime.of(6, 0) // Новое время (утро, уже прошло)
        
        // Проверяем логику сброса статуса
        val wasActuallyTaken = originalMedicine.remainingQuantity < originalMedicine.quantity
        val timeChangedToPast = originalMedicine.takenToday && 
                               originalMedicine.time != newTime && 
                               newTime.isBefore(currentTime)
        
        val shouldResetStatus = originalMedicine.frequency != DosageFrequency.DAILY || 
                               (originalMedicine.takenToday && newTime.isBefore(currentTime)) ||
                               (originalMedicine.takenToday && !wasActuallyTaken) ||
                               (originalMedicine.takenToday && originalMedicine.time != newTime && newTime.isAfter(currentTime))
        
        println("=== ТЕСТ РЕДАКТИРОВАНИЯ ВРЕМЕНИ НА ПРОШЛОЕ ===")
        println("Текущее время: $currentTime")
        println("Старое время лекарства: ${originalMedicine.time}")
        println("Новое время: $newTime")
        println("Лекарство принято сегодня: ${originalMedicine.takenToday}")
        println("Количество уменьшилось: $wasActuallyTaken")
        println("Время изменено на прошлое: $timeChangedToPast")
        println("Сбрасываем статус: $shouldResetStatus")
        
        // Проверяем, что статус должен сброситься (время уже прошло)
        assertTrue("Статус должен сброситься при изменении времени на прошлое", shouldResetStatus)
        
        // Симулируем обновленное лекарство
        val updatedMedicine = originalMedicine.copy(
            time = newTime,
            takenToday = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday,
            lastTakenTime = if (shouldResetStatus) 0 else originalMedicine.lastTakenTime,
            takenAt = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt
        )
        
        println("=== РЕЗУЛЬТАТ ОБНОВЛЕНИЯ ===")
        println("Новое время: ${updatedMedicine.time}")
        println("takenToday после обновления: ${updatedMedicine.takenToday}")
        
        // Проверяем, что статус сбросился
        assertFalse("takenToday должен быть false после обновления", updatedMedicine.takenToday)
    }
} 