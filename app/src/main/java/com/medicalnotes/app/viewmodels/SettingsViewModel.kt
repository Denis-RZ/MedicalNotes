package com.medicalnotes.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medicalnotes.app.utils.AppSettings
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataManager = DataManager(application)
    
    private val _settings = MutableLiveData<AppSettings>()
    val settings: LiveData<AppSettings> = _settings
    
    private val _dataStatistics = MutableLiveData<Map<String, Any>>()
    val dataStatistics: LiveData<Map<String, Any>> = _dataStatistics
    
    private val _backupList = MutableLiveData<List<File>>()
    val backupList: LiveData<List<File>> = _backupList
    
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    //  ДОБАВЛЕНО: LiveData для UserPreferences
    private val _userPreferences = MutableLiveData<com.medicalnotes.app.models.UserPreferences>()
    val userPreferences: LiveData<com.medicalnotes.app.models.UserPreferences> = _userPreferences
    
    init {
        loadSettings()
        loadUserPreferences()
        loadDataStatistics()
        loadBackupList()
    }
    
    fun loadSettings() {
        viewModelScope.launch {
            val currentSettings = dataManager.getSettings()
            _settings.value = currentSettings
        }
    }
    
    //  ДОБАВЛЕНО: Загрузка UserPreferences
    fun loadUserPreferences() {
        viewModelScope.launch {
            val preferences = dataManager.loadUserPreferences()
            _userPreferences.value = preferences
            android.util.Log.d("SettingsViewModel", "UserPreferences загружены: вибрация=${preferences.enableVibration}, звук=${preferences.enableSound}")
        }
    }
    
    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            val success = dataManager.updateSettings(settings)
            if (success) {
                _settings.value = settings
                _message.value = "Настройки сохранены"
            } else {
                _message.value = "Ошибка сохранения настроек"
            }
        }
    }
    
    //  ДОБАВЛЕНО: Обновление UserPreferences
    fun updateUserPreferences(
        enableVibration: Boolean? = null,
        enableSound: Boolean? = null
    ) {
        viewModelScope.launch {
            val currentPreferences = dataManager.loadUserPreferences()
            val updatedPreferences = currentPreferences.copy(
                enableVibration = enableVibration ?: currentPreferences.enableVibration,
                enableSound = enableSound ?: currentPreferences.enableSound,
                updatedAt = System.currentTimeMillis()
            )
            
            dataManager.updateUserPreferences(updatedPreferences)
            _userPreferences.value = updatedPreferences
            
            android.util.Log.d("SettingsViewModel", "UserPreferences обновлены: вибрация=${updatedPreferences.enableVibration}, звук=${updatedPreferences.enableSound}")
        }
    }
    
    fun loadDataStatistics() {
        viewModelScope.launch {
            val statistics = dataManager.getDataStatistics()
            _dataStatistics.value = statistics
        }
    }
    
    fun loadBackupList() {
        viewModelScope.launch {
            val backups = dataManager.getBackupList()
            _backupList.value = backups
        }
    }
    
    fun createBackup() {
        viewModelScope.launch {
            val backupPath = dataManager.createBackup()
            if (backupPath.isNotEmpty()) {
                _message.value = "Резервная копия создана: ${File(backupPath).name}"
                loadBackupList()
            } else {
                _message.value = "Ошибка создания резервной копии"
            }
        }
    }
    
    fun restoreFromBackup(backupPath: String) {
        viewModelScope.launch {
            val success = dataManager.restoreFromBackup(backupPath)
            if (success) {
                _message.value = "Данные восстановлены из резервной копии"
                loadDataStatistics()
            } else {
                _message.value = "Ошибка восстановления данных"
            }
        }
    }
    
    fun validateDataIntegrity() {
        viewModelScope.launch {
            val isValid = dataManager.validateDataIntegrity()
            if (isValid) {
                _message.value = "Целостность данных проверена успешно"
            } else {
                _message.value = "Обнаружены проблемы с данными"
            }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            // Очищаем все данные
            dataManager.saveMedicines(emptyList())
            dataManager.saveCustomButtons(emptyList())
            dataManager.saveUserPreferences(com.medicalnotes.app.models.UserPreferences())
            
            _message.value = "Все данные очищены"
            loadDataStatistics()
        }
    }
    
    fun cleanupOldBackups() {
        viewModelScope.launch {
            dataManager.cleanupOldBackups()
            loadBackupList()
            _message.value = "Старые резервные копии очищены"
        }
    }
    
    fun deleteBackup(backupFile: File) {
        viewModelScope.launch {
            val success = dataManager.getBackupList().find { it.absolutePath == backupFile.absolutePath }?.let { file ->
                file.delete()
            } ?: false
            
            if (success) {
                _message.value = "Резервная копия удалена"
                loadBackupList()
            } else {
                _message.value = "Ошибка удаления резервной копии"
            }
        }
    }
    
    fun getBackupInfo(backupFile: File): Map<String, Any>? {
        return dataManager.getBackupList().find { it.absolutePath == backupFile.absolutePath }?.let { file ->
            // Здесь можно добавить логику получения информации о бэкапе
            mapOf(
                "file_name" to file.name,
                "file_size" to file.length(),
                "last_modified" to file.lastModified()
            )
        }
    }
} 