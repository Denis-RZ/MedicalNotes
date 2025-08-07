package com.medicalnotes.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Менеджер разрешений для приложения MedicalNotes
 * Проверяет и запрашивает все необходимые разрешения
 */
object PermissionManager {
    
    // Коды запросов разрешений
    private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    private const val REQUEST_FOREGROUND_SERVICE_PERMISSION = 1002
    private const val REQUEST_SYSTEM_ALERT_WINDOW_PERMISSION = 1003
    private const val REQUEST_IGNORE_BATTERY_OPTIMIZATION = 1004
    
    // Список всех необходимых разрешений
    private val REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        add(Manifest.permission.VIBRATE)
        add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        add(Manifest.permission.USE_EXACT_ALARM)
        add(Manifest.permission.WAKE_LOCK)
        add(Manifest.permission.FOREGROUND_SERVICE)
        add(Manifest.permission.FOREGROUND_SERVICE_HEALTH)
        add(Manifest.permission.USE_FULL_SCREEN_INTENT)
        
        // Разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    /**
     * Проверяет все необходимые разрешения
     */
    fun checkAllPermissions(context: Context): PermissionStatus {
        android.util.Log.d("PermissionManager", "🔍 НАЧАЛО ПРОВЕРКИ РАЗРЕШЕНИЙ")
        
        val status = PermissionStatus()
        
        // Проверяем обычные разрешения
        android.util.Log.d("PermissionManager", "📋 Проверяем ${REQUIRED_PERMISSIONS.size} основных разрешений")
        for (permission in REQUIRED_PERMISSIONS) {
            val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            status.permissions[permission] = isGranted
            
            android.util.Log.d("PermissionManager", "  ${if (isGranted) "✅" else "❌"} $permission: ${if (isGranted) "ПРЕДОСТАВЛЕНО" else "ОТСУТСТВУЕТ"}")
            
            if (!isGranted) {
                status.missingPermissions.add(permission)
            }
        }
        
        // Проверяем специальные разрешения
        android.util.Log.d("PermissionManager", "🔧 Проверяем специальные разрешения")
        
        status.systemAlertWindowGranted = Settings.canDrawOverlays(context)
        android.util.Log.d("PermissionManager", "  ${if (status.systemAlertWindowGranted) "✅" else "❌"} System Alert Window: ${if (status.systemAlertWindowGranted) "РАЗРЕШЕНО" else "ЗАПРЕЩЕНО"}")
        
        status.notificationsEnabled = areNotificationsEnabled(context)
        android.util.Log.d("PermissionManager", "  ${if (status.notificationsEnabled) "✅" else "❌"} Уведомления: ${if (status.notificationsEnabled) "ВКЛЮЧЕНЫ" else "ОТКЛЮЧЕНЫ"}")
        
        status.batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        android.util.Log.d("PermissionManager", "  ${if (status.batteryOptimizationIgnored) "✅" else "❌"} Оптимизация батареи: ${if (status.batteryOptimizationIgnored) "ОТКЛЮЧЕНА" else "ВКЛЮЧЕНА"}")
        
        android.util.Log.d("PermissionManager", "📊 ИТОГОВЫЙ СТАТУС:")
        android.util.Log.d("PermissionManager", "  Основные разрешения: ${status.permissions.size - status.missingPermissions.size}/${status.permissions.size}")
        android.util.Log.d("PermissionManager", "  Отсутствующие: ${status.missingPermissions}")
        android.util.Log.d("PermissionManager", "  Все разрешено: ${status.isAllGranted()}")
        
        return status
    }
    
