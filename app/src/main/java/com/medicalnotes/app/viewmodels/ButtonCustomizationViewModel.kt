package com.medicalnotes.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medicalnotes.app.models.ButtonColor
import com.medicalnotes.app.models.ButtonSize
import com.medicalnotes.app.models.UserPreferences
import com.medicalnotes.app.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

class ButtonCustomizationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userPreferencesRepository = UserPreferencesRepository(application)
    
    private val _userPreferences = MutableLiveData<UserPreferences>()
    val userPreferences: LiveData<UserPreferences> = _userPreferences
    
    init {
        loadPreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            val preferences = userPreferencesRepository.getUserPreferences()
            _userPreferences.value = preferences
        }
    }
    
    fun updatePreferences(
        buttonSize: ButtonSize,
        buttonColor: ButtonColor,
        useHighContrast: Boolean,
        enableVibration: Boolean,
        enableSound: Boolean
    ) {
        viewModelScope.launch {
            val currentPreferences = _userPreferences.value ?: UserPreferences()
            val updatedPreferences = currentPreferences.copy(
                buttonSize = buttonSize,
                buttonColor = buttonColor,
                useHighContrast = useHighContrast,
                enableVibration = enableVibration,
                enableSound = enableSound,
                updatedAt = System.currentTimeMillis()
            )
            
            userPreferencesRepository.updateUserPreferences(updatedPreferences)
            _userPreferences.value = updatedPreferences
        }
    }
} 