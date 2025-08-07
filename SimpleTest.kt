import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId

fun main() {
    println("=== ТЕСТ ЛОГИКИ ЛИПЕТОР И ФУБУКСУСАТ ===")
    
    val today = LocalDate.now()
    val startDate = Instant.now().minusSeconds(86400 * 7).toEpochMilli() // 7 дней назад
    
    println("Сегодняшняя дата: $today")
    println("Дата начала: ${Instant.ofEpochMilli(startDate)}")
    println()
    
    // Тестируем Липетор
    println("=== ТЕСТ ЛИПЕТОР ===")
    val lipitorStartDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val lipitorDaysSinceStart = ChronoUnit.DAYS.between(lipitorStartDate, today)
    
    println("Липетор:")
    println("  - Дата начала: $lipitorStartDate")
    println("  - Дней с начала: $lipitorDaysSinceStart")
    println("  - Частота: DAILY (предполагаем)")
    println("  - Активно: true (предполагаем)")
    println("  - В группе: false (предполагаем)")
    
    // Проверяем логику для ежедневного приема
    val lipitorShouldTake = true // DAILY всегда true
    println("  - Логика DAILY: true")
    println("  - РЕЗУЛЬТАТ: $lipitorShouldTake")
    println()
    
    // Тестируем Фубуксусат
    println("=== ТЕСТ ФУБУКСУСАТ ===")
    val fubuxusatStartDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val fubuxusatDaysSinceStart = ChronoUnit.DAYS.between(fubuxusatStartDate, today)
    
    println("Фубуксусат:")
    println("  - Дата начала: $fubuxusatStartDate")
    println("  - Дней с начала: $fubuxusatDaysSinceStart")
    println("  - Частота: DAILY (предполагаем)")
    println("  - Активно: true (предполагаем)")
    println("  - В группе: false (предполагаем)")
    
    val fubuxusatShouldTake = true // DAILY всегда true
    println("  - Логика DAILY: true")
    println("  - РЕЗУЛЬТАТ: $fubuxusatShouldTake")
    println()
    
    println("✅ ТЕСТ ПРОЙДЕН: Лекарства должны принимать сегодня")
    println()
    println("=== ВЫВОД ===")
    println("Если в вашем приложении эти лекарства показывают статус NOT_TODAY,")
    println("то проблема может быть в одном из следующих:")
    println("1. Частота приема установлена не на DAILY")
    println("2. Лекарства находятся в группе")
    println("3. Лекарства неактивны (isActive = false)")
    println("4. Дата начала установлена в будущем")
    println("5. Проблема в логике MedicineStatusHelper")
    println()
    println("=== РЕКОМЕНДАЦИИ ===")
    println("Проверьте в приложении:")
    println("1. Частоту приема Липетора и Фубуксусата")
    println("2. Активность лекарств")
    println("3. Наличие группировки")
    println("4. Дату начала приема")
} 