package com.medicalnotes.app.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.medicalnotes.app.service.OverdueCheckService

/**
 * Утилита для проверки статуса службы и диагностики проблем
 */
object ServiceStatusChecker {
    
    /**
     * Проверяет, работает ли служба OverdueCheckService
     */
    fun isOverdueCheckServiceRunning(context: Context): Boolean {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            for (service in runningServices) {
                if (service.service.className == OverdueCheckService::class.java.name) {
                    android.util.Log.d("ServiceStatusChecker", "✅ Служба OverdueCheckService работает")
                    android.util.Log.d("ServiceStatusChecker", "   - PID: ${service.pid}")
                    android.util.Log.d("ServiceStatusChecker", "   - UID: ${service.uid}")
                    android.util.Log.d("ServiceStatusChecker", "   - Process: ${service.process}")
                    android.util.Log.d("ServiceStatusChecker", "   - Foreground: ${service.foreground}")
                    return true
                }
            }
            
            android.util.Log.w("ServiceStatusChecker", "❌ Служба OverdueCheckService НЕ работает")
            return false
            
        } catch (e: Exception) {
            android.util.Log.e("ServiceStatusChecker", "Ошибка проверки службы", e)
            return false
        }
    }
    
    /**
     * Принудительно запускает службу с диагностикой
     */
    fun forceStartOverdueCheckService(context: Context) {
        try {
            android.util.Log.d("ServiceStatusChecker", "=== ПРИНУДИТЕЛЬНЫЙ ЗАПУСК СЛУЖБЫ ===")
            
            // Проверяем текущий статус
            val wasRunning = isOverdueCheckServiceRunning(context)
            android.util.Log.d("ServiceStatusChecker", "Служба работала до запуска: $wasRunning")
            
            // Запускаем службу
            OverdueCheckService.startService(context)
            android.util.Log.d("ServiceStatusChecker", "✅ Команда запуска службы отправлена")
            
            // Проверяем статус после запуска
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val isRunning = isOverdueCheckServiceRunning(context)
                android.util.Log.d("ServiceStatusChecker", "Служба работает после запуска: $isRunning")
                
                if (!isRunning) {
                    android.util.Log.w("ServiceStatusChecker", "⚠️  Служба не запустилась, повторная попытка...")
                    // Повторная попытка
                    OverdueCheckService.startService(context)
                }
            }, 1000)
            
        } catch (e: Exception) {
            android.util.Log.e("ServiceStatusChecker", "Ошибка принудительного запуска службы", e)
        }
    }
    
    /**
     * Полная диагностика службы
     */
    fun diagnoseServiceStatus(context: Context) {
        android.util.Log.d("ServiceStatusChecker", "\n🔍 ДИАГНОСТИКА СЛУЖБЫ")
        android.util.Log.d("ServiceStatusChecker", "==================")
        
        // Проверяем регистрацию службы
        val serviceIntent = Intent(context, OverdueCheckService::class.java)
        val resolveInfo = context.packageManager.resolveService(serviceIntent, 0)
        
        if (resolveInfo != null) {
            android.util.Log.d("ServiceStatusChecker", "✅ Служба зарегистрирована в системе")
            android.util.Log.d("ServiceStatusChecker", "   - Имя: ${resolveInfo.serviceInfo.name}")
            android.util.Log.d("ServiceStatusChecker", "   - Экспортирована: ${resolveInfo.serviceInfo.exported}")
            android.util.Log.d("ServiceStatusChecker", "   - Включена: ${resolveInfo.serviceInfo.enabled}")
        } else {
            android.util.Log.e("ServiceStatusChecker", "❌ Служба НЕ зарегистрирована в системе!")
        }
        
        // Проверяем разрешения
        val hasForegroundPermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasForegroundHealthPermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_HEALTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("ServiceStatusChecker", "Разрешения:")
        android.util.Log.d("ServiceStatusChecker", "   - FOREGROUND_SERVICE: ${if (hasForegroundPermission) "✅" else "❌"}")
        android.util.Log.d("ServiceStatusChecker", "   - FOREGROUND_SERVICE_HEALTH: ${if (hasForegroundHealthPermission) "✅" else "❌"}")
        
        // Проверяем работу службы
        val isRunning = isOverdueCheckServiceRunning(context)
        android.util.Log.d("ServiceStatusChecker", "Статус работы: ${if (isRunning) "✅ Работает" else "❌ Не работает"}")
        
        // Проверяем все запущенные службы
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            android.util.Log.d("ServiceStatusChecker", "Всего запущенных служб: ${runningServices.size}")
            runningServices.forEach { service ->
                android.util.Log.d("ServiceStatusChecker", "   - ${service.service.className}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ServiceStatusChecker", "Ошибка получения списка служб", e)
        }
        
        android.util.Log.d("ServiceStatusChecker", "=== КОНЕЦ ДИАГНОСТИКИ ===")
    }
    
    /**
     * Проверяет и восстанавливает службу если нужно
     */
    fun checkAndRestoreService(context: Context) {
        android.util.Log.d("ServiceStatusChecker", "=== ПРОВЕРКА И ВОССТАНОВЛЕНИЕ СЛУЖБЫ ===")
        
        val isRunning = isOverdueCheckServiceRunning(context)
        
        if (!isRunning) {
            android.util.Log.w("ServiceStatusChecker", "Служба не работает, запускаем...")
            forceStartOverdueCheckService(context)
        } else {
            android.util.Log.d("ServiceStatusChecker", "Служба работает нормально")
        }
    }
} 