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

class ButtonCustomizationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityButtonCustomizationBinding
    private lateinit var viewModel: ButtonCustomizationViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityButtonCustomizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[ButtonCustomizationViewModel::class.java]
        
        setupViews()
        setupListeners()
        observeData()
    }
    
    private fun setupViews() {
        supportActionBar?.title = "Настройка кнопок"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настройка спиннеров
        setupSpinners()
    }
    
    private fun setupSpinners() {
        // Спиннер для размера кнопок
        val sizeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Маленькие", "Средние", "Большие", "Очень большие")
        )
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerButtonSize.adapter = sizeAdapter
        
        // Спиннер для цвета кнопок
        val colorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("Синий", "Серый", "Зеленый", "Оранжевый", "Красный", "Высокий контраст")
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
        
        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
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