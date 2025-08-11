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

// Метаданные группы для валидации
data class GroupMetadata(
    val groupId: Long,
    val groupName: String,
    val groupStartDate: Long,
    val groupFrequency: DosageFrequency,
    val groupSize: Int, // Количество лекарств в группе
    val groupValidationHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return groupId > 0 && 
               groupName.isNotBlank() && 
               groupStartDate > 0 && 
               groupSize > 0 && 
               groupValidationHash.isNotBlank()
    }
    
    fun generateValidationHash(): String {
        return "$groupId:$groupName:$groupStartDate:$groupFrequency:$groupSize".hashCode().toString()
    }
}

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val quantity: Int,
    val remainingQuantity: Int,
    val medicineType: String = "", // Тип лекарства (таблетки, уколы, капли и т.д.)
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
    // УДАЛЯЕМ дублирующие поля:
    // val takenAt: Long = 0, // Дублирует lastTakenTime
    // val shouldTakeToday: Boolean = true, // Вычисляемое поле
    // val isOverdue: Boolean = false, // Вычисляемое поле
    
    // УЛУЧШЕННАЯ ГРУППИРОВКА ЛЕКАРСТВ
    val groupId: Long? = null, // ID группы (null = не в группе)
    val groupName: String = "", // Название группы
    val groupOrder: Int = 0, // Порядок в группе (1, 2, 3...)
    val groupStartDate: Long = 0, // Дата начала группы (одинаковая для всех лекарств в группе)
    val groupFrequency: DosageFrequency = DosageFrequency.DAILY, // Частота группы
    val groupValidationHash: String = "", // Хеш для валидации целостности группы
    val groupMetadata: GroupMetadata? = null, // Метаданные группы
    
    // Связанные лекарства для одновременного приема
    val relatedMedicineIds: List<Long> = emptyList(), // ID лекарств, принимаемых вместе
    val isPartOfGroup: Boolean = false, // Является ли частью группы одновременного приема
    
    // Группировка по времени приема
    val timeGroupId: Long? = null, // ID группы времени (лекарства, принимаемые одновременно)
    val timeGroupName: String = "", // Название группы времени (например, "Завтрак", "Обед")
    val timeGroupOrder: Int = 0 // Порядок в группе времени
) {
    
    // ВАЛИДАЦИЯ ГРУППИРОВКИ
    fun isValidGroup(): Boolean {
        return groupId != null && 
               groupId > 0 &&
               groupName.isNotBlank() && 
               groupOrder > 0 && 
               groupStartDate > 0 &&
               groupValidationHash.isNotBlank()
    }
    
    fun isGroupConsistent(otherMedicines: List<Medicine>): Boolean {
        if (!isValidGroup()) return false
        
        val groupMedicines = otherMedicines.filter { it.groupId == this.groupId }
        if (groupMedicines.isEmpty()) return false
        
        // Проверяем, что все лекарства в группе имеют одинаковые базовые параметры
        val firstMedicine = groupMedicines.first()
        val hasConsistentStartDate = groupMedicines.all { it.groupStartDate == firstMedicine.groupStartDate }
        val hasConsistentFrequency = groupMedicines.all { it.groupFrequency == firstMedicine.groupFrequency }
        val hasValidOrder = groupMedicines.all { it.groupOrder > 0 && it.groupOrder <= groupMedicines.size }
        val hasUniqueOrder = groupMedicines.map { it.groupOrder }.distinct().size == groupMedicines.size
        
        return hasConsistentStartDate && hasConsistentFrequency && hasValidOrder && hasUniqueOrder
    }
    
    fun generateGroupValidationHash(): String {
        if (!isValidGroup()) return ""
        return "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString()
    }
    
    fun needsGroupValidation(): Boolean {
        return groupId != null && groupName.isNotBlank()
    }
    
    fun getGroupValidationStatus(otherMedicines: List<Medicine>): GroupValidationStatus {
        if (!needsGroupValidation()) return GroupValidationStatus.NOT_IN_GROUP
        
        if (!isValidGroup()) return GroupValidationStatus.INVALID_GROUP_DATA
        
        if (!isGroupConsistent(otherMedicines)) return GroupValidationStatus.INCONSISTENT_GROUP
        
        if (groupValidationHash != generateGroupValidationHash()) return GroupValidationStatus.INVALID_HASH
        
        return GroupValidationStatus.VALID
    }
    
    fun fixGroupData(otherMedicines: List<Medicine>): Medicine {
        if (!needsGroupValidation()) return this
        
        val groupMedicines = otherMedicines.filter { it.groupId == this.groupId }
        
        // Если группа пустая, убираем группировку
        if (groupMedicines.isEmpty()) {
            return this.copy(
                groupId = null,
                groupName = "",
                groupOrder = 0,
                groupStartDate = 0,
                groupFrequency = DosageFrequency.DAILY,
                groupValidationHash = "",
                groupMetadata = null
            )
        }
        
        // Исправляем базовые данные группы
        val firstMedicine = groupMedicines.first()
        val fixedGroupStartDate = if (groupStartDate <= 0) firstMedicine.startDate else groupStartDate
        val fixedGroupFrequency = if (groupFrequency == DosageFrequency.DAILY) firstMedicine.frequency else groupFrequency
        val fixedGroupOrder = if (groupOrder <= 0 || groupOrder > groupMedicines.size) {
            // Находим свободный порядок
            val usedOrders = groupMedicines.map { it.groupOrder }.filter { it > 0 }.toSet()
            (1..groupMedicines.size).find { it !in usedOrders } ?: 1
        } else groupOrder
        
        // Генерируем хеш с исправленными данными
        val fixedValidationHash = "$groupId:$groupName:$fixedGroupStartDate:$fixedGroupFrequency".hashCode().toString()
        
        return this.copy(
            groupStartDate = fixedGroupStartDate,
            groupFrequency = fixedGroupFrequency,
            groupOrder = fixedGroupOrder,
            groupValidationHash = fixedValidationHash
        )
    }
}

enum class GroupValidationStatus {
    NOT_IN_GROUP,        // Лекарство не в группе
    INVALID_GROUP_DATA,  // Неверные данные группы
    INCONSISTENT_GROUP,  // Несогласованность в группе
    INVALID_HASH,        // Неверный хеш валидации
    VALID               // Группа валидна
}

// Новая модель для групп времени приема
data class TimeGroup(
    val id: Long = 0,
    val name: String, // "Завтрак", "Обед", "Ужин", "Перед сном"
    val time: LocalTime,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 

 