package com.medicalnotes.app.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class CrashReporter {
    
    companion object {
        private const val TAG = "CrashReporter"
        private const val CRASH_LOG_FILE = "crash_log.txt"
        private const val MAX_LOG_SIZE = 10000 // 10KB
        
        // Глобальный обработчик необработанных исключений
        private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
        
        fun initialize(context: Context) {
            try {
                // Сохраняем стандартный обработчик
                defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
                
                // Устанавливаем наш обработчик
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    handleUncaughtException(context, thread, throwable)
                }
                
                android.util.Log.d(TAG, "CrashReporter инициализирован")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка инициализации CrashReporter", e)
            }
        }
        
        private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
            try {
                android.util.Log.e(TAG, "=== КРИТИЧЕСКАЯ ОШИБКА ===")
                android.util.Log.e(TAG, "Поток: ${thread.name}")
                android.util.Log.e(TAG, "Ошибка: ${throwable.message}")
                
                // Собираем детальную информацию об ошибке
                val crashInfo = collectCrashInfo(context, thread, throwable)
                
                // Сохраняем в файл
                saveCrashLog(context, crashInfo)
                
                // Показываем диалог с деталями ошибки
                showCrashDialog(context, crashInfo)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка в обработчике краша", e)
                // Если наш обработчик упал, вызываем стандартный
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
        
        private fun collectCrashInfo(context: Context, thread: Thread, throwable: Throwable): String {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val deviceInfo = collectDeviceInfo()
            val appInfo = collectAppInfo(context)
            val stackTrace = getStackTrace(throwable)
            val recentLogs = getRecentLogs()
            val systemInfo = collectSystemInfo(context)
            val memoryInfo = collectMemoryInfo()
            val threadInfo = collectThreadInfo(thread)
            val activityInfo = collectActivityInfo(context)
            
            return buildString {
                appendLine("=== ДЕТАЛИ КРИТИЧЕСКОЙ ОШИБКИ ===")
                appendLine("Время: $timestamp")
                appendLine("Unix timestamp: ${System.currentTimeMillis()}")
                appendLine()
                appendLine("=== ОСНОВНАЯ ИНФОРМАЦИЯ ОБ ОШИБКЕ ===")
                appendLine("Тип ошибки: ${throwable.javaClass.name}")
                appendLine("Простое имя: ${throwable.javaClass.simpleName}")
                appendLine("Сообщение: ${throwable.message}")
                appendLine("Причина: ${throwable.cause?.message ?: "Нет"}")
                appendLine("Количество suppressed исключений: ${throwable.suppressed.size}")
                appendLine()
                appendLine("=== ИНФОРМАЦИЯ ОБ УСТРОЙСТВЕ ===")
                appendLine(deviceInfo)
                appendLine()
                appendLine("=== СИСТЕМНАЯ ИНФОРМАЦИЯ ===")
                appendLine(systemInfo)
                appendLine()
                appendLine("=== ИНФОРМАЦИЯ О ПАМЯТИ ===")
                appendLine(memoryInfo)
                appendLine()
                appendLine("=== ИНФОРМАЦИЯ О ПРИЛОЖЕНИИ ===")
                appendLine(appInfo)
                appendLine()
                appendLine("=== ИНФОРМАЦИЯ О ПОТОКЕ ===")
                appendLine(threadInfo)
                appendLine()
                appendLine("=== ИНФОРМАЦИЯ О АКТИВНОСТИ ===")
                appendLine(activityInfo)
                appendLine()
                appendLine("=== ПОЛНЫЙ СТЕК ТРЕЙС ===")
                appendLine("Количество элементов стека: ${throwable.stackTrace.size}")
                appendLine()
                appendLine("Стек трейс:")
                appendLine(stackTrace)
                appendLine()
                appendLine("=== ПРИЧИНА ОШИБКИ (если есть) ===")
                if (throwable.cause != null) {
                    appendLine("Тип причины: ${throwable.cause!!.javaClass.name}")
                    appendLine("Сообщение причины: ${throwable.cause!!.message}")
                    appendLine("Стек трейс причины:")
                    appendLine(getStackTrace(throwable.cause!!))
                } else {
                    appendLine("Причина не найдена")
                }
                appendLine()
                appendLine("=== SUPPRESSED ИСКЛЮЧЕНИЯ ===")
                if (throwable.suppressed.isNotEmpty()) {
                    throwable.suppressed.forEachIndexed { index, suppressed ->
                        appendLine("Suppressed #$index:")
                        appendLine("  Тип: ${suppressed.javaClass.name}")
                        appendLine("  Сообщение: ${suppressed.message}")
                        appendLine("  Стек трейс:")
                        appendLine(getStackTrace(suppressed))
                        appendLine()
                    }
                } else {
                    appendLine("Suppressed исключения отсутствуют")
                }
                appendLine()
                appendLine("=== ПОСЛЕДНИЕ ЛОГИ ПРИЛОЖЕНИЯ ===")
                appendLine(recentLogs)
            }
        }
        
        private fun collectDeviceInfo(): String {
            return buildString {
                appendLine("Модель: ${Build.MODEL}")
                appendLine("Производитель: ${Build.MANUFACTURER}")
                appendLine("Бренд: ${Build.BRAND}")
                appendLine("Устройство: ${Build.DEVICE}")
                appendLine("Продукт: ${Build.PRODUCT}")
                appendLine("Версия Android: ${Build.VERSION.RELEASE}")
                appendLine("API Level: ${Build.VERSION.SDK_INT}")
                appendLine("Codename: ${Build.VERSION.CODENAME}")
                appendLine("Incremental: ${Build.VERSION.INCREMENTAL}")
                appendLine("Security Patch: ${Build.VERSION.SECURITY_PATCH}")
                appendLine("Архитектуры: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                appendLine("Основная архитектура: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "неизвестно"}")
                appendLine("Fingerprint: ${Build.FINGERPRINT}")
                appendLine("Hardware: ${Build.HARDWARE}")
                appendLine("Host: ${Build.HOST}")
                appendLine("ID: ${Build.ID}")
                appendLine("Tags: ${Build.TAGS}")
                appendLine("Type: ${Build.TYPE}")
                appendLine("User: ${Build.USER}")
                appendLine("Display: ${Build.DISPLAY}")
                appendLine("Bootloader: ${Build.BOOTLOADER}")
                appendLine("Radio: ${Build.getRadioVersion()}")
            }
        }
        
        private fun collectSystemInfo(context: Context): String {
            val info = StringBuilder()
            
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
                
                info.appendLine("=== СИСТЕМНАЯ ИНФОРМАЦИЯ ===")
                info.appendLine("Версия приложения: ${packageInfo.versionName}")
                
                // ✅ ИСПРАВЛЕНО: Используем современный подход вместо deprecated versionCode
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                }
                info.appendLine("Код версии: $versionCode")
                
                info.appendLine("Android версия: ${android.os.Build.VERSION.RELEASE}")
                info.appendLine("API уровень: ${android.os.Build.VERSION.SDK_INT}")
                info.appendLine("Модель устройства: ${android.os.Build.MODEL}")
                info.appendLine("Производитель: ${android.os.Build.MANUFACTURER}")
                
                // ✅ ИСПРАВЛЕНО: Используем современный подход вместо deprecated getRunningTasks
                val runningTasks = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        activityManager.getRunningTasks(5)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Cannot get running tasks", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                info.appendLine("Активные задачи: ${runningTasks.size}")
                runningTasks.take(3).forEachIndexed { index, task ->
                    info.appendLine("  ${index + 1}. ${task.topActivity?.className ?: "Unknown"}")
                }
                
                // ✅ ИСПРАВЛЕНО: Используем современный подход вместо deprecated getRunningServices
                val runningServices = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        activityManager.getRunningServices(10)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Cannot get running services", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                info.appendLine("Запущенные сервисы: ${runningServices.size}")
                runningServices.take(5).forEachIndexed { index, service ->
                    info.appendLine("  ${index + 1}. ${service.service.className}")
                }
                
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                info.appendLine("Доступная память: ${memoryInfo.availMem / 1024 / 1024} MB")
                info.appendLine("Общая память: ${memoryInfo.totalMem / 1024 / 1024} MB")
                info.appendLine("Порог памяти: ${memoryInfo.threshold / 1024 / 1024} MB")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error collecting system info", e)
                info.appendLine("Ошибка сбора системной информации: ${e.message}")
            }
            
            return info.toString()
        }
        
        private fun collectMemoryInfo(): String {
            val runtime = Runtime.getRuntime()
            return buildString {
                appendLine("Максимальная память: ${runtime.maxMemory() / 1024 / 1024}MB (${runtime.maxMemory()} bytes)")
                appendLine("Выделенная память: ${runtime.totalMemory() / 1024 / 1024}MB (${runtime.totalMemory()} bytes)")
                appendLine("Свободная память: ${runtime.freeMemory() / 1024 / 1024}MB (${runtime.freeMemory()} bytes)")
                appendLine("Используемая память: ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}MB (${runtime.totalMemory() - runtime.freeMemory()} bytes)")
                appendLine("Процент использования: ${((runtime.totalMemory() - runtime.freeMemory()) * 100 / runtime.maxMemory())}%")
            }
        }
        
        private fun collectThreadInfo(thread: Thread): String {
            return buildString {
                appendLine("Название потока: ${thread.name}")
                appendLine("ID потока: ${thread.id}")
                appendLine("Приоритет: ${thread.priority}")
                appendLine("Состояние: ${thread.state}")
                appendLine("Daemon: ${thread.isDaemon}")
                appendLine("Активен: ${thread.isAlive}")
                appendLine("Interrupted: ${thread.isInterrupted}")
                appendLine("Класс загрузчика: ${thread.contextClassLoader?.javaClass?.name ?: "null"}")
                appendLine("Группа потоков: ${thread.threadGroup?.name ?: "null"}")
                appendLine("Количество активных потоков в группе: ${thread.threadGroup?.activeCount() ?: 0}")
                appendLine("Количество активных групп: ${thread.threadGroup?.activeGroupCount() ?: 0}")
            }
        }
        
        private fun collectActivityInfo(context: Context): String {
            return try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val runningTasks = activityManager.getRunningTasks(1)
                val topActivity = runningTasks.firstOrNull()?.topActivity
                
                buildString {
                    appendLine("Текущая активность: ${topActivity?.className ?: "неизвестно"}")
                    appendLine("Пакет текущей активности: ${topActivity?.packageName ?: "неизвестно"}")
                    appendLine("Количество запущенных задач: ${runningTasks.size}")
                    appendLine("Количество запущенных процессов: ${activityManager.runningAppProcesses?.size ?: 0}")
                    appendLine("Количество запущенных сервисов: ${activityManager.getRunningServices(100).size}")
                    
                    // Информация о процессах
                    activityManager.runningAppProcesses?.forEach { processInfo ->
                        appendLine("Процесс: ${processInfo.processName} (PID: ${processInfo.pid})")
                        appendLine("  Важность: ${processInfo.importance}")
                        appendLine("  Компоненты: ${processInfo.pkgList.joinToString(", ")}")
                    }
                }
            } catch (e: Exception) {
                "Ошибка получения информации об активности: ${e.message}"
            }
        }
        
        private fun collectAppInfo(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                buildString {
                    appendLine("Версия приложения: ${packageInfo.versionName}")
                    appendLine("Код версии: ${packageInfo.versionCode}")
                    appendLine("Package: ${packageInfo.packageName}")
                    appendLine("Время установки: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(packageInfo.firstInstallTime))}")
                    appendLine("Время обновления: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(packageInfo.lastUpdateTime))}")
                }
            } catch (e: Exception) {
                "Ошибка получения информации о приложении: ${e.message}"
            }
        }
        
        private fun getStackTrace(throwable: Throwable): String {
            val stringWriter = java.io.StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            return stringWriter.toString()
        }
        
        private fun getRecentLogs(): String {
            return try {
                LogCollector.getLogsForCrashReport()
            } catch (e: Exception) {
                "Ошибка получения логов: ${e.message}"
            }
        }
        
        private fun saveCrashLog(context: Context, crashInfo: String) {
            try {
                val file = File(context.filesDir, CRASH_LOG_FILE)
                FileWriter(file).use { writer ->
                    writer.write(crashInfo)
                }
                android.util.Log.d(TAG, "Лог краша сохранен в: ${file.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка сохранения лога краша", e)
            }
        }
        
        private fun showCrashDialog(context: Context, crashInfo: String) {
            try {
                // Проверяем, что контекст - это Activity
                if (context is Activity && !context.isFinishing) {
                    context.runOnUiThread {
                        showCrashDialogInActivity(context, crashInfo)
                    }
                } else {
                    // Если контекст не Activity, показываем Toast
                    Toast.makeText(context, "Приложение упало. Проверьте логи.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка показа диалога краша", e)
            }
        }
        
        private fun showCrashDialogInActivity(activity: Activity, crashInfo: String) {
            try {
                // Сначала сохраняем отчет в файл для безопасности
                saveCrashLog(activity, crashInfo)
                
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("🚨 КРИТИЧЕСКАЯ ОШИБКА")
                    .setMessage("""
                        Приложение столкнулось с критической ошибкой.
                        
                        ✅ Отчет уже сохранен в файл
                        📱 Можно скопировать детали
                        📤 Можно отправить разработчику
                        
                        Что хотите сделать?
                    """.trimIndent())
                    .setPositiveButton("📋 Показать детали") { _, _ ->
                        showDetailedCrashInfo(activity, crashInfo)
                    }
                    .setNeutralButton("💾 Сохранить еще раз") { _, _ ->
                        saveCrashLog(activity, crashInfo)
                        Toast.makeText(activity, "Отчет сохранен в файл", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("❌ Закрыть") { _, _ ->
                        // Завершаем приложение
                        activity.finish()
                        System.exit(1)
                    }
                    .setCancelable(false)
                    .create()
                
                dialog.show()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка создания диалога краша", e)
                // Даже если диалог не показался, сохраняем отчет
                try {
                    saveCrashLog(activity, crashInfo)
                } catch (saveError: Exception) {
                    android.util.Log.e(TAG, "Ошибка сохранения отчета", saveError)
                }
                activity.finish()
                System.exit(1)
            }
        }
        
        private fun showDetailedCrashInfo(activity: Activity, crashInfo: String) {
            try {
                // Создаем ScrollView для длинного текста
                val scrollView = android.widget.ScrollView(activity).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                
                val textView = android.widget.TextView(activity).apply {
                    text = crashInfo
                    textSize = 10f
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextColor(activity.getColor(android.R.color.black))
                    setPadding(20, 20, 20, 20)
                    isScrollContainer = true
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                
                scrollView.addView(textView)
                
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("📋 ДЕТАЛИ КРИТИЧЕСКОЙ ОШИБКИ")
                    .setView(scrollView)
                    .setPositiveButton("📋 Копировать все") { _, _ ->
                        copyToClipboard(activity, crashInfo)
                        Toast.makeText(activity, "Полный отчет скопирован в буфер обмена", Toast.LENGTH_LONG).show()
                    }
                    .setNeutralButton("📤 Отправить") { _, _ ->
                        shareCrashReport(activity, crashInfo)
                    }
                    .setNegativeButton("❌ Закрыть") { _, _ ->
                        activity.finish()
                        System.exit(1)
                    }
                    .setCancelable(false)
                    .create()
                
                dialog.show()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка показа деталей краша", e)
                // Показываем простой диалог с частью информации
                try {
                    val simpleDialog = AlertDialog.Builder(activity)
                        .setTitle("📋 Ошибка показа деталей")
                        .setMessage("Не удалось показать полные детали. Основная информация:\n\n${crashInfo.take(1000)}...")
                        .setPositiveButton("Копировать") { _, _ ->
                            copyToClipboard(activity, crashInfo)
                        }
                        .setNegativeButton("Закрыть") { _, _ ->
                            activity.finish()
                            System.exit(1)
                        }
                        .setCancelable(false)
                        .create()
                    
                    simpleDialog.show()
                } catch (simpleError: Exception) {
                    android.util.Log.e(TAG, "Ошибка показа простого диалога", simpleError)
                    activity.finish()
                    System.exit(1)
                }
            }
        }
        
        private fun copyToClipboard(context: Context, text: String) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Детали ошибки", text)
                clipboard.setPrimaryClip(clip)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка копирования в буфер обмена", e)
            }
        }
        
        private fun shareCrashReport(activity: Activity, crashInfo: String) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "🚨 Отчет о критической ошибке Medical Notes")
                    putExtra(Intent.EXTRA_TEXT, crashInfo)
                }
                
                activity.startActivity(Intent.createChooser(intent, "Отправить отчет об ошибке"))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка отправки отчета", e)
                Toast.makeText(activity, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        fun getCrashLog(context: Context): String? {
            return try {
                val file = File(context.filesDir, CRASH_LOG_FILE)
                if (file.exists()) {
                    file.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка чтения лога краша", e)
                null
            }
        }
        
        fun clearCrashLog(context: Context) {
            try {
                val file = File(context.filesDir, CRASH_LOG_FILE)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка удаления лога краша", e)
            }
        }
    }
} 