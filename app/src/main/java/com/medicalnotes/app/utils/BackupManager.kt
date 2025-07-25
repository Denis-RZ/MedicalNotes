package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.UserPreferences
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_DIR = "backups"
        private const val BACKUP_PREFIX = "medical_notes_backup"
        private const val AUTO_BACKUP_PREFIX = "auto_backup"
    }
    
    private val gson = Gson()
    private val backupDir = File(context.filesDir, BACKUP_DIR)
    private val configManager = ConfigManager(context)
    
    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }
    
    fun createBackup(): String {
        return try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))
            
            val backupFileName = "${BACKUP_PREFIX}_${dateString}.json"
            val backupFile = File(backupDir, backupFileName)
            
            val backupData = createBackupData()
            val backupJson = gson.toJson(backupData)
            
            FileWriter(backupFile).use { it.write(backupJson) }
            
            Log.i(TAG, "Backup created: ${backupFile.absolutePath}")
            
            // Очищаем старые бэкапы
            cleanupOldBackups()
            
            backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            ""
        }
    }
    
    fun createAutoBackup(): String {
        return try {
            val timestamp = System.currentTimeMillis()
            val backupFileName = "${AUTO_BACKUP_PREFIX}_${timestamp}.json"
            val backupFile = File(backupDir, backupFileName)
            
            val backupData = createBackupData()
            val backupJson = gson.toJson(backupData)
            
            FileWriter(backupFile).use { it.write(backupJson) }
            
            Log.i(TAG, "Auto backup created: ${backupFile.name}")
            
            backupFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating auto backup", e)
            ""
        }
    }
    
    private fun createBackupData(): Map<String, Any> {
        val dataManager = DataManager(context)
        
        return mapOf(
            "version" to DataConfig.CURRENT_VERSION,
            "timestamp" to System.currentTimeMillis(),
            "medicines" to dataManager.loadMedicines(),
            "buttons" to dataManager.loadCustomButtons(),
            "preferences" to dataManager.loadUserPreferences(),
            "config" to configManager.loadConfig(),
            "metadata" to mapOf(
                "app_version" to "1.0",
                "backup_type" to "manual",
                "device_info" to android.os.Build.MODEL
            )
        )
    }
    
    fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file not found: $backupPath")
                return false
            }
            
            val json = FileReader(backupFile).readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData = gson.fromJson<Map<String, Any>>(json, type)
            
            val dataManager = DataManager(context)
            
            // Восстанавливаем данные
            if (backupData.containsKey("medicines")) {
                val medicinesJson = gson.toJson(backupData["medicines"])
                val medicines = gson.fromJson<List<Medicine>>(medicinesJson, 
                    object : TypeToken<List<Medicine>>() {}.type)
                dataManager.saveMedicines(medicines ?: emptyList())
            }
            
            if (backupData.containsKey("buttons")) {
                val buttonsJson = gson.toJson(backupData["buttons"])
                val buttons = gson.fromJson<List<CustomButton>>(buttonsJson,
                    object : TypeToken<List<CustomButton>>() {}.type)
                dataManager.saveCustomButtons(buttons ?: emptyList())
            }
            
            if (backupData.containsKey("preferences")) {
                val preferencesJson = gson.toJson(backupData["preferences"])
                val preferences = gson.fromJson(preferencesJson, UserPreferences::class.java)
                if (preferences != null) {
                    dataManager.saveUserPreferences(preferences)
                }
            }
            
            if (backupData.containsKey("config")) {
                val configJson = gson.toJson(backupData["config"])
                val config = gson.fromJson(configJson, DataConfig::class.java)
                if (config != null) {
                    configManager.saveConfig(config)
                }
            }
            
            Log.i(TAG, "Backup restored successfully from: $backupPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            false
        }
    }
    
    fun getBackupList(): List<File> {
        return try {
            if (!backupDir.exists()) {
                return emptyList()
            }
            
            backupDir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting backup list", e)
            emptyList()
        }
    }
    
    fun getBackupInfo(backupFile: File): Map<String, Any>? {
        return try {
            val json = FileReader(backupFile).readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData = gson.fromJson<Map<String, Any>>(json, type)
            
            val timestamp = backupData["timestamp"] as? Long ?: 0L
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            
            mapOf(
                "file_name" to backupFile.name,
                "file_size" to backupFile.length(),
                "timestamp" to timestamp,
                "date" to dateFormat.format(Date(timestamp)),
                "version" to (backupData["version"] as? Int ?: 0),
                "backup_type" to if (backupFile.name.startsWith(AUTO_BACKUP_PREFIX)) "auto" else "manual"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting backup info", e)
            null
        }
    }
    
    fun cleanupOldBackups() {
        try {
            val settings = configManager.getSettings()
            val maxBackups = settings.maxBackups
            
            val backupFiles = getBackupList()
            
            if (backupFiles.size > maxBackups) {
                val filesToDelete = backupFiles.drop(maxBackups)
                
                filesToDelete.forEach { file ->
                    if (file.delete()) {
                        Log.i(TAG, "Deleted old backup: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete backup: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old backups", e)
        }
    }
    
    fun deleteBackup(backupFile: File): Boolean {
        return try {
            if (backupFile.delete()) {
                Log.i(TAG, "Backup deleted: ${backupFile.name}")
                true
            } else {
                Log.w(TAG, "Failed to delete backup: ${backupFile.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            false
        }
    }
    
    fun validateBackup(backupFile: File): Boolean {
        return try {
            val json = FileReader(backupFile).readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData = gson.fromJson<Map<String, Any>>(json, type)
            
            // Проверяем наличие обязательных полей
            backupData.containsKey("version") &&
            backupData.containsKey("timestamp") &&
            backupData.containsKey("medicines") &&
            backupData.containsKey("buttons") &&
            backupData.containsKey("preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Backup validation failed", e)
            false
        }
    }
    
    fun getBackupStatistics(): Map<String, Any> {
        val backupFiles = getBackupList()
        val totalSize = backupFiles.sumOf { it.length() }
        val manualBackups = backupFiles.count { !it.name.startsWith(AUTO_BACKUP_PREFIX) }
        val autoBackups = backupFiles.count { it.name.startsWith(AUTO_BACKUP_PREFIX) }
        
        return mapOf(
            "total_backups" to backupFiles.size,
            "manual_backups" to manualBackups,
            "auto_backups" to autoBackups,
            "total_size_bytes" to totalSize,
            "total_size_mb" to (totalSize / (1024L * 1024L)),
            "oldest_backup" to (backupFiles.lastOrNull()?.lastModified() ?: 0L),
            "newest_backup" to (backupFiles.firstOrNull()?.lastModified() ?: 0L)
        )
    }
} 