package com.medicalnotes.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.medicalnotes.app.databinding.ActivityLanguageBinding
import com.medicalnotes.app.utils.LanguageManager
import com.medicalnotes.app.utils.LanguageChangeManager
import com.medicalnotes.app.viewmodels.SettingsViewModel

class LanguageActivity : BaseActivity() {
    
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var viewModel: SettingsViewModel
    private var currentLanguage: LanguageManager.Language = LanguageManager.Language.ENGLISH
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настройка обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEnabled) {
                    finish()
                }
            }
        })
        
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[SettingsViewModel::class.java]
        
        setupViews()
        setupListeners()
        loadCurrentLanguage()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.language_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настройка спиннера языков
        setupLanguageSpinner()
    }
    
    private fun setupLanguageSpinner() {
        val languages = LanguageManager.getAvailableLanguages()
        val languageNames = languages.map { language ->
            LanguageManager.getLanguageDisplayName(this, language)
        }
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter
    }
    
    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            saveLanguage()
        }
        
        binding.buttonCancel.setOnClickListener {
            finish()
        }
        
        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val languages = LanguageManager.getAvailableLanguages()
                if (position < languages.size) {
                    currentLanguage = languages[position]
                    updateLanguagePreview()
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Ничего не делаем
            }
        }
    }
    
    private fun loadCurrentLanguage() {
        currentLanguage = LanguageManager.getCurrentLanguage(this)
        val languages = LanguageManager.getAvailableLanguages()
        val currentIndex = languages.indexOf(currentLanguage)
        
        if (currentIndex >= 0) {
            binding.spinnerLanguage.setSelection(currentIndex)
        }
        
        updateLanguagePreview()
    }
    
    private fun updateLanguagePreview() {
        val previewText = when (currentLanguage) {
            LanguageManager.Language.ENGLISH -> {
                "Preview:\n" +
                "• Today's Medicines\n" +
                "• Add Medicine\n" +
                "• Settings\n" +
                "• Save\n" +
                "• Cancel"
            }
            LanguageManager.Language.RUSSIAN -> {
                "Предварительный просмотр:\n" +
                "• Лекарства на сегодня\n" +
                "• Добавить лекарство\n" +
                "• Настройки\n" +
                "• Сохранить\n" +
                "• Отмена"
            }
        }
        
        binding.textPreview.text = previewText
    }
    
    private fun saveLanguage() {
        val previousLanguage = LanguageManager.getCurrentLanguage(this)
        
        if (currentLanguage == previousLanguage) {
            Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Используем новый менеджер для смены языка
        val success = LanguageChangeManager.changeLanguage(this, currentLanguage)
        
        if (success) {
            // Обновляем UserPreferences
            viewModel.updateUserPreferences(language = currentLanguage)
            
            // Показываем диалог о необходимости перезапуска
            showRestartDialog()
        } else {
            Toast.makeText(this, "Error saving language", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.language_changed))
            .setMessage(getString(R.string.restart_required))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Перезапускаем приложение
                restartApp()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun restartApp() {
        try {
            android.util.Log.d("LanguageActivity", "restartApp: Starting activity restart process")
            
            // ПРОСТОЕ РЕШЕНИЕ: Перезапускаем только MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            
            android.util.Log.d("LanguageActivity", "restartApp: Launching MainActivity with new language")
            startActivity(intent)
            
            // Завершаем текущую Activity
            android.util.Log.d("LanguageActivity", "restartApp: Finishing current activity")
            finish()
            
        } catch (e: Exception) {
            android.util.Log.e("LanguageActivity", "Error restarting activity", e)
            Toast.makeText(this, "Please restart the app manually", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            android.util.Log.d("LanguageActivity", "Updating UI after language change")
            
            // Обновляем заголовок Activity
            title = getString(R.string.language_settings)
            supportActionBar?.title = getString(R.string.language_settings)
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            // Обновляем предварительный просмотр
            updateLanguagePreview()
            
            // Обновляем спиннер языков
            updateLanguageSpinner()
            
            android.util.Log.d("LanguageActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            android.util.Log.e("LanguageActivity", "Error updating UI after language change", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок
            binding.buttonSave.text = getString(R.string.save)
            binding.buttonCancel.text = getString(R.string.cancel)
            
        } catch (e: Exception) {
            android.util.Log.e("LanguageActivity", "Error updating button texts", e)
        }
    }
    
    private fun updateLanguageSpinner() {
        try {
            // Обновляем названия языков в спиннере
            val languages = LanguageManager.getAvailableLanguages()
            val languageNames = languages.map { language ->
                LanguageManager.getLanguageDisplayName(this, language)
            }
            
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                languageNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerLanguage.adapter = adapter
            
            // Восстанавливаем текущий выбор
            val currentIndex = languages.indexOf(currentLanguage)
            if (currentIndex >= 0) {
                binding.spinnerLanguage.setSelection(currentIndex)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LanguageActivity", "Error updating language spinner", e)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            finish()
        }
        return true
    }
} 