package com.medicalnotes.app

import android.app.Application

class MedicalNotesApplication : Application() {
    
    companion object {
        lateinit var instance: MedicalNotesApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        //  ДОБАВЛЕНО: Инициализация системы сбора логов
        try {
            android.util.Log.d("MedicalNotesApplication", "Инициализация LogCollector")
            com.medicalnotes.app.utils.LogCollector.initialize(this)
            android.util.Log.d("MedicalNotesApplication", "✓ LogCollector инициализирован")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка инициализации LogCollector", e)
        }
        
        //  ДОБАВЛЕНО: Инициализация системы отслеживания крашей
        try {
            android.util.Log.d("MedicalNotesApplication", "Инициализация CrashReporter")
            com.medicalnotes.app.utils.CrashReporter.initialize(this)
            android.util.Log.d("MedicalNotesApplication", "✓ CrashReporter инициализирован")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка инициализации CrashReporter", e)
        }
        
        //  ДОБАВЛЕНО: Автоматический запуск сервиса уведомлений
        try {
            android.util.Log.d("MedicalNotesApplication", "Запуск сервиса уведомлений при инициализации приложения")
            com.medicalnotes.app.service.NotificationService.startService(this)
            android.util.Log.d("MedicalNotesApplication", "✓ Сервис уведомлений запущен")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка запуска сервиса уведомлений", e)
        }
        
        //  ДОБАВЛЕНО: Автоматический запуск сервиса проверки просроченных лекарств
        try {
            android.util.Log.d("MedicalNotesApplication", "Запуск сервиса проверки просроченных лекарств при инициализации приложения")
            com.medicalnotes.app.service.OverdueCheckService.startService(this)
            android.util.Log.d("MedicalNotesApplication", "✓ Сервис проверки просроченных лекарств запущен")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка запуска сервиса проверки просроченных лекарств", e)
        }
        
        //  ДОБАВЛЕНО: Проверка и восстановление уведомлений при инициализации
        try {
            android.util.Log.d("MedicalNotesApplication", "Проверка и восстановление уведомлений при инициализации")
            val restorationManager = com.medicalnotes.app.utils.NotificationRestorationManager(this)
            restorationManager.checkAndRestoreNotifications()
            android.util.Log.d("MedicalNotesApplication", "✓ Проверка и восстановление уведомлений завершено")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка проверки и восстановления уведомлений", e)
        }
        
        //  ДОБАВЛЕНО: Инициализация системы управления языками
        try {
            android.util.Log.d("MedicalNotesApplication", "Инициализация LanguageManager")
            val currentLanguage = com.medicalnotes.app.utils.LanguageManager.getCurrentLanguage(this)
            android.util.Log.d("MedicalNotesApplication", "✓ LanguageManager инициализирован, текущий язык: ${currentLanguage.displayName}")
            
            // Применяем язык к контексту приложения
            val locale = java.util.Locale(currentLanguage.code)
            java.util.Locale.setDefault(locale)
            
            val config = android.content.res.Configuration(resources.configuration)
            config.setLocale(locale)
            
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            
            android.util.Log.d("MedicalNotesApplication", "✓ Язык применен к контексту приложения: ${currentLanguage.displayName}")
        } catch (e: Exception) {
            android.util.Log.e("MedicalNotesApplication", "Ошибка инициализации LanguageManager", e)
        }
    }
} 