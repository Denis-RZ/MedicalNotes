package com.medicalnotes.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.DosageFrequency
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocalizationTest {

    @Test
    fun testLocalizationProblem() {
        println("=== ТЕСТ ПРОБЛЕМЫ ЛОКАЛИЗАЦИИ ===")
        
        // Создаем тестовое лекарство с русскими данными
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки", // Сохранено на русском
            time = LocalTime.of(12, 0),
            notes = "Тестовые заметки",
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = DosageFrequency.EVERY_OTHER_DAY, // "Через день"
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        println("Исходные данные лекарства:")
        println("  - Название: ${medicine.name}")
        println("  - Дозировка: ${medicine.dosage}")
        println("  - Тип лекарства: ${medicine.medicineType}")
        println("  - Частота: ${medicine.frequency}")
        println("  - Количество: ${medicine.remainingQuantity}")
        
        // Симулируем переключение языка на английский
        val context = RuntimeEnvironment.application
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        
        println("\nПосле переключения на английский язык:")
        println("  - Тип лекарства остается: ${medicine.medicineType} (должно быть 'tablets')")
        println("  - Дозировка остается: ${medicine.dosage} (должно быть '1 tablet')")
        
        // Проблема: данные сохраняются в файле на языке создания
        // При смене языка они не конвертируются автоматически
        
        println("\n=== ПРОБЛЕМА ===")
        println("1. Данные сохраняются в файле на языке создания (русский)")
        println("2. При смене языка интерфейса данные остаются на исходном языке")
        println("3. Пользователь видит смешанный интерфейс: английский + русские данные")
        
        println("\n=== ВОЗМОЖНЫЕ РЕШЕНИЯ ===")
        println("1. Конвертация данных при смене языка")
        println("2. Сохранение данных в нейтральном формате")
        println("3. Динамическая локализация при отображении")
        
        println("✅ Тест проблемы локализации завершен")
    }
    
    @Test
    fun testDosageDescriptionLocalization() {
        println("=== ТЕСТ ЛОКАЛИЗАЦИИ getDosageDescription ===")
        
        // Создаем тестовое лекарство
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            notes = "Тестовые заметки",
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val context = RuntimeEnvironment.application
        
        // Тестируем на русском языке
        val russianConfig = Configuration(context.resources.configuration)
        russianConfig.setLocale(Locale("ru"))
        val russianContext = context.createConfigurationContext(russianConfig)
        
        val russianDescription = DosageCalculator.getDosageDescription(medicine, russianContext)
        println("Русский: $russianDescription")
        
        // Тестируем на английском языке
        val englishConfig = Configuration(context.resources.configuration)
        englishConfig.setLocale(Locale.ENGLISH)
        val englishContext = context.createConfigurationContext(englishConfig)
        
        val englishDescription = DosageCalculator.getDosageDescription(medicine, englishContext)
        println("Английский: $englishDescription")
        
        // Проверяем, что локализация работает
        assert(russianDescription.contains("Через день")) { "Русское описание должно содержать 'Через день'" }
        assert(englishDescription.contains("Every other day")) { "Английское описание должно содержать 'Every other day'" }
        
        println("✅ Тест локализации getDosageDescription прошел успешно")
    }

    @Test
    fun testDataLocalizationHelper() {
        println("=== ТЕСТ DataLocalizationHelper ===")
        
        // Создаем тестовое лекарство с русскими данными
        val medicine = Medicine(
            id = System.currentTimeMillis(),
            name = "Тестовое лекарство",
            dosage = "1 таблетка",
            quantity = 50,
            remainingQuantity = 50,
            medicineType = "таблетки",
            time = LocalTime.of(12, 0),
            notes = "Тестовые заметки",
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = DosageFrequency.EVERY_OTHER_DAY,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val context = RuntimeEnvironment.application
        
        println("Исходные данные:")
        println("  - Дозировка: ${medicine.dosage}")
        println("  - Тип лекарства: ${medicine.medicineType}")
        
        // Симулируем английскую локализацию
        val englishConfig = Configuration(context.resources.configuration)
        englishConfig.setLocale(Locale.ENGLISH)
        val englishContext = context.createConfigurationContext(englishConfig)
        
        val localizedMedicine = DataLocalizationHelper.localizeMedicineData(medicine, englishContext)
        
        println("\nПосле локализации на английский:")
        println("  - Дозировка: ${localizedMedicine.dosage}")
        println("  - Тип лекарства: ${localizedMedicine.medicineType}")
        
        // Проверяем, что локализация работает
        assert(localizedMedicine.dosage.contains("tablet")) { "Дозировка должна содержать 'tablet'" }
        assert(localizedMedicine.medicineType == "tablets") { "Тип лекарства должен быть 'tablets'" }
        
        println("✅ Тест DataLocalizationHelper прошел успешно")
    }

    @Test
    fun testActivityTitlesLocalization() {
        println("=== ТЕСТ ЛОКАЛИЗАЦИИ ЗАГОЛОВКОВ ACTIVITY ===")
        
        val context = RuntimeEnvironment.application
        
        // Тестируем на русском языке
        val russianConfig = Configuration(context.resources.configuration)
        russianConfig.setLocale(Locale("ru"))
        val russianContext = context.createConfigurationContext(russianConfig)
        
        val russianTitle = russianContext.getString(com.medicalnotes.app.R.string.medicine_manager_title)
        println("Русский заголовок: $russianTitle")
        
        // Тестируем на английском языке
        val englishConfig = Configuration(context.resources.configuration)
        englishConfig.setLocale(Locale.ENGLISH)
        val englishContext = context.createConfigurationContext(englishConfig)
        
        val englishTitle = englishContext.getString(com.medicalnotes.app.R.string.medicine_manager_title)
        println("Английский заголовок: $englishTitle")
        
        // Проверяем, что локализация работает
        assert(russianTitle == "Управление лекарствами") { "Русский заголовок должен быть 'Управление лекарствами'" }
        assert(englishTitle == "Medicine Management") { "Английский заголовок должен быть 'Medicine Management'" }
        
        println("✅ Тест локализации заголовков Activity прошел успешно")
    }

    @Test
    fun testAppNameLocalization() {
        println("=== ТЕСТ ЛОКАЛИЗАЦИИ НАЗВАНИЯ ПРИЛОЖЕНИЯ ===")
        
        val context = RuntimeEnvironment.application
        
        // Тестируем на русском языке
        val russianConfig = Configuration(context.resources.configuration)
        russianConfig.setLocale(Locale("ru"))
        val russianContext = context.createConfigurationContext(russianConfig)
        
        val russianAppName = russianContext.getString(com.medicalnotes.app.R.string.app_name)
        println("Русское название приложения: $russianAppName")
        
        // Тестируем на английском языке
        val englishConfig = Configuration(context.resources.configuration)
        englishConfig.setLocale(Locale.ENGLISH)
        val englishContext = context.createConfigurationContext(englishConfig)
        
        val englishAppName = englishContext.getString(com.medicalnotes.app.R.string.app_name)
        println("Английское название приложения: $englishAppName")
        
        // Проверяем, что локализация работает
        assert(russianAppName == "Медицинские заметки") { "Русское название должно быть 'Медицинские заметки'" }
        assert(englishAppName == "Medical Notes") { "Английское название должно быть 'Medical Notes'" }
        
        println("✅ Тест локализации названия приложения прошел успешно")
    }
} 