    /**
     * Запрашивает все недостающие разрешения
     */
    fun requestMissingPermissions(activity: FragmentActivity, onComplete: (PermissionStatus) -> Unit) {
        android.util.Log.d("PermissionManager", "🚀 НАЧАЛО ЗАПРОСА РАЗРЕШЕНИЙ")
        
        try {
            // Проверяем, что activity готова
            if (activity.isFinishing || activity.isDestroyed) {
                android.util.Log.w("PermissionManager", "⚠️ Activity завершается, пропускаем запрос разрешений")
                onComplete(checkAllPermissions(activity))
                return
            }
            
            android.util.Log.d("PermissionManager", "📋 Проверяем текущий статус разрешений")
            val status = checkAllPermissions(activity)
            
            if (status.isAllGranted()) {
                android.util.Log.d("PermissionManager", "✅ Все разрешения уже предоставлены - ничего не запрашиваем")
                onComplete(status)
                return
            }
            
            android.util.Log.d("PermissionManager", "🔧 Запрашиваем недостающие разрешения:")
            android.util.Log.d("PermissionManager", "  Основные разрешения: ${status.missingPermissions}")
            android.util.Log.d("PermissionManager", "  System Alert Window: ${if (!status.systemAlertWindowGranted) "НУЖНО" else "ОК"}")
            android.util.Log.d("PermissionManager", "  Уведомления: ${if (!status.notificationsEnabled) "НУЖНО" else "ОК"}")
            android.util.Log.d("PermissionManager", "  Батарея: ${if (!status.batteryOptimizationIgnored) "НУЖНО" else "ОК"}")
            
            // Запрашиваем обычные разрешения
            if (status.missingPermissions.isNotEmpty()) {
                android.util.Log.d("PermissionManager", "📱 Запрашиваем ${status.missingPermissions.size} основных разрешений")
                ActivityCompat.requestPermissions(
                    activity,
                    status.missingPermissions.toTypedArray(),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                android.util.Log.d("PermissionManager", "📱 Основные разрешения уже предоставлены")
            }
            
            // Показываем инструкции для специальных разрешений с задержкой
            activity.runOnUiThread {
                try {
                    android.util.Log.d("PermissionManager", "⏰ Планируем показ инструкций через 500мс")
                    // Небольшая задержка для полной инициализации UI
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            android.util.Log.d("PermissionManager", "📖 Показываем инструкции для специальных разрешений")
                            showSpecialPermissionsInstructions(activity, status)
                        } else {
                            android.util.Log.w("PermissionManager", "⚠️ Activity завершилась, пропускаем показ инструкций")
                        }
                    }, 500)
                } catch (e: Exception) {
                    android.util.Log.e("PermissionManager", "❌ Ошибка показа инструкций", e)
                }
            }
            
