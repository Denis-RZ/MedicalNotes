package com.medicalnotes.app.utils

import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.Medicine
import java.time.LocalTime

object TestDataGenerator {
    
    /**
     * Генерирует тестовые лекарства с группами для проверки функционала
     */
    fun generateTestMedicinesWithGroups(): List<Medicine> {
        val medicines = mutableListOf<Medicine>()
        
        // Группа 1: "Витамины" - через день
        medicines.add(
            Medicine(
                id = 1L,
                name = "Витамин D",
                dosage = "1000 МЕ",
                quantity = 30,
                remainingQuantity = 25,
                medicineType = "Капсулы",
                time = LocalTime.of(8, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                groupName = "Витамины",
                groupOrder = 1,
                notes = "Принимать утром"
            )
        )
        
        medicines.add(
            Medicine(
                id = 2L,
                name = "Витамин C",
                dosage = "500 мг",
                quantity = 30,
                remainingQuantity = 20,
                medicineType = "Таблетки",
                time = LocalTime.of(8, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                groupName = "Витамины",
                groupOrder = 2,
                notes = "После еды"
            )
        )
        
        medicines.add(
            Medicine(
                id = 3L,
                name = "Витамин B12",
                dosage = "1000 мкг",
                quantity = 30,
                remainingQuantity = 15,
                medicineType = "Таблетки",
                time = LocalTime.of(8, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                groupName = "Витамины",
                groupOrder = 3,
                notes = "Под язык"
            )
        )
        
        // Группа 2: "Сердце" - через день
        medicines.add(
            Medicine(
                id = 4L,
                name = "Аспирин",
                dosage = "100 мг",
                quantity = 30,
                remainingQuantity = 28,
                medicineType = "Таблетки",
                time = LocalTime.of(9, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                groupName = "Сердце",
                groupOrder = 1,
                notes = "Для разжижения крови"
            )
        )
        
        medicines.add(
            Medicine(
                id = 5L,
                name = "Магний",
                dosage = "400 мг",
                quantity = 30,
                remainingQuantity = 22,
                medicineType = "Таблетки",
                time = LocalTime.of(9, 0),
                frequency = DosageFrequency.EVERY_OTHER_DAY,
                groupName = "Сердце",
                groupOrder = 2,
                notes = "Для сердца"
            )
        )
        
        // Лекарства без группы
        medicines.add(
            Medicine(
                id = 6L,
                name = "Метформин",
                dosage = "500 мг",
                quantity = 60,
                remainingQuantity = 45,
                medicineType = "Таблетки",
                time = LocalTime.of(8, 30),
                frequency = DosageFrequency.DAILY,
                notes = "От диабета"
            )
        )
        
        medicines.add(
            Medicine(
                id = 7L,
                name = "Инсулин",
                dosage = "10 единиц",
                quantity = 1,
                remainingQuantity = 1,
                medicineType = "Уколы",
                time = LocalTime.of(19, 0),
                frequency = DosageFrequency.DAILY,
                isInsulin = true,
                notes = "Перед ужином"
            )
        )
        
        return medicines
    }
    
    /**
     * Генерирует тестовые данные и сохраняет их
     */
    fun generateAndSaveTestData(dataManager: DataManager): Boolean {
        return try {
            val testMedicines = generateTestMedicinesWithGroups()
            dataManager.saveMedicines(testMedicines)
            true
        } catch (e: Exception) {
            android.util.Log.e("TestDataGenerator", "Ошибка сохранения тестовых данных", e)
            false
        }
    }
    
    /**
     * Проверяет функционал групп
     */
    fun testGroupFunctionality(dataManager: DataManager): String {
        val report = StringBuilder()
        report.append("=== ТЕСТ ФУНКЦИОНАЛА ГРУПП ===\n\n")
        
        try {
            // 1. Проверяем загрузку лекарств
            val medicines = dataManager.loadMedicines()
            report.append("1. Загружено лекарств: ${medicines.size}\n")
            
            // 2. Проверяем получение групп
            val groups = dataManager.getExistingGroups()
            report.append("2. Найдено групп: ${groups.size}\n")
            groups.forEach { group ->
                report.append("   - $group\n")
            }
            
            // 3. Проверяем лекарства в группах
            groups.forEach { groupName ->
                val groupMedicines = medicines.filter { it.groupName == groupName }
                report.append("3. Группа '$groupName': ${groupMedicines.size} лекарств\n")
                groupMedicines.sortedBy { it.groupOrder }.forEach { medicine ->
                    report.append("   - ${medicine.name} (№${medicine.groupOrder})\n")
                }
            }
            
            // 4. Проверяем лекарства без группы
            val ungroupedMedicines = medicines.filter { it.groupName.isEmpty() }
            report.append("4. Лекарств без группы: ${ungroupedMedicines.size}\n")
            ungroupedMedicines.forEach { medicine ->
                report.append("   - ${medicine.name}\n")
            }
            
            // 5. Проверяем логику группировки
            val today = java.time.LocalDate.now()
            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, today)
            report.append("5. Активных лекарств на сегодня: ${activeMedicines.size}\n")
            
            // 6. Проверяем статусы лекарств
            activeMedicines.forEach { medicine ->
                val status = DosageCalculator.getMedicineStatus(medicine, today)
                report.append("   - ${medicine.name}: $status")
                if (medicine.groupName.isNotEmpty()) {
                    report.append(" (${medicine.groupName}, №${medicine.groupOrder})")
                }
                report.append("\n")
            }
            
            report.append("\n=== ТЕСТ ЗАВЕРШЕН ===\n")
            
        } catch (e: Exception) {
            report.append("ОШИБКА: ${e.message}\n")
            android.util.Log.e("TestDataGenerator", "Ошибка тестирования групп", e)
        }
        
        return report.toString()
    }
} 