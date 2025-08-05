package com.medicalnotes.app

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.medicalnotes.app.utils.LanguageManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageUITest {

    @Before
    fun setUp() {
        // Clear language settings before each test
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        LanguageManager.clearLanguageSettings(context)
    }

    @Test
    fun testLanguageTestActivityDisplaysCorrectStrings() {
        // Launch LanguageTestActivity
        ActivityScenario.launch(LanguageTestActivity::class.java)
        
        // Check that the activity displays the correct title
        onView(withText("Language Settings"))
            .check(matches(isDisplayed()))
        
        // Check that test strings are displayed
        onView(withText(containsString("Medical Notes")))
            .check(matches(isDisplayed()))
        
        onView(withText(containsString("Settings")))
            .check(matches(isDisplayed()))
        
        onView(withText(containsString("Add Medicine")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLanguageSwitchingInTestActivity() {
        // Launch LanguageTestActivity
        ActivityScenario.launch(LanguageTestActivity::class.java)
        
        // Click "Switch to Russian" button
        onView(withText("Switch to Russian"))
            .perform(click())
        
        // Activity should finish after language change
        // We can't directly test the language change in the same activity
        // because it requires app restart, but we can verify the button works
    }

    @Test
    fun testSettingsActivityLanguageButton() {
        // Launch SettingsActivity
        ActivityScenario.launch(SettingsActivity::class.java)
        
        // Check that language test button is present
        onView(withText("Тест языка"))
            .check(matches(isDisplayed()))
        
        // Click the language test button
        onView(withText("Тест языка"))
            .perform(click())
        
        // Should navigate to LanguageTestActivity
        onView(withText("Language Settings"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLanguageActivityUI() {
        // Launch LanguageActivity
        ActivityScenario.launch(LanguageActivity::class.java)
        
        // Check that language selection spinner is present
        onView(withId(R.id.spinnerLanguage))
            .check(matches(isDisplayed()))
        
        // Check that preview text is displayed
        onView(withId(R.id.textPreview))
            .check(matches(isDisplayed()))
        
        // Check that save button is present
        onView(withId(R.id.buttonSave))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMainActivityDisplaysLocalizedStrings() {
        // Set language to English first
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        
        // Launch MainActivity
        ActivityScenario.launch(MainActivity::class.java)
        
        // Check that main activity displays English strings
        onView(withText("Medical Notes"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStringResourceAvailability() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test that all required string resources are available in English
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // These strings should be available
        val appName = englishContext.getString(R.string.app_name)
        val settings = englishContext.getString(R.string.settings)
        val addMedicine = englishContext.getString(R.string.add_medicine)
        val saveMedicine = englishContext.getString(R.string.save_medicine)
        val cancel = englishContext.getString(R.string.cancel)
        
        // Verify strings are not empty
        assert(appName.isNotEmpty())
        assert(settings.isNotEmpty())
        assert(addMedicine.isNotEmpty())
        assert(saveMedicine.isNotEmpty())
        assert(cancel.isNotEmpty())
        
        // Test that all required string resources are available in Russian
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        val appNameRu = russianContext.getString(R.string.app_name)
        val settingsRu = russianContext.getString(R.string.settings)
        val addMedicineRu = russianContext.getString(R.string.add_medicine)
        val saveMedicineRu = russianContext.getString(R.string.save_medicine)
        val cancelRu = russianContext.getString(R.string.cancel)
        
        // Verify strings are not empty
        assert(appNameRu.isNotEmpty())
        assert(settingsRu.isNotEmpty())
        assert(addMedicineRu.isNotEmpty())
        assert(saveMedicineRu.isNotEmpty())
        assert(cancelRu.isNotEmpty())
        
        // Verify that English and Russian strings are different
        assert(appName != appNameRu)
        assert(settings != settingsRu)
        assert(addMedicine != addMedicineRu)
        assert(saveMedicine != saveMedicineRu)
        assert(cancel != cancelRu)
    }

    @Test
    fun testLanguageManagerIntegration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test complete language switching flow
        // 1. Set language to Russian
        val success1 = LanguageManager.setLanguage(context, LanguageManager.Language.RUSSIAN)
        assert(success1)
        
        // 2. Verify language is set
        val currentLanguage1 = LanguageManager.getCurrentLanguage(context)
        assert(currentLanguage1 == LanguageManager.Language.RUSSIAN)
        
        // 3. Apply language to context
        val russianContext = LanguageManager.applyLanguage(context, LanguageManager.Language.RUSSIAN)
        
        // 4. Verify context has Russian locale
        val russianLocale = russianContext.resources.configuration.locales[0]
        assert(russianLocale.language == "ru")
        
        // 5. Set language to English
        val success2 = LanguageManager.setLanguage(context, LanguageManager.Language.ENGLISH)
        assert(success2)
        
        // 6. Verify language is set
        val currentLanguage2 = LanguageManager.getCurrentLanguage(context)
        assert(currentLanguage2 == LanguageManager.Language.ENGLISH)
        
        // 7. Apply language to context
        val englishContext = LanguageManager.applyLanguage(context, LanguageManager.Language.ENGLISH)
        
        // 8. Verify context has English locale
        val englishLocale = englishContext.resources.configuration.locales[0]
        assert(englishLocale.language == "en")
    }
} 