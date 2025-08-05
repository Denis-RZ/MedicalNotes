package com.medicalnotes.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

@RunWith(RobolectricTestRunner::class)
class LanguageChangeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Очищаем настройки языка перед каждым тестом
        LanguageManager.clearLanguageSettings(context)
    }

    @Test
    fun `test REAL PROBLEM - Language settings are saved but UI doesn't change`() {
        // ПРОБЛЕМА: Настройки сохраняются, но UI не меняется
        
        // 1. Устанавливаем русский язык
        val success = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language setting success: $success")
        
        // 2. Проверяем, что настройка сохранилась
        val currentLanguage = LanguageManager.getCurrentLanguage(context)
        println("✓ Current language from settings: ${currentLanguage.displayName}")
        
        // 3. ПРОБЛЕМА: Применяем язык к контексту
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 4. Проверяем, что контекст действительно имеет русскую локаль
        val russianLocale = russianContext.resources.configuration.locale
        println("✓ Context locale: ${russianLocale.language}")
        
        // 5. ПРОБЛЕМА: Получаем строки из контекста
        val appNameRussian = russianContext.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ App name from Russian context: '$appNameRussian'")
        
        // 6. ПРОБЛЕМА: Получаем строки из оригинального контекста (это то, что видит пользователь)
        val appNameOriginal = context.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ App name from original context: '$appNameOriginal'")
        
        // 7. ПРОБЛЕМА: Сравниваем - они должны быть разными, но могут быть одинаковыми!
        println("⚠️  PROBLEM: Are strings different? ${appNameRussian != appNameOriginal}")
        
        if (appNameRussian == appNameOriginal) {
            println("❌ CRITICAL PROBLEM: UI strings are NOT changing despite language setting!")
            println("   This means the user will NOT see the language change in the interface.")
        } else {
            println("✅ SUCCESS: UI strings are changing correctly.")
        }
        
        // 8. Дополнительная проверка - тестируем несколько строк
        val settingsRussian = russianContext.getString(com.medicalnotes.app.R.string.settings)
        val settingsOriginal = context.getString(com.medicalnotes.app.R.string.settings)
        
        println("✓ Settings from Russian context: '$settingsRussian'")
        println("✓ Settings from original context: '$settingsOriginal'")
        println("⚠️  Settings different? ${settingsRussian != settingsOriginal}")
    }

    @Test
    fun `test REAL PROBLEM - BaseActivity language application`() {
        // ПРОБЛЕМА: BaseActivity должен применять язык, но может не работать правильно
        
        // 1. Устанавливаем английский язык
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 2. Создаем контекст с английским языком
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 3. Проверяем, что локаль установлена правильно
        val englishLocale = englishContext.resources.configuration.locale
        println("✓ English context locale: ${englishLocale.language}")
        
        // 4. ПРОБЛЕМА: Проверяем, что строки действительно английские
        val appNameEnglish = englishContext.getString(com.medicalnotes.app.R.string.app_name)
        println("✓ App name from English context: '$appNameEnglish'")
        
        // 5. Проверяем, что это не русский текст
        val isEnglish = appNameEnglish.contains("Medical") || appNameEnglish.contains("Notes")
        println("⚠️  Is text actually English? $isEnglish")
        
        if (!isEnglish) {
            println("❌ CRITICAL PROBLEM: English context is still showing Russian text!")
        }
    }

    @Test
    fun `test REAL PROBLEM - Configuration update`() {
        // ПРОБЛЕМА: Обновление конфигурации может не работать на всех устройствах
        
        // 1. Устанавливаем русский язык
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 2. Применяем язык
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 3. ПРОБЛЕМА: Проверяем, обновилась ли конфигурация в оригинальном контексте
        val originalConfig = context.resources.configuration
        val russianConfig = russianContext.resources.configuration
        
        println("✓ Original context locale: ${originalConfig.locale.language}")
        println("✓ Russian context locale: ${russianConfig.locale.language}")
        
        // 4. ПРОБЛЕМА: Если они разные, значит обновление конфигурации не сработало
        val configsMatch = originalConfig.locale.language == russianConfig.locale.language
        println("⚠️  Do configurations match? $configsMatch")
        
        if (!configsMatch) {
            println("❌ CRITICAL PROBLEM: Configuration update failed!")
            println("   The original context still has the old locale.")
            println("   This means UI elements will NOT change language.")
        }
    }

    @Test
    fun `test REAL PROBLEM - SharedPreferences persistence`() {
        // Проверяем, что настройки действительно сохраняются
        
        // 1. Устанавливаем русский язык
        val success = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language setting success: $success")
        
        // 2. Создаем новый контекст (симулируем перезапуск приложения)
        val newContext = RuntimeEnvironment.getApplication()
        
        // 3. Проверяем, что настройка сохранилась
        val currentLanguage = LanguageManager.getCurrentLanguage(newContext)
        println("✓ Language after 'restart': ${currentLanguage.displayName}")
        
        if (currentLanguage != LanguageManager.Language.RUSSIAN) {
            println("❌ CRITICAL PROBLEM: Language setting was lost after 'restart'!")
        } else {
            println("✅ SUCCESS: Language setting persisted correctly.")
        }
    }

    @Test
    fun `test REAL PROBLEM - String resource availability`() {
        // Проверяем, что все необходимые строки доступны в обоих языках
        
        val requiredStrings = listOf(
            com.medicalnotes.app.R.string.app_name,
            com.medicalnotes.app.R.string.settings,
            com.medicalnotes.app.R.string.add_medicine,
            com.medicalnotes.app.R.string.save_medicine,
            com.medicalnotes.app.R.string.cancel
        )
        
        // Тестируем английский
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        println("=== ENGLISH STRINGS ===")
        requiredStrings.forEach { stringId ->
            try {
                val text = englishContext.getString(stringId)
                println("✓ $stringId: '$text'")
                if (text.isEmpty()) {
                    println("❌ WARNING: Empty string for $stringId")
                }
            } catch (e: Exception) {
                println("❌ ERROR: Missing string $stringId: ${e.message}")
            }
        }
        
        // Тестируем русский
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        println("=== RUSSIAN STRINGS ===")
        requiredStrings.forEach { stringId ->
            try {
                val text = russianContext.getString(stringId)
                println("✓ $stringId: '$text'")
                if (text.isEmpty()) {
                    println("❌ WARNING: Empty string for $stringId")
                }
            } catch (e: Exception) {
                println("❌ ERROR: Missing string $stringId: ${e.message}")
            }
        }
    }
} 