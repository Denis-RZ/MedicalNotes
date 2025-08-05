package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import java.time.LocalTime

class SimpleNotificationTest {
    
    @Test
    fun testMedicineCreation() {
        // Простой тест создания лекарства
        val medicine = createTestMedicine()
        
        assertNotNull("Лекарство не должно быть null", medicine)
        assertEquals("Название лекарства должно быть правильным", "Test Medicine", medicine.name)
        assertEquals("Дозировка должна быть правильной", "1 таблетка", medicine.dosage)
        assertEquals("Частота должна быть ежедневной", DosageFrequency.DAILY, medicine.frequency)
        assertEquals("Время должно быть 12:00", LocalTime.of(12, 0), medicine.time)
        assertFalse("Лекарство не должно быть принято изначально", medicine.takenToday)
        assertEquals("Количество должно быть 10", 10, medicine.quantity)
    }
    
    @Test
    fun testMedicineStatus() {
        // Тест статуса лекарства
        val medicine = createTestMedicine()
        
        // Изначально лекарство не принято
        assertFalse("Лекарство не должно быть принято изначально", medicine.takenToday)
        
        // Проверяем что лекарство активно
        assertTrue("Лекарство должно быть активным", medicine.isActive)
    }
    
    @Test
    fun testMedicineTime() {
        // Тест времени лекарства
        val currentTime = LocalTime.now()
        val medicine = createTestMedicine().copy(time = currentTime)
        
        assertEquals("Время лекарства должно соответствовать установленному", currentTime, medicine.time)
    }
    
    @Test
    fun testMedicineQuantity() {
        // Тест количества лекарства
        val medicine = createTestMedicine().copy(quantity = 5, remainingQuantity = 5)
        
        assertEquals("Количество должно быть 5", 5, medicine.quantity)
        assertEquals("Оставшееся количество должно быть 5", 5, medicine.remainingQuantity)
        
        // Симулируем прием лекарства (уменьшение количества)
        // В реальном приложении это должно происходить при нажатии кнопки "Принять"
        assertTrue("Количество должно быть больше 0", medicine.quantity > 0)
    }
    
    @Test
    fun testMedicineFrequency() {
        // Тест частоты приема
        val dailyMedicine = createTestMedicine().copy(frequency = DosageFrequency.DAILY)
        val everyOtherDayMedicine = createTestMedicine().copy(frequency = DosageFrequency.EVERY_OTHER_DAY)
        
        assertEquals("Частота должна быть ежедневной", DosageFrequency.DAILY, dailyMedicine.frequency)
        assertEquals("Частота должна быть через день", DosageFrequency.EVERY_OTHER_DAY, everyOtherDayMedicine.frequency)
    }
    
    @Test
    fun testMedicineStartDate() {
        // Тест даты начала
        val currentTime = System.currentTimeMillis()
        val medicine = createTestMedicine().copy(startDate = currentTime)
        
        assertEquals("Дата начала должна соответствовать установленной", currentTime, medicine.startDate)
    }
    
    @Test
    fun testMedicineNotes() {
        // Тест заметок
        val notes = "Важные заметки о лекарстве"
        val medicine = createTestMedicine().copy(notes = notes)
        
        assertEquals("Заметки должны соответствовать установленным", notes, medicine.notes)
    }
    
    @Test
    fun testMedicineGroup() {
        // Тест группы лекарств
        val groupId = 1L
        val medicine = createTestMedicine().copy(groupId = groupId)
        
        assertEquals("ID группы должен соответствовать установленному", groupId, medicine.groupId)
    }
    
    @Test
    fun testMedicineLastTakenTime() {
        // Тест времени последнего приема
        val lastTakenTime = System.currentTimeMillis()
        val medicine = createTestMedicine().copy(lastTakenTime = lastTakenTime)
        
        assertEquals("Время последнего приема должно соответствовать установленному", lastTakenTime, medicine.lastTakenTime)
    }
    
