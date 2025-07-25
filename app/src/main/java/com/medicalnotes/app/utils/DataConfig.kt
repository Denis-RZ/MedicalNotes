package com.medicalnotes.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class DataConfig(
    val version: Int = 1,
    val lastModified: Long = System.currentTimeMillis(),
    val dataStructure: JsonObject = JsonObject(),
    val settings: AppSettings = AppSettings()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val CONFIG_FILE_NAME = "data_config.json"
    }
}

data class AppSettings(
    val autoBackup: Boolean = true,
    val backupInterval: Long = 24 * 60 * 60 * 1000, // 24 часа
    val maxBackups: Int = 7,
    val dataCompression: Boolean = false,
    val encryptionEnabled: Boolean = false,
    val notificationAdvanceMinutes: Int = 15,
    val lowStockThreshold: Int = 5,
    val emergencyContacts: List<String> = emptyList()
)

class ConfigManager(private val context: Context) {
    
    private val gson = Gson()
    private val configFile = File(context.filesDir, DataConfig.CONFIG_FILE_NAME)
    
    fun loadConfig(): DataConfig {
        return try {
            if (!configFile.exists()) {
                return createDefaultConfig()
            }
            
            val json = FileReader(configFile).readText()
            val config = gson.fromJson(json, DataConfig::class.java)
            
            // Проверяем версию и выполняем миграцию если нужно
            if (config.version < DataConfig.CURRENT_VERSION) {
                return migrateConfig(config)
            }
            
            config
        } catch (e: Exception) {
            android.util.Log.e("ConfigManager", "Error loading config", e)
            createDefaultConfig()
        }
    }
    
    fun saveConfig(config: DataConfig): Boolean {
        return try {
            val updatedConfig = config.copy(
                version = DataConfig.CURRENT_VERSION,
                lastModified = System.currentTimeMillis()
            )
            val json = gson.toJson(updatedConfig)
            FileWriter(configFile).use { it.write(json) }
            true
        } catch (e: Exception) {
            android.util.Log.e("ConfigManager", "Error saving config", e)
            false
        }
    }
    
    private fun createDefaultConfig(): DataConfig {
        val defaultConfig = DataConfig(
            version = DataConfig.CURRENT_VERSION,
            lastModified = System.currentTimeMillis(),
            settings = AppSettings()
        )
        saveConfig(defaultConfig)
        return defaultConfig
    }
    
    private fun migrateConfig(oldConfig: DataConfig): DataConfig {
        // Здесь можно добавить логику миграции при изменении версии
        android.util.Log.i("ConfigManager", "Migrating config from version ${oldConfig.version} to ${DataConfig.CURRENT_VERSION}")
        
        val migratedConfig = oldConfig.copy(
            version = DataConfig.CURRENT_VERSION,
            lastModified = System.currentTimeMillis()
        )
        
        saveConfig(migratedConfig)
        return migratedConfig
    }
    
    fun updateSettings(settings: AppSettings): Boolean {
        val currentConfig = loadConfig()
        val updatedConfig = currentConfig.copy(settings = settings)
        return saveConfig(updatedConfig)
    }
    
    fun getSettings(): AppSettings {
        return loadConfig().settings
    }
} 