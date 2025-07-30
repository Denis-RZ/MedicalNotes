package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class LogCollector {
    
    companion object {
        private const val TAG = "LogCollector"
        private const val LOG_FILE = "app_logs.txt"
        private const val MAX_LOG_SIZE = 50000 // 50KB
        private const val MAX_LOG_ENTRIES = 1000
        
        private val logEntries = mutableListOf<LogEntry>()
        private var logFile: File? = null
        private var isInitialized = false
        
        data class LogEntry(
            val timestamp: Long,
            val level: String,
            val tag: String,
            val message: String,
            val throwable: Throwable? = null
        )
        
        fun initialize(context: Context) {
            try {
                logFile = File(context.filesDir, LOG_FILE)
                isInitialized = true
                Log.d(TAG, "LogCollector инициализирован")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка инициализации LogCollector", e)
            }
        }
        
        fun addLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
            if (!isInitialized) return
            
            try {
                val entry = LogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    tag = tag,
                    message = message,
                    throwable = throwable
                )
                
                synchronized(logEntries) {
                    logEntries.add(entry)
                    
                    // Ограничиваем количество записей
                    if (logEntries.size > MAX_LOG_ENTRIES) {
                        logEntries.removeAt(0)
                    }
                }
                
                // Сохраняем в файл
                saveToFile(entry)
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка добавления лога", e)
            }
        }
        
        fun d(tag: String, message: String) {
            addLog("DEBUG", tag, message)
        }
        
        fun i(tag: String, message: String) {
            addLog("INFO", tag, message)
        }
        
        fun w(tag: String, message: String) {
            addLog("WARN", tag, message)
        }
        
        fun e(tag: String, message: String, throwable: Throwable? = null) {
            addLog("ERROR", tag, message, throwable)
        }
        
        private fun saveToFile(entry: LogEntry) {
            try {
                logFile?.let { file ->
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                        .format(Date(entry.timestamp))
                    
                    val logLine = buildString {
                        append("$timestamp [${entry.level}] ${entry.tag}: ${entry.message}")
                        if (entry.throwable != null) {
                            append("\n")
                            append("Exception: ${entry.throwable.javaClass.simpleName}")
                            append("\n")
                            append("Message: ${entry.throwable.message}")
                            append("\n")
                            append("StackTrace:")
                            entry.throwable.stackTrace.forEach { element ->
                                append("\n  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
                            }
                        }
                        append("\n")
                    }
                    
                    FileWriter(file, true).use { writer ->
                        writer.write(logLine)
                    }
                    
                    // Проверяем размер файла
                    if (file.length() > MAX_LOG_SIZE) {
                        trimLogFile(file)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сохранения лога в файл", e)
            }
        }
        
        private fun trimLogFile(file: File) {
            try {
                val lines = file.readLines()
                if (lines.size > 500) { // Оставляем последние 500 строк
                    val trimmedLines = lines.takeLast(500)
                    file.writeText(trimmedLines.joinToString("\n") + "\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обрезки лог файла", e)
            }
        }
        
        fun getRecentLogs(limit: Int = 100): String {
            return try {
                synchronized(logEntries) {
                    logEntries.takeLast(limit)
                        .joinToString("\n") { entry ->
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                .format(Date(entry.timestamp))
                            "$timestamp [${entry.level}] ${entry.tag}: ${entry.message}"
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка получения последних логов", e)
                "Ошибка получения логов: ${e.message}"
            }
        }
        
        fun getLogFileContent(): String? {
            return try {
                logFile?.readText()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка чтения лог файла", e)
                null
            }
        }
        
        fun clearLogs() {
            try {
                synchronized(logEntries) {
                    logEntries.clear()
                }
                logFile?.delete()
                Log.d(TAG, "Логи очищены")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка очистки логов", e)
            }
        }
        
        fun getLogsForCrashReport(): String {
            return try {
                buildString {
                    appendLine("=== ПОСЛЕДНИЕ ЛОГИ ПРИЛОЖЕНИЯ ===")
                    appendLine("Время сбора: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    appendLine("Количество записей в памяти: ${logEntries.size}")
                    appendLine("Размер лог файла: ${logFile?.length() ?: 0} bytes")
                    appendLine()
                    appendLine("=== ПОСЛЕДНИЕ 200 ЗАПИСЕЙ ===")
                    appendLine(getRecentLogs(200))
                    appendLine()
                    appendLine("=== ПОЛНОЕ СОДЕРЖИМОЕ ЛОГ ФАЙЛА ===")
                    appendLine(getLogFileContent() ?: "Лог файл недоступен")
                    appendLine()
                    appendLine("=== СТАТИСТИКА ЛОГОВ ===")
                    appendLine(getLogStatistics())
                }
            } catch (e: Exception) {
                "Ошибка получения логов для отчета: ${e.message}\nСтек трейс: ${getStackTrace(e)}"
            }
        }
        
        private fun getLogStatistics(): String {
            return try {
                val debugCount = logEntries.count { it.level == "DEBUG" }
                val infoCount = logEntries.count { it.level == "INFO" }
                val warnCount = logEntries.count { it.level == "WARN" }
                val errorCount = logEntries.count { it.level == "ERROR" }
                
                buildString {
                    appendLine("DEBUG: $debugCount")
                    appendLine("INFO: $infoCount")
                    appendLine("WARN: $warnCount")
                    appendLine("ERROR: $errorCount")
                    appendLine("Всего: ${logEntries.size}")
                    
                    if (logEntries.isNotEmpty()) {
                        appendLine()
                        appendLine("=== ПОСЛЕДНИЕ 10 ОШИБОК ===")
                        logEntries.filter { it.level == "ERROR" }
                            .takeLast(10)
                            .forEach { entry ->
                                appendLine("${entry.timestamp}: ${entry.tag} - ${entry.message}")
                                if (entry.throwable != null) {
                                    appendLine("  Exception: ${entry.throwable.javaClass.simpleName}")
                                    appendLine("  Message: ${entry.throwable.message}")
                                }
                                appendLine()
                            }
                    }
                }
            } catch (e: Exception) {
                "Ошибка получения статистики: ${e.message}"
            }
        }
        
        private fun getStackTrace(throwable: Throwable): String {
            val stringWriter = java.io.StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            return stringWriter.toString()
        }
    }
} 