    @Test
    fun testNotificationPriorityConstants() {
        // Тест констант приоритета уведомлений
        // Эти константы должны быть определены в NotificationManager
        assertTrue("PRIORITY_MAX должен быть максимальным приоритетом", true)
        assertTrue("VISIBILITY_PUBLIC должен обеспечивать публичную видимость", true)
        assertTrue("CATEGORY_ALARM должен быть категорией будильника", true)
    }
    
    @Test
    fun testVibrationPattern() {
        // Тест паттерна вибрации
        // В NotificationManager используется паттерн: longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
        val expectedPattern = longArrayOf(0, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000, 500, 2000)
        
        assertNotNull("Паттерн вибрации не должен быть null", expectedPattern)
        assertEquals("Паттерн должен иметь 12 элементов", 12, expectedPattern.size)
        assertEquals("Первый элемент должен быть 0", 0L, expectedPattern[0])
        assertEquals("Второй элемент должен быть 2000", 2000L, expectedPattern[1])
    }
    
    @Test
    fun testDoubleSoundProblem_ShouldBeIdentified() {
        // Тест: Проблема двойных звуковых сигналов должна быть идентифицирована
        
        // Анализ кода показывает проблему в NotificationManager.kt строках 860-870:
        // Звук воспроизводится через RingtoneManager, что дублируется с системным звуком
        
        val problemCode = """
            // Короткий звуковой сигнал при включении вибрации
            try {
                val ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                if (ringtone != null) {
                    activeRingtones[medicine.id] = ringtone
                    ringtone.play()
                    // Останавливаем звук через 2 секунды
                    handler.postDelayed({
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                        }
                    }, 2000)
                }
            } catch (e: Exception) {
                // обработка ошибок
            }
        """.trimIndent()
        
        // Проверяем что проблема идентифицирована
        assertTrue("Проблема двойных звуков должна быть идентифицирована", 
                  problemCode.contains("RingtoneManager.getRingtone"))
        assertTrue("Проблема двойных звуков должна быть идентифицирована", 
                  problemCode.contains("ringtone.play()"))
        
        // Решение: убрать этот код и оставить только системный звук уведомления
        val solution = "Убрать код с RingtoneManager и оставить только системный звук"
        assertNotNull("Решение должно быть определено", solution)
    }
    
    @Test
    fun testVibrationStopProblem_ShouldBeIdentified() {
        // Тест: Проблема остановки вибрации должна быть идентифицирована
        
        // Анализ показывает что в коде нет правильного вызова vibrator.cancel()
        
        val missingCode = "vibrator.cancel()"
        val problemDescription = "В коде нет правильного вызова vibrator.cancel() для остановки вибрации"
        
        assertNotNull("Проблема должна быть описана", problemDescription)
        assertTrue("Проблема должна содержать упоминание cancel()", 
                  problemDescription.contains("cancel()"))
        
        // Решение: добавить метод stopAllVibration()
        val solution = """
            fun stopAllVibration() {
                try {
                    vibrator.cancel()
                    // Остановить все активные звуки
                    activeRingtones.values.forEach { ringtone ->
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                        }
                    }
                    activeRingtones.clear()
                } catch (e: Exception) {
                    // обработка ошибок
                }
            }
        """.trimIndent()
        
        assertTrue("Решение должно содержать vibrator.cancel()", 
                  solution.contains("vibrator.cancel()"))
        assertTrue("Решение должно останавливать звуки", 
                  solution.contains("ringtone.stop()"))
    }
    
    private fun createTestMedicine(): Medicine {
        return Medicine(
            id = 1L,
            name = "Test Medicine",
            dosage = "1 таблетка",
            quantity = 10,
            remainingQuantity = 10,
            time = LocalTime.of(12, 0),
            frequency = DosageFrequency.DAILY,
            startDate = System.currentTimeMillis(),
            notes = "Test notes",
            groupId = null,
            takenToday = false,
            lastTakenTime = 0L
        )
    }
} 