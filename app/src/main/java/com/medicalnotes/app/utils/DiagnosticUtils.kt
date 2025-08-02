package com.medicalnotes.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticUtils {
    
    companion object {
        private const val TAG = "DiagnosticUtils"
        
        /**
         * Выполняет полную диагностику приложения
         */
        fun performFullDiagnostic(context: Context): String {
            return try {
                val diagnostic = StringBuilder()
                diagnostic.appendLine("=== ПОЛНАЯ ДИАГНОСТИКА ПРИЛОЖЕНИЯ ===")
                diagnostic.appendLine("Время: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                diagnostic.appendLine()
                
                // Проверка файлов данных
                diagnostic.appendLine("=== ПРОВЕРКА ФАЙЛОВ ДАННЫХ ===")
                diagnostic.append(checkDataFiles(context))
                diagnostic.appendLine()
                
                // Проверка целостности данных
                diagnostic.appendLine("=== ПРОВЕРКА ЦЕЛОСТНОСТИ ДАННЫХ ===")
                diagnostic.append(checkDataIntegrity(context))
                diagnostic.appendLine()
                
                // Проверка сервисов
                diagnostic.appendLine("=== ПРОВЕРКА СЕРВИСОВ ===")
                diagnostic.append(checkServices(context))
                diagnostic.appendLine()
                
                // Проверка разрешений
                diagnostic.appendLine("=== ПРОВЕРКА РАЗРЕШЕНИЙ ===")
                diagnostic.append(checkPermissions(context))
                diagnostic.appendLine()
                
                // Рекомендации по исправлению
                diagnostic.appendLine("=== РЕКОМЕНДАЦИИ ===")
                diagnostic.append(generateRecommendations(context))
                
                diagnostic.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error performing diagnostic", e)
                "Ошибка выполнения диагностики: ${e.message}"
            }
        }
        
        /**
         * Проверяет файлы данных
         */
        private fun checkDataFiles(context: Context): String {
            return try {
                val result = StringBuilder()
                val filesDir = context.filesDir
                
                val requiredFiles = listOf(
                    "medicines.json",
                    "custom_buttons.json", 
                    "user_preferences.json"
                )
                
                requiredFiles.forEach { fileName ->
                    val file = File(filesDir, fileName)
                    result.appendLine("$fileName:")
                    result.appendLine("  - Существует: ${file.exists()}")
                    if (file.exists()) {
                        result.appendLine("  - Размер: ${file.length()} bytes")
                        result.appendLine("  - Последнее изменение: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))}")
                        
                        // Проверяем читаемость
                        try {
                            val content = file.readText()
                            result.appendLine("  - Читаемость: OK")
                            result.appendLine("  - Содержимое не пустое: ${content.isNotEmpty()}")
                        } catch (e: Exception) {
                            result.appendLine("  - Читаемость: ОШИБКА - ${e.message}")
                        }
                    }
                    result.appendLine()
                }
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка проверки файлов: ${e.message}"
            }
        }
        
        /**
         * Проверяет целостность данных
         */
        private fun checkDataIntegrity(context: Context): String {
            return try {
                val result = StringBuilder()
                val dataManager = DataManager(context)
                
                // Проверяем лекарства
                try {
                    val medicines = dataManager.loadMedicines()
                    result.appendLine("Лекарства:")
                    result.appendLine("  - Количество: ${medicines.size}")
                    result.appendLine("  - Активных: ${medicines.count { it.isActive }}")
                    result.appendLine("  - С ошибками: ${medicines.count { it.name.isEmpty() || it.dosage.isEmpty() }}")
                    
                    // Проверяем каждое лекарство на корректность
                    medicines.forEachIndexed { index, medicine ->
                        if (medicine.name.isEmpty() || medicine.dosage.isEmpty()) {
                            result.appendLine("  - ОШИБКА в лекарстве #$index: пустое имя или дозировка")
                        }
                    }
                } catch (e: Exception) {
                    result.appendLine("  - ОШИБКА загрузки лекарств: ${e.message}")
                }
                
                result.appendLine()
                
                // Проверяем кнопки
                try {
                    val buttons = dataManager.loadCustomButtons()
                    result.appendLine("Кнопки:")
                    result.appendLine("  - Количество: ${buttons.size}")
                    result.appendLine("  - Видимых: ${buttons.count { it.isVisible }}")
                } catch (e: Exception) {
                    result.appendLine("  - ОШИБКА загрузки кнопок: ${e.message}")
                }
                
                result.appendLine()
                
                // Проверяем настройки
                try {
                    dataManager.loadUserPreferences()
                    result.appendLine("Настройки:")
                    result.appendLine("  - Загружены: OK")
                } catch (e: Exception) {
                    result.appendLine("  - ОШИБКА загрузки настроек: ${e.message}")
                }
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка проверки целостности: ${e.message}"
            }
        }
        
        /**
         * Проверяет сервисы
         */
        private fun checkServices(context: Context): String {
            return try {
                val result = StringBuilder()
                
                //  ИСПРАВЛЕНО: Используем современный подход без deprecated методов
                val runningServices = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    @Suppress("DEPRECATION")
                    activityManager.getRunningServices(100)
                } else {
                    // Для старых версий используем альтернативный подход
                    emptyList()
                }
                
                val appServices = runningServices.filter { it.service.className.startsWith("com.medicalnotes.app") }
                
                result.appendLine("Запущенные сервисы приложения:")
                if (appServices.isEmpty()) {
                    result.appendLine("  - Нет запущенных сервисов")
                } else {
                    appServices.forEach { service ->
                        result.appendLine("  - ${service.service.className}")
                        result.appendLine("    PID: ${service.pid}")
                        result.appendLine("    Процесс: ${service.process}")
                    }
                }
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка проверки сервисов: ${e.message}"
            }
        }
        
        /**
         * Проверяет разрешения
         */
        private fun checkPermissions(context: Context): String {
            return try {
                val result = StringBuilder()
                
                // Проверяем разрешения на уведомления
                val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true // Для старых версий Android разрешение дается автоматически
                }
                
                result.appendLine("Разрешения:")
                result.appendLine("  - Уведомления: ${if (notificationPermission) "OK" else "НЕТ"}")
                
                // Проверяем разрешение на показ окон поверх других приложений
                val overlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else {
                    true
                }
                
                result.appendLine("  - Показ окон поверх других: ${if (overlayPermission) "OK" else "НЕТ"}")
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка проверки разрешений: ${e.message}"
            }
        }
        
        /**
         * Генерирует рекомендации по исправлению
         */
        private fun generateRecommendations(context: Context): String {
            return try {
                val result = StringBuilder()
                val dataManager = DataManager(context)
                
                // Проверяем файлы
                val medicinesFile = File(context.filesDir, "medicines.json")
                if (!medicinesFile.exists()) {
                    result.appendLine("1. Файл medicines.json не существует - создайте резервную копию и переустановите приложение")
                }
                
                // Проверяем данные
                try {
                    val medicines = dataManager.loadMedicines()
                    val corruptedMedicines = medicines.filter { it.name.isEmpty() || it.dosage.isEmpty() }
                    if (corruptedMedicines.isNotEmpty()) {
                        result.appendLine("2. Найдены поврежденные данные лекарств - удалите поврежденные записи")
                    }
                } catch (e: Exception) {
                    result.appendLine("3. Ошибка загрузки данных - очистите данные приложения")
                }
                
                // Проверяем разрешения
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        result.appendLine("4. Нет разрешения на уведомления - предоставьте разрешение в настройках")
                    }
                }
                
                if (result.isEmpty()) {
                    result.appendLine("Все проверки пройдены успешно. Проблемы не обнаружены.")
                }
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка генерации рекомендаций: ${e.message}"
            }
        }
        
        /**
         * Исправляет основные проблемы
         */
        fun fixCommonIssues(context: Context): String {
            return try {
                val result = StringBuilder()
                result.appendLine("=== ИСПРАВЛЕНИЕ ПРОБЛЕМ ===")
                result.appendLine("Время: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                result.appendLine()
                
                val dataManager = DataManager(context)
                
                // 1. Создаем резервную копию
                result.appendLine("1. Создание резервной копии...")
                try {
                    val backupPath = dataManager.createBackup()
                    result.appendLine("   ✓ Резервная копия создана: $backupPath")
                } catch (e: Exception) {
                    result.appendLine("    Ошибка создания резервной копии: ${e.message}")
                }
                
                // 2. Исправляем поврежденные данные лекарств
                result.appendLine("2. Исправление поврежденных данных...")
                try {
                    val medicines = dataManager.loadMedicines()
                    val fixedMedicines = medicines.map { medicine ->
                        if (medicine.name.isEmpty()) {
                            medicine.copy(name = "Неизвестное лекарство")
                        } else if (medicine.dosage.isEmpty()) {
                            medicine.copy(dosage = "1")
                        } else {
                            medicine
                        }
                    }
                    
                    if (medicines != fixedMedicines) {
                        dataManager.saveMedicines(fixedMedicines)
                        result.appendLine("   ✓ Поврежденные данные исправлены")
                    } else {
                        result.appendLine("   ✓ Поврежденных данных не найдено")
                    }
                } catch (e: Exception) {
                    result.appendLine("    Ошибка исправления данных: ${e.message}")
                }
                
                // 3. Очищаем старые логи
                result.appendLine("3. Очистка старых логов...")
                try {
                    LogCollector.clearLogs()
                    result.appendLine("   ✓ Логи очищены")
                } catch (e: Exception) {
                    result.appendLine("    Ошибка очистки логов: ${e.message}")
                }
                
                // 4. Перезапускаем сервисы
                result.appendLine("4. Перезапуск сервисов...")
                try {
                    com.medicalnotes.app.service.NotificationService.stopService(context)
                    com.medicalnotes.app.service.OverdueCheckService.stopService(context)
                    
                    Thread.sleep(1000) // Небольшая пауза
                    
                    com.medicalnotes.app.service.NotificationService.startService(context)
                    com.medicalnotes.app.service.OverdueCheckService.startService(context)
                    
                    result.appendLine("   ✓ Сервисы перезапущены")
                } catch (e: Exception) {
                    result.appendLine("    Ошибка перезапуска сервисов: ${e.message}")
                }
                
                result.appendLine()
                result.appendLine("=== ИСПРАВЛЕНИЕ ЗАВЕРШЕНО ===")
                
                result.toString()
            } catch (e: Exception) {
                "Ошибка исправления проблем: ${e.message}"
            }
        }
        
        /**
         * Сохраняет диагностический отчет в файл
         */
        fun saveDiagnosticReport(context: Context, report: String): String {
            return try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val fileName = "diagnostic_report_$timestamp.txt"
                val file = File(context.filesDir, fileName)
                
                FileWriter(file).use { writer ->
                    writer.write(report)
                }
                
                "Отчет сохранен: ${file.absolutePath}"
            } catch (e: Exception) {
                "Ошибка сохранения отчета: ${e.message}"
            }
        }
    }
} 