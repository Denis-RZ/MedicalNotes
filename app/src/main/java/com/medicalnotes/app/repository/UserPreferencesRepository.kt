package com.medicalnotes.app.repository

import android.content.Context
import com.medicalnotes.app.models.UserPreferences
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserPreferencesRepository(private val context: Context) {
    
    private val dataManager = DataManager(context)
    
    suspend fun getUserPreferences(): UserPreferences = withContext(Dispatchers.IO) {
        return@withContext dataManager.loadUserPreferences()
    }
    
    suspend fun updateUserPreferences(preferences: UserPreferences) = withContext(Dispatchers.IO) {
        dataManager.updateUserPreferences(preferences)
    }
    
    suspend fun toggleElderlyMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        val currentPreferences = dataManager.loadUserPreferences()
        val updatedPreferences = currentPreferences.copy(
            isElderlyMode = enabled,
            updatedAt = System.currentTimeMillis()
        )
        dataManager.updateUserPreferences(updatedPreferences)
    }
    
    suspend fun toggleLargeText(enabled: Boolean) = withContext(Dispatchers.IO) {
        val currentPreferences = dataManager.loadUserPreferences()
        val updatedPreferences = currentPreferences.copy(
            largeTextEnabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        dataManager.updateUserPreferences(updatedPreferences)
    }
    
    suspend fun updateSonPhoneNumber(phoneNumber: String) = withContext(Dispatchers.IO) {
        val currentPreferences = dataManager.loadUserPreferences()
        val updatedPreferences = currentPreferences.copy(
            sonPhoneNumber = phoneNumber,
            updatedAt = System.currentTimeMillis()
        )
        dataManager.updateUserPreferences(updatedPreferences)
    }
    
    suspend fun updateEmergencyPhoneNumber(phoneNumber: String) = withContext(Dispatchers.IO) {
        val currentPreferences = dataManager.loadUserPreferences()
        val updatedPreferences = currentPreferences.copy(
            emergencyPhoneNumber = phoneNumber,
            updatedAt = System.currentTimeMillis()
        )
        dataManager.updateUserPreferences(updatedPreferences)
    }
} 