package com.medicalnotes.app

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.MainMedicineAdapter
import com.medicalnotes.app.databinding.ActivityMainBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.UserPreferencesRepository
import com.medicalnotes.app.utils.MedicineGroupingUtil
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var todayMedicineAdapter: MainMedicineAdapter
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Инициализация ViewModel и Repository
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            userPreferencesRepository = UserPreferencesRepository(this)

            // Настройка UI
            setupViews()
            setupButtons()
            observeData()

            // Загрузка данных
            viewModel.loadAllMedicines()
            loadTodayMedicines()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            android.widget.Toast.makeText(this, "Ошибка инициализации: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViews() {
        // Настройка RecyclerView
        todayMedicineAdapter = MainMedicineAdapter(
            onMedicineClick = { medicine -> takeMedicine(medicine) }
        )

        binding.recyclerViewTodayMedicines.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = todayMedicineAdapter
        }

        // Настройка кнопки повтора
        binding.buttonRetry.setOnClickListener {
            loadTodayMedicines()
        }
    }

    private fun setupButtons() {
        // Кнопка добавления лекарства
        binding.fabAddMedicine.setOnClickListener {
            val intent = android.content.Intent(this, AddMedicineActivity::class.java)
            startActivity(intent)
        }

        // Кнопка управления лекарствами
        binding.buttonManageMedicines.setOnClickListener {
            val intent = android.content.Intent(this, MedicineManagerActivity::class.java)
            startActivity(intent)
        }

        // Кнопки в BottomAppBar
        binding.buttonManageMedicinesBottom.setOnClickListener {
            val intent = android.content.Intent(this, MedicineManagerActivity::class.java)
            startActivity(intent)
        }

        binding.buttonManageGroupsBottom.setOnClickListener {
            val intent = android.content.Intent(this, GroupManagementActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSettingsBottom.setOnClickListener {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Кнопка переключения логов
        binding.buttonToggleLogs.setOnClickListener {
            toggleLogsVisibility()
        }

        // Кнопка экспорта данных
        binding.buttonExportData.setOnClickListener {
            exportData()
        }

        // Кнопка тестирования групп
        binding.buttonTestGroups.setOnClickListener {
            testGroups()
        }

        // Кнопка очистки логов
        binding.buttonClearLogs.setOnClickListener {
            clearLogs()
        }
    }

    private fun observeData() {
        try {
            viewModel.todayMedicines.observe(this) { medicines ->
                if (medicines.isEmpty()) {
                    showEmptyState()
                    addLog("Нет лекарств на сегодня")
                } else {
                    showContentState()
                    addLog("=== ОБНОВЛЕНИЕ СПИСКА ЛЕКАРСТВ ===")
                    addLog("Получено лекарств: ${medicines.size}")
                    addLog("Текущее время: ${LocalDateTime.now()}")
                    
                    var hasOverdueMedicines = false
                    
                    medicines.forEach { medicine ->
                        val status = com.medicalnotes.app.utils.DosageCalculator.getMedicineStatus(medicine)
                        addLog("Отображается: ${medicine.name} - Статус: $status")
                        addLog("  Время приема: ${medicine.time}")
                        addLog("  Частота: ${medicine.frequency}")
                        
                        if (status == MedicineStatus.OVERDUE) {
                            hasOverdueMedicines = true
                            addLog("⚠️ ПРОСРОЧЕНО: ${medicine.name}")
                        } else if (status == MedicineStatus.UPCOMING) {
                            addLog("📅 ПРЕДСТОИТ: ${medicine.name} - время еще не пришло")
                        } else if (status == MedicineStatus.TAKEN_TODAY) {
                            addLog("✅ ПРИНЯТО: ${medicine.name} - уже принято сегодня")
                        } else {
                            addLog("❌ НЕ СЕГОДНЯ: ${medicine.name} - не по расписанию")
                        }
                    }
                    
                    // Проверяем, нужно ли показывать группированные карточки
                    val shouldShowGrouped = MedicineGroupingUtil.shouldShowGroupedCards(medicines, LocalDate.now())
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
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in observeData", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadAllMedicines()
            loadTodayMedicines()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun takeMedicine(medicine: Medicine) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updatedMedicine = medicine.copy(
                    takenToday = true,
                    isMissed = false
                )
                viewModel.updateMedicine(updatedMedicine)
                
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Лекарство ${medicine.name} принято!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun editMedicine(medicine: Medicine) {
        val intent = android.content.Intent(this, EditMedicineActivity::class.java).apply {
            putExtra("medicine_id", medicine.id)
        }
        startActivity(intent)
    }

    private fun deleteMedicine(medicine: Medicine) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Удаление лекарства")
            .setMessage("Вы уверены, что хотите удалить лекарство '${medicine.name}'?")
            .setPositiveButton("Удалить") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        viewModel.deleteMedicine(medicine.id)
                        CoroutineScope(Dispatchers.Main).launch {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Лекарство удалено",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Ошибка удаления: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addLog(message: String) {
        val timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logMessage = "[$timestamp] $message"
        
        binding.textViewLogs.append("$logMessage\n")
        
        // Автоматическая прокрутка к концу
        val scrollAmount = binding.textViewLogs.layout.getLineTop(binding.textViewLogs.lineCount) - binding.textViewLogs.height
        if (scrollAmount > 0) {
            binding.textViewLogs.scrollTo(0, scrollAmount)
        }
    }

    private fun clearLogs() {
        binding.textViewLogs.text = ""
        addLog("=== ЛОГИ ОЧИЩЕНЫ ===")
    }

    private fun loadTodayMedicines() {
        showLoadingState()
        viewModel.loadTodayMedicines()
    }

    private fun showLoadingState() {
        binding.progressBarTodayMedicines.visibility = View.VISIBLE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.VISIBLE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun showErrorState(errorMessage: String = "Не удалось загрузить лекарства") {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.VISIBLE
        binding.textErrorMessage.text = errorMessage
    }

    private fun showContentState() {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.VISIBLE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun toggleLogsVisibility() {
        if (binding.layoutLogs.visibility == View.VISIBLE) {
            binding.layoutLogs.visibility = View.GONE
        } else {
            binding.layoutLogs.visibility = View.VISIBLE
            addLog("=== ЛОГИ ВКЛЮЧЕНЫ ===")
        }
    }

    private fun exportData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = viewModel.allMedicines.value ?: emptyList()
                val exportData = medicines.joinToString("\n") { medicine ->
                    "${medicine.name} - ${medicine.dosage} - ${medicine.time}"
                }
                
                val fileName = "medical_notes_export_${LocalDate.now()}.txt"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(exportData)
                
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Данные экспортированы в $fileName",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    addLog("Экспорт данных: $fileName")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка экспорта: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    addLog("Ошибка экспорта: ${e.message}")
                }
            }
        }
    }

    private fun testGroups() {
        addLog("=== ТЕСТИРОВАНИЕ ГРУПП ===")
        val medicines = viewModel.allMedicines.value ?: emptyList()
        val groupedMedicines = medicines.filter { it.groupName.isNotEmpty() }
        
        addLog("Всего лекарств: ${medicines.size}")
        addLog("Лекарств в группах: ${groupedMedicines.size}")
        
        val groups = groupedMedicines.groupBy { it.groupName }
        groups.forEach { (groupName, groupMedicines) ->
            addLog("Группа '$groupName': ${groupMedicines.size} лекарств")
            groupMedicines.forEach { medicine ->
                addLog("  - ${medicine.name} (порядок: ${medicine.groupOrder})")
            }
        }
        
        android.widget.Toast.makeText(
            this,
            "Тест групп завершен. Проверьте логи.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}  