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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class XmlDataManager(private val context: Context) {
    
    private val gson = Gson()
    private val medicinesFile = File(context.filesDir, "medicines.json")
    private val buttonsFile = File(context.filesDir, "custom_buttons.json")
    private val preferencesFile = File(context.filesDir, "user_preferences.json")
    
    // Лекарства
    fun saveMedicines(medicines: List<Medicine>): Boolean {
        return try {
            val json = gson.toJson(medicines)
            FileWriter(medicinesFile).use { it.write(json) }
            true
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error saving medicines", e)
            false
        }
    }
    
    fun loadMedicines(): List<Medicine> {
        return try {
            if (!medicinesFile.exists()) {
                return emptyList()
            }
            val json = FileReader(medicinesFile).readText()
            val type = object : TypeToken<List<Medicine>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error loading medicines", e)
            emptyList()
        }
    }
    
    fun addMedicine(medicine: Medicine): Boolean {
        val medicines = loadMedicines().toMutableList()
        val newMedicine = medicine.copy(id = System.currentTimeMillis())
        medicines.add(newMedicine)
        return saveMedicines(medicines)
    }
    
    fun updateMedicine(medicine: Medicine): Boolean {
        val medicines = loadMedicines().toMutableList()
        val index = medicines.indexOfFirst { it.id == medicine.id }
        if (index != -1) {
            medicines[index] = medicine.copy(updatedAt = System.currentTimeMillis())
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
        return loadMedicines().find { it.id == id }
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
        return loadMedicines().filter { it.isActive && it.remainingQuantity <= 5 }
    }
    
    fun decrementMedicineQuantity(medicineId: Long): Boolean {
        val medicines = loadMedicines().toMutableList()
        val index = medicines.indexOfFirst { it.id == medicineId }
        if (index != -1 && medicines[index].remainingQuantity > 0) {
            medicines[index] = medicines[index].copy(
                remainingQuantity = medicines[index].remainingQuantity - 1,
                updatedAt = System.currentTimeMillis()
            )
            return saveMedicines(medicines)
        }
        return false
    }
    
    // Пользовательские кнопки
    fun saveCustomButtons(buttons: List<CustomButton>): Boolean {
        return try {
            val json = gson.toJson(buttons)
            FileWriter(buttonsFile).use { it.write(json) }
            true
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error saving buttons", e)
            false
        }
    }
    
    fun loadCustomButtons(): List<CustomButton> {
        return try {
            if (!buttonsFile.exists()) {
                return emptyList()
            }
            val json = FileReader(buttonsFile).readText()
            val type = object : TypeToken<List<CustomButton>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error loading buttons", e)
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
    
    // Пользовательские настройки
    fun saveUserPreferences(preferences: UserPreferences): Boolean {
        return try {
            val json = gson.toJson(preferences)
            FileWriter(preferencesFile).use { it.write(json) }
            true
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error saving preferences", e)
            false
        }
    }
    
    fun loadUserPreferences(): UserPreferences {
        return try {
            if (!preferencesFile.exists()) {
                return UserPreferences()
            }
            val json = FileReader(preferencesFile).readText()
            gson.fromJson(json, UserPreferences::class.java) ?: UserPreferences()
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error loading preferences", e)
            UserPreferences()
        }
    }
    
    fun updateUserPreferences(preferences: UserPreferences): Boolean {
        return saveUserPreferences(preferences.copy(updatedAt = System.currentTimeMillis()))
    }
    
    // Резервное копирование
    fun createBackup(): String {
        val backupData = mapOf(
            "medicines" to loadMedicines(),
            "buttons" to loadCustomButtons(),
            "preferences" to loadUserPreferences(),
            "timestamp" to System.currentTimeMillis()
        )
        val backupJson = gson.toJson(backupData)
        val backupFile = File(context.filesDir, "backup_${System.currentTimeMillis()}.json")
        FileWriter(backupFile).use { it.write(backupJson) }
        return backupFile.absolutePath
    }
    
    fun restoreFromBackup(backupPath: String): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) return false
            
            val json = FileReader(backupFile).readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val backupData = gson.fromJson<Map<String, Any>>(json, type)
            
            // Восстанавливаем данные
            if (backupData.containsKey("medicines")) {
                val medicinesJson = gson.toJson(backupData["medicines"])
                val medicines = gson.fromJson<List<Medicine>>(medicinesJson, 
                    object : TypeToken<List<Medicine>>() {}.type)
                saveMedicines(medicines ?: emptyList())
            }
            
            if (backupData.containsKey("buttons")) {
                val buttonsJson = gson.toJson(backupData["buttons"])
                val buttons = gson.fromJson<List<CustomButton>>(buttonsJson,
                    object : TypeToken<List<CustomButton>>() {}.type)
                saveCustomButtons(buttons ?: emptyList())
            }
            
            if (backupData.containsKey("preferences")) {
                val preferencesJson = gson.toJson(backupData["preferences"])
                val preferences = gson.fromJson(preferencesJson, UserPreferences::class.java)
                if (preferences != null) {
                    saveUserPreferences(preferences)
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e("XmlDataManager", "Error restoring backup", e)
            false
        }
    }
} 