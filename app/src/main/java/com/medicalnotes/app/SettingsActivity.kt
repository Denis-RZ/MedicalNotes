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

class SettingsActivity : BaseActivity() {
    
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
        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Отображение информации о версии
        binding.textViewVersion.text = "${getString(R.string.version)}: ${VersionUtils.getShortVersionInfo(this)}"
        binding.textViewBuildTime.text = "${getString(R.string.updated)}: ${VersionUtils.getLastUpdateTime(this)}"
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val installTime = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(packageInfo.firstInstallTime))
            binding.textViewInstallTime.text = "${getString(R.string.installed)}: $installTime"
        } catch (e: Exception) {
            binding.textViewInstallTime.text = "${getString(R.string.installed)}: ${getString(R.string.unknown)}"
        }
    }
    
    private fun setupListeners() {
        // Слайдеры
        binding.sliderAdvanceMinutes.addOnChangeListener { _, value, _ ->
            binding.textAdvanceMinutes.text = "${value.toInt()} ${getString(R.string.minutes)}"
        }
        
        binding.sliderLowStockThreshold.addOnChangeListener { _, value, _ ->
            binding.textLowStockThreshold.text = "${value.toInt()} ${getString(R.string.pieces)}"
        }
        
        binding.sliderMaxBackups.addOnChangeListener { _, value, _ ->
            binding.textMaxBackups.text = "${value.toInt()} ${getString(R.string.copies)}"
        }
        
        // Кнопки
        binding.buttonDataBackup.setOnClickListener {
            startActivity(Intent(this, DataBackupActivity::class.java))
        }
        
        binding.buttonCustomizeButtons.setOnClickListener {
            startActivity(Intent(this, ButtonCustomizationActivity::class.java))
        }
        
        binding.buttonLanguageSettings.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }
        
        binding.buttonLanguageTest.setOnClickListener {
            startActivity(Intent(this, LanguageTestActivity::class.java))
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
                binding.textAdvanceMinutes.text = "${it.notificationAdvanceMinutes} ${getString(R.string.minutes)}"
                binding.sliderLowStockThreshold.value = it.lowStockThreshold.toFloat()
                binding.textLowStockThreshold.text = "${it.lowStockThreshold} ${getString(R.string.pieces)}"
                binding.switchAutoBackup.isChecked = it.autoBackup
                binding.switchDataCompression.isChecked = it.dataCompression
                binding.sliderMaxBackups.value = it.maxBackups.toFloat()
                binding.textMaxBackups.text = "${it.maxBackups} ${getString(R.string.copies)}"
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
            Toast.makeText(this, getString(R.string.no_backups_available), Toast.LENGTH_SHORT).show()
            return
        }
        
        val backupNames = backups.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_backup_restore))
            .setItems(backupNames) { _, which ->
                val selectedBackup = backups[which]
                showConfirmRestoreDialog(selectedBackup)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showConfirmRestoreDialog(backupFile: File) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_restore))
            .setMessage(getString(R.string.restore_backup_message, backupFile.name))
            .setPositiveButton(getString(R.string.restore)) { _, _ ->
                viewModel.restoreFromBackup(backupFile.absolutePath)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showBackupListDialog() {
        val backups = viewModel.backupList.value ?: emptyList()
        if (backups.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_backups), Toast.LENGTH_SHORT).show()
            return
        }
        
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val backupInfo = backups.map { file ->
            "${file.name}\n${getString(R.string.size)}: ${file.length() / 1024} ${getString(R.string.kb)}\n${getString(R.string.date)}: ${dateFormat.format(Date(file.lastModified()))}"
        }.joinToString("\n\n")
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.backup_list))
            .setMessage(backupInfo)
            .setPositiveButton(getString(R.string.clear_old), { _, _ ->
                viewModel.cleanupOldBackups()
            })
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }
    
    private fun showDataStatisticsDialog() {
        val statistics = viewModel.dataStatistics.value ?: emptyMap()
        val statsText = statistics.entries.joinToString("\n") { (key, value) ->
            when (key) {
                "medicines_count" -> "${getString(R.string.total_medicines)}: $value"
                "active_medicines" -> "${getString(R.string.active_medicines)}: $value"
                "buttons_count" -> "${getString(R.string.total_buttons)}: $value"
                "visible_buttons" -> "${getString(R.string.visible_buttons)}: $value"
                "low_stock_medicines" -> "${getString(R.string.low_stock_medicines)}: $value"
                "backup_count" -> "${getString(R.string.backup_count)}: $value"
                "config_version" -> "${getString(R.string.config_version)}: $value"
                else -> "$key: $value"
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.data_statistics))
            .setMessage(statsText)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_data))
            .setMessage(getString(R.string.clear_data_confirm))
            .setPositiveButton(getString(R.string.delete_all)) { _, _ ->
                viewModel.clearAllData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
                .setTitle(getString(R.string.test_overdue_medicine))
                .setMessage(getString(R.string.test_overdue_message))
                .setPositiveButton(getString(R.string.create_and_close)) { _, _ ->
                    createTestMedicineAndClose()
                }
                .setNegativeButton(getString(R.string.cancel), null)
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
                getString(R.string.test_medicine_created, testMedicine.time), 
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
                .setTitle(getString(R.string.stop_notifications))
                .setMessage(getString(R.string.stop_notifications_message))
                .setPositiveButton(getString(R.string.stop_all)) { _, _ ->
                    forceStopAllNotifications()
                }
                .setNegativeButton(getString(R.string.cancel), null)
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
            Toast.makeText(this, getString(R.string.all_notifications_stopped), Toast.LENGTH_LONG).show()
            
            android.util.Log.d("SettingsActivity", "✓ Принудительная остановка завершена")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Ошибка принудительной остановки", e)
            Toast.makeText(this, "Ошибка остановки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            android.util.Log.d("SettingsActivity", "Updating UI after language change")
            
            // Обновляем заголовок Activity
            title = getString(R.string.settings)
            supportActionBar?.title = getString(R.string.settings)
            
            // Обновляем информацию о версии
            binding.textViewVersion.text = "${getString(R.string.version)}: ${VersionUtils.getShortVersionInfo(this)}"
            binding.textViewBuildTime.text = "${getString(R.string.updated)}: ${VersionUtils.getLastUpdateTime(this)}"
            
            // Обновляем текст слайдеров
            updateSliderTexts()
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            android.util.Log.d("SettingsActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error updating UI after language change", e)
        }
    }
    
    private fun updateSliderTexts() {
        try {
            // Обновляем текст слайдеров
            binding.textAdvanceMinutes.text = "${binding.sliderAdvanceMinutes.value.toInt()} ${getString(R.string.minutes)}"
            binding.textLowStockThreshold.text = "${binding.sliderLowStockThreshold.value.toInt()} ${getString(R.string.pieces)}"
            binding.textMaxBackups.text = "${binding.sliderMaxBackups.value.toInt()} ${getString(R.string.copies)}"
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error updating slider texts", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок (используем только существующие элементы)
            try {
                binding.buttonDataBackup?.text = getString(R.string.data_backup)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonDataBackup not found")
            }
            
            try {
                binding.buttonCustomizeButtons?.text = getString(R.string.customize_buttons)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonCustomizeButtons not found")
            }
            
            try {
                binding.buttonLanguageSettings?.text = getString(R.string.language_settings)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonLanguageSettings not found")
            }
            
            try {
                binding.buttonLanguageTest?.text = getString(R.string.language_test)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonLanguageTest not found")
            }
            
            try {
                binding.buttonNotificationManager?.text = getString(R.string.notification_manager)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonNotificationManager not found")
            }
            
            try {
                binding.buttonCreateBackup?.text = getString(R.string.create_backup)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonCreateBackup not found")
            }
            
            try {
                binding.buttonRestoreBackup?.text = getString(R.string.restore_backup)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonRestoreBackup not found")
            }
            
            try {
                binding.buttonClearData?.text = getString(R.string.clear_data)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonClearData not found")
            }
            
            try {
                binding.buttonStopNotifications?.text = getString(R.string.stop_notifications)
            } catch (e: Exception) {
                android.util.Log.w("SettingsActivity", "buttonStopNotifications not found")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error updating button texts", e)
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