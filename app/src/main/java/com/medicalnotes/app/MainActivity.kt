package com.medicalnotes.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.MainMedicineAdapter
import com.medicalnotes.app.adapters.MultiMedicineAdapter
import com.medicalnotes.app.utils.MedicineGroupingUtil
import com.medicalnotes.app.utils.DataMigrationManager
import com.medicalnotes.app.databinding.ActivityMainBinding
import android.widget.TextView
import android.widget.Toast
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.UserPreferencesRepository
import com.medicalnotes.app.service.NotificationService
import com.medicalnotes.app.utils.ButtonManager
import com.medicalnotes.app.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import com.medicalnotes.app.utils.TestDataGenerator

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var buttonManager: ButtonManager
    private lateinit var todayMedicineAdapter: MainMedicineAdapter
    private lateinit var multiMedicineAdapter: MultiMedicineAdapter
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private var overdueVibrationHandler: android.os.Handler? = null
    private var updateHandler: android.os.Handler? = null
    
    // Система логирования
    private val logBuffer = StringBuilder()
    private val maxLogLines = 100
    
    // Функции для логирования
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logMessage = "[$timestamp] $message\n"
        
        // Добавляем в буфер
        logBuffer.append(logMessage)
        
        // Ограничиваем количество строк
        val lines = logBuffer.split("\n")
        if (lines.size > maxLogLines) {
            val excessLines = lines.size - maxLogLines
            val startIndex = logBuffer.indexOf("\n") + 1
            for (i in 0 until excessLines) {
                val nextIndex = logBuffer.indexOf("\n", startIndex)
                if (nextIndex != -1) {
                    logBuffer.delete(startIndex, nextIndex + 1)
                }
            }
        }
        
        // Обновляем UI только если binding инициализирован
        if (::binding.isInitialized) {
            runOnUiThread {
                try {
                    binding.textViewLogs.text = logBuffer.toString()
                    
                    // Прокручиваем вниз
                    if (binding.textViewLogs.layout != null) {
                        val scrollAmount = binding.textViewLogs.layout.getLineTop(binding.textViewLogs.lineCount) - binding.textViewLogs.height
                        if (scrollAmount > 0) {
                            binding.textViewLogs.scrollTo(0, scrollAmount)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Ошибка обновления логов в UI", e)
                }
            }
        }
        
        // Также записываем в системный лог
        android.util.Log.d("MainActivity", message)
    }
    
    private fun clearLogs() {
        logBuffer.clear()
        if (::binding.isInitialized) {
            binding.textViewLogs.text = ""
        }
        addLog("Логи очищены")
    }
    
    private fun toggleLogsVisibility() {
        if (::binding.isInitialized) {
            val isVisible = binding.layoutLogs.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutLogs.visibility = View.GONE
                binding.buttonToggleLogs.text = "ПОКАЗАТЬ ЛОГИ"
                addLog("Логи скрыты")
            } else {
                binding.layoutLogs.visibility = View.VISIBLE
                binding.buttonToggleLogs.text = "СКРЫТЬ ЛОГИ"
                addLog("Логи показаны")
            }
        }
    }
    
    private fun exportDataForDebug() {
        try {
            addLog("Экспорт XML данных...")
            
            // Получаем данные напрямую из DataManager
            val dataManager = com.medicalnotes.app.utils.DataManager(this)
            val allMedicines = dataManager.loadMedicines()
            
            addLog("Загружено лекарств из файла: ${allMedicines.size}")
            
            // Создаем XML отчет
            val xmlReport = StringBuilder()
            xmlReport.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            xmlReport.append("<medical_data>\n")
            xmlReport.append("  <export_info>\n")
            xmlReport.append("    <date>${java.time.LocalDate.now()}</date>\n")
            xmlReport.append("    <time>${java.time.LocalTime.now()}</time>\n")
            xmlReport.append("    <total_medicines>${allMedicines.size}</total_medicines>\n")
            xmlReport.append("  </export_info>\n")
            
            xmlReport.append("  <medicines>\n")
            allMedicines.forEach { medicine ->
                xmlReport.append("    <medicine>\n")
                xmlReport.append("      <id>${medicine.id}</id>\n")
                xmlReport.append("      <name>${medicine.name}</name>\n")
                xmlReport.append("      <dosage>${medicine.dosage}</dosage>\n")
                xmlReport.append("      <quantity>${medicine.quantity}</quantity>\n")
                xmlReport.append("      <remaining_quantity>${medicine.remainingQuantity}</remaining_quantity>\n")
                xmlReport.append("      <medicine_type>${medicine.medicineType}</medicine_type>\n")
                xmlReport.append("      <time>${medicine.time}</time>\n")
                xmlReport.append("      <notes>${medicine.notes}</notes>\n")
                xmlReport.append("      <is_active>${medicine.isActive}</is_active>\n")
                xmlReport.append("      <is_insulin>${medicine.isInsulin}</is_insulin>\n")
                xmlReport.append("      <is_missed>${medicine.isMissed}</is_missed>\n")
                xmlReport.append("      <last_taken_time>${medicine.lastTakenTime}</last_taken_time>\n")
                xmlReport.append("      <missed_count>${medicine.missedCount}</missed_count>\n")
                xmlReport.append("      <frequency>${medicine.frequency}</frequency>\n")
                xmlReport.append("      <dosage_times>${medicine.dosageTimes}</dosage_times>\n")
                xmlReport.append("      <custom_days>${medicine.customDays}</custom_days>\n")
                xmlReport.append("      <custom_times>${medicine.customTimes}</custom_times>\n")
                xmlReport.append("      <start_date>${medicine.startDate}</start_date>\n")
                xmlReport.append("      <multiple_doses>${medicine.multipleDoses}</multiple_doses>\n")
                xmlReport.append("      <doses_per_day>${medicine.dosesPerDay}</doses_per_day>\n")
                xmlReport.append("      <dose_times>${medicine.doseTimes}</dose_times>\n")
                xmlReport.append("      <created_at>${medicine.createdAt}</created_at>\n")
                xmlReport.append("      <updated_at>${medicine.updatedAt}</updated_at>\n")
                xmlReport.append("      <taken_today>${medicine.takenToday}</taken_today>\n")
                xmlReport.append("      <taken_at>${medicine.takenAt}</taken_at>\n")
                xmlReport.append("      <should_take_today>${medicine.shouldTakeToday}</should_take_today>\n")
                xmlReport.append("      <is_overdue>${medicine.isOverdue}</is_overdue>\n")
                xmlReport.append("      <group_id>${medicine.groupId}</group_id>\n")
                xmlReport.append("      <group_name>${medicine.groupName}</group_name>\n")
                xmlReport.append("      <group_order>${medicine.groupOrder}</group_order>\n")
                xmlReport.append("      <related_medicine_ids>${medicine.relatedMedicineIds}</related_medicine_ids>\n")
                xmlReport.append("      <is_part_of_group>${medicine.isPartOfGroup}</is_part_of_group>\n")
                xmlReport.append("    </medicine>\n")
            }
            xmlReport.append("  </medicines>\n")
            
            // Добавляем информацию о файлах
            xmlReport.append("  <files_info>\n")
            val medicinesFile = File(filesDir, "medicines.json")
            xmlReport.append("    <medicines_file_exists>${medicinesFile.exists()}</medicines_file_exists>\n")
            if (medicinesFile.exists()) {
                xmlReport.append("    <medicines_file_size>${medicinesFile.length()}</medicines_file_size>\n")
                xmlReport.append("    <medicines_file_last_modified>${medicinesFile.lastModified()}</medicines_file_last_modified>\n")
            }
            xmlReport.append("  </files_info>\n")
            
            xmlReport.append("</medical_data>\n")
            
            // Сохраняем XML файл
            val fileName = "medicines_data_${System.currentTimeMillis()}.xml"
            val file = File(filesDir, fileName)
            file.writeText(xmlReport.toString())
            
            addLog("XML файл сохранен: $fileName")
            addLog("Размер файла: ${file.length()} байт")
            
            // Показываем диалог для отправки
            showExportDialog(file)
            
        } catch (e: Exception) {
            addLog("Ошибка экспорта XML: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun showExportDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Экспорт данных")
            .setMessage("Отчет сохранен в файл: ${file.name}\n\nХотите отправить файл?")
            .setPositiveButton("Отправить") { _, _ ->
                shareFile(file)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun shareFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Отчет данных MedicalNotes")
                putExtra(Intent.EXTRA_TEXT, "Отчет данных приложения MedicalNotes")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Отправить отчет"))
            addLog("Файл готов к отправке")
            
        } catch (e: Exception) {
            addLog("Ошибка отправки: ${e.message}")
            Toast.makeText(this, "Ошибка отправки файла", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getVibratorStatus(): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                "Современный вибратор: ${if (vibrator.hasVibrator()) "ДОСТУПЕН" else "НЕДОСТУПЕН"}"
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                "Устаревший вибратор: ${if (vibrator.hasVibrator()) "ДОСТУПЕН" else "НЕДОСТУПЕН"}"
            }
        } catch (e: Exception) {
            "Ошибка получения статуса вибратора: ${e.message}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Теперь можно использовать addLog
            addLog("=== ЗАПУСК ПРИЛОЖЕНИЯ ===")
            addLog("Статус вибратора: ${getVibratorStatus()}")
            addLog("Binding inflated")
            addLog("Content view set")
            
            viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]
            addLog("ViewModel created")
            buttonManager = ButtonManager(this)
            addLog("ButtonManager created")
            userPreferencesRepository = UserPreferencesRepository(this)
            addLog("UserPreferencesRepository created")
            
            // Проверяем и выполняем миграцию данных
            val migrationManager = DataMigrationManager(this)
            if (migrationManager.isMigrationNeeded()) {
                addLog("=== МИГРАЦИЯ ДАННЫХ ===")
                addLog("Текущая версия данных: ${migrationManager.getCurrentDataVersion()}")
                addLog("Выполняется миграция...")
                
                val migrationSuccess = migrationManager.checkAndMigrateData()
                if (migrationSuccess) {
                    addLog("✓ Миграция данных завершена успешно")
                    android.widget.Toast.makeText(this, "Данные обновлены", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    addLog("❌ Ошибка миграции данных")
                    android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                addLog("Миграция данных не требуется")
            }
            
            setupViews()
            addLog("Views setup completed")
            setupButtons()
            addLog("Buttons setup completed")
            observeData()
            addLog("Data observation setup completed")
            startNotificationService()
            addLog("Notification service started")
            
            // Запускаем автоматическое обновление каждую минуту
            startPeriodicUpdate()
            
            addLog("onCreate completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            e.printStackTrace()
            // Показываем сообщение об ошибке пользователю
            android.widget.Toast.makeText(this, "Ошибка загрузки приложения: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    

    
    private fun setupViews() {
        addLog("setupViews started")
        
        try {
            // Настройка RecyclerView для сегодняшних лекарств
            todayMedicineAdapter = MainMedicineAdapter(
                onMedicineClick = { medicine ->
                    addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯЛ' ===")
                    addLog("Лекарство: ${medicine.name} (ID: ${medicine.id})")
                    addLog("Статус вибратора ДО: ${getVibratorStatus()}")
                    
                    try {
                        // 1. Немедленно останавливаем вибрацию и звук
                        addLog("1. Останавливаем NotificationManager вибрацию...")
                        val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
                        notificationManager.stopVibration()
                        addLog("✓ NotificationManager вибрация остановлена")
                        
                        // 2. Отменяем уведомления лекарства
                        addLog("2. Отменяем уведомления лекарства...")
                        notificationManager.cancelMedicineNotification(medicine.id)
                        addLog("✓ Уведомления лекарства отменены")
                        
                        // 3. Останавливаем вибрацию просроченных лекарств
                        addLog("3. Останавливаем вибрацию просроченных лекарств...")
                        stopOverdueVibration()
                        
                        // 4. Отменяем уведомления о просроченных лекарствах
                        addLog("4. Отменяем системные уведомления...")
                        val systemNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        systemNotificationManager.cancel((medicine.id + 200000).toInt())
                        addLog("✓ Системные уведомления отменены")
                        
                        // 4.5. Принудительно останавливаем вибрацию и звук
                        addLog("4.5. Принудительно останавливаем вибрацию и звук...")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                            val vibrator = vibratorManager.defaultVibrator
                            if (vibrator.hasVibrator()) {
                                vibrator.cancel()
                                addLog("✓ Современный вибратор остановлен")
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (vibrator.hasVibrator()) {
                                vibrator.cancel()
                                addLog("✓ Устаревший вибратор остановлен")
                            }
                        }
                        
                        // Останавливаем звук
                        try {
                            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                            addLog("✓ Звук уведомлений остановлен")
                        } catch (e: Exception) {
                            addLog("❌ Ошибка остановки звука: ${e.message}")
                        }
                        
                        // 5. Отметить как принятое
                        addLog("5. Отмечаем лекарство как принятое...")
                        viewModel.markMedicineAsTaken(medicine.id)
                        addLog("✓ Лекарство отмечено как принятое")
                        
                        // 6. Отправляем уведомление о принятии лекарства
                        addLog("6. Отправляем уведомление о принятии...")
                        notificationManager.markMedicineAsTaken(medicine.id)
                        addLog("✓ Уведомление о принятии отправлено")
                        
                        // 7. Принудительно останавливаем вибрацию через broadcast
                        addLog("7. Отправляем broadcast для остановки вибрации...")
                        try {
                            val stopVibrationIntent = android.content.Intent("android.intent.action.STOP_VIBRATION")
                            sendBroadcast(stopVibrationIntent)
                            addLog("✓ Broadcast для остановки вибрации отправлен")
                        } catch (e: Exception) {
                            addLog("❌ Ошибка при отправке broadcast: ${e.message}")
                        }
                        
                        // 8. Проверяем статус вибратора ПОСЛЕ всех операций
                        addLog("8. Статус вибратора ПОСЛЕ: ${getVibratorStatus()}")
                        
                        // 9. Показываем подтверждение пользователю
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "${medicine.name} отмечено как принятое",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        
                        addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯЛ' ЗАВЕРШЕНО ===")
                        
                    } catch (e: Exception) {
                        addLog("❌ Ошибка при нажатии кнопки 'Принял': ${e.message}")
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Ошибка: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            
            // Настройка RecyclerView для группированных лекарств
            multiMedicineAdapter = MultiMedicineAdapter(
                onMultiMedicineClick = { multiItem ->
                    addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯТЬ ВСЕ' ===")
                    addLog("Время: ${multiItem.time}")
                    addLog("Количество лекарств: ${multiItem.medicines.size}")
                    
                    try {
                        // Отмечаем все лекарства в группе как принятые
                        multiItem.medicines.forEach { medicine ->
                            addLog("Отмечаем как принятое: ${medicine.name}")
                            viewModel.markMedicineAsTaken(medicine.id)
                        }
                        
                        // Останавливаем вибрацию и уведомления
                        val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
                        notificationManager.stopVibration()
                        stopOverdueVibration()
                        
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Все лекарства отмечены как принятые",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        
                        addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯТЬ ВСЕ' ЗАВЕРШЕНО ===")
                        
                    } catch (e: Exception) {
                        addLog("❌ Ошибка при нажатии кнопки 'Принять все': ${e.message}")
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Ошибка: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
            android.util.Log.d("MainActivity", "MedicineAdapter created")
            
            binding.recyclerViewTodayMedicines.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = todayMedicineAdapter
            }
            android.util.Log.d("MainActivity", "RecyclerView configured")
            
            android.util.Log.d("MainActivity", "Views setup completed")
            
            android.util.Log.d("MainActivity", "setupViews completed")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in setupViews", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun setupButtons() {
        // Кнопка управления лекарствами
        binding.buttonManageMedicines.setOnClickListener {
            addLog("Нажата кнопка: УПРАВЛЕНИЕ ЛЕКАРСТВАМИ")
            startActivity(Intent(this, ElderlyMedicineManagementActivity::class.java))
        }
        
        // Кнопка переключения режима для пожилых
        binding.buttonToggleLayout.setOnClickListener {
            addLog("Нажата кнопка: РЕЖИМ ДЛЯ ПОЖИЛЫХ")
            toggleElderlyMode()
        }
        
        // Кнопка показа/скрытия логов
        binding.buttonToggleLogs.setOnClickListener {
            toggleLogsVisibility()
        }
        
        // Кнопка экспорта данных
        binding.buttonExportData.setOnClickListener {
            exportDataForDebug()
        }

        // Кнопка тестирования групп
        binding.buttonTestGroups.setOnClickListener {
            addLog("Нажата кнопка: ТЕСТ ГРУПП")
            runAutomaticGroupTests()
        }
        
        // Кнопка очистки логов
        binding.buttonClearLogs.setOnClickListener {
            clearLogs()
        }
    }
    
    private fun observeData() {
        viewModel.todayMedicines.observe(this) { medicines ->
            addLog("=== ОБНОВЛЕНИЕ СПИСКА ЛЕКАРСТВ ===")
            addLog("Получено лекарств: ${medicines.size}")
            addLog("Текущее время: ${java.time.LocalDateTime.now()}")
            
            var hasOverdueMedicines = false
            
            medicines.forEach { medicine ->
                val status = com.medicalnotes.app.utils.DosageCalculator.getMedicineStatus(medicine)
                addLog("Отображается: ${medicine.name} - Статус: $status")
                addLog("  Время приема: ${medicine.time}")
                addLog("  Частота: ${medicine.frequency}")
                
                if (status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE) {
                    hasOverdueMedicines = true
                    addLog("⚠️ ПРОСРОЧЕНО: ${medicine.name} - запускаем вибрацию")
                    startOverdueVibrationForMedicine(medicine)
                } else if (status == com.medicalnotes.app.utils.MedicineStatus.UPCOMING) {
                    addLog("📅 ПРЕДСТОИТ: ${medicine.name} - время еще не пришло")
                } else if (status == com.medicalnotes.app.utils.MedicineStatus.TAKEN_TODAY) {
                    addLog("✅ ПРИНЯТО: ${medicine.name} - уже принято сегодня")
                } else {
                    addLog("❌ НЕ СЕГОДНЯ: ${medicine.name} - не по расписанию")
                }
            }
            
            if (!hasOverdueMedicines) {
                addLog("Просроченных лекарств нет - останавливаем вибрацию")
                stopOverdueVibration()
            }
            
            // Проверяем, нужно ли показывать группированные карточки
            val shouldShowGrouped = MedicineGroupingUtil.shouldShowGroupedCards(medicines, java.time.LocalDate.now())
            addLog("Показывать группированные карточки: $shouldShowGrouped")
            
            if (shouldShowGrouped) {
                // Показываем группированные карточки
                val groupedItems = MedicineGroupingUtil.groupMedicinesByTime(medicines)
                addLog("Создано группированных карточек: ${groupedItems.size}")
                
                // Отображаем информацию о группах времени
                groupedItems.forEach { (time, timeMedicines) ->
                    if (timeMedicines.size > 1) {
                        addLog("Время $time: ${timeMedicines.size} лекарств")
                    }
                }
                
                // Показываем обычные карточки (временно)
                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                todayMedicineAdapter.submitList(medicines)
                
                addLog("Переключились на обычные карточки")
                addLog("Используется адаптер: MainMedicineAdapter")
            } else {
                // Показываем обычные карточки
                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                todayMedicineAdapter.submitList(medicines)
                
                addLog("Переключились на обычные карточки")
                addLog("Используется адаптер: MainMedicineAdapter")
            }
        }
        
        // Загружаем настройки пользователя
        loadUserPreferences()
    }
    

    
    private fun toggleElderlyMode() {
        // Переключаем режим для пожилых
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentPreferences = userPreferencesRepository.getUserPreferences()
                val newElderlyMode = !currentPreferences.isElderlyMode
                userPreferencesRepository.toggleElderlyMode(newElderlyMode)
                
                val message = if (newElderlyMode) {
                    "Режим для пожилых включен"
                } else {
                    "Режим для пожилых выключен"
                }
                
                android.widget.Toast.makeText(
                    this@MainActivity,
                    message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Обновляем текст кнопки
                updateToggleButtonText(newElderlyMode)
                
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Ошибка при переключении режима: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun updateToggleButtonText(isElderlyMode: Boolean) {
        binding.buttonToggleLayout.text = if (isElderlyMode) {
            "ВЫКЛЮЧИТЬ РЕЖИМ ДЛЯ ПОЖИЛЫХ"
        } else {
            "ВКЛЮЧИТЬ РЕЖИМ ДЛЯ ПОЖИЛЫХ"
        }
    }
    
    private fun loadUserPreferences() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences()
                updateToggleButtonText(preferences.isElderlyMode)
            } catch (e: Exception) {
                addLog("❌ Ошибка загрузки настроек: ${e.message}")
            }
        }
    }
    

    
    private fun startNotificationService() {
        // Запускаем фоновый сервис для уведомлений
        NotificationService.startService(this)
        
        // Запрашиваем игнорирование оптимизации батареи для надежной работы уведомлений
        requestBatteryOptimizationPermission()
    }
    
    private fun requestBatteryOptimizationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadAllMedicines()
            viewModel.loadTodayMedicines()
            
            // Обрабатываем входящие уведомления
            handleIncomingNotifications()
            
            // Показываем информацию о работе в фоне
            showBackgroundWorkInfo()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleIncomingNotifications() {
        // Проверяем, было ли приложение открыто через уведомление
        intent?.let { intent ->
            when {
                intent.hasExtra("medicine_id") -> {
                    val medicineId = intent.getLongExtra("medicine_id", -1)
                    if (medicineId != -1L) {
                        // Подсвечиваем лекарство, на которое пришло уведомление
                        highlightMedicine(medicineId)
                        
                        // Показываем сообщение о необходимости принять лекарство
                        val retryAttempt = intent.getIntExtra("retry_attempt", 1)
                        val message = if (retryAttempt > 1) {
                            "Это ${retryAttempt}-е напоминание о приеме лекарства!"
                        } else {
                            "Время принять лекарство!"
                        }
                        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                intent.hasExtra("medicine_taken") -> {
                    val medicineId = intent.getLongExtra("medicine_taken", -1)
                    if (medicineId != -1L) {
                        android.widget.Toast.makeText(this, "Лекарство принято!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                intent.hasExtra("emergency_escalation") -> {
                    android.widget.Toast.makeText(
                        this, 
                        "КРИТИЧЕСКОЕ НАПОМИНАНИЕ! Примите лекарство немедленно!", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                intent.hasExtra("overdue") -> {
                    val medicineId = intent.getLongExtra("medicine_id", -1)
                    if (medicineId != -1L) {
                        // Подсвечиваем просроченное лекарство
                        highlightMedicine(medicineId)
                        
                        android.widget.Toast.makeText(
                            this, 
                            "ПРОСРОЧЕНО! Выпейте препарат немедленно!", 
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun highlightMedicine(medicineId: Long) {
        // Находим лекарство в списке и подсвечиваем его
        viewModel.todayMedicines.value?.find { it.id == medicineId }?.let { medicine ->
            // Можно добавить анимацию или выделение для этого лекарства
            android.util.Log.d("MainActivity", "Highlighting medicine: ${medicine.name}")
        }
    }
    

    
    private fun showBackgroundWorkInfo() {
        // Показываем информацию только при первом запуске
        val sharedPrefs = getSharedPreferences("medical_notes_prefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPrefs.getBoolean("is_first_run", true)
        
        if (isFirstRun) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Работа в фоновом режиме")
                .setMessage("Приложение будет работать в фоновом режиме и отправлять уведомления даже когда оно закрыто.\n\n" +
                        "Для надежной работы:\n" +
                        "• Разрешите игнорирование оптимизации батареи\n" +
                        "• Не закрывайте приложение из списка недавних\n" +
                        "• Уведомления будут приходить точно в назначенное время")
                .setPositiveButton("Понятно") { _, _ ->
                    sharedPrefs.edit().putBoolean("is_first_run", false).apply()
                }
                .setCancelable(false)
                .show()
        }
        }
    
    private fun startOverdueVibrationForMedicine(medicine: com.medicalnotes.app.models.Medicine) {
        addLog("Запуск вибрации для просроченного лекарства: ${medicine.name}")
        
        try {
            // Создаем Handler если его нет
            if (overdueVibrationHandler == null) {
                overdueVibrationHandler = android.os.Handler(android.os.Looper.getMainLooper())
                addLog("Создан новый Handler для вибрации")
            }
            
            // Функция для вибрации
            val vibrateRunnable = object : Runnable {
                override fun run() {
                    try {
                        addLog("Вибрация и звук для ${medicine.name}...")
                        
                        // Вибрация
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                            val vibrator = vibratorManager.defaultVibrator
                            if (vibrator.hasVibrator()) {
                                val vibrationEffect = android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                                vibrator.vibrate(vibrationEffect)
                                addLog("✓ Вибрация выполнена (современный API)")
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (vibrator.hasVibrator()) {
                                vibrator.vibrate(1000)
                                addLog("✓ Вибрация выполнена (устаревший API)")
                            }
                        }
                        
                        // Звук звонка
                        try {
                            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                            val ringtone = android.media.RingtoneManager.getRingtone(this@MainActivity, notificationUri)
                            
                            if (ringtone != null) {
                                ringtone.play()
                                addLog("✓ Звук звонка воспроизведен")
                            } else {
                                addLog("⚠ Звук звонка недоступен")
                            }
                        } catch (e: Exception) {
                            addLog("❌ Ошибка воспроизведения звука: ${e.message}")
                        }
                    } catch (e: Exception) {
                        addLog("❌ Ошибка вибрации: ${e.message}")
                    }
                    
                                            // Повторяем через 3 секунды
                        overdueVibrationHandler?.postDelayed(this, 3000)
                }
            }
            
            // Запускаем вибрацию
            overdueVibrationHandler?.post(vibrateRunnable)
            addLog("✓ Вибрация запущена для ${medicine.name}")
            
        } catch (e: Exception) {
            addLog("❌ Ошибка запуска вибрации: ${e.message}")
        }
    }
    
    private fun startPeriodicUpdate() {
        addLog("Запуск автоматического обновления каждую минуту")
        
        updateHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val updateRunnable = object : Runnable {
            override fun run() {
                try {
                    addLog("=== АВТОМАТИЧЕСКОЕ ОБНОВЛЕНИЕ ===")
                    viewModel.loadTodayMedicines()
                } catch (e: Exception) {
                    addLog("❌ Ошибка автоматического обновления: ${e.message}")
                }
                
                // Повторяем через 1 минуту
                updateHandler?.postDelayed(this, 60000)
            }
        }
        
        updateHandler?.post(updateRunnable)
    }
    
    private fun stopOverdueVibration() {
        addLog("=== ОСТАНОВКА ВИБРАЦИИ ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
        
        try {
            // Останавливаем Handler для вибрации просроченных лекарств
            if (overdueVibrationHandler != null) {
                overdueVibrationHandler?.removeCallbacksAndMessages(null)
                overdueVibrationHandler = null
                addLog("Handler для вибрации остановлен")
            } else {
                addLog("Handler для вибрации уже был null")
            }
            
            // Останавливаем вибрацию современным способом
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                if (vibrator.hasVibrator()) {
                    vibrator.cancel()
                    addLog("Современный вибратор остановлен")
                } else {
                    addLog("Современный вибратор недоступен")
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (vibrator.hasVibrator()) {
                    vibrator.cancel()
                    addLog("Устаревший вибратор остановлен")
                } else {
                    addLog("Устаревший вибратор недоступен")
                }
            }
            
            // Останавливаем звук
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                addLog("✓ Звук уведомлений остановлен")
            } catch (e: Exception) {
                addLog("❌ Ошибка остановки звука: ${e.message}")
            }
            
            addLog("Вибрация и звук просроченных лекарств остановлены успешно")
        } catch (e: Exception) {
            addLog("❌ Ошибка при остановке вибрации просроченных лекарств: ${e.message}")
        }
    }
    
    /**
     * Автоматическое тестирование функционала групп
     */
    private fun testGroupFunctionality() {
        addLog("=== НАЧАЛО ТЕСТИРОВАНИЯ ГРУПП ===")
        
        try {
            // 1. Тест создания групп
            testGroupCreation()
            
            // 2. Тест добавления лекарств в группы
            testAddingMedicinesToGroups()
            
            // 3. Тест отображения групп
            testGroupDisplay()
            
            // 4. Тест изменения порядка в группах
            testGroupOrdering()
            
            // 5. Тест удаления из групп
            testGroupRemoval()
            
            // 6. Тест группировки по времени
            testTimeGrouping()
            
            addLog("=== ТЕСТИРОВАНИЕ ГРУПП ЗАВЕРШЕНО ===")
            
        } catch (e: Exception) {
            addLog("ОШИБКА В ТЕСТИРОВАНИИ: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun testGroupCreation() {
        addLog("1. Тест создания групп...")
        
        val testGroups = listOf("Витамины", "Сердечные", "Диабет")
        val existingGroups = emptyList<String>() // Временно пустой список
        
        addLog("Существующие группы: ${existingGroups.joinToString(", ")}")
        
        // Проверяем, что группы создаются корректно
        testGroups.forEach { groupName ->
            if (!existingGroups.contains(groupName)) {
                addLog("Создаем группу: $groupName")
                // Здесь должна быть логика создания группы
            }
        }
    }
    
    private fun testAddingMedicinesToGroups() {
        addLog("2. Тест добавления лекарств в группы...")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        
        testMedicines.forEach { medicine ->
            addLog("Лекарство: ${medicine.name}")
            addLog("  - Группа: ${medicine.groupName}")
            addLog("  - Порядок: ${medicine.groupOrder}")
            addLog("  - Время: ${medicine.time}")
            
            // Проверяем корректность данных
            if (medicine.groupName.isNotEmpty() && medicine.groupOrder <= 0) {
                addLog("  ⚠️ ОШИБКА: Группа указана, но порядок <= 0")
            }
            
            if (medicine.groupName.isEmpty() && medicine.groupOrder > 0) {
                addLog("  ⚠️ ОШИБКА: Порядок указан, но группа не указана")
            }
        }
    }
    
    private fun testGroupDisplay() {
        addLog("3. Тест отображения групп...")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val groupedMedicines = testMedicines.groupBy { it.groupName }
        
        groupedMedicines.forEach { (groupName, groupMedicines) ->
            if (groupName.isNotEmpty()) {
                addLog("Группа '$groupName': ${groupMedicines.size} лекарств")
                
                // Проверяем порядок
                val sortedByOrder = groupMedicines.sortedBy { it.groupOrder }
                val hasOrderIssues = groupMedicines.any { it.groupOrder <= 0 }
                
                if (hasOrderIssues) {
                    addLog("  ⚠️ ОШИБКА: Некорректный порядок в группе")
                }
                
                // Проверяем дубликаты порядка
                val orderCounts = groupMedicines.groupBy { it.groupOrder }
                val duplicates = orderCounts.filter { it.value.size > 1 }
                
                if (duplicates.isNotEmpty()) {
                    addLog("  ⚠️ ОШИБКА: Дубликаты порядка: ${duplicates.keys}")
                }
            }
        }
    }
    
    private fun testGroupOrdering() {
        addLog("4. Тест изменения порядка в группах...")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val groupedMedicines = testMedicines.groupBy { it.groupName }
        
        groupedMedicines.forEach { (groupName, groupMedicines) ->
            if (groupName.isNotEmpty() && groupMedicines.size > 1) {
                addLog("Тестируем порядок в группе '$groupName'")
                
                // Проверяем последовательность порядка
                val orders = groupMedicines.map { it.groupOrder }.sorted()
                val expectedOrders = (1..groupMedicines.size).toList()
                
                if (orders != expectedOrders) {
                    addLog("  ⚠️ ОШИБКА: Нарушена последовательность порядка")
                    addLog("    Ожидалось: $expectedOrders")
                    addLog("    Фактически: $orders")
                }
            }
        }
    }
    
    private fun testGroupRemoval() {
        addLog("5. Тест удаления из групп...")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val medicinesInGroups = testMedicines.filter { it.groupName.isNotEmpty() }
        
        addLog("Лекарств в группах: ${medicinesInGroups.size}")
        
        // Проверяем, что лекарства без групп не имеют groupOrder
        val medicinesWithoutGroups = testMedicines.filter { it.groupName.isEmpty() }
        val invalidOrder = medicinesWithoutGroups.any { it.groupOrder > 0 }
        
        if (invalidOrder) {
            addLog("  ⚠️ ОШИБКА: Лекарства без групп имеют порядок > 0")
        }
    }
    
    private fun testTimeGrouping() {
        addLog("6. Тест группировки по времени...")
        
        val testMedicines = TestDataGenerator.generateTestMedicinesWithGroups()
        val timeGroups = testMedicines.groupBy { it.time }
        
        timeGroups.forEach { (time, timeMedicines) ->
            if (timeMedicines.size > 1) {
                addLog("Время $time: ${timeMedicines.size} лекарств")
                
                // Проверяем, что лекарства в одном времени имеют разные группы или не имеют групп
                val groupNames = timeMedicines.map { it.groupName }.distinct()
                
                if (groupNames.size == 1 && groupNames[0].isNotEmpty()) {
                    addLog("  ⚠️ ВНИМАНИЕ: Все лекарства в одно время в одной группе")
                }
            }
        }
    }
    
    /**
     * Запускает автоматические тесты групп
     */
    private fun runAutomaticGroupTests() {
        addLog("=== ЗАПУСК АВТОМАТИЧЕСКИХ ТЕСТОВ ГРУПП ===")
        
        try {
            val testResults = com.medicalnotes.app.utils.GroupTestSuite.runAllTests()
            
            testResults.forEach { result ->
                addLog(result)
            }
            
            // Анализ результатов
            val errors = testResults.filter { it.contains("❌ ОШИБКА") }
            val warnings = testResults.filter { it.contains("⚠️ ВНИМАНИЕ") }
            
            addLog("=== РЕЗУЛЬТАТЫ ТЕСТИРОВАНИЯ ===")
            addLog("Всего тестов: ${testResults.size}")
            addLog("Ошибок: ${errors.size}")
            addLog("Предупреждений: ${warnings.size}")
            
            if (errors.isNotEmpty()) {
                addLog("❌ НАЙДЕНЫ КРИТИЧЕСКИЕ ОШИБКИ!")
                errors.forEach { error ->
                    addLog("  - $error")
                }
            }
            
            if (warnings.isNotEmpty()) {
                addLog("⚠️ НАЙДЕНЫ ПРЕДУПРЕЖДЕНИЯ:")
                warnings.forEach { warning ->
                    addLog("  - $warning")
                }
            }
            
            if (errors.isEmpty() && warnings.isEmpty()) {
                addLog("✅ ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО!")
            }
            
        } catch (e: Exception) {
            addLog("КРИТИЧЕСКАЯ ОШИБКА В ТЕСТИРОВАНИИ: ${e.message}")
            e.printStackTrace()
        }
    }
    


}  