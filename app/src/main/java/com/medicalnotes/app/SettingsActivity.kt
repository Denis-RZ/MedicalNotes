package com.medicalnotes.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.medicalnotes.app.databinding.ActivitySettingsBinding
import com.medicalnotes.app.utils.AppSettings
import com.medicalnotes.app.utils.VersionUtils
import com.medicalnotes.app.viewmodels.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настройка обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Логика обработки кнопки "Назад"
                if (isEnabled) {
                    finish()
                }
            }
        })
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[SettingsViewModel::class.java]
        
        setupViews()
        setupListeners()
        observeData()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Настройки"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Отображение информации о версии
        binding.textViewVersion.text = "Версия: ${VersionUtils.getShortVersionInfo(this)}"
        binding.textViewBuildTime.text = "Обновлено: ${VersionUtils.getLastUpdateTime(this)}"
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val installTime = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(packageInfo.firstInstallTime))
            binding.textViewInstallTime.text = "Установлено: $installTime"
        } catch (e: Exception) {
            binding.textViewInstallTime.text = "Установлено: Неизвестно"
        }
    }
    
    private fun setupListeners() {
        // Слайдеры
        binding.sliderAdvanceMinutes.addOnChangeListener { _, value, _ ->
            binding.textAdvanceMinutes.text = "${value.toInt()} минут"
        }
        
        binding.sliderLowStockThreshold.addOnChangeListener { _, value, _ ->
            binding.textLowStockThreshold.text = "${value.toInt()} шт."
        }
        
        binding.sliderMaxBackups.addOnChangeListener { _, value, _ ->
            binding.textMaxBackups.text = "${value.toInt()} копий"
        }
        
        // Кнопки
        binding.buttonDataBackup.setOnClickListener {
            startActivity(Intent(this, DataBackupActivity::class.java))
        }
        
        binding.buttonCustomizeButtons.setOnClickListener {
            startActivity(Intent(this, ButtonCustomizationActivity::class.java))
        }
        
        binding.buttonNotificationManager.setOnClickListener {
            startActivity(Intent(this, NotificationManagerActivity::class.java))
        }
        
        binding.buttonCreateBackup.setOnClickListener {
            viewModel.createBackup()
        }
        
        binding.buttonRestoreBackup.setOnClickListener {
            showBackupRestoreDialog()
        }
        
        binding.buttonBackupList.setOnClickListener {
            showBackupListDialog()
        }
        
        binding.buttonDataStatistics.setOnClickListener {
            showDataStatisticsDialog()
        }
        
        binding.buttonValidateData.setOnClickListener {
            viewModel.validateDataIntegrity()
        }
        
        binding.buttonClearData.setOnClickListener {
            showClearDataDialog()
        }
        
        binding.buttonCrashReport.setOnClickListener {
            CrashReportActivity.start(this)
        }
        
        binding.buttonTestOverdueMedicine.setOnClickListener {
            testOverdueMedicine()
        }
        
        binding.buttonStopNotifications.setOnClickListener {
            stopAllNotifications()
        }
        
        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun observeData() {
        viewModel.settings.observe(this) { settings ->
            settings?.let {
                binding.switchNotifications.isChecked = true // Уведомления всегда включены
                binding.sliderAdvanceMinutes.value = it.notificationAdvanceMinutes.toFloat()
                binding.textAdvanceMinutes.text = "${it.notificationAdvanceMinutes} минут"
                binding.sliderLowStockThreshold.value = it.lowStockThreshold.toFloat()
                binding.textLowStockThreshold.text = "${it.lowStockThreshold} шт."
                binding.switchAutoBackup.isChecked = it.autoBackup
                binding.switchDataCompression.isChecked = it.dataCompression
                binding.sliderMaxBackups.value = it.maxBackups.toFloat()
                binding.textMaxBackups.text = "${it.maxBackups} копий"
                binding.switchHighContrast.isChecked = false // По умолчанию выключен
            }
        }
        
        //  ИСПРАВЛЕНО: Загружаем настройки вибрации и звука из UserPreferences
        viewModel.userPreferences.observe(this) { preferences ->
            preferences?.let {
                binding.switchVibration.isChecked = it.enableVibration
                binding.switchSound.isChecked = it.enableSound
                android.util.Log.d("SettingsActivity", "Настройки загружены: вибрация=${it.enableVibration}, звук=${it.enableSound}")
            }
        }
        
        viewModel.message.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun saveSettings() {
        val settings = AppSettings(
            autoBackup = binding.switchAutoBackup.isChecked,
            backupInterval = 24 * 60 * 60 * 1000, // 24 часа
            maxBackups = binding.sliderMaxBackups.value.toInt(),
            dataCompression = binding.switchDataCompression.isChecked,
            encryptionEnabled = false,
            notificationAdvanceMinutes = binding.sliderAdvanceMinutes.value.toInt(),
            lowStockThreshold = binding.sliderLowStockThreshold.value.toInt(),
            emergencyContacts = emptyList()
        )
        
        viewModel.updateSettings(settings)
        
        //  ИСПРАВЛЕНО: Сохраняем настройки вибрации и звука в UserPreferences
        viewModel.updateUserPreferences(
            enableVibration = binding.switchVibration.isChecked,
            enableSound = binding.switchSound.isChecked
        )
        
        android.util.Log.d("SettingsActivity", "Настройки сохранены: вибрация=${binding.switchVibration.isChecked}, звук=${binding.switchSound.isChecked}")
    }
    
    private fun showBackupRestoreDialog() {
        val backups = viewModel.backupList.value ?: emptyList()
        if (backups.isEmpty()) {
            Toast.makeText(this, "Нет доступных резервных копий", Toast.LENGTH_SHORT).show()
            return
        }
        
        val backupNames = backups.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Выберите резервную копию для восстановления")
            .setItems(backupNames) { _, which ->
                val selectedBackup = backups[which]
                showConfirmRestoreDialog(selectedBackup)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showConfirmRestoreDialog(backupFile: File) {
        AlertDialog.Builder(this)
            .setTitle("Подтверждение восстановления")
            .setMessage("Восстановить данные из резервной копии '${backupFile.name}'? Текущие данные будут заменены.")
            .setPositiveButton("Восстановить") { _, _ ->
                viewModel.restoreFromBackup(backupFile.absolutePath)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showBackupListDialog() {
        val backups = viewModel.backupList.value ?: emptyList()
        if (backups.isEmpty()) {
            Toast.makeText(this, "Нет резервных копий", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val backupInfo = backups.map { file ->
            "${file.name}\nРазмер: ${file.length() / 1024} KB\nДата: ${dateFormat.format(Date(file.lastModified()))}"
        }.joinToString("\n\n")
        
        AlertDialog.Builder(this)
            .setTitle("Список резервных копий")
            .setMessage(backupInfo)
            .setPositiveButton("Очистить старые", { _, _ ->
                viewModel.cleanupOldBackups()
            })
            .setNegativeButton("Закрыть", null)
            .show()
    }
    
    private fun showDataStatisticsDialog() {
        val statistics = viewModel.dataStatistics.value ?: emptyMap()
        val statsText = statistics.entries.joinToString("\n") { (key, value) ->
            when (key) {
                "medicines_count" -> "Всего лекарств: $value"
                "active_medicines" -> "Активных лекарств: $value"
                "buttons_count" -> "Всего кнопок: $value"
                "visible_buttons" -> "Видимых кнопок: $value"
                "low_stock_medicines" -> "Лекарств с низким запасом: $value"
                "backup_count" -> "Резервных копий: $value"
                "config_version" -> "Версия конфигурации: $value"
                else -> "$key: $value"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Статистика данных")
            .setMessage(statsText)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Очистка данных")
            .setMessage("Вы уверены, что хотите удалить ВСЕ данные? Это действие нельзя отменить.")
            .setPositiveButton("Удалить все") { _, _ ->
                viewModel.clearAllData()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    /**
     * ДОБАВЛЕНО: Тестирование просроченного лекарства
     */
    private fun testOverdueMedicine() {
        try {
            android.util.Log.d("SettingsActivity", "=== ТЕСТИРОВАНИЕ ПРОСРОЧЕННОГО ЛЕКАРСТВА ===")
            
            // Показываем диалог подтверждения
            AlertDialog.Builder(this)
                .setTitle("Тест просроченного лекарства")
                .setMessage("Создать тестовое лекарство на 2 минуты вперед и закрыть приложение?\n\nЧерез 2 минуты появится уведомление с вибрацией и звуком.")
                .setPositiveButton("Создать и закрыть") { _, _ ->
                    createTestMedicineAndClose()
                }
                .setNegativeButton("Отмена", null)
                .show()
                
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Ошибка тестирования просроченного лекарства", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * ДОБАВЛЕНО: Создание тестового лекарства и закрытие приложения
     */
    private fun createTestMedicineAndClose() {
        try {
            android.util.Log.d("SettingsActivity", "Создание тестового лекарства...")
            
            // Создаем тестовое лекарство
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this)
            val testMedicine = notificationManager.createTestOverdueMedicine(this)
            
            // Показываем подтверждение
            Toast.makeText(this, 
                "Тестовое лекарство создано!\nВремя приема: ${testMedicine.time}\nПриложение закроется через 3 секунды...", 
                Toast.LENGTH_LONG
            ).show()
            
            android.util.Log.d("SettingsActivity", "Тестовое лекарство создано: ${testMedicine.name} на ${testMedicine.time}")
            
            // Закрываем приложение через 3 секунды
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    android.util.Log.d("SettingsActivity", "Закрытие приложения...")
                    
                    // Закрываем все Activity
                    finishAffinity()
                    
                    // Принудительно завершаем процесс (опционально)
                    android.os.Process.killProcess(android.os.Process.myPid())
                    
                } catch (e: Exception) {
                    android.util.Log.e("SettingsActivity", "Ошибка закрытия приложения", e)
                }
            }, 3000) // 3 секунды
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Ошибка создания тестового лекарства", e)
            Toast.makeText(this, "Ошибка создания тестового лекарства: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * ДОБАВЛЕНО: Принудительная остановка всех уведомлений
     */
    private fun stopAllNotifications() {
        try {
            android.util.Log.d("SettingsActivity", "=== ПРИНУДИТЕЛЬНАЯ ОСТАНОВКА УВЕДОМЛЕНИЙ ===")
            
            // Показываем диалог подтверждения
            AlertDialog.Builder(this)
                .setTitle("Остановить уведомления")
                .setMessage("Принудительно остановить все вибрации, звуки и уведомления?\n\nЭто может быть полезно, если стандартные кнопки на карточке лекарства не работают.")
                .setPositiveButton("Остановить все") { _, _ ->
                    forceStopAllNotifications()
                }
                .setNegativeButton("Отмена", null)
                .show()
                
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Ошибка остановки уведомлений", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * ДОБАВЛЕНО: Принудительная остановка всех уведомлений
     */
    private fun forceStopAllNotifications() {
        try {
            android.util.Log.d("SettingsActivity", "Принудительная остановка уведомлений...")
            
            // Останавливаем все уведомления
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this)
            notificationManager.forceStopAllNotifications()
            
            // Показываем подтверждение
            Toast.makeText(this, "Все уведомления, вибрации и звуки остановлены!", Toast.LENGTH_LONG).show()
            
            android.util.Log.d("SettingsActivity", "✓ Принудительная остановка завершена")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Ошибка принудительной остановки", e)
            Toast.makeText(this, "Ошибка остановки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Используем современный подход вместо устаревшего onBackPressed()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Для Android 12+ используем onBackPressedDispatcher
            onBackPressedDispatcher.onBackPressed()
        } else {
            // Для старых версий используем finish()
            finish()
        }
        return true
    }
} 