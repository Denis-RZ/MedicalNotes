package com.medicalnotes.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.medicalnotes.app.utils.LanguageManager

class LanguageTestActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем простой layout программно
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val titleText = TextView(this).apply {
            text = getString(R.string.language_settings)
            textSize = 20f
            setPadding(0, 0, 0, 30)
        }
        
        val currentLanguageText = TextView(this).apply {
            text = "Current Language: ${LanguageManager.getCurrentLanguage(this@LanguageTestActivity).displayName}"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }
        
        val testStringsText = TextView(this).apply {
            text = """
                Test Strings:
                • ${getString(R.string.app_name)}
                • ${getString(R.string.settings)}
                • ${getString(R.string.add_medicine)}
                • ${getString(R.string.save_medicine)}
                • ${getString(R.string.cancel)}
            """.trimIndent()
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        
        val switchToEnglishButton = Button(this).apply {
            text = "Switch to English"
            setOnClickListener {
                LanguageManager.setLanguage(this@LanguageTestActivity, LanguageManager.Language.ENGLISH)
                Toast.makeText(this@LanguageTestActivity, "Language set to English. Please restart app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        
        val switchToRussianButton = Button(this).apply {
            text = "Switch to Russian"
            setOnClickListener {
                LanguageManager.setLanguage(this@LanguageTestActivity, LanguageManager.Language.RUSSIAN)
                Toast.makeText(this@LanguageTestActivity, "Language set to Russian. Please restart app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        
        val clearSettingsButton = Button(this).apply {
            text = "Clear Language Settings"
            setOnClickListener {
                LanguageManager.clearLanguageSettings(this@LanguageTestActivity)
                Toast.makeText(this@LanguageTestActivity, "Language settings cleared. Please restart app.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        
        layout.addView(titleText)
        layout.addView(currentLanguageText)
        layout.addView(testStringsText)
        layout.addView(switchToEnglishButton)
        layout.addView(switchToRussianButton)
        layout.addView(clearSettingsButton)
        
        setContentView(layout)
    }
} 