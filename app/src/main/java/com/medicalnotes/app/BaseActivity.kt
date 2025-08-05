package com.medicalnotes.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medicalnotes.app.utils.LanguageManager
import com.medicalnotes.app.utils.LanguageChangeManager

open class BaseActivity : AppCompatActivity(), LanguageChangeManager.LanguageChangeListener {
    
    private var isLanguageApplied = false
    private lateinit var languageChangeReceiver: LanguageChangeManager.LanguageChangeReceiver
    
    override fun attachBaseContext(newBase: Context) {
        val currentLanguage = LanguageManager.getCurrentLanguage(newBase)
        android.util.Log.d("BaseActivity", "attachBaseContext: applying language ${currentLanguage.displayName} to ${this.javaClass.simpleName}")
        
        // ПРОСТОЕ РЕШЕНИЕ: Применяем язык напрямую
        val locale = java.util.Locale(currentLanguage.code)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
        
        android.util.Log.d("BaseActivity", "attachBaseContext: completed for ${this.javaClass.simpleName}")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем язык до вызова super.onCreate()
        if (!isLanguageApplied) {
            applyLanguageToActivity()
            isLanguageApplied = true
        }
        
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("BaseActivity", "onCreate: Language applied for ${this.javaClass.simpleName}")
    }
    
    private fun applyLanguageToActivity() {
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        android.util.Log.d("BaseActivity", "applyLanguageToActivity: Applying ${currentLanguage.displayName} to ${this.javaClass.simpleName}")
        
        val locale = java.util.Locale(currentLanguage.code)
        java.util.Locale.setDefault(locale)
        
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        
        // Обновляем ресурсы
        try {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            android.util.Log.d("BaseActivity", "applyLanguageToActivity: Successfully updated configuration for ${this.javaClass.simpleName}")
        } catch (e: Exception) {
            android.util.Log.e("BaseActivity", "applyLanguageToActivity: Error updating configuration for ${this.javaClass.simpleName}", e)
        }
        
        // Создаем новый контекст с правильной конфигурацией
        val newContext = createConfigurationContext(config)
        android.util.Log.d("BaseActivity", "applyLanguageToActivity: Created new context with locale ${config.locale} for ${this.javaClass.simpleName}")
    }
    
    override fun getResources(): android.content.res.Resources {
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        val locale = java.util.Locale(currentLanguage.code)
        val config = Configuration(super.getResources().configuration)
        config.setLocale(locale)
        
        val newContext = createConfigurationContext(config)
        android.util.Log.d("BaseActivity", "getResources: Returning resources with locale ${locale.language} for ${this.javaClass.simpleName}")
        
        return newContext.resources
    }
    
    override fun onResume() {
        super.onResume()
        
        // Проверяем, не изменился ли язык во время работы приложения
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        android.util.Log.d("BaseActivity", "onResume: Current language is ${currentLanguage.displayName} for ${this.javaClass.simpleName}")
        
        // Регистрируем receiver для уведомлений о смене языка
        registerLanguageChangeReceiver()
    }
    
    override fun onPause() {
        super.onPause()
        
        // Отменяем регистрацию receiver
        unregisterLanguageChangeReceiver()
    }
    
    private fun registerLanguageChangeReceiver() {
        languageChangeReceiver = object : LanguageChangeManager.LanguageChangeReceiver() {
            override fun onLanguageChanged(context: Context?, newLanguage: LanguageManager.Language) {
                android.util.Log.d("BaseActivity", "onLanguageChanged: Language changed to ${newLanguage.displayName} in ${this@BaseActivity.javaClass.simpleName}")
                
                // Обновляем UI на главном потоке
                runOnUiThread {
                    onLanguageChanged()
                }
            }
        }
        
        LanguageChangeManager.registerLanguageChangeReceiver(this, languageChangeReceiver)
    }
    
    private fun unregisterLanguageChangeReceiver() {
        if (::languageChangeReceiver.isInitialized) {
            LanguageChangeManager.unregisterLanguageChangeReceiver(this, languageChangeReceiver)
        }
    }
    
    override fun onLanguageChanged() {
        android.util.Log.d("BaseActivity", "onLanguageChanged: Updating UI for ${this.javaClass.simpleName}")
        
        // Обновляем заголовок Activity
        title = getString(com.medicalnotes.app.R.string.app_name)
        
        // Обновляем ActionBar
        supportActionBar?.title = getString(com.medicalnotes.app.R.string.app_name)
        
        // Вызываем метод для обновления UI в наследниках
        updateUIAfterLanguageChange()
    }
    
    /**
     * Метод для переопределения в наследниках для обновления UI после смены языка
     */
    protected open fun updateUIAfterLanguageChange() {
        // По умолчанию ничего не делаем
        // Наследники могут переопределить этот метод
    }
} 