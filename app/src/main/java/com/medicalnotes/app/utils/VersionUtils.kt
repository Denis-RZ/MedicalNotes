package com.medicalnotes.app.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object VersionUtils {
    
    /**
     * Получает полную информацию о версии приложения
     */
    fun getVersionInfo(context: Context): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            val installTime = Date(packageInfo.firstInstallTime)
            val lastUpdateTime = Date(packageInfo.lastUpdateTime)
            
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            
            """
            Версия: $versionName ($versionCode)
            Установлено: ${dateFormat.format(installTime)}
            Обновлено: ${dateFormat.format(lastUpdateTime)}
            """.trimIndent()
        } catch (e: Exception) {
            "Версия: Неизвестно"
        }
    }
    
    /**
     * Получает краткую информацию о версии
     */
    fun getShortVersionInfo(context: Context): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            "v${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "v?.?"
        }
    }
    
    /**
     * Получает только номер версии
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "?.?"
        }
    }
    
    /**
     * Получает только код версии
     */
    fun getVersionCode(context: Context): Int {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Проверяет, является ли это новой версией
     */
    fun isNewVersion(context: Context, previousVersionCode: Int): Boolean {
        val currentVersionCode = getVersionCode(context)
        return currentVersionCode > previousVersionCode
    }
    
    /**
     * Получает время последнего обновления
     */
    fun getLastUpdateTime(context: Context): String {
        return try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val lastUpdateTime = Date(packageInfo.lastUpdateTime)
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            dateFormat.format(lastUpdateTime)
        } catch (e: Exception) {
            "Неизвестно"
        }
    }
} 