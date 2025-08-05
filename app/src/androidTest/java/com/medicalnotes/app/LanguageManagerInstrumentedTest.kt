package com.medicalnotes.app

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.medicalnotes.app.utils.LanguageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharedPreferences = context.getSharedPreferences("language_preferences", Context.MODE_PRIVATE)
        
        // Clear any existing language settings
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun testLanguageManagerInitialization() {
        // Test that LanguageManager can be initialized without errors
        val currentLanguage = LanguageManager.getCurrentLanguage(context)
        assertNotNull("Current language should not be null", currentLanguage)
        assertTrue("Language should be either English or Russian", 
            currentLanguage == LanguageManager.Language.ENGLISH || 
            currentLanguage == LanguageManager.Language.RUSSIAN)
    }

    @Test
    fun testLanguageSettingAndRetrieval() {
        // Test setting English language
        val englishSuccess = LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        assertTrue("Setting English language should succeed", englishSuccess)
        
        val retrievedEnglish = LanguageManager.getCurrentLanguage(context)
        assertEquals("Retrieved language should be English", 
            LanguageManager.Language.ENGLISH, retrievedEnglish)

        // Test setting Russian language
        val russianSuccess = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        assertTrue("Setting Russian language should succeed", russianSuccess)
        
        val retrievedRussian = LanguageManager.getCurrentLanguage(context)
        assertEquals("Retrieved language should be Russian", 
            LanguageManager.Language.RUSSIAN, retrievedRussian)
    }

    @Test
    fun testLanguageApplication() {
        // Test applying English language
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        val englishLocale = englishContext.resources.configuration.locales[0]
        assertEquals("Applied locale should be English", "en", englishLocale.language)

        // Test applying Russian language
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        val russianLocale = russianContext.resources.configuration.locales[0]
        assertEquals("Applied locale should be Russian", "ru", russianLocale.language)
    }

    @Test
    fun testNeedsRestartLogic() {
        // Set initial language to English
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        
        // Test that switching to Russian requires restart
        val needsRestartForRussian = LanguageManager.needsRestart(context, LanguageManager.Language.RUSSIAN)
        assertTrue("Switching to Russian should require restart", needsRestartForRussian)
        
        // Test that staying on English doesn't require restart
        val needsRestartForEnglish = LanguageManager.needsRestart(context, LanguageManager.Language.ENGLISH)
        assertFalse("Staying on English should not require restart", needsRestartForEnglish)
    }

    @Test
    fun testClearLanguageSettings() {
        // Set a language preference
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        val beforeClear = LanguageManager.getCurrentLanguage(context)
        assertEquals("Language should be Russian before clear", 
            LanguageManager.Language.RUSSIAN, beforeClear)
        
        // Clear settings
        LanguageManager.clearLanguageSettings(context)
        
        // Should fall back to system language
        val afterClear = LanguageManager.getCurrentLanguage(context)
        assertTrue("After clear should be system language", 
            afterClear == LanguageManager.Language.ENGLISH || 
            afterClear == LanguageManager.Language.RUSSIAN)
    }

    @Test
    fun testStringResourceLocalization() {
        // Test that string resources are properly localized
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // Get app name in both languages
        val englishAppName = englishContext.getString(R.string.app_name)
        val russianAppName = russianContext.getString(R.string.app_name)
        
        // App names should be different (localized)
        assertNotEquals("App names should be different in different languages", 
            englishAppName, russianAppName)
        
        // Both should not be empty
        assertFalse("English app name should not be empty", englishAppName.isEmpty())
        assertFalse("Russian app name should not be empty", russianAppName.isEmpty())
    }

    @Test
    fun testLanguagePersistence() {
        // Set language preference
        LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // Create new context (simulating app restart)
        val newContext = ApplicationProvider.getApplicationContext<Context>()
        
        // Language preference should persist
        val persistedLanguage = LanguageManager.getCurrentLanguage(newContext)
        assertEquals("Language preference should persist across contexts", 
            LanguageManager.Language.RUSSIAN, persistedLanguage)
    }

    @Test
    fun testSystemLanguageDetection() {
        val systemLanguage = LanguageManager.getSystemLanguage()
        assertNotNull("System language should not be null", systemLanguage)
        assertTrue("System language should be valid", 
            systemLanguage in LanguageManager.Language.values())
    }
} 