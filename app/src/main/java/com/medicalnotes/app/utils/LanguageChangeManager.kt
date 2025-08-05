package com.medicalnotes.app.utils

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

/**
 * Менеджер для управления сменой языка и обновлением интерфейса
 * Решает проблему, когда UI элементы не обновляются после смены языка
 */
object LanguageChangeManager {
    
    private const val TAG = "LanguageChangeManager"
    private const val ACTION_LANGUAGE_CHANGED = "com.medicalnotes.app.LANGUAGE_CHANGED"
    private const val EXTRA_NEW_LANGUAGE = "new_language"
    
    /**
     * Устанавливает новый язык и обновляет интерфейс
     */
    fun changeLanguage(context: Context, newLanguage: LanguageManager.Language): Boolean {
        Log.d(TAG, "changeLanguage: Changing to ${newLanguage.displayName}")
        
        // 1. Сохраняем новый язык
        val saveSuccess = LanguageManager.setLanguage(context, newLanguage)
        if (!saveSuccess) {
            Log.e(TAG, "changeLanguage: Failed to save language setting")
            return false
        }
        
        // 2. Применяем язык к контексту
        val newContext = LanguageManager.applyLanguage(context, newLanguage)
        
        // 3. Обновляем конфигурацию оригинального контекста
        updateContextConfiguration(context, newLanguage)
        
        // 4. Отправляем уведомление о смене языка
        broadcastLanguageChange(context, newLanguage)
        
        Log.d(TAG, "changeLanguage: Language change completed successfully")
        return true
    }
    
    /**
     * Обновляет конфигурацию контекста
     */
    private fun updateContextConfiguration(context: Context, language: LanguageManager.Language) {
        try {
            val newConfig = Configuration(context.resources.configuration)
            newConfig.setLocale(Locale(language.code))
            
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(newConfig, context.resources.displayMetrics)
            
            Log.d(TAG, "updateContextConfiguration: Configuration updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "updateContextConfiguration: Failed to update configuration", e)
        }
    }
    
    /**
     * Отправляет broadcast о смене языка
     */
    private fun broadcastLanguageChange(context: Context, language: LanguageManager.Language) {
        val intent = Intent(ACTION_LANGUAGE_CHANGED).apply {
            putExtra(EXTRA_NEW_LANGUAGE, language.code)
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        Log.d(TAG, "broadcastLanguageChange: Broadcast sent for ${language.displayName}")
    }
    
    /**
     * Регистрирует receiver для уведомлений о смене языка
     */
    fun registerLanguageChangeReceiver(context: Context, receiver: LanguageChangeReceiver) {
        val filter = IntentFilter(ACTION_LANGUAGE_CHANGED)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        Log.d(TAG, "registerLanguageChangeReceiver: Receiver registered")
    }
    
    /**
     * Отменяет регистрацию receiver
     */
    fun unregisterLanguageChangeReceiver(context: Context, receiver: LanguageChangeReceiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        Log.d(TAG, "unregisterLanguageChangeReceiver: Receiver unregistered")
    }
    
    /**
     * Принудительно пересоздает Activity
     */
    fun recreateActivity(activity: Activity) {
        Log.d(TAG, "recreateActivity: Recreating ${activity.javaClass.simpleName}")
        activity.recreate()
    }
    
    /**
     * Обновляет все UI элементы в Activity
     */
    fun updateActivityUI(activity: Activity) {
        Log.d(TAG, "updateActivityUI: Updating UI for ${activity.javaClass.simpleName}")
        
        // Обновляем заголовок Activity
        activity.title = activity.getString(com.medicalnotes.app.R.string.app_name)
        
        // Обновляем ActionBar
        if (activity is androidx.appcompat.app.AppCompatActivity) {
            activity.supportActionBar?.title = activity.getString(com.medicalnotes.app.R.string.app_name)
        }
        
        // Уведомляем Activity о необходимости обновления UI
        if (activity is LanguageChangeListener) {
            activity.onLanguageChanged()
        }
        
        Log.d(TAG, "updateActivityUI: UI update completed")
    }
    
    /**
     * Интерфейс для Activity, которые должны реагировать на смену языка
     */
    interface LanguageChangeListener {
        fun onLanguageChanged()
    }
    
    /**
     * BroadcastReceiver для получения уведомлений о смене языка
     */
    abstract class LanguageChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LANGUAGE_CHANGED) {
                val languageCode = intent.getStringExtra(EXTRA_NEW_LANGUAGE)
                val language = LanguageManager.Language.values().find { it.code == languageCode }
                
                if (language != null) {
                    onLanguageChanged(context, language)
                }
            }
        }
        
        abstract fun onLanguageChanged(context: Context?, newLanguage: LanguageManager.Language)
    }
} 