package com.medicalnotes.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.models.UserPreferences
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import java.io.File
import java.time.LocalTime

class DataMigrationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DataMigrationManager"
        private const val PREFS_NAME = "migration_prefs"
        private const val KEY_DATA_VERSION = "data_version"
        private const val CURRENT_DATA_VERSION = 2
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Проверяет и выполняет миграцию данных если необходимо
     */
    fun checkAndMigrateData(): Boolean {
        val currentVersion = prefs.getInt(KEY_DATA_VERSION, 1)
        
        Log.d(TAG, "Current data version: $currentVersion, Target version: $CURRENT_DATA_VERSION")
        
        if (currentVersion >= CURRENT_DATA_VERSION) {
            Log.d(TAG, "Data is up to date, no migration needed")
            return true
        }
        
        Log.i(TAG, "Starting data migration from version $currentVersion to $CURRENT_DATA_VERSION")
        
        try {
            // Создаем резервную копию перед миграцией
            createBackupBeforeMigration()
            
            // Выполняем миграцию пошагово
            for (version in currentVersion + 1..CURRENT_DATA_VERSION) {
                Log.d(TAG, "Migrating to version $version")
                when (version) {
                    2 -> migrateToVersion2()
                    // Добавляйте новые версии здесь
                }
            }
            
            // Обновляем версию данных
            prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
            
            Log.i(TAG, "Data migration completed successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during data migration", e)
            // Восстанавливаем из резервной копии
            restoreFromBackup()
            return false
        }
    }
    
    /**
     * Миграция к версии 2 - добавляет новые поля для группировки
     */
    private fun migrateToVersion2() {
        Log.d(TAG, "Migrating to version 2 - adding grouping fields")
        
        val dataManager = DataManager(context)
        
        // Мигрируем лекарства
        val medicines = dataManager.loadMedicines()
        val migratedMedicines = medicines.map { medicine ->
            medicine.copy(
                // Добавляем новые поля с значениями по умолчанию
                groupId = medicine.groupId ?: null,
                groupName = medicine.groupName.ifEmpty { "" },
                groupOrder = if (medicine.groupOrder == 0) 0 else medicine.groupOrder,
                relatedMedicineIds = medicine.relatedMedicineIds.ifEmpty { emptyList() },
                isPartOfGroup = medicine.isPartOfGroup,
                timeGroupId = medicine.timeGroupId ?: null,
                timeGroupName = medicine.timeGroupName.ifEmpty { "" },
                timeGroupOrder = if (medicine.timeGroupOrder == 0) 0 else medicine.timeGroupOrder,
                // Добавляем поля для новой схемы приема
                frequency = medicine.frequency ?: DosageFrequency.DAILY,
                dosageTimes = medicine.dosageTimes.ifEmpty { listOf(DosageTime.MORNING) },
                customDays = medicine.customDays.ifEmpty { emptyList() },
                customTimes = medicine.customTimes.ifEmpty { emptyList() },
                startDate = medicine.startDate,
                multipleDoses = medicine.multipleDoses,
                dosesPerDay = if (medicine.dosesPerDay == 0) 1 else medicine.dosesPerDay,
                doseTimes = medicine.doseTimes.ifEmpty { emptyList() },
                // Добавляем поле типа лекарства
                medicineType = medicine.medicineType.ifEmpty { "Таблетки" }
            )
        }
        
        // Сохраняем мигрированные данные
        val success = dataManager.saveMedicines(migratedMedicines)
        if (!success) {
            throw Exception("Failed to save migrated medicines")
        }
        
        Log.d(TAG, "Successfully migrated ${migratedMedicines.size} medicines to version 2")
    }
    
    /**
     * Создает резервную копию перед миграцией
     */
    private fun createBackupBeforeMigration() {
        try {
            val backupDir = File(context.filesDir, "migration_backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            
            // Копируем файлы данных
            val filesToBackup = listOf(
                "medicines.json",
                "custom_buttons.json", 
                "user_preferences.json"
            )
            
            filesToBackup.forEach { fileName ->
                val sourceFile = File(context.filesDir, fileName)
                if (sourceFile.exists()) {
                    val backupFile = File(backupDir, "${fileName}_${timestamp}")
                    sourceFile.copyTo(backupFile, overwrite = true)
                    Log.d(TAG, "Backed up $fileName to ${backupFile.name}")
                }
            }
            
            Log.d(TAG, "Migration backup created successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating migration backup", e)
            throw e
        }
    }
    
    /**
     * Восстанавливает данные из резервной копии
     */
    private fun restoreFromBackup() {
        try {
            val backupDir = File(context.filesDir, "migration_backup")
            if (!backupDir.exists()) {
                Log.w(TAG, "Backup directory not found, cannot restore")
                return
            }
            
            // Находим последнюю резервную копию
            val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".json") }
            if (backupFiles.isNullOrEmpty()) {
                Log.w(TAG, "No backup files found")
                return
            }
            
            val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
            if (latestBackup == null) {
                Log.w(TAG, "Could not find latest backup")
                return
            }
            
            // Восстанавливаем файлы
            val timestamp = latestBackup.name.substringAfterLast("_")
            val filesToRestore = listOf("medicines.json", "custom_buttons.json", "user_preferences.json")
            
            filesToRestore.forEach { fileName ->
                val backupFile = File(backupDir, "${fileName}_${timestamp}")
                if (backupFile.exists()) {
                    val targetFile = File(context.filesDir, fileName)
                    backupFile.copyTo(targetFile, overwrite = true)
                    Log.d(TAG, "Restored $fileName from backup")
                }
            }
            
            Log.d(TAG, "Data restored from backup successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from backup", e)
        }
    }
    
    /**
     * Получает текущую версию данных
     */
    fun getCurrentDataVersion(): Int {
        return prefs.getInt(KEY_DATA_VERSION, 1)
    }
    
    /**
     * Проверяет, нужна ли миграция
     */
    fun isMigrationNeeded(): Boolean {
        return getCurrentDataVersion() < CURRENT_DATA_VERSION
    }
} 