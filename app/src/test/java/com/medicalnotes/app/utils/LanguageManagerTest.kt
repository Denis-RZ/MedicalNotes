package com.medicalnotes.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanguageManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        sharedPreferences = context.getSharedPreferences("language_preferences", Context.MODE_PRIVATE)
        
        // Clear any existing language settings
        sharedPreferences.edit().clear().apply()
    }

    @Test
    fun `test getCurrentLanguage returns system language when no preference set`() {
        // Given: No language preference is set
        sharedPreferences.edit().clear().apply()
        
        // When: Getting current language
        val result = LanguageManager.getCurrentLanguage(context)
        
        // Then: Should return system language (either English or Russian)
        assert(result == LanguageManager.Language.ENGLISH || result == LanguageManager.Language.RUSSIAN)
    }

    @Test
    fun `test setLanguage saves preference correctly`() {
        // Given: Language to set
        val language = LanguageManager.Language.ENGLISH
        
        // When: Setting language
        val success = LanguageManager.setLanguage(context, language)
        
        // Then: Should return true and save preference
        assert(success)
        val savedLanguage = sharedPreferences.getString("selected_language", null)
        assert(savedLanguage == language.code)
    }

    @Test
    fun `test getCurrentLanguage returns saved preference`() {
        // Given: Language preference is saved
        val expectedLanguage = LanguageManager.Language.RUSSIAN
        sharedPreferences.edit().putString("selected_language", expectedLanguage.code).apply()
        
        // When: Getting current language
        val result = LanguageManager.getCurrentLanguage(context)
        
        // Then: Should return saved language
        assert(result == expectedLanguage)
    }

    @Test
    fun `test needsRestart returns true when language changes`() {
        // Given: Current language is English
        sharedPreferences.edit().putString("selected_language", LanguageManager.Language.ENGLISH.code).apply()
        
        // When: Checking if restart needed for Russian
        val needsRestart = LanguageManager.needsRestart(context, LanguageManager.Language.RUSSIAN)
        
        // Then: Should return true
        assert(needsRestart)
    }

    @Test
    fun `test needsRestart returns false when language is same`() {
        // Given: Current language is English
        sharedPreferences.edit().putString("selected_language", LanguageManager.Language.ENGLISH.code).apply()
        
        // When: Checking if restart needed for English
        val needsRestart = LanguageManager.needsRestart(context, LanguageManager.Language.ENGLISH)
        
        // Then: Should return false
        assert(!needsRestart)
    }

    @Test
    fun `test clearLanguageSettings removes all preferences`() {
        // Given: Language preference is set
        sharedPreferences.edit().putString("selected_language", LanguageManager.Language.RUSSIAN.code).apply()
        assert(sharedPreferences.getString("selected_language", null) != null)
        
        // When: Clearing language settings
        LanguageManager.clearLanguageSettings(context)
        
        // Then: Preferences should be cleared
        assert(sharedPreferences.getString("selected_language", null) == null)
    }

    @Test
    fun `test language codes are valid`() {
        // When: Checking all language codes
        val languages = LanguageManager.Language.values()
        
        // Then: All codes should be valid
        languages.forEach { language ->
            assert(language.code.isNotEmpty())
            assert(language.displayName.isNotEmpty())
        }
    }

    @Test
    fun `test getAvailableLanguages returns all languages`() {
        // When: Getting available languages
        val availableLanguages = LanguageManager.getAvailableLanguages()
        
        // Then: Should return all language options
        assert(availableLanguages.size == LanguageManager.Language.values().size)
        assert(availableLanguages.contains(LanguageManager.Language.ENGLISH))
        assert(availableLanguages.contains(LanguageManager.Language.RUSSIAN))
    }
} 