package com.medicalnotes.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import java.util.*

object LanguageManager {
    
    private const val PREF_NAME = "language_preferences"
    private const val KEY_LANGUAGE = "selected_language"
    
    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        RUSSIAN("ru", "Русский")
    }
    
    /**
     * Получить текущий язык из настроек
     */
    fun getCurrentLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val languageCode = prefs.getString(KEY_LANGUAGE, null)
        
        Log.d("LanguageManager", "getCurrentLanguage: SharedPreferences value = '$languageCode'")
        
        val language = if (languageCode != null) {
            val foundLanguage = Language.values().find { it.code == languageCode }
            Log.d("LanguageManager", "getCurrentLanguage: Found language = ${foundLanguage?.displayName}")
            foundLanguage ?: Language.ENGLISH
        } else {
            val systemLanguage = getSystemLanguage()
            Log.d("LanguageManager", "getCurrentLanguage: Using system language = ${systemLanguage.displayName}")
            systemLanguage
        }
        
        Log.d("LanguageManager", "getCurrentLanguage: Final result = ${language.displayName} (${language.code})")
        return language
    }
    
    /**
     * Установить язык приложения
     */
    fun setLanguage(context: Context, language: Language): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(KEY_LANGUAGE, language.code)
            val success = editor.commit() // Используем commit() вместо apply() для немедленного сохранения
            
            Log.d("LanguageManager", "setLanguage: Setting language to ${language.displayName} (${language.code}), success = $success")
            
            // Проверяем, что значение действительно сохранилось
            val savedValue = prefs.getString(KEY_LANGUAGE, null)
            Log.d("LanguageManager", "setLanguage: Verification - saved value = '$savedValue'")
            
            return success
        } catch (e: Exception) {
            Log.e("LanguageManager", "Error setting language", e)
            return false
        }
    }
    
    /**
     * Применить язык к контексту
     */
    fun applyLanguage(context: Context, language: Language): Context {
        Log.d("LanguageManager", "applyLanguage: Applying ${language.displayName} to context")
        
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        Log.d("LanguageManager", "applyLanguage: Created configuration with locale = ${config.locale}")
        
        // ПРОБЛЕМА: Обновляем ресурсы в основном контексте
        try {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            Log.d("LanguageManager", "applyLanguage: Successfully updated original context configuration")
        } catch (e: Exception) {
            Log.w("LanguageManager", "Could not update original context configuration", e)
        }
        
        // ВОЗВРАЩАЕМ ОРИГИНАЛЬНЫЙ КОНТЕКСТ с обновленной конфигурацией
        // Это важно, потому что UI использует именно оригинальный контекст
        return context
    }
    
    /**
     * Получить системный язык
     */
    private fun getSystemLanguage(): Language {
        val systemLocale = Locale.getDefault()
        Log.d("LanguageManager", "getSystemLanguage: System locale = ${systemLocale.language}")
        return when (systemLocale.language) {
            "ru" -> Language.RUSSIAN
            else -> Language.ENGLISH
        }
    }
    
    /**
     * Получить список доступных языков
     */
    fun getAvailableLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    /**
     * Проверить, нужно ли перезапускать приложение
     */
    fun needsRestart(context: Context, newLanguage: Language): Boolean {
        val currentLanguage = getCurrentLanguage(context)
        val needsRestart = currentLanguage != newLanguage
        Log.d("LanguageManager", "needsRestart: current=${currentLanguage.displayName}, new=${newLanguage.displayName}, needsRestart=$needsRestart")
        return needsRestart
    }
    
    /**
     * Получить отображаемое имя языка
     */
    fun getLanguageDisplayName(context: Context, language: Language): String {
        return when (language) {
            Language.ENGLISH -> context.getString(com.medicalnotes.app.R.string.english)
            Language.RUSSIAN -> context.getString(com.medicalnotes.app.R.string.russian)
        }
    }
    
    /**
     * Получить отображаемое имя текущего языка
     */
    fun getCurrentLanguageDisplayName(context: Context): String {
        val currentLanguage = getCurrentLanguage(context)
        return getLanguageDisplayName(context, currentLanguage)
    }
    
    /**
     * Очистить настройки языка (для отладки)
     */
    fun clearLanguageSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d("LanguageManager", "clearLanguageSettings: Language settings cleared")
    }
} 