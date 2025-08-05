package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SimpleLogicTest {
    
    @Test
    fun testDosageCalculatorLogic() {
        Log.d("SimpleLogicTest", "=== ТЕСТ ЛОГИКИ DOSAGECALCULATOR ===")
        
        val today = LocalDate.now()
        val pastTime = LocalTime.now().minusHours(2)
        
        // Создаем лекарство
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 30,
            remainingQuantity = 30,
            medicineType = "таблетки",
            time = pastTime,
            frequency = DosageFrequency.DAILY,
            dosageTimes = listOf(DosageTime.MORNING),
            startDate = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            isActive = true,
            takenToday = false,
            lastTakenTime = 0
        )
        
        Log.d("SimpleLogicTest", "1. Создаем лекарство:")
        Log.d("SimpleLogicTest", "   - Время: ${medicine.time}")
        Log.d("SimpleLogicTest", "   - TakenToday: ${medicine.takenToday}")
        Log.d("SimpleLogicTest", "   - LastTakenTime: ${medicine.lastTakenTime}")
        Log.d("SimpleLogicTest", "   - StartDate: ${medicine.startDate}")
        
        // Проверяем логику DosageCalculator
        val shouldTake = DosageCalculator.shouldTakeMedicine(medicine, today)
        val isActive = medicine.isActive
        
        val lastTakenDate = if (medicine.lastTakenTime > 0) {
            LocalDate.ofEpochDay(medicine.lastTakenTime / (24 * 60 * 60 * 1000))
        } else {
            LocalDate.MIN
        }
        
        val lastTakenDateNotToday = lastTakenDate != today
        
        Log.d("SimpleLogicTest", "2. Анализ логики:")
        Log.d("SimpleLogicTest", "   - shouldTakeMedicine: $shouldTake")
        Log.d("SimpleLogicTest", "   - isActive: $isActive")
        Log.d("SimpleLogicTest", "   - lastTakenDate: $lastTakenDate")
        Log.d("SimpleLogicTest", "   - today: $today")
        Log.d("SimpleLogicTest", "   - lastTakenDate != today: $lastTakenDateNotToday")
        
        Log.d("SimpleLogicTest", "3. Итоговая логика:")
        Log.d("SimpleLogicTest", "   - isActive && shouldTake: ${isActive && shouldTake}")
        Log.d("SimpleLogicTest", "   - lastTakenDate != today: ${lastTakenDate != today}")
        Log.d("SimpleLogicTest", "   - Результат: ${(isActive && shouldTake) && (lastTakenDate != today)}")
        
        // Проверяем, что логика работает правильно
        assert(isActive) { "Лекарство должно быть активным" }
        assert(shouldTake) { "Лекарство должно приниматься сегодня" }
        assert(lastTakenDateNotToday) { "Лекарство не должно быть принято сегодня" }
        
        Log.d("SimpleLogicTest", "✅ Логика работает правильно")
    }
} 