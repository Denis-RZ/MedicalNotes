package com.medicalnotes.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.MedicineAdapter
import com.medicalnotes.app.databinding.ActivityMedicineManagementElderlyBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.JournalAction
import com.medicalnotes.app.repository.UserPreferencesRepository
import com.medicalnotes.app.utils.DisplayUtils
import com.medicalnotes.app.utils.NotificationManager
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.DateTimeFormatter

class ElderlyMedicineManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMedicineManagementElderlyBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var medicineAdapter: MedicineAdapter
    private lateinit var notificationManager: NotificationManager
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineManagementElderlyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]
        notificationManager = NotificationManager(this)
        userPreferencesRepository = UserPreferencesRepository(this)
        
        setupViews()
        setupListeners()
        observeData()
        applyUserPreferences()
    }
    
    private fun setupViews() {
        // Настройка RecyclerView для всех лекарств
        medicineAdapter = MedicineAdapter(
            onMedicineClick = { medicine ->
                // Показать диалог редактирования
                showEditMedicineDialog(medicine)
            },
            onSkipClick = { medicine ->
                // Отметить как пропущенное
                viewModel.markMedicineAsSkipped(medicine.id)
            },
            onEditClick = { medicine ->
                // Открываем экран редактирования лекарства
                val intent = Intent(this, EditMedicineActivity::class.java).apply {
                    putExtra("medicine_id", medicine.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { medicine ->
                // Показать диалог подтверждения удаления
                showDeleteConfirmationDialog(medicine)
            }
        )
        
        binding.recyclerViewAllMedicines.apply {
            layoutManager = LinearLayoutManager(this@ElderlyMedicineManagementActivity)
            adapter = medicineAdapter
        }
    }
    
    private fun setupListeners() {
        // Кнопка журнала
        binding.buttonJournal.setOnClickListener {
            showJournalDialog()
        }
        
        // Кнопка добавления лекарства
        binding.buttonAddMedicine.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }
        
        // Кнопка редактирования лекарства
        binding.buttonEditMedicine.setOnClickListener {
            showSelectMedicineToEditDialog()
        }
        
        // Кнопка настроек
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Кнопка настроек уведомлений
        binding.buttonNotificationSettings.setOnClickListener {
            startActivity(Intent(this, NotificationManagerActivity::class.java))
        }
        
        // Кнопка управления группами
        binding.buttonGroupManagement.setOnClickListener {
            startActivity(Intent(this, GroupManagementActivity::class.java))
        }
        
        // Кнопка возврата
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun observeData() {
        viewModel.allMedicines.observe(this) { medicines ->
            medicineAdapter.submitList(medicines)
        }
    }
    
    private fun applyUserPreferences() {
        // Применяем настройки пользователя к интерфейсу
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = userPreferencesRepository.getUserPreferences()
                
                CoroutineScope(Dispatchers.Main).launch {
                    preferences?.let { prefs ->
                        // Применяем размер шрифта
                        if (prefs.largeTextEnabled) {
                            binding.root.setTag(com.medicalnotes.app.R.id.tag_large_text, true)
                            DisplayUtils.applyLargeText(binding.root)
                        }
                        
                        // Применяем цвета
                        if (prefs.useHighContrast) {
                            DisplayUtils.applyHighContrast(binding.root)
                        }
                        
                        // Применяем вибрацию
                        if (prefs.enableVibration) {
                            // Включаем вибрацию для кнопок
                            binding.buttonAddMedicine.isHapticFeedbackEnabled = true
                            binding.buttonEditMedicine.isHapticFeedbackEnabled = true
                            binding.buttonSettings.isHapticFeedbackEnabled = true
                        }
                        
                        // Применяем звуки
                        if (prefs.enableSound) {
                            // Звуки будут воспроизводиться через NotificationManager
                        }
                        
                        // Применяем упрощенный интерфейс
                        if (prefs.isElderlyMode) {
                            DisplayUtils.applySimpleInterface(binding.root)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ElderlyMedicineManagementActivity", "Error applying user preferences", e)
            }
        }
    }
    

    
    private fun showJournalDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataManager = DataManager(this@ElderlyMedicineManagementActivity)
                val today = java.time.LocalDate.now()
                val todayEntries = dataManager.getJournalEntriesForDate(today)
                val allEntries = dataManager.loadJournalEntries()
                
                CoroutineScope(Dispatchers.Main).launch {
                    val messageBuilder = StringBuilder()
                    messageBuilder.appendLine("ЖУРНАЛ ПРИЕМА ЛЕКАРСТВ")
                    messageBuilder.appendLine("=".repeat(30))
                    messageBuilder.appendLine()
                    
                    if (todayEntries.isNotEmpty()) {
                        messageBuilder.appendLine("СЕГОДНЯ (${today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}):")
                        messageBuilder.appendLine()
                        
                        todayEntries.sortedByDescending { it.timestamp }.forEach { entry ->
                            val time = entry.timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val actionText = when (entry.action) {
                                JournalAction.TAKEN -> "ПРИНЯТО"
                                JournalAction.SKIPPED -> "ПРОПУЩЕНО"
                                JournalAction.MISSED -> "ПРОСРОЧЕНО"
                                JournalAction.EDITED -> "ИЗМЕНЕНО"
                                JournalAction.DELETED -> "УДАЛЕНО"
                            }
                            messageBuilder.appendLine("$time - ${entry.medicineName} ($actionText)")
                        }
                    } else {
                        messageBuilder.appendLine("Сегодня нет записей в журнале")
                    }
                    
                    messageBuilder.appendLine()
                    messageBuilder.appendLine("ВСЕГО ЗАПИСЕЙ: ${allEntries.size}")
                    
                    if (allEntries.isNotEmpty()) {
                        val lastEntry = allEntries.maxByOrNull { it.timestamp }
                        lastEntry?.let {
                            messageBuilder.appendLine("ПОСЛЕДНЯЯ ЗАПИСЬ: ${it.timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                        }
                    }
                    
                    AlertDialog.Builder(this@ElderlyMedicineManagementActivity)
                        .setTitle("ЖУРНАЛ ПРИЕМА ЛЕКАРСТВ")
                        .setMessage(messageBuilder.toString())
                        .setPositiveButton("ПОНЯТНО", null)
                        .setNegativeButton("ОЧИСТИТЬ ЖУРНАЛ") { _, _ ->
                            showClearJournalConfirmation()
                        }
                        .setNeutralButton("ЭКСПОРТ") { _, _ ->
                            exportJournal()
                        }
                        .show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ElderlyMedicineManagementActivity", "Error loading journal", e)
                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@ElderlyMedicineManagementActivity)
                        .setTitle("ОШИБКА")
                        .setMessage("Не удалось загрузить журнал: ${e.message}")
                        .setPositiveButton("ПОНЯТНО", null)
                        .show()
                }
            }
        }
    }
    
    private fun showClearJournalConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("ОЧИСТИТЬ ЖУРНАЛ")
            .setMessage("Вы уверены, что хотите очистить весь журнал приёма лекарств?\n\nЭто действие нельзя отменить.")
            .setPositiveButton("ОЧИСТИТЬ") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dataManager = DataManager(this@ElderlyMedicineManagementActivity)
                        val success = dataManager.clearJournalEntries()
                        
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                Toast.makeText(this@ElderlyMedicineManagementActivity, "Журнал очищен", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ElderlyMedicineManagementActivity, "Ошибка очистки журнала", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@ElderlyMedicineManagementActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }
    
    private fun exportJournal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataManager = DataManager(this@ElderlyMedicineManagementActivity)
                val entries = dataManager.loadJournalEntries()
                
                val exportBuilder = StringBuilder()
                exportBuilder.appendLine("ЖУРНАЛ ПРИЕМА ЛЕКАРСТВ")
                exportBuilder.appendLine("Экспорт от ${java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                exportBuilder.appendLine("=".repeat(50))
                exportBuilder.appendLine()
                
                entries.sortedByDescending { it.timestamp }.forEach { entry ->
                    val date = entry.timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    val time = entry.timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val actionText = when (entry.action) {
                        JournalAction.TAKEN -> "ПРИНЯТО"
                        JournalAction.SKIPPED -> "ПРОПУЩЕНО"
                        JournalAction.MISSED -> "ПРОСРОЧЕНО"
                        JournalAction.EDITED -> "ИЗМЕНЕНО"
                        JournalAction.DELETED -> "УДАЛЕНО"
                    }
                    exportBuilder.appendLine("$date $time - ${entry.medicineName} ($actionText)")
                    if (entry.notes.isNotEmpty()) {
                        exportBuilder.appendLine("  Заметка: ${entry.notes}")
                    }
                    exportBuilder.appendLine()
                }
                
                // Сохраняем в файл
                val exportFile = File(this@ElderlyMedicineManagementActivity.filesDir, "journal_export_${System.currentTimeMillis()}.txt")
                exportFile.writeText(exportBuilder.toString())
                
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@ElderlyMedicineManagementActivity, "Журнал экспортирован в ${exportFile.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@ElderlyMedicineManagementActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showEditMedicineDialog(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("РЕДАКТИРОВАТЬ ЛЕКАРСТВО")
            .setMessage("Редактировать '${medicine.name}'?")
            .setPositiveButton("РЕДАКТИРОВАТЬ") { _, _ ->
                val intent = Intent(this, EditMedicineActivity::class.java).apply {
                    putExtra("medicine_id", medicine.id)
                }
                startActivity(intent)
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }
    
    private fun showDeleteConfirmationDialog(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("УДАЛИТЬ ЛЕКАРСТВО")
            .setMessage("Вы уверены, что хотите удалить лекарство \"${medicine.name}\"?\n\nЭто действие нельзя отменить.")
            .setPositiveButton("УДАЛИТЬ") { _, _ ->
                viewModel.deleteMedicine(medicine.id)
                Toast.makeText(this, "Лекарство \"${medicine.name}\" удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }
    
    private fun showSelectMedicineToEditDialog() {
        viewModel.allMedicines.value?.let { medicines ->
            if (medicines.isEmpty()) {
                showErrorDialog("Нет лекарств", "Сначала добавьте лекарство")
                return
            }
            
            val medicineNames = medicines.map { it.name }.toTypedArray()
            
                    AlertDialog.Builder(this)
            .setTitle("ВЫБЕРИТЕ ЛЕКАРСТВО ДЛЯ РЕДАКТИРОВАНИЯ")
                .setItems(medicineNames) { _, which ->
                    val selectedMedicine = medicines[which]
                    showEditMedicineDialog(selectedMedicine)
                }
                .setNegativeButton("ОТМЕНА", null)
                .show()
        }
    }
    

    
    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadAllMedicines()
    }
} 