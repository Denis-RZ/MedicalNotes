package com.medicalnotes.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.medicalnotes.app.databinding.ActivityButtonCustomizationBinding
import com.medicalnotes.app.models.ButtonAction
import com.medicalnotes.app.models.ButtonColor
import com.medicalnotes.app.models.ButtonSize
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.viewmodels.ButtonCustomizationViewModel

class ButtonCustomizationActivity : BaseActivity() {
    
    private lateinit var binding: ActivityButtonCustomizationBinding
    private lateinit var viewModel: ButtonCustomizationViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настройка обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Логика обработки кнопки "Назад"
                if (isEnabled) {
                    finish()
                }
            }
        })
        
        binding = ActivityButtonCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[ButtonCustomizationViewModel::class.java]
        
        setupViews()
        setupListeners()
        observeData()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.button_customization)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настройка спиннеров
        setupSpinners()
    }
    
    private fun setupSpinners() {
        // Спиннер для размера кнопок
        val sizeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(getString(R.string.small), getString(R.string.medium), getString(R.string.large), getString(R.string.extra_large))
        )
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerButtonSize.adapter = sizeAdapter
        
        // Спиннер для цвета кнопок
        val colorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(getString(R.string.blue), getString(R.string.gray), getString(R.string.green), getString(R.string.orange), getString(R.string.red), getString(R.string.high_contrast))
        )
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerButtonColor.adapter = colorAdapter
    }
    
    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            saveSettings()
        }
        
        binding.buttonReset.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun observeData() {
        viewModel.userPreferences.observe(this) { preferences ->
            preferences?.let {
                // Устанавливаем текущие настройки
                binding.spinnerButtonSize.setSelection(it.buttonSize.ordinal)
                binding.spinnerButtonColor.setSelection(it.buttonColor.ordinal)
                binding.checkBoxHighContrast.isChecked = it.useHighContrast
                binding.checkBoxVibration.isChecked = it.enableVibration
                binding.checkBoxSound.isChecked = it.enableSound
            }
        }
    }
    
    private fun saveSettings() {
        val buttonSize = ButtonSize.values()[binding.spinnerButtonSize.selectedItemPosition]
        val buttonColor = ButtonColor.values()[binding.spinnerButtonColor.selectedItemPosition]
        val useHighContrast = binding.checkBoxHighContrast.isChecked
        val enableVibration = binding.checkBoxVibration.isChecked
        val enableSound = binding.checkBoxSound.isChecked
        
        viewModel.updatePreferences(
            buttonSize = buttonSize,
            buttonColor = buttonColor,
            useHighContrast = useHighContrast,
            enableVibration = enableVibration,
            enableSound = enableSound
        )
        
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun resetToDefaults() {
        binding.spinnerButtonSize.setSelection(ButtonSize.LARGE.ordinal)
        binding.spinnerButtonColor.setSelection(ButtonColor.PRIMARY.ordinal)
        binding.checkBoxHighContrast.isChecked = false
        binding.checkBoxVibration.isChecked = true
        binding.checkBoxSound.isChecked = true
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 