package com.medicalnotes.app.utils

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.medicalnotes.app.models.Medicine
import java.util.*

/**
 * Утилита для динамической локализации данных лекарств
 * Решает проблему смешанного языка интерфейса при смене локали
 */
object DataLocalizationHelper {
    
    private const val TAG = "DataLocalizationHelper"
    
    /**
     * Локализует данные лекарства для текущего языка интерфейса
     */
    fun localizeMedicineData(medicine: Medicine, context: Context): Medicine {
        val currentLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        
        Log.d(TAG, "Локализация данных для языка: ${currentLocale.language}")
        
        return medicine.copy(
            dosage = localizeDosage(medicine.dosage, currentLocale, context),
            medicineType = localizeMedicineType(medicine.medicineType, currentLocale, context)
        )
    }
    
    /**
     * Локализует дозировку лекарства
     */
    private fun localizeDosage(dosage: String, locale: Locale, context: Context): String {
        if (dosage.isBlank()) return dosage
        
        return when (locale.language) {
            "en" -> {
                // Конвертируем русские дозировки в английские
                dosage.replace("таблетка", "tablet")
                    .replace("таблетки", "tablets")
                    .replace("таблеток", "tablets")
                    .replace("капля", "drop")
                    .replace("капли", "drops")
                    .replace("капель", "drops")
                    .replace("мл", "ml")
                    .replace("мг", "mg")
            }
            "ru" -> {
                // Конвертируем английские дозировки в русские
                dosage.replace("tablet", "таблетка")
                    .replace("tablets", "таблетки")
                    .replace("drop", "капля")
                    .replace("drops", "капли")
                    .replace("ml", "мл")
                    .replace("mg", "мг")
            }
            else -> dosage
        }
    }
    
    /**
     * Локализует тип лекарства
     */
    fun localizeMedicineType(medicineType: String, locale: Locale, context: Context): String {
        if (medicineType.isBlank()) {
            // Возвращаем локализованное значение по умолчанию
            return when (locale.language) {
                "en" -> "tablets"
                "ru" -> "таблетки"
                else -> "tablets"
            }
        }
        
        return when (locale.language) {
            "en" -> {
                // Конвертируем русские типы в английские
                when (medicineType.lowercase()) {
                    "таблетки", "таблетка" -> "tablets"
                    "капли", "капля" -> "drops"
                    "уколы", "укол" -> "injections"
                    "сироп", "сиропы" -> "syrup"
                    "мазь", "мази" -> "ointment"
                    "крем", "кремы" -> "cream"
                    "спрей", "спреи" -> "spray"
                    else -> medicineType.lowercase()
                }
            }
            "ru" -> {
                // Конвертируем английские типы в русские
                when (medicineType.lowercase()) {
                    "tablets", "tablet" -> "таблетки"
                    "drops", "drop" -> "капли"
                    "injections", "injection" -> "уколы"
                    "syrup" -> "сироп"
                    "ointment" -> "мазь"
                    "cream" -> "крем"
                    "spray" -> "спрей"
                    else -> medicineType
                }
            }
            else -> medicineType
        }
    }
    
    /**
     * Локализует список лекарств
     */
    fun localizeMedicinesList(medicines: List<Medicine>, context: Context): List<Medicine> {
        return medicines.map { localizeMedicineData(it, context) }
    }
    
    /**
     * Проверяет, нужна ли локализация данных
     */
    fun needsLocalization(medicine: Medicine, context: Context): Boolean {
        val currentLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        
        // Проверяем, содержит ли дозировка русские слова при английской локали
        if (currentLocale.language == "en") {
            val hasRussianDosage = medicine.dosage.contains("таблетка") || 
                                  medicine.dosage.contains("капля") ||
                                  medicine.dosage.contains("мл") ||
                                  medicine.dosage.contains("мг")
            
            val hasRussianType = medicine.medicineType.contains("таблетки") ||
                                medicine.medicineType.contains("капли")
            
            return hasRussianDosage || hasRussianType
        }
        
        // Проверяем, содержит ли дозировка английские слова при русской локали
        if (currentLocale.language == "ru") {
            val hasEnglishDosage = medicine.dosage.contains("tablet") || 
                                  medicine.dosage.contains("drop") ||
                                  medicine.dosage.contains("ml") ||
                                  medicine.dosage.contains("mg")
            
            val hasEnglishType = medicine.medicineType.contains("tablets") ||
                                medicine.medicineType.contains("drops")
            
            return hasEnglishDosage || hasEnglishType
        }
        
        return false
    }
    
    /**
     * Получает локализованное описание частоты приема
     */
    fun getLocalizedFrequencyDescription(frequency: com.medicalnotes.app.models.DosageFrequency, context: Context): String {
        return when (frequency) {
            com.medicalnotes.app.models.DosageFrequency.DAILY -> 
                context.getString(com.medicalnotes.app.R.string.frequency_daily)
            com.medicalnotes.app.models.DosageFrequency.EVERY_OTHER_DAY -> 
                context.getString(com.medicalnotes.app.R.string.frequency_every_other_day)
            com.medicalnotes.app.models.DosageFrequency.TWICE_A_WEEK -> 
                context.getString(com.medicalnotes.app.R.string.frequency_twice_a_week)
            com.medicalnotes.app.models.DosageFrequency.THREE_TIMES_A_WEEK -> 
                context.getString(com.medicalnotes.app.R.string.frequency_three_times_a_week)
            com.medicalnotes.app.models.DosageFrequency.WEEKLY -> 
                context.getString(com.medicalnotes.app.R.string.frequency_weekly)
            com.medicalnotes.app.models.DosageFrequency.CUSTOM -> 
                context.getString(com.medicalnotes.app.R.string.frequency_custom)
        }
    }
} 