package com.medicalnotes.app.models

import java.time.LocalDateTime

data class MedicineJournalEntry(
    val id: Long = 0,
    val medicineId: Long,
    val medicineName: String,
    val action: JournalAction,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val notes: String = "",
    val dosage: String = "",
    val quantity: Int = 0
)

enum class JournalAction {
    TAKEN,      // Принято
    SKIPPED,    // Пропущено
    MISSED,     // Просрочено
    EDITED,     // Отредактировано
    DELETED     // Удалено
} 