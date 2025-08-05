package com.medicalnotes.app.utils

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SimpleLanguageTest {

    @Test
    fun testLanguageProblem() {
        val context = RuntimeEnvironment.getApplication()
        
        println("=== ТЕСТ ПРОБЛЕМЫ СМЕНЫ ЯЗЫКОВ ===")
        
        // 1. Очищаем настройки
        LanguageManager.clearLanguageSettings(context)
        
        // 2. Устанавливаем русский язык
        val success = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        println("Настройка языка: $success")
        
        // 3. Получаем текущий язык
        val currentLang = LanguageManager.getCurrentLanguage(context)
        println("Текущий язык: ${currentLang.displayName}")
        
        // 4. Применяем русский язык к контексту
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 5. Проверяем, что контексты имеют разные локали
        val russianLocale = russianContext.resources.configuration.locale
        val originalLocale = context.resources.configuration.locale
        
        println("Локаль русского контекста: ${russianLocale.language}")
        println("Локаль оригинального контекста: ${originalLocale.language}")
        
        // 6. Проверяем, разные ли локали
        val areDifferent = russianLocale.language != originalLocale.language
        println("Локали разные? $areDifferent")
        
        if (areDifferent) {
            println("✅ УСПЕХ: Локали меняются правильно!")
            println("   Это означает, что LanguageManager работает корректно.")
        } else {
            println("❌ ПРОБЛЕМА: Локали НЕ меняются!")
            println("   Это означает, что LanguageManager НЕ применяет язык к контексту.")
        }
        
        // 7. Дополнительная проверка - проверяем конфигурации
        val russianConfig = russianContext.resources.configuration
        val originalConfig = context.resources.configuration
        
        println("Конфигурация русского контекста: ${russianConfig.locale}")
        println("Конфигурация оригинального контекста: ${originalConfig.locale}")
        
        val configsMatch = russianConfig.locale.language == originalConfig.locale.language
        println("Конфигурации совпадают? $configsMatch")
        
        if (!configsMatch) {
            println("✅ УСПЕХ: Конфигурации разные - язык применяется!")
        } else {
            println("❌ ПРОБЛЕМА: Конфигурации одинаковые - язык НЕ применяется!")
        }
        
        println("=== КОНЕЦ ТЕСТА ===")
    }
} 