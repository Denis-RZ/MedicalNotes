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

class DataExportManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DataExportManager"
        private const val EXPORT_DIR = "exports"
        private const val MEDICINES_FILE = "medicines.json"
        private const val BUTTONS_FILE = "custom_buttons.json"
        private const val PREFERENCES_FILE = "user_preferences.json"
    }
    
    private val gson = Gson()
    private val exportDir = File(context.getExternalFilesDir(null), EXPORT_DIR)
    
    init {
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
    }
    
    /**
     * Экспортирует все данные в папку на внешнем хранилище
     */
    fun exportAllData(): Boolean {
        return try {
            Log.d(TAG, "Starting data export...")
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFolder = File(exportDir, "backup_$timestamp")
            
            if (!exportFolder.exists()) {
                exportFolder.mkdirs()
            }
            
            // Экспортируем лекарства
            val medicinesExported = exportMedicines(exportFolder)
            Log.d(TAG, "Medicines exported: $medicinesExported")
            
            // Экспортируем кнопки
            val buttonsExported = exportCustomButtons(exportFolder)
            Log.d(TAG, "Buttons exported: $buttonsExported")
            
            // Экспортируем настройки
            val preferencesExported = exportUserPreferences(exportFolder)
            Log.d(TAG, "Preferences exported: $preferencesExported")
            
            // Создаем файл с информацией об экспорте
            createExportInfo(exportFolder, timestamp)
            
            Log.i(TAG, "Data export completed successfully to: ${exportFolder.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during data export", e)
            false
        }
    }
    
    /**
     * Импортирует данные из папки экспорта
     */
    fun importAllData(backupFolder: File): Boolean {
        return try {
            Log.d(TAG, "Starting data import from: ${backupFolder.absolutePath}")
            
            val dataManager = DataManager(context)
            
            // Импортируем лекарства
            val medicinesFile = File(backupFolder, MEDICINES_FILE)
            if (medicinesFile.exists()) {
                val medicines = loadMedicinesFromFile(medicinesFile)
                dataManager.saveMedicines(medicines)
                Log.d(TAG, "Imported ${medicines.size} medicines")
            }
            
            // Импортируем кнопки
            val buttonsFile = File(backupFolder, BUTTONS_FILE)
            if (buttonsFile.exists()) {
                val buttons = loadButtonsFromFile(buttonsFile)
                dataManager.saveCustomButtons(buttons)
                Log.d(TAG, "Imported ${buttons.size} buttons")
            }
            
            // Импортируем настройки
            val preferencesFile = File(backupFolder, PREFERENCES_FILE)
            if (preferencesFile.exists()) {
                val preferences = loadPreferencesFromFile(preferencesFile)
                if (preferences != null) {
                    dataManager.updateUserPreferences(preferences)
                    Log.d(TAG, "Imported user preferences")
                }
            }
            
            Log.i(TAG, "Data import completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during data import", e)
            false
        }
    }
    
    /**
     * Получает список доступных резервных копий
     */
    fun getAvailableBackups(): List<File> {
        return try {
            if (!exportDir.exists()) {
                return emptyList()
            }
            
            exportDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("backup_") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available backups", e)
            emptyList()
        }
    }
    
    /**
     * Автоматически импортирует последнюю резервную копию при первом запуске
     */
    fun autoImportLatestBackup(): Boolean {
        return try {
            val backups = getAvailableBackups()
            if (backups.isNotEmpty()) {
                val latestBackup = backups.first()
                Log.d(TAG, "Auto-importing latest backup: ${latestBackup.name}")
                return importAllData(latestBackup)
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-import", e)
            false
        }
    }
    
    private fun exportMedicines(exportFolder: File): Boolean {
        return try {
            val medicinesFile = File(context.filesDir, MEDICINES_FILE)
            if (medicinesFile.exists()) {
                val targetFile = File(exportFolder, MEDICINES_FILE)
                medicinesFile.copyTo(targetFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting medicines", e)
            false
        }
    }
    
    private fun exportCustomButtons(exportFolder: File): Boolean {
        return try {
            val buttonsFile = File(context.filesDir, BUTTONS_FILE)
            if (buttonsFile.exists()) {
                val targetFile = File(exportFolder, BUTTONS_FILE)
                buttonsFile.copyTo(targetFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting buttons", e)
            false
        }
    }
    
    private fun exportUserPreferences(exportFolder: File): Boolean {
        return try {
            val preferencesFile = File(context.filesDir, PREFERENCES_FILE)
            if (preferencesFile.exists()) {
                val targetFile = File(exportFolder, PREFERENCES_FILE)
                preferencesFile.copyTo(targetFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting preferences", e)
            false
        }
    }
    
    private fun createExportInfo(exportFolder: File, timestamp: String) {
        try {
            val infoFile = File(exportFolder, "export_info.txt")
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            
            val info = """
                Экспорт данных MedicalNotes
                Дата: $timestamp
                Версия приложения: ${packageInfo.versionName}
                Код версии: $versionCode
                
                Файлы в этой папке:
                - medicines.json - данные о лекарствах
                - custom_buttons.json - настройки кнопок
                - user_preferences.json - пользовательские настройки
                
                Для восстановления данных скопируйте эти файлы в папку приложения
                или используйте функцию импорта в настройках приложения.
            """.trimIndent()
            
            FileWriter(infoFile).use { it.write(info) }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating export info", e)
        }
    }
    
    private fun loadMedicinesFromFile(file: File): List<Medicine> {
        return try {
            val content = FileReader(file).readText()
            val type = object : TypeToken<List<Medicine>>() {}.type
            gson.fromJson(content, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading medicines from file", e)
            emptyList()
        }
    }
    
    private fun loadButtonsFromFile(file: File): List<CustomButton> {
        return try {
            val content = FileReader(file).readText()
            val type = object : TypeToken<List<CustomButton>>() {}.type
            gson.fromJson(content, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading buttons from file", e)
            emptyList()
        }
    }
    
    private fun loadPreferencesFromFile(file: File): UserPreferences? {
        return try {
            val content = FileReader(file).readText()
            gson.fromJson(content, UserPreferences::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preferences from file", e)
            null
        }
    }
} 