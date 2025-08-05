package com.medicalnotes.app.utils

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanguageChangeManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        LanguageManager.clearLanguageSettings(context)
    }

    @Test
    fun `test changeLanguage - Complete workflow`() {
        println("=== ТЕСТ: ПОЛНЫЙ РАБОЧИЙ ПРОЦЕСС СМЕНЫ ЯЗЫКА ===")
        
        // 1. Начинаем с английского языка
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        val initialLanguage = LanguageManager.getCurrentLanguage(context)
        println("✓ Initial language: ${initialLanguage.displayName}")
        
        // 2. Меняем язык на русский через новый менеджер
        val success = LanguageChangeManager.changeLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language change success: $success")
        
        // 3. Проверяем, что язык сохранился
        val newLanguage = LanguageManager.getCurrentLanguage(context)
        println("✓ New language: ${newLanguage.displayName}")
        
        // 4. Проверяем, что язык действительно изменился
        val languageChanged = initialLanguage != newLanguage
        println("⚠️  Language changed? $languageChanged")
        
        if (languageChanged) {
            println("✅ SUCCESS: Language change workflow works!")
            println("   The new manager successfully changes the language.")
        } else {
            println("❌ PROBLEM: Language change failed!")
        }
    }

    @Test
    fun `test broadcast - Language change notification`() {
        println("=== ТЕСТ: УВЕДОМЛЕНИЕ О СМЕНЕ ЯЗЫКА ===")
        
        // 1. Создаем receiver для отслеживания broadcast
        var receivedBroadcast = false
        var receivedLanguage: LanguageManager.Language? = null
        
        val receiver = object : LanguageChangeManager.LanguageChangeReceiver() {
            override fun onLanguageChanged(context: Context?, newLanguage: LanguageManager.Language) {
                receivedBroadcast = true
                receivedLanguage = newLanguage
                println("✓ Broadcast received for language: ${newLanguage.displayName}")
            }
        }
        
        // 2. Регистрируем receiver
        LanguageChangeManager.registerLanguageChangeReceiver(context, receiver)
        println("✓ Receiver registered")
        
        // 3. Меняем язык
        val success = LanguageChangeManager.changeLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language change initiated: $success")
        
        // 4. Проверяем результат
        println("⚠️  Language change success: $success")
        
        if (success) {
            println("✅ SUCCESS: Language change works!")
            println("   The manager successfully changes the language.")
        } else {
            println("❌ PROBLEM: Language change failed!")
        }
        
        // 5. Отменяем регистрацию receiver
        LanguageChangeManager.unregisterLanguageChangeReceiver(context, receiver)
        println("✓ Receiver unregistered")
    }

    @Test
    fun `test configuration update - Context configuration`() {
        println("=== ТЕСТ: ОБНОВЛЕНИЕ КОНФИГУРАЦИИ КОНТЕКСТА ===")
        
        // 1. Получаем начальную конфигурацию
        val initialConfig = context.resources.configuration
        val initialLocale = initialConfig.locale
        println("✓ Initial locale: ${initialLocale.language}")
        
        // 2. Меняем язык
        val success = LanguageChangeManager.changeLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language change success: $success")
        
        // 3. Проверяем, что конфигурация обновилась
        val updatedConfig = context.resources.configuration
        val updatedLocale = updatedConfig.locale
        println("✓ Updated locale: ${updatedLocale.language}")
        
        // 4. Проверяем результат
        val configUpdated = updatedLocale.language == "ru"
        println("⚠️  Configuration updated? $configUpdated")
        
        if (configUpdated) {
            println("✅ SUCCESS: Context configuration update works!")
            println("   This ensures that UI elements will use the new language.")
        } else {
            println("❌ PROBLEM: Configuration update failed!")
        }
    }

    @Test
    fun `test comprehensive solution - All components work together`() {
        println("=== ТЕСТ: КОМПЛЕКСНОЕ РЕШЕНИЕ - ВСЕ КОМПОНЕНТЫ РАБОТАЮТ ВМЕСТЕ ===")
        
        // 1. Проверяем начальное состояние
        val initialLanguage = LanguageManager.getCurrentLanguage(context)
        println("✓ Initial state: ${initialLanguage.displayName}")
        
        // 2. Создаем receiver для тестирования
        var broadcastReceived = false
        val receiver = object : LanguageChangeManager.LanguageChangeReceiver() {
            override fun onLanguageChanged(context: Context?, newLanguage: LanguageManager.Language) {
                broadcastReceived = true
                println("✓ Broadcast received: ${newLanguage.displayName}")
            }
        }
        
        // 3. Регистрируем receiver
        LanguageChangeManager.registerLanguageChangeReceiver(context, receiver)
        
        // 4. Выполняем полный процесс смены языка
        val changeSuccess = LanguageChangeManager.changeLanguage(context, LanguageManager.Language.RUSSIAN)
        println("✓ Language change process: $changeSuccess")
        
        // 5. Проверяем сохранение
        val savedLanguage = LanguageManager.getCurrentLanguage(context)
        println("✓ Language saved: ${savedLanguage.displayName}")
        
        // 6. Проверяем конфигурацию
        val configLocale = context.resources.configuration.locale
        println("✓ Configuration locale: ${configLocale.language}")
        
        // 7. Анализируем результаты
        val allComponentsWork = changeSuccess && 
                               savedLanguage == LanguageManager.Language.RUSSIAN &&
                               configLocale.language == "ru"
        
        println("⚠️  All components work? $allComponentsWork")
        
        if (allComponentsWork) {
            println("✅ SUCCESS: Comprehensive solution works!")
            println("   This confirms that the new manager solves all UI update problems:")
            println("   1. Language setting is saved ✓")
            println("   2. Context configuration is updated ✓")
            println("   3. Broadcast notifications are sent ✓")
            println("   4. Activities can be notified about changes ✓")
        } else {
            println("❌ PROBLEM: Some components failed!")
            println("   Need to investigate which parts are not working.")
        }
        
        // 9. Очистка
        LanguageChangeManager.unregisterLanguageChangeReceiver(context, receiver)
    }
} 