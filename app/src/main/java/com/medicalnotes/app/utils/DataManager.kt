package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.UserPreferences
import com.medicalnotes.app.models.DosageFrequency
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalTime
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import com.medicalnotes.app.models.MedicineJournalEntry
import java.time.LocalDate

class DataManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DataManager"
        private const val MEDICINES_FILE = "medicines.json"
        private const val BUTTONS_FILE = "custom_buttons.json"
        private const val PREFERENCES_FILE = "user_preferences.json"
        private const val BACKUP_DIR = "backups"
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .setLenient() // Более мягкая обработка JSON
        .create()
    private val configManager = ConfigManager(context)
    private val backupManager = BackupManager(context)
    
    // Файлы данных
    private val medicinesFile = File(context.filesDir, MEDICINES_FILE)
    private val buttonsFile = File(context.filesDir, BUTTONS_FILE)
    private val preferencesFile = File(context.filesDir, PREFERENCES_FILE)
    private val backupDir = File(context.filesDir, BACKUP_DIR)
    
    init {
        Log.d(TAG, "DataManager initialization started")
        try {
            initializeDataStructure()
            Log.d(TAG, "DataManager initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during DataManager initialization", e)
            e.printStackTrace()
        }
    }
    
    private fun initializeDataStructure() {
        try {
            Log.d(TAG, "Creating directories...")
            // Создаем директории если их нет
            if (!backupDir.exists()) {
                backupDir.mkdirs()
                Log.d(TAG, "Backup directory created")
            }
            
            Log.d(TAG, "Loading config...")
            // Инициализируем конфигурацию
            configManager.loadConfig()
            Log.d(TAG, "Config loaded")
            
            Log.d(TAG, "Creating files...")
            // Создаем файлы если их нет
            createFileIfNotExists(medicinesFile, "[]")
            createFileIfNotExists(buttonsFile, "[]")
            createFileIfNotExists(preferencesFile, "{}")
            
            Log.i(TAG, "Data structure initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing data structure", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun createFileIfNotExists(file: File, defaultContent: String) {
        if (!file.exists()) {
            try {
                FileWriter(file).use { it.write(defaultContent) }
                Log.i(TAG, "Created file: ${file.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating file: ${file.name}", e)
            }
        }
    }
    
    /**
     * Мигрирует данные лекарств для совместимости с новыми полями
     */
    private fun migrateMedicinesData(medicines: List<Medicine>): List<Medicine> {
        return medicines.map { medicine ->
            try {
                // Проверяем, есть ли новые поля, если нет - добавляем значения по умолчанию
                medicine.copy(
                    groupId = medicine.groupId ?: null,
                    groupName = medicine.groupName.ifEmpty { "" },
                    groupOrder = if (medicine.groupOrder == 0) 0 else medicine.groupOrder
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating medicine ${medicine.name}", e)
                // Возвращаем лекарство с дефолтными значениями для новых полей
                medicine.copy(
                    groupId = null,
                    groupName = "",
                    groupOrder = 0
                )
            }
        }
    }
    
    // ==================== ЛЕКАРСТВА ====================
    
    fun saveMedicines(medicines: List<Medicine>): Boolean {
        return try {
            // Создаем резервную копию перед сохранением
            if (medicinesFile.exists()) {
                val backupFile = File(context.filesDir, "medicines_backup.json")
                medicinesFile.copyTo(backupFile, overwrite = true)
                Log.d(TAG, "Backup created before saving")
            }
            
            Log.d(TAG, "Saving ${medicines.size} medicines")
            val json = gson.toJson(medicines)
            Log.d(TAG, "JSON generated successfully, length: ${json.length}")
            Log.d(TAG, "JSON sample: ${json.take(500)}...")
            
            val content = if (configManager.getSettings().dataCompression) {
                compressData(json)
            } else {
                json
            }
            
            FileWriter(medicinesFile).use { it.write(content) }
            Log.d(TAG, "Medicines saved to file successfully")
            
            // Автоматическое резервное копирование
            if (configManager.getSettings().autoBackup) {
                backupManager.createAutoBackup()
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving medicines", e)
            e.printStackTrace()
            false
        }
    }
    
    fun loadMedicines(): List<Medicine> {
        return try {
            if (!medicinesFile.exists()) {
                return emptyList()
            }
            
            val content = FileReader(medicinesFile).readText()
            val json = if (configManager.getSettings().dataCompression) {
                decompressData(content)
            } else {
                content
            }
            
            val type = object : TypeToken<List<Medicine>>() {}.type
            val medicines: List<Medicine> = gson.fromJson(json, type) ?: emptyList()
            
            // Миграция данных для совместимости со старыми данными
            return migrateMedicinesData(medicines)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading medicines", e)
            // Попробуем загрузить с резервного файла или создать новый
            try {
                val backupFile = File(context.filesDir, "medicines_backup.json")
                if (backupFile.exists()) {
                    Log.d(TAG, "Trying to load from backup file")
                    val backupContent = backupFile.readText()
                    val type = object : TypeToken<List<Medicine>>() {}.type
                    val backupMedicines: List<Medicine> = gson.fromJson(backupContent, type) ?: emptyList()
                    return migrateMedicinesData(backupMedicines)
                } else {
                    Log.d(TAG, "No backup file found, returning empty list")
                    return emptyList()
                }
            } catch (backupException: Exception) {
                Log.e(TAG, "Error loading from backup", backupException)
                // Если даже резервная копия не работает, очищаем данные
                Log.w(TAG, "Clearing corrupted data and starting fresh")
                try {
                    medicinesFile.delete()
                    Log.i(TAG, "Corrupted medicines file deleted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting corrupted file", e)
                }
                emptyList()
            }
        }
    }
    
    fun addMedicine(medicine: Medicine): Boolean {
        return try {
            val medicines = loadMedicines().toMutableList()
            val newMedicine = medicine.copy(id = System.currentTimeMillis())
            medicines.add(newMedicine)
            
            // Логируем добавление для отладки
            Log.d(TAG, "Adding medicine: ${newMedicine.name}")
            Log.d(TAG, "  Fields: dosageTimes=${newMedicine.dosageTimes}, customDays=${newMedicine.customDays}, customTimes=${newMedicine.customTimes}")
            
            val result = saveMedicines(medicines)
            Log.d(TAG, "Medicine added: ${newMedicine.name}, result: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error adding medicine: ${medicine.name}", e)
            false
        }
    }
    
    fun updateMedicine(medicine: Medicine): Boolean {
        val medicines = loadMedicines().toMutableList()
        val index = medicines.indexOfFirst { it.id == medicine.id }
        if (index != -1) {
            val originalMedicine = medicines[index]
            medicines[index] = medicine.copy(updatedAt = System.currentTimeMillis())
            
            // Логируем обновление для отладки
            Log.d(TAG, "Updating medicine: ${medicine.name}")
            Log.d(TAG, "  Original: dosageTimes=${originalMedicine.dosageTimes}, customDays=${originalMedicine.customDays}, customTimes=${originalMedicine.customTimes}")
            Log.d(TAG, "  Updated: dosageTimes=${medicine.dosageTimes}, customDays=${medicine.customDays}, customTimes=${medicine.customTimes}")
            
            return saveMedicines(medicines)
        }
        return false
    }
    
    fun deleteMedicine(medicineId: Long): Boolean {
        val medicines = loadMedicines().toMutableList()
        medicines.removeAll { it.id == medicineId }
        return saveMedicines(medicines)
    }
    
    fun getMedicineById(id: Long): Medicine? {
        val medicines = loadMedicines()
        val medicine = medicines.find { it.id == id }
        Log.d(TAG, "getMedicineById: ID=$id, Found=${medicine?.name ?: "null"}, Total medicines=${medicines.size}")
        return medicine
    }
    
    fun getActiveMedicines(): List<Medicine> {
        return loadMedicines().filter { it.isActive }.sortedBy { it.time }
    }
    
    fun getMedicinesByTimeRange(startTime: LocalTime, endTime: LocalTime): List<Medicine> {
        return loadMedicines().filter { 
            it.isActive && it.time >= startTime && it.time <= endTime 
        }.sortedBy { it.time }
    }
    
    fun getLowStockMedicines(): List<Medicine> {
        val threshold = configManager.getSettings().lowStockThreshold
        return loadMedicines().filter { it.isActive && it.remainingQuantity <= threshold }
    }
    
    /**
     * Получает список существующих групп
     */
    fun getExistingGroups(): List<String> {
        val medicines = loadMedicines()
        return medicines
            .filter { it.groupName.isNotEmpty() }
            .map { it.groupName }
            .distinct()
            .sorted()
    }
    
    /**
     * Получает следующий порядковый номер в группе
     */
    fun getNextGroupOrder(groupName: String): Int {
        val medicines = loadMedicines()
        val groupMedicines = medicines.filter { it.groupName == groupName }
        return if (groupMedicines.isEmpty()) 1 else groupMedicines.maxOf { it.groupOrder } + 1
    }
    
    fun decrementMedicineQuantity(medicineId: Long): Boolean {
        val medicines = loadMedicines().toMutableList()
        val index = medicines.indexOfFirst { it.id == medicineId }
        if (index != -1 && medicines[index].remainingQuantity > 0) {
            medicines[index] = medicines[index].copy(
                remainingQuantity = medicines[index].remainingQuantity - 1,
                lastTakenTime = System.currentTimeMillis(),
                isMissed = false,
                updatedAt = System.currentTimeMillis()
            )
            return saveMedicines(medicines)
        }
        return false
    }
    
    fun markMedicineAsSkipped(medicineId: Long): Boolean {
        val medicines = loadMedicines().toMutableList()
        val index = medicines.indexOfFirst { it.id == medicineId }
        if (index != -1) {
            medicines[index] = medicines[index].copy(
                isMissed = true,
                missedCount = medicines[index].missedCount + 1,
                updatedAt = System.currentTimeMillis()
            )
            return saveMedicines(medicines)
        }
        return false
    }
    
    // ==================== ПОЛЬЗОВАТЕЛЬСКИЕ КНОПКИ ====================
    
    fun saveCustomButtons(buttons: List<CustomButton>): Boolean {
        return try {
            val json = gson.toJson(buttons)
            val content = if (configManager.getSettings().dataCompression) {
                compressData(json)
            } else {
                json
            }
            
            FileWriter(buttonsFile).use { it.write(content) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving buttons", e)
            false
        }
    }
    
    fun loadCustomButtons(): List<CustomButton> {
        return try {
            if (!buttonsFile.exists()) {
                return emptyList()
            }
            
            val content = FileReader(buttonsFile).readText()
            val json = if (configManager.getSettings().dataCompression) {
                decompressData(content)
            } else {
                content
            }
            
            val type = object : TypeToken<List<CustomButton>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading buttons", e)
            emptyList()
        }
    }
    
    fun addCustomButton(button: CustomButton): Boolean {
        val buttons = loadCustomButtons().toMutableList()
        val newButton = button.copy(id = System.currentTimeMillis())
        buttons.add(newButton)
        return saveCustomButtons(buttons)
    }
    
    fun updateCustomButton(button: CustomButton): Boolean {
        val buttons = loadCustomButtons().toMutableList()
        val index = buttons.indexOfFirst { it.id == button.id }
        if (index != -1) {
            buttons[index] = button.copy(createdAt = System.currentTimeMillis())
            return saveCustomButtons(buttons)
        }
        return false
    }
    
    fun deleteCustomButton(buttonId: Long): Boolean {
        val buttons = loadCustomButtons().toMutableList()
        buttons.removeAll { it.id == buttonId }
        return saveCustomButtons(buttons)
    }
    
    fun getVisibleButtons(): List<CustomButton> {
        return loadCustomButtons().filter { it.isVisible }.sortedBy { it.order }
    }
    
    fun insertDefaultButtons(defaultButtons: List<CustomButton>): Boolean {
        val existingButtons = loadCustomButtons()
        if (existingButtons.isEmpty()) {
            val buttonsWithIds = defaultButtons.mapIndexed { index, button ->
                button.copy(id = System.currentTimeMillis() + index)
            }
            return saveCustomButtons(buttonsWithIds)
        }
        return true
    }
    
    // ==================== ПОЛЬЗОВАТЕЛЬСКИЕ НАСТРОЙКИ ====================
    
    fun saveUserPreferences(preferences: UserPreferences): Boolean {
        return try {
            val json = gson.toJson(preferences)
            val content = if (configManager.getSettings().dataCompression) {
                compressData(json)
            } else {
                json
            }
            
            FileWriter(preferencesFile).use { it.write(content) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving preferences", e)
            false
        }
    }
    
    fun loadUserPreferences(): UserPreferences {
        return try {
            if (!preferencesFile.exists()) {
                return UserPreferences()
            }
            
            val content = FileReader(preferencesFile).readText()
            val json = if (configManager.getSettings().dataCompression) {
                decompressData(content)
            } else {
                content
            }
            
            gson.fromJson(json, UserPreferences::class.java) ?: UserPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preferences", e)
            UserPreferences()
        }
    }
    
    fun updateUserPreferences(preferences: UserPreferences): Boolean {
        return saveUserPreferences(preferences.copy(updatedAt = System.currentTimeMillis()))
    }
    
    // ==================== КОНФИГУРАЦИЯ ====================
    
    fun getSettings(): AppSettings {
        return configManager.getSettings()
    }
    
    fun updateSettings(settings: AppSettings): Boolean {
        return configManager.updateSettings(settings)
    }
    
    // ==================== РЕЗЕРВНОЕ КОПИРОВАНИЕ ====================
    
    fun createBackup(): String {
        return backupManager.createBackup()
    }
    
    fun restoreFromBackup(backupPath: String): Boolean {
        return backupManager.restoreFromBackup(backupPath)
    }
    
    fun getBackupList(): List<File> {
        return backupManager.getBackupList()
    }
    
    fun cleanupOldBackups() {
        backupManager.cleanupOldBackups()
    }
    
    // ==================== ЖУРНАЛ ПРИЕМА ЛЕКАРСТВ ====================
    
    fun addJournalEntry(entry: MedicineJournalEntry): Boolean {
        return try {
            val entries = loadJournalEntries().toMutableList()
            val newEntry = entry.copy(id = System.currentTimeMillis())
            entries.add(newEntry)
            
            val json = gson.toJson(entries)
            val content = if (configManager.getSettings().dataCompression) {
                compressData(json)
            } else {
                json
            }
            
            val journalFile = File(context.filesDir, "medicine_journal.json")
            FileWriter(journalFile).use { it.write(content) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding journal entry", e)
            false
        }
    }
    
    fun loadJournalEntries(): List<MedicineJournalEntry> {
        return try {
            val journalFile = File(context.filesDir, "medicine_journal.json")
            if (!journalFile.exists()) {
                return emptyList()
            }
            
            val content = FileReader(journalFile).readText()
            val json = if (configManager.getSettings().dataCompression) {
                decompressData(content)
            } else {
                content
            }
            
            val type = object : TypeToken<List<MedicineJournalEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading journal entries", e)
            emptyList()
        }
    }
    
    fun getJournalEntriesForMedicine(medicineId: Long): List<MedicineJournalEntry> {
        return loadJournalEntries().filter { it.medicineId == medicineId }
    }
    
    fun getJournalEntriesForDate(date: LocalDate): List<MedicineJournalEntry> {
        return loadJournalEntries().filter { 
            it.timestamp.toLocalDate() == date 
        }
    }
    
    fun clearJournalEntries(): Boolean {
        return try {
            val journalFile = File(context.filesDir, "medicine_journal.json")
            if (journalFile.exists()) {
                journalFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing journal entries", e)
            false
        }
    }

    // ==================== УТИЛИТЫ ====================
    
    private fun compressData(data: String): String {
        return try {
            val output = java.io.ByteArrayOutputStream()
            GZIPOutputStream(output).use { gzip ->
                gzip.write(data.toByteArray())
            }
            android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing data", e)
            data
        }
    }
    
    private fun decompressData(compressedData: String): String {
        return try {
            val decoded = android.util.Base64.decode(compressedData, android.util.Base64.DEFAULT)
            val input = java.io.ByteArrayInputStream(decoded)
            GZIPInputStream(input).use { gzip ->
                String(gzip.readBytes())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decompressing data", e)
            compressedData
        }
    }
    
    fun validateDataIntegrity(): Boolean {
        return try {
            loadMedicines()
            loadCustomButtons()
            loadUserPreferences()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Data integrity check failed", e)
            false
        }
    }
    
    fun getDataStatistics(): Map<String, Any> {
        return mapOf(
            "medicines_count" to loadMedicines().size,
            "active_medicines" to loadMedicines().count { it.isActive },
            "buttons_count" to loadCustomButtons().size,
            "visible_buttons" to loadCustomButtons().count { it.isVisible },
            "low_stock_medicines" to getLowStockMedicines().size,
            "backup_count" to getBackupList().size,
            "config_version" to configManager.loadConfig().version
        )
    }
} 