            android.util.Log.d("PermissionManager", "✅ Запрос разрешений завершен")
            onComplete(status)
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "❌ КРИТИЧЕСКАЯ ОШИБКА запроса разрешений", e)
            onComplete(checkAllPermissions(activity))
        }
    }
    
    /**
     * Показывает инструкции для специальных разрешений
     */
    private fun showSpecialPermissionsInstructions(activity: FragmentActivity, status: PermissionStatus) {
        val instructions = mutableListOf<String>()
        
        if (!status.systemAlertWindowGranted) {
            instructions.add("• Разрешить показ поверх других приложений")
        }
        
        if (!status.notificationsEnabled) {
            instructions.add("• Включить уведомления в настройках приложения")
        }
        
        if (!status.batteryOptimizationIgnored) {
            instructions.add("• Отключить оптимизацию батареи для приложения")
        }
        
        if (instructions.isNotEmpty()) {
            showPermissionInstructionsDialog(activity, instructions)
        }
    }
    
    /**
     * Показывает диалог с инструкциями по настройке разрешений
     */
    private fun showPermissionInstructionsDialog(activity: FragmentActivity, instructions: List<String>) {
        try {
            // Дополнительная проверка состояния activity
            if (activity.isFinishing || activity.isDestroyed) {
                android.util.Log.w("PermissionManager", "Activity завершается, пропускаем показ диалога")
                return
            }
            
            val message = "Для корректной работы уведомлений необходимо:\n\n" + 
                         instructions.joinToString("\n") + 
                         "\n\nНажмите 'Настройки' для перехода к настройкам приложения."
            
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Настройка разрешений")
                .setMessage(message)
                .setPositiveButton("Настройки") { _, _ ->
                    try {
                        openAppSettings(activity)
                    } catch (e: Exception) {
                        android.util.Log.e("PermissionManager", "Ошибка открытия настроек", e)
                    }
                }
                .setNegativeButton("Позже") { dialog, _ ->
                    try {
                        dialog.dismiss()
                    } catch (e: Exception) {
                        android.util.Log.e("PermissionManager", "Ошибка закрытия диалога", e)
                    }
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка показа диалога инструкций", e)
        }
    }
    
    /**
     * Открывает настройки приложения
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            android.util.Log.d("PermissionManager", "Открыты настройки приложения")
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка открытия настроек", e)
        }
    }
    
    /**
     * Открывает настройки уведомлений
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        putExtra("app_package", context.packageName)
                        putExtra("app_uid", context.applicationInfo.uid)
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            android.util.Log.d("PermissionManager", "Открыты настройки уведомлений")
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка открытия настроек уведомлений", e)
            // Fallback к общим настройкам приложения
            openAppSettings(context)
        }
    }
    
    /**
     * Открывает настройки оптимизации батареи
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            android.util.Log.d("PermissionManager", "Открыты настройки оптимизации батареи")
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка открытия настроек батареи", e)
            // Fallback к общим настройкам приложения
            openAppSettings(context)
        }
    }
    
    /**
     * Проверяет, включены ли уведомления
     */
    private fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true // Для старых версий Android считаем, что разрешено
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка проверки уведомлений", e)
            true
        }
    }
    
    /**
     * Проверяет, отключена ли оптимизация батареи
     */
    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true // Для старых версий Android считаем, что отключено
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Ошибка проверки оптимизации батареи", e)
            true
        }
    }
    
    /**
     * Обрабатывает результат запроса разрешений
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onComplete: (PermissionStatus) -> Unit
    ) {
        android.util.Log.d("PermissionManager", "📋 onRequestPermissionsResult вызван")
        android.util.Log.d("PermissionManager", "  RequestCode: $requestCode")
        android.util.Log.d("PermissionManager", "  Permissions: ${permissions.joinToString(", ")}")
        android.util.Log.d("PermissionManager", "  GrantResults: ${grantResults.joinToString(", ")}")
        
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                android.util.Log.d("PermissionManager", "🔧 Обрабатываем результат запроса уведомлений")
                val status = PermissionStatus()
                
                for (i in permissions.indices) {
                    val permission = permissions[i]
                    val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                    status.permissions[permission] = granted
                    
                    android.util.Log.d("PermissionManager", "  ${if (granted) "✅" else "❌"} $permission: ${if (granted) "ПРЕДОСТАВЛЕНО" else "ОТКЛОНЕНО"}")
                    
                    if (!granted) {
                        status.missingPermissions.add(permission)
                    }
                }
                
                android.util.Log.d("PermissionManager", "📊 Итоговый статус после запроса:")
                android.util.Log.d("PermissionManager", "  Все разрешено: ${status.isAllGranted()}")
                android.util.Log.d("PermissionManager", "  Отсутствующие: ${status.missingPermissions}")
                onComplete(status)
            }
            else -> {
                android.util.Log.w("PermissionManager", "⚠️ Неизвестный requestCode: $requestCode")
                // Для неизвестного requestCode просто возвращаем пустой статус
                onComplete(PermissionStatus())
            }
        }
    }
    
    /**
     * Класс для хранения статуса разрешений
     */
    data class PermissionStatus(
        val permissions: MutableMap<String, Boolean> = mutableMapOf(),
        val missingPermissions: MutableList<String> = mutableListOf(),
        var systemAlertWindowGranted: Boolean = false,
        var notificationsEnabled: Boolean = true,
        var batteryOptimizationIgnored: Boolean = true
    ) {
        fun isAllGranted(): Boolean {
            return missingPermissions.isEmpty() && 
                   systemAlertWindowGranted && 
                   notificationsEnabled && 
                   batteryOptimizationIgnored
        }
        
        fun getMissingPermissionsDescription(): String {
            val descriptions = mutableListOf<String>()
            
            if (missingPermissions.isNotEmpty()) {
                descriptions.add("Основные разрешения: ${missingPermissions.joinToString(", ")}")
            }
            
            if (!systemAlertWindowGranted) {
                descriptions.add("Показ поверх других приложений")
            }
            
            if (!notificationsEnabled) {
                descriptions.add("Уведомления")
            }
            
            if (!batteryOptimizationIgnored) {
                descriptions.add("Оптимизация батареи")
            }
            
            return descriptions.joinToString("\n")
        }
    }
} 