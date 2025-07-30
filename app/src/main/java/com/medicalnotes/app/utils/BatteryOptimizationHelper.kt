package com.medicalnotes.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Утилита для работы с оптимизацией батареи
 */
class BatteryOptimizationHelper(private val context: Context) {
    
    /**
     * Проверяет, игнорируется ли приложение оптимизацией батареи
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // На старых версиях Android оптимизации батареи нет
        }
    }
    
    /**
     * Запрашивает разрешение на игнорирование оптимизации батареи
     */
    fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.util.Log.d("BatteryOptimizationHelper", "Запрос на игнорирование оптимизации батареи отправлен")
            } catch (e: Exception) {
                android.util.Log.e("BatteryOptimizationHelper", "Ошибка запроса игнорирования оптимизации батареи", e)
            }
        }
    }
    
    /**
     * Открывает настройки оптимизации батареи
     */
    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d("BatteryOptimizationHelper", "Открыты настройки оптимизации батареи")
        } catch (e: Exception) {
            android.util.Log.e("BatteryOptimizationHelper", "Ошибка открытия настроек оптимизации батареи", e)
        }
    }
    
    /**
     * Проверяет и запрашивает необходимые разрешения для надежной работы уведомлений
     */
    fun checkAndRequestNotificationPermissions() {
        try {
            android.util.Log.d("BatteryOptimizationHelper", "Проверка разрешений для уведомлений")
            
            // Проверяем игнорирование оптимизации батареи
            if (!isIgnoringBatteryOptimizations()) {
                android.util.Log.d("BatteryOptimizationHelper", "Приложение не игнорирует оптимизацию батареи, запрашиваем разрешение")
                requestIgnoreBatteryOptimization()
            } else {
                android.util.Log.d("BatteryOptimizationHelper", "Приложение уже игнорирует оптимизацию батареи")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BatteryOptimizationHelper", "Ошибка проверки разрешений для уведомлений", e)
        }
    }
} 