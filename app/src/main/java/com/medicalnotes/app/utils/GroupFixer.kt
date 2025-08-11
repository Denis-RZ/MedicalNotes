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
        
        val fixedMedicines = medicines.toMutableList()
        var changesMade = false

        // Группируем лекарства по groupName
        val groupedMedicines = medicines.filter { it.groupName != null }
            .groupBy { it.groupName }

        Log.d("GroupFixer", "Найдено групп: ${groupedMedicines.size}")

        for ((groupName, groupMedicines) in groupedMedicines) {
            if (groupMedicines.size > 1) {
                Log.d("GroupFixer", "Обрабатываем группу '$groupName': ${groupMedicines.size} лекарств")

                // Проверяем, есть ли несогласованности
                val firstMedicine = groupMedicines.first()
                val commonGroupId = firstMedicine.groupId ?: System.currentTimeMillis()
                val commonGroupStartDate = firstMedicine.groupStartDate

                Log.d("GroupFixer", "Базовые значения для группы '$groupName':")
                Log.d("GroupFixer", "  - groupId: $commonGroupId")
                Log.d("GroupFixer", "  - groupStartDate: $commonGroupStartDate")

                // Проверяем каждое лекарство в группе
                for (medicine in groupMedicines) {
                    val needsUpdate = medicine.groupId != commonGroupId || 
                                    medicine.groupStartDate != commonGroupStartDate

                    if (needsUpdate) {
                        Log.d("GroupFixer", "ИСПРАВЛЯЕМ: ${medicine.name}")
                        Log.d("GroupFixer", "  - groupId: ${medicine.groupId} -> $commonGroupId")
                        Log.d("GroupFixer", "  - groupStartDate: ${medicine.groupStartDate} -> $commonGroupStartDate")

                        val updatedMedicine = medicine.copy(
                            groupId = commonGroupId,
                            groupStartDate = commonGroupStartDate
                        )

                        // Находим индекс лекарства для обновления
                        val index = if (medicine.id > 0) {
                            fixedMedicines.indexOfFirst { it.id == medicine.id }
                        } else {
                            // Для лекарств с id = 0 ищем по имени, времени и дозировке
                            fixedMedicines.indexOfFirst {
                                it.name == medicine.name && 
                                it.time == medicine.time && 
                                it.dosage == medicine.dosage
                            }
                        }

                        if (index != -1) {
                            fixedMedicines[index] = updatedMedicine
                            changesMade = true
                            Log.d("GroupFixer", "✅ Исправлено: ${medicine.name}")
                        } else {
                            Log.e("GroupFixer", "❌ Не удалось найти лекарство для обновления: ${medicine.name}")
                        }
                    } else {
                        Log.d("GroupFixer", "✓ Уже корректно: ${medicine.name}")
                    }
                }
            }
        }

        if (changesMade) {
            Log.d("GroupFixer", "=== ИЗМЕНЕНИЯ ОБНАРУЖЕНЫ ===")
            Log.d("GroupFixer", "Исправленные лекарства:")
            fixedMedicines.forEach { medicine ->
                if (medicine.groupName != null) {
                    Log.d("GroupFixer", "  - ${medicine.name}: groupId=${medicine.groupId}, groupStartDate=${medicine.groupStartDate}")
                }
            }
        } else {
            Log.d("GroupFixer", "=== ИЗМЕНЕНИЙ НЕ ОБНАРУЖЕНО ===")
        }

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