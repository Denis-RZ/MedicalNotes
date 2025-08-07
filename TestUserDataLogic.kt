import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Модель лекарства для тестирования
data class TestMedicine(
    val id: Long,
    val name: String,
    val dosage: String,
    val quantity: Int,
    val remainingQuantity: Int,
    val medicineType: String,
    val time: String,
    val frequency: String,
    val startDate: Long,
    val isActive: Boolean,
    val takenToday: Boolean,
    val lastTakenTime: Long,
    val takenAt: Long,
    val isMissed: Boolean,
    val missedCount: Int,
    val isOverdue: Boolean,
    val groupId: String?,
    val groupName: String,
    val groupOrder: Int,
    val groupStartDate: Long,
    val groupFrequency: String,
    val multipleDoses: Boolean,
    val doseTimes: String,
    val customDays: String,
    val updatedAt: Long
)

// Перечисление частоты приема
enum class DosageFrequency {
    DAILY, EVERY_OTHER_DAY, TWICE_A_WEEK, THREE_TIMES_A_WEEK, WEEKLY, CUSTOM
}

fun main() {
    println("=== АНАЛИЗ ЛОГИКИ ГРУППИРОВКИ С РЕАЛЬНЫМИ ДАННЫМИ ===")
    
    // Создаем тестовые данные из XML
    val lipetor = TestMedicine(
        id = 1754381301015,
        name = "Липетор",
        dosage = "20",
        quantity = 44,
        remainingQuantity = 44,
        medicineType = "Tablets",
        time = "17:41",
        frequency = "EVERY_OTHER_DAY",
        startDate = 1754381301006,
        isActive = true,
        takenToday = false,
        lastTakenTime = 1754473507174,
        takenAt = 0,
        isMissed = false,
        missedCount = 0,
        isOverdue = false,
        groupId = "1754451744031",
        groupName = "Тестер",
        groupOrder = 1,
        groupStartDate = 1754451744031,
        groupFrequency = "EVERY_OTHER_DAY",
        multipleDoses = false,
        doseTimes = "17:41",
        customDays = "",
        updatedAt = 1754540857591
    )
    
    val fubuxusat = TestMedicine(
        id = 1754381353482,
        name = "Фубуксусат",
        dosage = "Полтоблетки",
        quantity = 34,
        remainingQuantity = 34,
        medicineType = "Tablets",
        time = "16:15",
        frequency = "EVERY_OTHER_DAY",
        startDate = 1754381353472,
        isActive = true,
        takenToday = false,
        lastTakenTime = 1754471876018,
        takenAt = 0,
        isMissed = false,
        missedCount = 0,
        isOverdue = false,
        groupId = "1754451755574",
        groupName = "Тестер",
        groupOrder = 2,
        groupStartDate = 1754451755574,
        groupFrequency = "EVERY_OTHER_DAY",
        multipleDoses = false,
        doseTimes = "16:15",
        customDays = "",
        updatedAt = 1754540857591
    )
    
    val medicines = listOf(lipetor, fubuxusat)
    
    // Анализируем даты
    println("\n=== АНАЛИЗ ДАТ ===")
    
    val today = LocalDate.now()
    println("Сегодня: $today")
    
    // Конвертируем timestamp в даты
    val lipetorStartDate = Instant.ofEpochMilli(lipetor.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val lipetorGroupStartDate = Instant.ofEpochMilli(lipetor.groupStartDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val lipetorLastTakenDate = Instant.ofEpochMilli(lipetor.lastTakenTime).atZone(ZoneId.systemDefault()).toLocalDate()
    
    val fubuxusatStartDate = Instant.ofEpochMilli(fubuxusat.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val fubuxusatGroupStartDate = Instant.ofEpochMilli(fubuxusat.groupStartDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val fubuxusatLastTakenDate = Instant.ofEpochMilli(fubuxusat.lastTakenTime).atZone(ZoneId.systemDefault()).toLocalDate()
    
    println("Липетор:")
    println("  - Дата начала: $lipetorStartDate")
    println("  - Дата начала группы: $lipetorGroupStartDate")
    println("  - Последний прием: $lipetorLastTakenDate")
    println("  - Порядок в группе: ${lipetor.groupOrder}")
    
    println("Фубуксусат:")
    println("  - Дата начала: $fubuxusatStartDate")
    println("  - Дата начала группы: $fubuxusatGroupStartDate")
    println("  - Последний прием: $fubuxusatLastTakenDate")
    println("  - Порядок в группе: ${fubuxusat.groupOrder}")
    
    // Проверяем логику группировки
    println("\n=== ПРОВЕРКА ЛОГИКИ ГРУППИРОВКИ ===")
    
    // Используем дату начала группы для расчета
    val groupStartDate = lipetorGroupStartDate // Оба лекарства в одной группе
    val daysSinceGroupStart = ChronoUnit.DAYS.between(groupStartDate, today)
    
    println("Дней с начала группы: $daysSinceGroupStart")
    println("День группы (остаток от деления на 2): ${daysSinceGroupStart % 2}")
    
    // Проверяем логику для каждого лекарства
    println("\n--- ЛИПЕТОР (порядок 1) ---")
    val lipetorGroupDay = (daysSinceGroupStart % 2).toInt()
    val lipetorShouldTake = lipetorGroupDay == 0 // Порядок 1 принимается в день 0
    println("День группы: $lipetorGroupDay")
    println("Должен принимать (порядок 1, день 0): $lipetorShouldTake")
    
    // Проверяем, был ли прием вчера
    val yesterday = today.minusDays(1)
    val lipetorWasTakenYesterday = lipetorLastTakenDate == yesterday
    println("Принят вчера: $lipetorWasTakenYesterday")
    
    println("\n--- ФУБУКСУСАТ (порядок 2) ---")
    val fubuxusatGroupDay = (daysSinceGroupStart % 2).toInt()
    val fubuxusatShouldTake = fubuxusatGroupDay == 1 // Порядок 2 принимается в день 1
    println("День группы: $fubuxusatGroupDay")
    println("Должен принимать (порядок 2, день 1): $fubuxusatShouldTake")
    
    val fubuxusatWasTakenYesterday = fubuxusatLastTakenDate == yesterday
    println("Принят вчера: $fubuxusatWasTakenYesterday")
    
    // Финальная логика (как в приложении)
    println("\n=== ФИНАЛЬНАЯ ЛОГИКА ===")
    
    val lipetorFinalResult = if (lipetorWasTakenYesterday && !lipetorShouldTake) {
        false
    } else if (lipetorWasTakenYesterday && lipetorShouldTake) {
        true
    } else {
        lipetorShouldTake
    }
    
    val fubuxusatFinalResult = if (fubuxusatWasTakenYesterday && !fubuxusatShouldTake) {
        false
    } else if (fubuxusatWasTakenYesterday && fubuxusatShouldTake) {
        true
    } else {
        fubuxusatShouldTake
    }
    
    println("Липетор - финальный результат: $lipetorFinalResult")
    println("Фубуксусат - финальный результат: $fubuxusatFinalResult")
    
    // Проверяем, что не оба принимаются одновременно
    val bothTaken = lipetorFinalResult && fubuxusatFinalResult
    println("Оба принимаются одновременно: $bothTaken")
    
    if (bothTaken) {
        println("❌ ОШИБКА: Оба лекарства не должны приниматься одновременно!")
    } else {
        println("✅ КОРРЕКТНО: Только одно или ни одного лекарства принимается сегодня")
    }
    
    // Показываем, какие лекарства должны быть на экране "Лекарства на сегодня"
    println("\n=== РЕЗУЛЬТАТ ДЛЯ ЭКРАНА 'ЛЕКАРСТВА НА СЕГОДНЯ' ===")
    
    val todayMedicines = mutableListOf<String>()
    if (lipetorFinalResult) todayMedicines.add("Липетор (17:41)")
    if (fubuxusatFinalResult) todayMedicines.add("Фубуксусат (16:15)")
    
    if (todayMedicines.isEmpty()) {
        println("Сегодня не нужно принимать лекарства из группы 'Тестер'")
    } else {
        println("Сегодня нужно принять:")
        todayMedicines.forEach { println("  - $it") }
    }
    
    println("\n=== ДОПОЛНИТЕЛЬНАЯ ИНФОРМАЦИЯ ===")
    println("Время последнего приема Липетора: ${lipetorLastTakenDate} (${lipetor.time})")
    println("Время последнего приема Фубуксусата: ${fubuxusatLastTakenDate} (${fubuxusat.time})")
    println("Вчера было: $yesterday")
} 