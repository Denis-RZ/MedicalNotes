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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val CRASH_LOG_FILE = "crash_log.txt"
    private const val MAX_LOG_SIZE = 10000 // 10KB
    
    // Глобальный обработчик необработанных исключений
    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
    private var isInitialized = false
    private var applicationContext: Context? = null
        
        // Добавляем глобальный обработчик исключений для корутин
        private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e(TAG, " ИСКЛЮЧЕНИЕ В КОРУТИНЕ!")
            android.util.Log.e(TAG, "Ошибка: ${throwable.message}")
            throwable.printStackTrace()
            
            // Используем сохраненный контекст приложения для показа диалога
            val context = applicationContext
            if (context != null) {
                handleUncaughtException(context, Thread.currentThread(), throwable)
            } else {
                android.util.Log.e(TAG, " Не удалось получить контекст приложения для корутины")
            }
        }

        fun initialize(context: Context) {
            try {
                if (isInitialized) {
                    android.util.Log.d(TAG, "CrashReporter уже инициализирован")
                    return
                }
                
                android.util.Log.d(TAG, " Начинаем инициализацию CrashReporter...")
                
                // Сохраняем контекст приложения для использования в корутинах
                applicationContext = context.applicationContext
                android.util.Log.d(TAG, "Контекст приложения сохранен: ${applicationContext?.javaClass?.name}")
                
                // Сохраняем стандартный обработчик
                defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
                android.util.Log.d(TAG, "Стандартный обработчик сохранен: ${defaultExceptionHandler?.javaClass?.name}")
                
                // Устанавливаем наш обработчик
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    android.util.Log.e(TAG, " НАШ ОБРАБОТЧИК СРАБОТАЛ!")
                    android.util.Log.e(TAG, "Поток: ${thread.name}")
                    android.util.Log.e(TAG, "Ошибка: ${throwable.message}")
                    handleUncaughtException(context, thread, throwable)
                }
                
                // Устанавливаем глобальный обработчик исключений для корутин
                GlobalScope.launch(coroutineExceptionHandler) {
                    android.util.Log.d(TAG, "Глобальный обработчик исключений корутин установлен")
                }
                
                isInitialized = true
                android.util.Log.d(TAG, " CrashReporter успешно инициализирован")
                
                // Тестовый вызов для проверки работы
                android.util.Log.d(TAG, "Тестирование CrashReporter...")
                
                // Проверяем, что наш обработчик установлен
                val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
                android.util.Log.d(TAG, "Текущий обработчик: ${currentHandler?.javaClass?.name}")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, " Ошибка инициализации CrashReporter", e)
                e.printStackTrace()
                // Даже если инициализация не удалась, пытаемся показать ошибку
                showSimpleErrorDialog(context, "Ошибка инициализации CrashReporter", e)
            }
        }
        
        private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
            try {
                android.util.Log.e(TAG, " === КРИТИЧЕСКАЯ ОШИБКА ===")
                android.util.Log.e(TAG, "Поток: ${thread.name}")
                android.util.Log.e(TAG, "Ошибка: ${throwable.message}")
                android.util.Log.e(TAG, "Полный стек трейс:")
                throwable.printStackTrace()
                
                android.util.Log.d(TAG, "Начинаем сбор информации о краше...")
                
                // Собираем детальную информацию об ошибке
                val crashInfo = collectCrashInfo(context, thread, throwable)
                
                android.util.Log.d(TAG, "Информация о краше собрана, сохраняем в файл...")
                
                // Сохраняем в файл
                saveCrashLog(context, crashInfo)
                
                android.util.Log.d(TAG, "Файл сохранен, показываем диалог...")
                
                // Показываем диалог с деталями ошибки
                showCrashDialog(context, crashInfo)
                
                android.util.Log.d(TAG, "Диалог показан, обработка краша завершена")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, " Ошибка в обработчике краша", e)
                e.printStackTrace()
                
                // Показываем простую ошибку если наш обработчик упал
                showSimpleErrorDialog(context, "Ошибка в обработчике краша", e)
                
                // Если наш обработчик упал, вызываем стандартный
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
        
        fun showSimpleErrorDialog(context: Context, title: String, throwable: Throwable) {
            try {
                val message = """
                    $title
                    
                    Тип: ${throwable.javaClass.name}
                    Сообщение: ${throwable.message}
                    
                    Стек трейс:
                    ${getStackTrace(throwable)}
                """.trimIndent()
                
                if (context is Activity && !context.isFinishing) {
                    context.runOnUiThread {
                        AlertDialog.Builder(context)
                            .setTitle(" Ошибка")
                            .setMessage(message)
                            .setPositiveButton("Копировать") { _, _ ->
                                copyToClipboard(context, message)
                            }
                            .setNegativeButton("Закрыть") { _, _ ->
                                context.finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
                } else {
                    // Показываем техническую информацию вместо простого сообщения
                    val technicalInfo = """
                         ТЕХНИЧЕСКАЯ ОШИБКА
                        
                        $title
                        Тип: ${throwable.javaClass.simpleName}
                        Сообщение: ${throwable.message}
                        Время: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}
                        
                        Стек трейс сохранен в лог.
                    """.trimIndent()
                    
                    Toast.makeText(context, technicalInfo, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Не удалось показать диалог ошибки", e)
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
                appendLine(" === ДЕТАЛИ КРИТИЧЕСКОЙ ОШИБКИ ===")
                appendLine("Время: $timestamp")
                appendLine("Unix timestamp: ${System.currentTimeMillis()}")
                appendLine("Время загрузки системы: ${System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()}ms")
                appendLine()
                appendLine("=== ОСНОВНАЯ ИНФОРМАЦИЯ ОБ ОШИБКЕ ===")
                appendLine("Тип ошибки: ${throwable.javaClass.name}")
                appendLine("Простое имя: ${throwable.javaClass.simpleName}")
                appendLine("Сообщение: ${throwable.message}")
                appendLine("Причина: ${throwable.cause?.message ?: "Нет"}")
                appendLine("Количество suppressed исключений: ${throwable.suppressed.size}")
                appendLine("Критичность: ${if (throwable is OutOfMemoryError || throwable is StackOverflowError) "КРИТИЧНО" else "ОБЫЧНО"}")
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
                appendLine("Время загрузки: ${android.os.SystemClock.elapsedRealtime()}ms")
                appendLine("Время работы: ${System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()}ms")
                appendLine("Количество ядер: ${Runtime.getRuntime().availableProcessors()}")
                appendLine("Максимальная память: ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB")
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
                
                //  ИСПРАВЛЕНО: Используем современный подход вместо deprecated versionCode
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
                
                //  ИСПРАВЛЕНО: Используем современный подход без deprecated методов
                val runningTasks = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        @Suppress("DEPRECATION")
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
                
                //  ИСПРАВЛЕНО: Используем современный подход без deprecated методов
                val runningServices = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        @Suppress("DEPRECATION")
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
                info.appendLine("Состояние системы: ${if (memoryInfo.lowMemory) "КРИТИЧНО - мало памяти" else "НОРМА - достаточно памяти"}")
                info.appendLine("Процент свободной памяти: ${(memoryInfo.availMem * 100 / memoryInfo.totalMem)}%")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error collecting system info", e)
                info.appendLine("Ошибка сбора системной информации: ${e.message}")
            }
            
            return info.toString()
        }
        
        private fun collectMemoryInfo(): String {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100 / maxMemory)
            
            return buildString {
                appendLine("Максимальная память: ${maxMemory / 1024 / 1024}MB (${maxMemory} bytes)")
                appendLine("Выделенная память: ${runtime.totalMemory() / 1024 / 1024}MB (${runtime.totalMemory()} bytes)")
                appendLine("Свободная память: ${runtime.freeMemory() / 1024 / 1024}MB (${runtime.freeMemory()} bytes)")
                appendLine("Используемая память: ${usedMemory / 1024 / 1024}MB (${usedMemory} bytes)")
                appendLine("Процент использования: ${usagePercent}%")
                appendLine("Состояние памяти: ${when {
                    usagePercent > 90 -> "КРИТИЧНО - высокое использование"
                    usagePercent > 75 -> "ВНИМАНИЕ - повышенное использование"
                    usagePercent > 50 -> "НОРМА - умеренное использование"
                    else -> "ХОРОШО - низкое использование"
                }}")
                appendLine("Доступно для выделения: ${(maxMemory - usedMemory) / 1024 / 1024}MB")
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
                appendLine("Тип потока: ${when {
                    thread.name.contains("main", ignoreCase = true) -> "ГЛАВНЫЙ UI поток"
                    thread.name.contains("ui", ignoreCase = true) -> "UI поток"
                    thread.name.contains("background", ignoreCase = true) -> "ФОНОВЫЙ поток"
                    thread.name.contains("async", ignoreCase = true) -> "АСИНХРОННЫЙ поток"
                    thread.name.contains("io", ignoreCase = true) -> "IO поток"
                    thread.name.contains("network", ignoreCase = true) -> "СЕТЕВОЙ поток"
                    else -> "ДРУГОЙ поток"
                }}")
                appendLine("Критичность: ${if (thread.name.contains("main", ignoreCase = true)) "КРИТИЧНО" else "ОБЫЧНО"}")
            }
        }
        
        private fun collectActivityInfo(context: Context): String {
            return try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val runningTasks = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    @Suppress("DEPRECATION")
                    activityManager.getRunningTasks(1)
                } else {
                    emptyList()
                }
                val topActivity = runningTasks.firstOrNull()?.topActivity
                
                buildString {
                    appendLine("Текущая активность: ${topActivity?.className ?: "неизвестно"}")
                    appendLine("Пакет текущей активности: ${topActivity?.packageName ?: "неизвестно"}")
                    appendLine("Количество запущенных задач: ${runningTasks.size}")
                    appendLine("Количество запущенных процессов: ${activityManager.runningAppProcesses?.size ?: 0}")
                    appendLine("Количество запущенных сервисов: ${if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { @Suppress("DEPRECATION") activityManager.getRunningServices(100).size } else { 0 }}")
                    
                    // Определяем тип активности
                    val activityType = when {
                        topActivity?.className?.contains("MainActivity") == true -> "ГЛАВНАЯ активность"
                        topActivity?.className?.contains("CrashReportActivity") == true -> "ОТЧЕТ О КРАШЕ"
                        topActivity?.className?.contains("SettingsActivity") == true -> "НАСТРОЙКИ"
                        topActivity?.className?.contains("AddMedicineActivity") == true -> "ДОБАВЛЕНИЕ ЛЕКАРСТВА"
                        topActivity?.className?.contains("EditMedicineActivity") == true -> "РЕДАКТИРОВАНИЕ ЛЕКАРСТВА"
                        else -> "ДРУГАЯ активность"
                    }
                    appendLine("Тип активности: $activityType")
                    
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
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                
                buildString {
                    appendLine("Версия приложения: ${packageInfo.versionName}")
                    appendLine("Код версии: $versionCode")
                    appendLine("Package: ${packageInfo.packageName}")
                    appendLine("Время установки: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(packageInfo.firstInstallTime))}")
                    appendLine("Время обновления: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(packageInfo.lastUpdateTime))}")
                    appendLine("Время работы приложения: ${System.currentTimeMillis() - packageInfo.firstInstallTime}ms")
                    appendLine("Время с последнего обновления: ${System.currentTimeMillis() - packageInfo.lastUpdateTime}ms")
                    appendLine("Статус приложения: ${if (context.packageManager.getApplicationInfo(context.packageName, 0).enabled) "АКТИВНО" else "ОТКЛЮЧЕНО"}")
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
                android.util.Log.d(TAG, "showCrashDialog вызван")
                android.util.Log.d(TAG, "Контекст: ${context.javaClass.name}")
                android.util.Log.d(TAG, "Контекст является Activity: ${context is Activity}")
                
                // Проверяем, что контекст - это Activity
                if (context is Activity && !context.isFinishing) {
                    android.util.Log.d(TAG, "Показываем диалог в Activity")
                    context.runOnUiThread {
                        showCrashDialogInActivity(context, crashInfo)
                    }
                } else {
                    android.util.Log.d(TAG, "Контекст не Activity, запускаем CrashReportActivity")
                    // Если контекст не Activity, запускаем CrashReportActivity для показа технической информации
                    try {
                        val intent = Intent(context, com.medicalnotes.app.CrashReportActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        android.util.Log.d(TAG, "CrashReportActivity запущена")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Ошибка запуска CrashReportActivity", e)
                        // Если не удалось запустить Activity, показываем техническую информацию в Toast
                        val technicalInfo = """
                             КРИТИЧЕСКАЯ ОШИБКА
                            
                            Тип: ${crashInfo.lines().find { it.contains("Тип ошибки:") }?.substringAfter("Тип ошибки: ") ?: "Неизвестно"}
                            Сообщение: ${crashInfo.lines().find { it.contains("Сообщение:") }?.substringAfter("Сообщение: ") ?: "Неизвестно"}
                            Время: ${crashInfo.lines().find { it.contains("Время:") }?.substringAfter("Время: ") ?: "Неизвестно"}
                            
                            Полный отчет сохранен в файл.
                        """.trimIndent()
                        
                        Toast.makeText(context, technicalInfo, Toast.LENGTH_LONG).show()
                        android.util.Log.d(TAG, "Показан Toast с технической информацией")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Ошибка показа диалога краша", e)
                e.printStackTrace()
            }
        }
        
        private fun showCrashDialogInActivity(activity: Activity, crashInfo: String) {
            try {
                // Сначала сохраняем отчет в файл для безопасности
                saveCrashLog(activity, crashInfo)
                
                //  УЛУЧШЕНО: Сразу показываем детали ошибки
                showDetailedCrashInfo(activity, crashInfo)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, " Ошибка создания диалога краша", e)
                e.printStackTrace()
                
                // Даже если диалог не показался, сохраняем отчет
                try {
                    saveCrashLog(activity, crashInfo)
                } catch (saveError: Exception) {
                    android.util.Log.e(TAG, " Ошибка сохранения отчета", saveError)
                    saveError.printStackTrace()
                }
                
                // Показываем простой диалог с частью информации
                try {
                    val simpleDialog = AlertDialog.Builder(activity)
                        .setTitle(" КРИТИЧЕСКАЯ ОШИБКА")
                        .setMessage("""
                            Приложение столкнулось с критической ошибкой.
                            
                            Основная информация:
                            ${crashInfo.take(2000)}...
                            
                            Полный отчет сохранен в файл.
                        """.trimIndent())
                        .setPositiveButton("Копировать") { _, _ ->
                            copyToClipboard(activity, crashInfo)
                            Toast.makeText(activity, "Отчет скопирован", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Закрыть") { _, _ ->
                            activity.finish()
                            System.exit(1)
                        }
                        .setCancelable(false)
                        .create()
                    
                    simpleDialog.show()
                } catch (simpleError: Exception) {
                    android.util.Log.e(TAG, " Ошибка показа простого диалога", simpleError)
                    simpleError.printStackTrace()
                    activity.finish()
                    System.exit(1)
                }
            }
        }
        
        private fun showDetailedCrashInfo(activity: Activity, crashInfo: String) {
            try {
                //  УЛУЧШЕНО: Создаем более надежный ScrollView
                val scrollView = android.widget.ScrollView(activity).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                
                val textView = android.widget.TextView(activity).apply {
                    text = crashInfo
                    textSize = 8f // Уменьшаем размер для большего текста
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextColor(activity.getColor(android.R.color.black))
                    setPadding(16, 16, 16, 16)
                    isScrollContainer = true
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    // Текст можно выделять для копирования
                }
                
                scrollView.addView(textView)
                
                val dialog = AlertDialog.Builder(activity)
                    .setTitle(" ПОЛНЫЙ СТЕК ТРЕЙС ОШИБКИ")
                    .setView(scrollView)
                    .setPositiveButton(" Копировать все") { _, _ ->
                        copyToClipboard(activity, crashInfo)
                        Toast.makeText(activity, " Полный отчет скопирован в буфер обмена", Toast.LENGTH_LONG).show()
                    }
                    .setNeutralButton("📤 Отправить") { _, _ ->
                        shareCrashReport(activity, crashInfo)
                    }
                    .setNegativeButton(" Закрыть") { _, _ ->
                        activity.finish()
                        System.exit(1)
                    }
                    .setCancelable(false)
                    .create()
                
                dialog.show()
                
                //  ДОБАВЛЕНО: Логируем успешный показ
                android.util.Log.d(TAG, " Детали краша успешно показаны")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, " Ошибка показа деталей краша", e)
                e.printStackTrace()
                
                // Показываем простой диалог с частью информации
                try {
                    val simpleDialog = AlertDialog.Builder(activity)
                        .setTitle(" Ошибка показа деталей")
                        .setMessage("""
                            Не удалось показать полные детали.
                            
                            Основная информация:
                            ${crashInfo.take(1500)}...
                            
                            Полный отчет сохранен в файл.
                        """.trimIndent())
                        .setPositiveButton("Копировать") { _, _ ->
                            copyToClipboard(activity, crashInfo)
                            Toast.makeText(activity, "Отчет скопирован", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Закрыть") { _, _ ->
                            activity.finish()
                            System.exit(1)
                        }
                        .setCancelable(false)
                        .create()
                    
                    simpleDialog.show()
                } catch (simpleError: Exception) {
                    android.util.Log.e(TAG, " Ошибка показа простого диалога", simpleError)
                    simpleError.printStackTrace()
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
                    putExtra(Intent.EXTRA_SUBJECT, " Отчет о критической ошибке Medical Notes")
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
        
        // Тестовая функция для проверки работы CrashReporter
        fun testCrashReporter(context: Context) {
            try {
                android.util.Log.d(TAG, "🧪 Тестирование CrashReporter...")
                
                // Проверяем инициализацию
                if (!isInitialized) {
                    android.util.Log.w(TAG, "CrashReporter не инициализирован!")
                    return
                }
                
                // Проверяем текущий обработчик
                val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
                android.util.Log.d(TAG, "Текущий обработчик: ${currentHandler?.javaClass?.name}")
                
                // Создаем тестовую ошибку
                val testException = RuntimeException("Тестовая ошибка для проверки CrashReporter")
                
                // Показываем тестовый диалог
                showSimpleErrorDialog(context, "Тест CrashReporter", testException)
                
                android.util.Log.d(TAG, " Тест CrashReporter завершен")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, " Ошибка тестирования CrashReporter", e)
            }
        }
        
        // Функция для получения обработчика исключений корутин
        fun getCoroutineExceptionHandler(): CoroutineExceptionHandler {
            return coroutineExceptionHandler
        }
    }