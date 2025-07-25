package com.medicalnotes.app.models

import java.time.LocalTime

enum class DosageFrequency {
    DAILY,           // Каждый день
    EVERY_OTHER_DAY, // Через день
    TWICE_A_WEEK,    // 2 раза в неделю
    THREE_TIMES_A_WEEK, // 3 раза в неделю
    WEEKLY,          // Раз в неделю
    CUSTOM           // Пользовательская схема
}

enum class DosageTime {
    MORNING,         // Утром
    AFTERNOON,       // Днем
    EVENING,         // Вечером
    NIGHT,           // Ночью
    CUSTOM           // Пользовательское время
}

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val quantity: Int,
    val remainingQuantity: Int,
    val time: LocalTime,
    val notes: String = "",
    val isActive: Boolean = true,
    val isInsulin: Boolean = false,
    val isMissed: Boolean = false,
    val lastTakenTime: Long = 0,
    val missedCount: Int = 0,
    
    // Новая схема приема
    val frequency: DosageFrequency = DosageFrequency.DAILY,
    val dosageTimes: List<DosageTime> = listOf(DosageTime.MORNING),
    val customDays: List<Int> = emptyList(), // Дни недели (1-7, где 1=понедельник)
    val customTimes: List<LocalTime> = emptyList(), // Пользовательские времена
    val startDate: Long = System.currentTimeMillis(), // Дата начала приема
    
    // Для нескольких приемов в день
    val multipleDoses: Boolean = false,
    val dosesPerDay: Int = 1,
    val doseTimes: List<LocalTime> = emptyList(), // Времена приемов в день
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Отслеживание принятых лекарств
    val takenToday: Boolean = false,
    val takenAt: Long = 0, // Время принятия
    val shouldTakeToday: Boolean = true, // Нужно ли принимать сегодня
    val isOverdue: Boolean = false, // Просрочено ли принятие
    
    // Группировка лекарств
    val groupId: Long? = null, // ID группы (null = не в группе)
    val groupName: String = "", // Название группы
    val groupOrder: Int = 0 // Порядок в группе (1, 2, 3...)
) 