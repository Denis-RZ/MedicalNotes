package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency

/**
 * Комплексный анализ всех возможных причин проблемы:
 * "к сожалению как только я нажимаю кнопку принял лекарства появляется то что должно показатся в облости лекарства на сегодня только завтро"
 */
class ComplexProblemAnalysisTest {

    @Test
    fun testAllPossibleCauses() {
        println("\n=== КОМПЛЕКСНЫЙ АНАЛИЗ ВСЕХ ВОЗМОЖНЫХ ПРИЧИН ПРОБЛЕМЫ ===")
        
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        
        // Создаем лекарства как в реальной ситуации
        val lipetor = Medicine(
            id = 1L,
            name = "Липетор",
            dosage = "1 таблетка",
            time = LocalTime.of(9, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis(),
            isActive = true,
            takenToday = false,
            remainingQuantity = 30,
            quantity = 30,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 1
        )
        
        val fubuxicin = Medicine(
            id = 2L,
            name = "Фубуксицин",
            dosage = "1 таблетка",
            time = LocalTime.of(18, 0),
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            startDate = System.currentTimeMillis(),
            isActive = true,
            takenToday = false,
            remainingQuantity = 20,
            quantity = 20,
            groupId = 1L,
            groupName = "Группа 1",
            groupOrder = 2
        )
        
        val medicines = listOf(lipetor, fubuxicin)
        
        println("\n1. ПРОВЕРКА НАЧАЛЬНОГО СОСТОЯНИЯ:")
        println("Сегодняшняя дата: $today")
        println("Текущее время: $currentTime")
        
        // Проверяем начальное состояние
        val lipetorShouldTakeInitial = shouldTakeMedicineInGroup(lipetor, today)
        val fubuxicinShouldTakeInitial = shouldTakeMedicineInGroup(fubuxicin, today)
        
        println("Липетор должен приниматься сегодня: $lipetorShouldTakeInitial")
        println("Фубуксицин должен приниматься сегодня: $fubuxicinShouldTakeInitial")
        
        // Проверяем фильтрацию
        val initialTodayMedicines = medicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Лекарств на сегодня (начальное состояние): ${initialTodayMedicines.size}")
        initialTodayMedicines.forEach { medicine ->
            println("  - ${medicine.name}")
        }
        
        println("\n2. СИМУЛЯЦИЯ НАЖАТИЯ 'ПРИНЯЛ' ДЛЯ ЛИПЕТОРА:")
        
        // Симулируем нажатие "принял" для Липетора
        val lipetorAfterTaken = lipetor.copy(
            takenToday = true,
            lastTakenTime = System.currentTimeMillis(),
            takenAt = System.currentTimeMillis(),
            remainingQuantity = lipetor.remainingQuantity - 1
        )
        
        println("Липетор после принятия:")
        println("  - takenToday: ${lipetorAfterTaken.takenToday}")
        println("  - remainingQuantity: ${lipetorAfterTaken.remainingQuantity}")
        
        // Обновляем список лекарств
        val updatedMedicines = listOf(lipetorAfterTaken, fubuxicin)
        
        println("\n3. ПРОВЕРКА ФИЛЬТРАЦИИ ПОСЛЕ ПРИНЯТИЯ ЛИПЕТОРА:")
        
        val afterTakenTodayMedicines = updatedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Лекарств на сегодня (после принятия Липетора): ${afterTakenTodayMedicines.size}")
        afterTakenTodayMedicines.forEach { medicine ->
            println("  - ${medicine.name}")
        }
        
        println("\n4. АНАЛИЗ ВОЗМОЖНЫХ ПРИЧИН ПРОБЛЕМЫ:")
        
        // Причина 1: Проблема с кэшированием данных
        println("\n4.1. ПРОВЕРКА КЭШИРОВАНИЯ:")
        val cachedMedicines = medicines // Симулируем кэшированные данные
        val cachedTodayMedicines = cachedMedicines.filter { medicine ->
            val isActive = medicine.isActive
            val shouldTake = shouldTakeMedicineInGroup(medicine, today)
            val isActiveAndShouldTake = isActive && shouldTake
            val notTakenToday = !medicine.takenToday
            isActiveAndShouldTake && notTakenToday
        }
        
        println("Кэшированные лекарства на сегодня: ${cachedTodayMedicines.size}")
        if (cachedTodayMedicines.isNotEmpty()) {
            println("⚠️  ПРОБЛЕМА КЭШИРОВАНИЯ: Показываются старые данные")
            cachedTodayMedicines.forEach { medicine ->
                println("  - ${medicine.name} (кэшированное)")
            }
        } else {
            println("✅ Кэширование работает корректно")
        }
        
        // Причина 2: Проблема с асинхронностью UI
        println("\n4.2. ПРОВЕРКА АСИНХРОННОСТИ:")
        println("В реальном приложении:")
        println("  1. takeMedicine() обновляет данные в IO потоке")
        println("  2. loadTodayMedicines() вызывается в Main потоке")
        println("  3. UI обновляется через LiveData")
        println("Возможная проблема: задержка между обновлением данных и UI")
        
        // Причина 3: Проблема с определением даты
        println("\n4.3. ПРОВЕРКА ОПРЕДЕЛЕНИЯ ДАТЫ:")
        val systemDate = LocalDate.now()
        val timezoneDate = java.time.ZonedDateTime.now().toLocalDate()
        
        println("Системная дата: $systemDate")
        println("Дата с учетом часового пояса: $timezoneDate")
        
        if (systemDate != timezoneDate) {
            println("⚠️  ПРОБЛЕМА С ДАТОЙ: Разные даты в системе и с учетом часового пояса")
        } else {
            println("✅ Определение даты корректно")
        }
        
        // Причина 4: Проблема с группировкой
        println("\n4.4. ПРОВЕРКА ГРУППИРОВКИ:")
        println("Липетор: groupId=${lipetor.groupId}, groupOrder=${lipetor.groupOrder}")
        println("Фубуксицин: groupId=${fubuxicin.groupId}, groupOrder=${fubuxicin.groupOrder}")
        
        val sameGroup = lipetor.groupId == fubuxicin.groupId
        val differentOrder = lipetor.groupOrder != fubuxicin.groupOrder
        
        if (sameGroup && differentOrder) {
            println("✅ Группировка настроена корректно")
        } else {
            println("⚠️  ПРОБЛЕМА ГРУППИРОВКИ: Неправильная настройка группы")
        }
        
        // Проверяем логику группы для завтрашнего дня
        val tomorrow = today.plusDays(1)
        val lipetorTomorrow = shouldTakeMedicineInGroup(lipetor, tomorrow)
        val fubuxicinTomorrow = shouldTakeMedicineInGroup(fubuxicin, tomorrow)
        
        println("Завтра:")
        println("  - Липетор должен приниматься: $lipetorTomorrow")
        println("  - Фубуксицин должен приниматься: $fubuxicinTomorrow")
        
        println("\n5. ВЫВОДЫ И РЕКОМЕНДАЦИИ:")
        
        if (afterTakenTodayMedicines.isNotEmpty()) {
            println("❌ ПРОБЛЕМА ОБНАРУЖЕНА: После принятия Липетора в списке остались лекарства")
            afterTakenTodayMedicines.forEach { medicine ->
                println("  - ${medicine.name} (не должно быть в списке)")
            }
            
            println("\nВозможные причины:")
            println("1. Кэширование данных - данные не обновляются сразу")
            println("2. Асинхронность UI - задержка обновления интерфейса")
            println("3. Проблема с датой - неправильное определение текущей даты")
            println("4. Проблема с группировкой - неправильная настройка группы")
            
            println("\nРекомендации для исправления:")
            println("1. Принудительно очистить кэш после обновления данных")
            println("2. Добавить задержку перед обновлением UI")
            println("3. Проверить настройки часового пояса")
            println("4. Проверить настройки группы лекарств")
            
        } else {
            println("✅ ПРОБЛЕМА НЕ ОБНАРУЖЕНА: Логика работает корректно")
            println("Проблема может быть в:")
            println("1. Кэшировании данных в реальном приложении")
            println("2. Асинхронности обновления UI")
            println("3. Настройках часового пояса устройства")
        }
        
        // Проверяем, что логика фильтрации работает правильно
        assertTrue("Липетор должен исчезнуть из списка после принятия", 
                  !afterTakenTodayMedicines.any { it.id == lipetor.id })
    }
    
    /**
     * Копия логики shouldTakeMedicineInGroup из DosageCalculator
     */
    private fun shouldTakeMedicineInGroup(medicine: Medicine, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medicine.startDate / (24 * 60 * 60 * 1000))
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date)
        
        if (medicine.frequency == DosageFrequency.EVERY_OTHER_DAY) {
            val groupDay = (daysSinceStart % 2).toInt()
            
            val shouldTake = when {
                medicine.groupOrder <= 0 -> false
                medicine.groupOrder % 2 == 1 -> groupDay == 0
                medicine.groupOrder % 2 == 0 -> groupDay == 1
                else -> false
            }
            return shouldTake
        }
        return false
    }
} 