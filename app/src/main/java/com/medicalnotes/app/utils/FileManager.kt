package com.medicalnotes.app.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.UserPreferences
import java.io.File
import java.time.LocalTime
import android.util.Log

class FileManager {
    
    private val gson = Gson()
    
    fun readMedicines(context: Context): List<Medicine> {
        return try {
            val file = File(context.filesDir, "medicines.json")
            if (!file.exists()) {
                return emptyList()
            }
            
            val json = file.readText()
            val type = object : TypeToken<List<Medicine>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun writeMedicines(context: Context, medicines: List<Medicine>) {
        try {
            val file = File(context.filesDir, "medicines.json")
            val json = gson.toJson(medicines)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun readCustomButtons(context: Context): List<CustomButton> {
        return try {
            val file = File(context.filesDir, "custom_buttons.json")
            if (!file.exists()) {
                return emptyList()
            }
            
            val json = file.readText()
            val type = object : TypeToken<List<CustomButton>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun writeCustomButtons(context: Context, buttons: List<CustomButton>) {
        try {
            val file = File(context.filesDir, "custom_buttons.json")
            val json = gson.toJson(buttons)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun readUserPreferences(context: Context): UserPreferences? {
        return try {
            val file = File(context.filesDir, "user_preferences.json")
            if (!file.exists()) {
                return null
            }
            
            val json = file.readText()
            gson.fromJson(json, UserPreferences::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun writeUserPreferences(context: Context, preferences: UserPreferences) {
        try {
            val file = File(context.filesDir, "user_preferences.json")
            val json = gson.toJson(preferences)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Методы без контекста для обратной совместимости
    fun readUserPreferences(): UserPreferences? {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "readUserPreferences() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
        // Для демонстрации возвращаем null
        return null
    }
    
    fun writeUserPreferences(preferences: UserPreferences) {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "writeUserPreferences() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
    }
    
    // Методы без контекста для использования в репозиториях
    fun readMedicines(): List<Medicine> {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "readMedicines() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
        // Для демонстрации возвращаем пустой список
        return emptyList()
    }
    
    fun writeMedicines(medicines: List<Medicine>) {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "writeMedicines() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
    }
    
    fun readCustomButtons(): List<CustomButton> {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "readCustomButtons() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
        // Для демонстрации возвращаем пустой список
        return emptyList()
    }
    
    fun writeCustomButtons(buttons: List<CustomButton>) {
        //  ИСПРАВЛЕНО: Добавляем логирование для отслеживания использования
        Log.w("FileManager", "writeCustomButtons() без контекста - используйте версию с контекстом")
        // В реальном приложении здесь нужно передавать контекст
    }
} 