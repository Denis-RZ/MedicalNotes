package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import android.util.Log

object GroupFixer {
    
    /**
     * Исправляет групповые несогласованности в списке лекарств
     * Объединяет лекарства с одинаковым названием группы в одну группу
     */
    fun fixGroupInconsistencies(medicines: List<Medicine>): List<Medicine> {
        Log.d("GroupFixer", "=== ИСПРАВЛЕНИЕ ГРУППОВЫХ НЕСОГЛАСОВАННОСТЕЙ ===")
        Log.d("GroupFixer", "Всего лекарств: ${medicines.size}")
        
        // Группируем лекарства по названию группы
        val groupsByName = medicines.filter { it.groupName.isNotBlank() }
            .groupBy { it.groupName }
        
        Log.d("GroupFixer", "Найдено групп по названию: ${groupsByName.size}")
        
        // Находим группы, которые нужно исправить (одинаковое название, но разные ID)
        val groupsToFix = groupsByName.filter { (_, groupMedicines) ->
            val uniqueGroupIds = groupMedicines.map { it.groupId }.distinct()
            uniqueGroupIds.size > 1
        }
        
        if (groupsToFix.isEmpty()) {
            Log.d("GroupFixer", "Несоответствий не найдено!")
            return medicines
        }
        
        Log.d("GroupFixer", "Найдено групп для исправления: ${groupsToFix.size}")
        
        val fixedMedicines = medicines.toMutableList()
        
        groupsToFix.forEach { (groupName, groupMedicines) ->
            Log.d("GroupFixer", "Исправляем группу: $groupName")
            
            // Используем самую раннюю дату начала как общую дату начала группы
            val earliestStartDate = groupMedicines.minOf { it.groupStartDate }
            Log.d("GroupFixer", "  Общая дата начала группы: $earliestStartDate")
            
            // Используем первый ID группы как общий ID
            val commonGroupId = groupMedicines.first().groupId ?: 0L
            Log.d("GroupFixer", "  Общий ID группы: $commonGroupId")
            
            // Сортируем лекарства по их исходному порядку в группе
            val sortedMedicines = groupMedicines.sortedBy { it.groupOrder }
            
            // Обновляем каждое лекарство
            sortedMedicines.forEachIndexed { index, medicine ->
                val newOrder = index + 1
                Log.d("GroupFixer", "  ${medicine.name}: порядок ${medicine.groupOrder} -> $newOrder")
                
                // Находим индекс лекарства в общем списке
                val medicineIndex = fixedMedicines.indexOfFirst { it.id == medicine.id }
                if (medicineIndex != -1) {
                    // Обновляем лекарство
                    val updatedMedicine = fixedMedicines[medicineIndex].copy(
                        groupId = commonGroupId,
                        groupStartDate = earliestStartDate,
                        groupOrder = newOrder,
                        groupValidationHash = generateGroupValidationHash(commonGroupId, groupName, earliestStartDate, medicine.groupFrequency)
                    )
                    fixedMedicines[medicineIndex] = updatedMedicine
                }
            }
        }
        
        Log.d("GroupFixer", "Исправление завершено!")
        return fixedMedicines
    }
    
    /**
     * Генерирует хеш валидации для группы
     */
    private fun generateGroupValidationHash(groupId: Long, groupName: String, groupStartDate: Long, groupFrequency: DosageFrequency): String {
        return "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString()
    }
    
    /**
     * Проверяет, есть ли несогласованности в группах
     */
    fun hasGroupInconsistencies(medicines: List<Medicine>): Boolean {
        val groupsByName = medicines.filter { it.groupName.isNotBlank() }
            .groupBy { it.groupName }
        
        return groupsByName.any { (_, groupMedicines) ->
            val uniqueGroupIds = groupMedicines.map { it.groupId }.distinct()
            uniqueGroupIds.size > 1
        }
    }
    
    /**
     * Получает отчет о групповых несогласованностях
     */
    fun getGroupInconsistencyReport(medicines: List<Medicine>): String {
        val report = StringBuilder()
        report.appendLine("=== ОТЧЕТ О ГРУППОВЫХ НЕСОГЛАСОВАННОСТЯХ ===")
        report.appendLine()
        
        val groupsByName = medicines.filter { it.groupName.isNotBlank() }
            .groupBy { it.groupName }
        
        report.appendLine("Всего групп по названию: ${groupsByName.size}")
        report.appendLine()
        
        groupsByName.forEach { (groupName, groupMedicines) ->
            val uniqueGroupIds = groupMedicines.map { it.groupId }.distinct()
            report.appendLine("Группа '$groupName':")
            report.appendLine("  ID групп: $uniqueGroupIds")
            report.appendLine("  Количество лекарств: ${groupMedicines.size}")
            
            groupMedicines.forEach { medicine ->
                report.appendLine("    ${medicine.name}: ID=${medicine.groupId}, порядок=${medicine.groupOrder}, дата=${medicine.groupStartDate}")
            }
            report.appendLine()
        }
        
        return report.toString()
    }
} 