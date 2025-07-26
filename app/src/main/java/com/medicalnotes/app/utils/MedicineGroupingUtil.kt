package com.medicalnotes.app.utils

import com.medicalnotes.app.models.Medicine
import java.time.LocalTime

/**
 * Утилита для группировки лекарств
 */
object MedicineGroupingUtil {

    /**
     * Группирует лекарства по названию группы
     */
    fun groupMedicinesByName(medicines: List<Medicine>): Map<String, List<Medicine>> {
        return medicines
            .filter { it.groupName.isNotEmpty() }
            .groupBy { it.groupName }
            .mapValues { (_, groupMedicines) ->
                groupMedicines.sortedBy { it.groupOrder }
            }
    }

    /**
     * Группирует лекарства по времени приема
     */
    fun groupMedicinesByTime(medicines: List<Medicine>): Map<LocalTime, List<Medicine>> {
        return medicines.groupBy { it.time }
    }

    /**
     * Проверяет валидность группы
     */
    fun validateGroup(groupName: String, medicines: List<Medicine>): ValidationResult {
        if (groupName.isEmpty()) {
            return ValidationResult(false, "Название группы не может быть пустым")
        }

        if (medicines.isEmpty()) {
            return ValidationResult(false, "Группа должна содержать хотя бы одно лекарство")
        }

        // Проверяем порядок
        val orders = medicines.map { it.groupOrder }.sorted()
        val expectedOrders = (1..medicines.size).toList()
        
        if (orders != expectedOrders) {
            return ValidationResult(false, "Нарушена последовательность порядка в группе")
        }

        // Проверяем дубликаты порядка
        val duplicates = orders.groupBy { it }.filter { it.value.size > 1 }
        if (duplicates.isNotEmpty()) {
            return ValidationResult(false, "Найдены дубликаты порядка: ${duplicates.keys}")
        }

        return ValidationResult(true, "Группа валидна")
    }

    /**
     * Создает новую группу
     */
    fun createGroup(groupName: String, medicines: List<Medicine>): List<Medicine> {
        return medicines.mapIndexed { index, medicine ->
            medicine.copy(
                groupName = groupName,
                groupOrder = index + 1
            )
        }
    }

    /**
     * Добавляет лекарство в группу
     */
    fun addMedicineToGroup(
        medicine: Medicine,
        groupName: String,
        existingMedicines: List<Medicine>
    ): Medicine {
        val groupMedicines = existingMedicines.filter { it.groupName == groupName }
        val newOrder = groupMedicines.size + 1
        
        return medicine.copy(
            groupName = groupName,
            groupOrder = newOrder
        )
    }

    /**
     * Удаляет лекарство из группы
     */
    fun removeMedicineFromGroup(medicine: Medicine): Medicine {
        return medicine.copy(
            groupName = "",
            groupOrder = 0
        )
    }

    /**
     * Изменяет порядок лекарства в группе
     */
    fun changeMedicineOrder(
        medicine: Medicine,
        newOrder: Int,
        groupMedicines: List<Medicine>
    ): List<Medicine> {
        if (newOrder < 1 || newOrder > groupMedicines.size) {
            return groupMedicines
        }

        val currentOrder = medicine.groupOrder
        val updatedMedicines = groupMedicines.toMutableList()

        // Обновляем порядок выбранного лекарства
        val medicineIndex = updatedMedicines.indexOfFirst { it.id == medicine.id }
        if (medicineIndex != -1) {
            updatedMedicines[medicineIndex] = medicine.copy(groupOrder = newOrder)
        }

        // Пересчитываем порядок остальных лекарств
        updatedMedicines.forEach { med ->
            if (med.id != medicine.id) {
                val oldOrder = med.groupOrder
                val newMedOrder = when {
                    oldOrder < currentOrder && oldOrder >= newOrder -> oldOrder + 1
                    oldOrder > currentOrder && oldOrder <= newOrder -> oldOrder - 1
                    else -> oldOrder
                }
                
                val medIndex = updatedMedicines.indexOfFirst { it.id == med.id }
                if (medIndex != -1) {
                    updatedMedicines[medIndex] = med.copy(groupOrder = newMedOrder)
                }
            }
        }

        return updatedMedicines.sortedBy { it.groupOrder }
    }

    /**
     * Перемещает лекарство вверх в группе
     */
    fun moveMedicineUp(medicine: Medicine, groupMedicines: List<Medicine>): List<Medicine> {
        val currentOrder = medicine.groupOrder
        if (currentOrder <= 1) return groupMedicines
        
        return changeMedicineOrder(medicine, currentOrder - 1, groupMedicines)
    }

    /**
     * Перемещает лекарство вниз в группе
     */
    fun moveMedicineDown(medicine: Medicine, groupMedicines: List<Medicine>): List<Medicine> {
        val currentOrder = medicine.groupOrder
        if (currentOrder >= groupMedicines.size) return groupMedicines
        
        return changeMedicineOrder(medicine, currentOrder + 1, groupMedicines)
    }

    /**
     * Автоматически группирует лекарства по времени приема
     */
    fun autoGroupByTime(medicines: List<Medicine>): List<Medicine> {
        val timeGroups = groupMedicinesByTime(medicines)
        val updatedMedicines = mutableListOf<Medicine>()

        timeGroups.forEach { (time, timeMedicines) ->
            if (timeMedicines.size > 1) {
                // Создаем группу для лекарств в одно время
                val groupName = "Время ${time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
                
                timeMedicines.forEachIndexed { index, medicine ->
                    updatedMedicines.add(
                        medicine.copy(
                            groupName = groupName,
                            groupOrder = index + 1
                        )
                    )
                }
            } else {
                // Лекарство остается без группы
                updatedMedicines.add(timeMedicines.first())
            }
        }

        return updatedMedicines
    }

    /**
     * Получает статистику по группам
     */
    fun getGroupStatistics(medicines: List<Medicine>): GroupStatistics {
        val groupedMedicines = groupMedicinesByName(medicines)
        
        return GroupStatistics(
            totalGroups = groupedMedicines.size,
            totalMedicinesInGroups = groupedMedicines.values.sumOf { it.size },
            averageMedicinesPerGroup = if (groupedMedicines.isNotEmpty()) {
                groupedMedicines.values.sumOf { it.size }.toDouble() / groupedMedicines.size
            } else 0.0,
            largestGroup = groupedMedicines.maxByOrNull { it.value.size }?.key ?: "",
            largestGroupSize = groupedMedicines.maxOfOrNull { it.value.size } ?: 0
        )
    }

    /**
     * Проверяет, нужно ли показывать группированные карточки
     */
    fun shouldShowGroupedCards(medicines: List<Medicine>, date: java.time.LocalDate): Boolean {
        val activeMedicines = medicines.filter { medicine ->
            // Простая проверка активности лекарства
            medicine.remainingQuantity > 0
        }
        
        // Показываем группированные карточки, если есть несколько лекарств в одно время
        val timeGroups = activeMedicines.groupBy { it.time }
        return timeGroups.any { (_, medicinesInGroup) -> medicinesInGroup.size > 1 }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )

    data class GroupStatistics(
        val totalGroups: Int,
        val totalMedicinesInGroups: Int,
        val averageMedicinesPerGroup: Double,
        val largestGroup: String,
        val largestGroupSize: Int
    )
} 