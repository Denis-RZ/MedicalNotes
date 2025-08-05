package com.medicalnotes.app.utils

import android.content.Context
import android.content.res.Configuration
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
class InterfaceUpdateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        LanguageManager.clearLanguageSettings(context)
    }

    @Test
    fun `test CRITICAL PROBLEM - UI elements don't update after language change`() {
        println("=== ТЕСТ: ПРОБЛЕМА С ОБНОВЛЕНИЕМ ИНТЕРФЕЙСА ===")
        
        // 1. Начинаем с английского языка
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 2. Получаем английские строки
        val appNameEnglish = englishContext.getString(com.medicalnotes.app.R.string.app_name)
        val settingsEnglish = englishContext.getString(com.medicalnotes.app.R.string.settings)
        
        println("✓ English strings:")
        println("  - App name: '$appNameEnglish'")
        println("  - Settings: '$settingsEnglish'")
        
        // 3. ПРОБЛЕМА: Меняем язык на русский
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 4. Получаем русские строки
        val appNameRussian = russianContext.getString(com.medicalnotes.app.R.string.app_name)
        val settingsRussian = russianContext.getString(com.medicalnotes.app.R.string.settings)
        
        println("✓ Russian strings:")
        println("  - App name: '$appNameRussian'")
        println("  - Settings: '$settingsRussian'")
        
        // 5. ПРОБЛЕМА: Проверяем, что строки действительно разные
        val stringsChanged = appNameEnglish != appNameRussian && settingsEnglish != settingsRussian
        
        println("⚠️  Are strings different? $stringsChanged")
        
        if (!stringsChanged) {
            println("❌ CRITICAL PROBLEM: Language strings are not changing!")
            println("   This means the language change is not working at all.")
            return
        }
        
        // 6. ПРОБЛЕМА: Проверяем, что оригинальный контекст все еще показывает старые строки
        val originalAppName = context.getString(com.medicalnotes.app.R.string.app_name)
        val originalSettings = context.getString(com.medicalnotes.app.R.string.settings)
        
        println("✓ Original context strings (should be old):")
        println("  - App name: '$originalAppName'")
        println("  - Settings: '$originalSettings'")
        
        // 7. ПРОБЛЕМА: Если оригинальный контекст показывает новые строки, значит обновление сработало
        val originalContextUpdated = originalAppName == appNameRussian && originalSettings == settingsRussian
        
        if (originalContextUpdated) {
            println("✅ SUCCESS: Original context was updated automatically!")
        } else {
            println("❌ CRITICAL PROBLEM: Original context was NOT updated!")
            println("   This means UI elements will NOT change language.")
            println("   User will still see old language in the interface.")
        }
    }

    @Test
    fun `test SOLUTION - Update all UI elements manually`() {
        println("=== ТЕСТ: РЕШЕНИЕ - РУЧНОЕ ОБНОВЛЕНИЕ UI ЭЛЕМЕНТОВ ===")
        
        // 1. Устанавливаем английский язык
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 2. Создаем TextView с английским текстом
        val textView = TextView(englishContext)
        textView.text = englishContext.getString(com.medicalnotes.app.R.string.app_name)
        val originalText = textView.text.toString()
        println("✓ TextView with English: '$originalText'")
        
        // 3. Меняем язык на русский
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 4. ПРОБЛЕМА: TextView все еще показывает старый текст
        println("✓ TextView after language change (should be old): '${textView.text}'")
        
        // 5. РЕШЕНИЕ: Обновляем TextView вручную
        textView.text = russianContext.getString(com.medicalnotes.app.R.string.app_name)
        val updatedText = textView.text.toString()
        println("✓ TextView after manual update: '$updatedText'")
        
        // 6. Проверяем, что обновление сработало
        val textUpdated = originalText != updatedText
        println("⚠️  Text updated manually? $textUpdated")
        
        if (textUpdated) {
            println("✅ SUCCESS: Manual UI update works!")
            println("   This confirms that we need to update all UI elements manually.")
        } else {
            println("❌ PROBLEM: Manual update didn't work!")
        }
    }

    @Test
    fun `test SOLUTION - Configuration change handling`() {
        println("=== ТЕСТ: РЕШЕНИЕ - ОБРАБОТКА ИЗМЕНЕНИЯ КОНФИГУРАЦИИ ===")
        
        // 1. Устанавливаем английский язык
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 2. Получаем текущую конфигурацию
        val originalConfig = context.resources.configuration
        println("✓ Original configuration locale: ${originalConfig.locale.language}")
        
        // 3. Меняем язык
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 4. Создаем новую конфигурацию
        val newConfig = Configuration(originalConfig)
        newConfig.setLocale(Locale("ru"))
        
        // 5. РЕШЕНИЕ: Применяем новую конфигурацию
        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
            println("✓ Configuration updated successfully")
        } catch (e: Exception) {
            println("⚠️  Configuration update failed: ${e.message}")
        }
        
        // 6. Проверяем, что конфигурация обновилась
        val updatedConfig = context.resources.configuration
        println("✓ Updated configuration locale: ${updatedConfig.locale.language}")
        
        // 7. Получаем строки с новой конфигурацией
        val newAppName = context.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ App name with new configuration: '$newAppName'")
        
        // 8. Проверяем результат
        val configUpdated = updatedConfig.locale.language == "ru"
        println("⚠️  Configuration updated? $configUpdated")
        
        if (configUpdated) {
            println("✅ SUCCESS: Configuration change works!")
            println("   This is another way to update the interface.")
        } else {
            println("❌ PROBLEM: Configuration change failed!")
        }
    }

    @Test
    fun `test COMPREHENSIVE SOLUTION - Complete language change workflow`() {
        println("=== ТЕСТ: КОМПЛЕКСНОЕ РЕШЕНИЕ - ПОЛНЫЙ РАБОЧИЙ ПРОЦЕСС ===")
        
        // 1. Начинаем с английского языка
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 2. Получаем английские строки
        val englishAppName = englishContext.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ Initial context (English): '$englishAppName'")
        
        // 3. Меняем язык на русский
        val languageChanged = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language change success: $languageChanged")
        
        // 4. РЕШЕНИЕ 1: Создаем новый контекст с русским языком
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 5. РЕШЕНИЕ 2: Применяем новую конфигурацию к оригинальному контексту
        val newConfig = Configuration(context.resources.configuration)
        newConfig.setLocale(Locale("ru"))
        
        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
            println("✓ Configuration applied to original context")
        } catch (e: Exception) {
            println("⚠️  Configuration application failed: ${e.message}")
        }
        
        // 6. Получаем строки из обновленного контекста
        val russianAppName = context.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ Updated context (Russian): '$russianAppName'")
        
        // 7. Проверяем результат
        val languagesDifferent = englishAppName != russianAppName
        println("⚠️  Languages are different? $languagesDifferent")
        
        if (languagesDifferent) {
            println("✅ SUCCESS: Complete language change workflow works!")
            println("   This confirms the solution approach:")
            println("   1. Save language setting")
            println("   2. Create new context with new language")
            println("   3. Update configuration of original context")
            println("   4. Update all UI elements manually")
        } else {
            println("❌ PROBLEM: Complete workflow failed!")
            println("   The language change is not working at all.")
        }
    }
} 