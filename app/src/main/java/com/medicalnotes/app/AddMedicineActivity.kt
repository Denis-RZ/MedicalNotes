package com.medicalnotes.app

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.databinding.ActivityAddMedicineBinding
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.models.GroupMetadata
import com.medicalnotes.app.service.NotificationService
import com.medicalnotes.app.utils.CustomTimePickerDialog
import com.medicalnotes.app.viewmodels.AddMedicineViewModel
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AddMedicineActivity : BaseActivity() {
    
    private lateinit var binding: ActivityAddMedicineBinding
    private lateinit var viewModel: AddMedicineViewModel
    private var selectedTime: LocalTime = LocalTime.of(8, 0)
    private var selectedFrequency = DosageFrequency.DAILY
    private var selectedTimes = mutableListOf<LocalTime>()
    private var selectedMedicineType = ""
    private var selectedDays = mutableSetOf<Int>() // Дни недели (1=понедельник, 7=воскресенье)
    private var selectedRelatedMedicines = mutableListOf<Medicine>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        //  ДОБАВЛЕНО: Уведомляем сервис о начале редактирования
        com.medicalnotes.app.service.OverdueCheckService.setEditingActive(true)
        
        binding = ActivityAddMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[AddMedicineViewModel::class.java]
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(com.medicalnotes.app.R.string.add_medicine)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настраиваем AutoCompleteTextView для типов лекарств
        setupMedicineTypeDropdown()
        
        // Устанавливаем время по умолчанию
        updateTimeDisplay()
        updateFrequencyDisplay()
    }
    
    private fun setupMedicineTypeDropdown() {
        val medicineTypes = listOf(
            getString(com.medicalnotes.app.R.string.medicine_type_tablets),
            getString(com.medicalnotes.app.R.string.medicine_type_capsules), 
            getString(com.medicalnotes.app.R.string.medicine_type_injections),
            getString(com.medicalnotes.app.R.string.medicine_type_ozempic),
            getString(com.medicalnotes.app.R.string.medicine_type_mounjaro),
            getString(com.medicalnotes.app.R.string.medicine_type_insulin),
            getString(com.medicalnotes.app.R.string.medicine_type_drops),
            getString(com.medicalnotes.app.R.string.medicine_type_syrup),
            getString(com.medicalnotes.app.R.string.medicine_type_inhalations),
            getString(com.medicalnotes.app.R.string.medicine_type_ointments),
            getString(com.medicalnotes.app.R.string.medicine_type_gels),
            getString(com.medicalnotes.app.R.string.medicine_type_creams),
            getString(com.medicalnotes.app.R.string.medicine_type_suppositories),
            getString(com.medicalnotes.app.R.string.medicine_type_sprays),
            getString(com.medicalnotes.app.R.string.medicine_type_aerosols),
            getString(com.medicalnotes.app.R.string.medicine_type_powders),
            getString(com.medicalnotes.app.R.string.medicine_type_suspensions),
            getString(com.medicalnotes.app.R.string.medicine_type_emulsions),
            getString(com.medicalnotes.app.R.string.medicine_type_other)
        )
        
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medicineTypes)
        binding.autoCompleteMedicineType.setAdapter(adapter)
        
        //  ИСПРАВЛЕНО: Правильно устанавливаем значение по умолчанию
        binding.autoCompleteMedicineType.setText(getString(com.medicalnotes.app.R.string.medicine_type_tablets), false)
        selectedMedicineType = getString(com.medicalnotes.app.R.string.medicine_type_tablets)
        android.util.Log.d("AddMedicine", "Set default medicine type: $selectedMedicineType")
        
        //  ИСПРАВЛЕНО: Обработчик выбора типа лекарства
        binding.autoCompleteMedicineType.setOnItemClickListener { _, _, position, _ ->
            selectedMedicineType = medicineTypes[position]
            android.util.Log.d("AddMedicine", "Medicine type selected: $selectedMedicineType")
            
            // Автоматически отмечаем чекбокс инсулина для соответствующих типов
            binding.checkBoxInsulin.isChecked = selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_insulin) || 
                                               selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_ozempic) || 
                                               selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_mounjaro)
            
            android.util.Log.d("AddMedicine", "Insulin checkbox set to: ${binding.checkBoxInsulin.isChecked}")
        }
        
        //  ИСПРАВЛЕНО: Обработчики для AutoCompleteTextView
        binding.autoCompleteMedicineType.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.autoCompleteMedicineType.showDropDown()
            }
        }
        
        // Добавляем обработчик изменения текста для отслеживания ручного ввода
        binding.autoCompleteMedicineType.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val newType = s?.toString()?.trim() ?: ""
                if (newType.isNotEmpty()) {
                    selectedMedicineType = newType
                    android.util.Log.d("AddMedicine", "Medicine type changed via text input: $selectedMedicineType")
                }
            }
        })
    }
    
    private fun setupListeners() {
        binding.buttonTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.buttonFrequency.setOnClickListener {
            showFrequencyDialog()
        }
        
        // Убираем старый обработчик - теперь используется AutoCompleteTextView
        // binding.autoCompleteMedicineType.setOnClickListener {
        //     showMedicineTypeDialog()
        // }
        
        binding.buttonWeekDays.setOnClickListener {
            showWeekDaysDialog()
        }
        
        binding.buttonSelectMedicines.setOnClickListener {
            showMedicineSelectionDialog()
        }
        
        // Показываем/скрываем группировку и дни недели в зависимости от выбранной частоты
        // Убираем неправильный addTextChangedListener для кнопки
        

        
        binding.switchMultipleDoses.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutMultipleTimes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        binding.buttonAddTime.setOnClickListener {
            showTimePickerForMultiple()
        }
        
        binding.buttonChangeGroup.setOnClickListener {
            showGroupSelectionDialog()
        }
        
        // binding.buttonSettings.setOnClickListener {
        //     startActivity(Intent(this, SettingsActivity::class.java))
        // }
        
        // binding.buttonCustomize.setOnClickListener {
        //     startActivity(Intent(this, ButtonCustomizationActivity::class.java))
        // }
        
        // binding.buttonManageGroups.setOnClickListener {
        //     startActivity(Intent(this, GroupManagementActivity::class.java))
        // }
        
        // binding.buttonCreateGroupFromExisting.setOnClickListener {
        //     showCreateGroupFromExistingDialog()
        // }
        
        // binding.buttonTimeGroup.setOnClickListener {
        //     showTimeGroupDialog()
        // }
        
        binding.buttonSave.setOnClickListener {
            saveMedicine()
        }
        
        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun showTimePicker() {
        CustomTimePickerDialog(this, selectedTime) { time ->
            selectedTime = time
            updateTimeDisplay()
        }.show()
    }
    
    private fun updateTimeDisplay() {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        binding.buttonTime.text = selectedTime.format(formatter)
    }
    
    private fun updateFrequencyDisplay() {
        // Проверяем, что Activity все еще активна
        if (isFinishing || isDestroyed) {
            android.util.Log.d("AddMedicine", "Activity finishing or destroyed, skipping UI update")
            return
        }
        
        try {
            val frequencyText = when (selectedFrequency) {
                DosageFrequency.DAILY -> getString(com.medicalnotes.app.R.string.frequency_daily)
                DosageFrequency.EVERY_OTHER_DAY -> getString(com.medicalnotes.app.R.string.frequency_every_other_day)
                DosageFrequency.TWICE_A_WEEK -> getString(com.medicalnotes.app.R.string.frequency_twice_a_week)
                DosageFrequency.THREE_TIMES_A_WEEK -> getString(com.medicalnotes.app.R.string.frequency_three_times_a_week)
                DosageFrequency.WEEKLY -> getString(com.medicalnotes.app.R.string.frequency_weekly)
                DosageFrequency.CUSTOM -> getString(com.medicalnotes.app.R.string.frequency_custom)
            }
            
            // Безопасно обновляем текст кнопки
            try {
                if (!isFinishing && !isDestroyed) {
                    binding.buttonFrequency.text = frequencyText
                }
            } catch (e: Exception) {
                android.util.Log.e("AddMedicine", "Error updating button text", e)
            }
            
            // Показываем/скрываем группировку и дни недели в зависимости от выбранной частоты
            val isEveryOtherDay = selectedFrequency == DosageFrequency.EVERY_OTHER_DAY
            val isCustom = selectedFrequency == DosageFrequency.CUSTOM
            
            // Безопасно обновляем видимость элементов
            try {
                if (!isFinishing && !isDestroyed) {
                    binding.layoutGrouping.visibility = if (isEveryOtherDay) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                android.util.Log.e("AddMedicine", "Error updating layoutGrouping visibility", e)
            }
            
            try {
                if (!isFinishing && !isDestroyed) {
                    binding.layoutWeekDays.visibility = if (isCustom) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                android.util.Log.e("AddMedicine", "Error updating layoutWeekDays visibility", e)
            }
            
            android.util.Log.d("AddMedicine", "Frequency display updated: $frequencyText")
        } catch (e: Exception) {
            android.util.Log.e("AddMedicine", "Error updating frequency display", e)
            try {
                if (!isFinishing && !isDestroyed) {
                    binding.buttonFrequency.text = getString(com.medicalnotes.app.R.string.frequency_daily)
                }
            } catch (e2: Exception) {
                android.util.Log.e("AddMedicine", "Error setting default button text", e2)
            }
            selectedFrequency = DosageFrequency.DAILY
        }
    }
    
    private fun showFrequencyDialog() {
        // Проверяем, что Activity все еще активна
        if (isFinishing || isDestroyed) {
            android.util.Log.d("AddMedicine", "Activity finishing or destroyed, skipping frequency dialog")
            return
        }
        
        val frequencies = arrayOf(
            getString(com.medicalnotes.app.R.string.frequency_daily),
            getString(com.medicalnotes.app.R.string.frequency_every_other_day), 
            getString(com.medicalnotes.app.R.string.frequency_twice_a_week),
            getString(com.medicalnotes.app.R.string.frequency_three_times_a_week),
            getString(com.medicalnotes.app.R.string.frequency_weekly),
            getString(com.medicalnotes.app.R.string.frequency_custom)
        )
        
        try {
            AlertDialog.Builder(this)
                .setTitle(getString(com.medicalnotes.app.R.string.frequency_selection_dialog))
                .setItems(frequencies) { _, which ->
                    // Проверяем, что Activity все еще активна перед обновлением
                    if (!isFinishing && !isDestroyed) {
                        selectedFrequency = when (which) {
                            0 -> DosageFrequency.DAILY
                            1 -> DosageFrequency.EVERY_OTHER_DAY
                            2 -> DosageFrequency.TWICE_A_WEEK
                            3 -> DosageFrequency.THREE_TIMES_A_WEEK
                            4 -> DosageFrequency.WEEKLY
                            5 -> DosageFrequency.CUSTOM
                            else -> DosageFrequency.DAILY
                        }
                        updateFrequencyDisplay()
                    }
                }
                .setOnCancelListener {
                    android.util.Log.d("AddMedicine", "Frequency dialog cancelled")
                }
                .setOnDismissListener {
                    android.util.Log.d("AddMedicine", "Frequency dialog dismissed")
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.e("AddMedicine", "Error showing frequency dialog", e)
            Toast.makeText(this, "Ошибка показа диалога", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showGroupSelectionDialog() {
        val dataManager = DataManager(this)
        val existingGroups = dataManager.getExistingGroups()
        
        if (existingGroups.isEmpty()) {
            // Если групп нет, просто показываем поля для создания новой
            return
        }
        
        val options = existingGroups.toMutableList()
        options.add("Создать новую группу")
        
        AlertDialog.Builder(this)
            .setTitle("Выберите группу")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < existingGroups.size) {
                    // Выбрали существующую группу
                    val selectedGroup = existingGroups[which]
                    binding.editTextGroupName.setText(selectedGroup)
                    
                    // Автоматически определяем следующий порядковый номер
                    val nextOrder = dataManager.getNextGroupOrder(selectedGroup)
                    binding.editTextGroupOrder.setText(nextOrder.toString())
                    
                    // Делаем поля только для чтения
                    binding.editTextGroupName.isEnabled = false
                    binding.editTextGroupOrder.isEnabled = false
                } else {
                    // Выбрали "Создать новую группу"
                    binding.editTextGroupName.setText("")
                    binding.editTextGroupOrder.setText("1")
                    
                    // Делаем поля редактируемыми
                    binding.editTextGroupName.isEnabled = true
                    binding.editTextGroupOrder.isEnabled = true
                    binding.editTextGroupName.requestFocus()
                }
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showTimeGroupDialog() {
        val timeOptions = arrayOf(
            "08:00 - Утро",
            "12:00 - Обед", 
            "18:00 - Вечер",
            "20:00 - На ночь",
            "Выбрать другое время"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Выберите время приема группы")
            .setItems(timeOptions) { _, which ->
                when (which) {
                    0 -> selectedTime = LocalTime.of(8, 0)
                    1 -> selectedTime = LocalTime.of(12, 0)
                    2 -> selectedTime = LocalTime.of(18, 0)
                    3 -> selectedTime = LocalTime.of(20, 0)
                    4 -> showTimePicker() // Показать time picker для выбора произвольного времени
                }
                updateTimeDisplay()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    // Метод showMedicineTypeDialog удален - теперь используется AutoCompleteTextView
    
    private fun showWeekDaysDialog() {
        val weekDays = arrayOf(
            "Понедельник",
            "Вторник", 
            "Среда",
            "Четверг",
            "Пятница",
            "Суббота",
            "Воскресенье"
        )
        
        val checkedItems = BooleanArray(7) { i ->
            selectedDays.contains(i + 1) // i+1 потому что дни недели 1-7
        }
        
        AlertDialog.Builder(this)
            .setTitle("Выберите дни недели")
            .setMultiChoiceItems(weekDays, checkedItems) { _, which, isChecked ->
                val dayNumber = which + 1 // 1=понедельник, 7=воскресенье
                if (isChecked) {
                    selectedDays.add(dayNumber)
                } else {
                    selectedDays.remove(dayNumber)
                }
                updateWeekDaysDisplay()
            }
            .setPositiveButton("OK", null)
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun updateWeekDaysDisplay() {
        // Проверяем, что Activity все еще активна
        if (isFinishing || isDestroyed) {
            android.util.Log.d("AddMedicine", "Activity finishing or destroyed, skipping week days update")
            return
        }
        
        try {
            if (selectedDays.isEmpty()) {
                if (!isFinishing && !isDestroyed) {
                    binding.buttonWeekDays.text = "Выбрать дни"
                }
            } else {
                val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                val selectedDayNames = selectedDays.sorted()
                    .filter { it in 1..7 } // Фильтруем только валидные дни недели
                    .map { dayNames[it - 1] }
                if (!isFinishing && !isDestroyed) {
                    binding.buttonWeekDays.text = "Дни: ${selectedDayNames.joinToString(", ")}"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddMedicine", "Error updating week days display", e)
            if (!isFinishing && !isDestroyed) {
                binding.buttonWeekDays.text = "Выбрать дни"
            }
            selectedDays.clear() // Очищаем некорректные данные
        }
    }
    
    private fun showMedicineSelectionDialog() {
        val dataManager = DataManager(this)
        val existingMedicines = dataManager.loadMedicines()
        
        if (existingMedicines.isEmpty()) {
            Toast.makeText(this, "Нет доступных лекарств для выбора", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Создаем список названий лекарств для диалога
        val medicineNames = existingMedicines.map { 
            "${it.name} (${it.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})" 
        }.toTypedArray()
        
        // Отмечаем уже выбранные лекарства
        val checkedItems = BooleanArray(medicineNames.size) { index ->
            existingMedicines[index] in selectedRelatedMedicines
        }
        
        AlertDialog.Builder(this)
            .setTitle("Выберите связанные лекарства")
            .setMessage("Выберите лекарства, которые принимаются вместе с этим препаратом:")
            .setMultiChoiceItems(medicineNames, checkedItems) { _, which, isChecked ->
                val selectedMedicine = existingMedicines[which]
                if (isChecked) {
                    if (!selectedRelatedMedicines.contains(selectedMedicine)) {
                        selectedRelatedMedicines.add(selectedMedicine)
                    }
                } else {
                    selectedRelatedMedicines.remove(selectedMedicine)
                }
                updateRelatedMedicinesDisplay()
            }
            .setPositiveButton("OK") { _, _ ->
                updateRelatedMedicinesDisplay()
            }
            .setNegativeButton("Отмена") { _, _ ->
                // Восстанавливаем предыдущее состояние
                selectedRelatedMedicines.clear()
            }
            .setNeutralButton("Очистить") { _, _ ->
                selectedRelatedMedicines.clear()
                updateRelatedMedicinesDisplay()
            }
            .show()
    }
    
    private fun updateRelatedMedicinesDisplay() {
        if (selectedRelatedMedicines.isEmpty()) {
            binding.buttonSelectMedicines.text = "Выбрать связанные лекарства"
            binding.textSelectedMedicines.visibility = View.GONE
        } else {
            val count = selectedRelatedMedicines.size
            binding.buttonSelectMedicines.text = "Выбрано: $count лекарств"
            
            // Показываем краткую информацию о выбранных лекарствах
            val medicineInfo = selectedRelatedMedicines.joinToString(", ") { 
                "${it.name} (${it.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})" 
            }
            
            binding.textSelectedMedicines.text = medicineInfo
            binding.textSelectedMedicines.visibility = View.VISIBLE
        }
    }
    
    private fun updateSelectedMedicinesDisplay() {
        if (selectedRelatedMedicines.isEmpty()) {
            binding.textSelectedMedicines.visibility = View.GONE
        } else {
            val count = selectedRelatedMedicines.size
            val medicineInfo = selectedRelatedMedicines.joinToString(", ") { 
                "${it.name} (${it.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})" 
            }
            
            binding.textSelectedMedicines.text = "Выбрано: $count лекарств\n$medicineInfo"
            binding.textSelectedMedicines.visibility = View.VISIBLE
        }
    }
    
    private fun showTimePickerForMultiple() {
        CustomTimePickerDialog(this, LocalTime.of(8, 0)) { time ->
            if (!selectedTimes.contains(time)) {
                selectedTimes.add(time)
                selectedTimes.sort()
                updateMultipleTimesDisplay()
            }
        }.show()
    }
    
    private fun updateMultipleTimesDisplay() {
        if (selectedTimes.isNotEmpty()) {
            val timesText = selectedTimes.joinToString(", ") { 
                it.format(DateTimeFormatter.ofPattern("HH:mm")) 
            }
            binding.textSelectedTimes.text = "Времена приема: $timesText"
        } else {
            binding.textSelectedTimes.text = "Времена приема: 08:00"
        }
    }
    
    private fun showLoadingState() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.bottomAppBar.visibility = View.GONE
    }
    
    private fun hideLoadingState() {
        binding.loadingOverlay.visibility = View.GONE
        binding.bottomAppBar.visibility = View.VISIBLE
    }
    
    private fun saveMedicine() {
        val name = binding.editTextName.text.toString().trim()
        val dosage = binding.editTextDosage.text.toString().trim()
        val quantityText = binding.editTextQuantity.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()
        val isInsulin = binding.checkBoxInsulin.isChecked
        
        // Показываем loading state
        showLoadingState()
        
        // Валидация
        if (name.isEmpty()) {
            hideLoadingState()
            binding.editTextName.error = "Введите название лекарства"
            return
        }
        
        if (quantityText.isEmpty()) {
            hideLoadingState()
            binding.editTextQuantity.error = "Введите количество"
            return
        }
        
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            hideLoadingState()
            binding.editTextQuantity.error = "Введите корректное количество"
            return
        }
        
        //  ИСПРАВЛЕНО: Определяем dosageTimes на основе выбранной частоты
        val dosageTimes = when (selectedFrequency) {
            DosageFrequency.DAILY -> listOf(DosageTime.MORNING, DosageTime.AFTERNOON, DosageTime.EVENING)
            DosageFrequency.EVERY_OTHER_DAY -> listOf(DosageTime.MORNING)
            DosageFrequency.TWICE_A_WEEK -> listOf(DosageTime.MORNING, DosageTime.EVENING)
            DosageFrequency.THREE_TIMES_A_WEEK -> listOf(DosageTime.MORNING, DosageTime.AFTERNOON, DosageTime.EVENING)
            DosageFrequency.WEEKLY -> listOf(DosageTime.MORNING)
            DosageFrequency.CUSTOM -> listOf(DosageTime.CUSTOM)
        }
        
        android.util.Log.d("AddMedicine", "=== СОЗДАНИЕ ЛЕКАРСТВА ===")
        android.util.Log.d("AddMedicine", "Название: $name")
        android.util.Log.d("AddMedicine", "Частота: $selectedFrequency")
        android.util.Log.d("AddMedicine", "DosageTimes: $dosageTimes")
        android.util.Log.d("AddMedicine", "Время: $selectedTime")
        android.util.Log.d("AddMedicine", "Группа: ${binding.editTextGroupName.text.toString().trim()}")
        android.util.Log.d("AddMedicine", "Порядок в группе: ${binding.editTextGroupOrder.text.toString().toIntOrNull() ?: 1}")
        
        // Создаем лекарство с безопасными значениями
        val medicine = try {
            Medicine(
                id = 0,
                name = name,
                dosage = dosage.ifEmpty { "1" },
                quantity = quantity,
                remainingQuantity = quantity,
                medicineType = selectedMedicineType.ifEmpty { getString(com.medicalnotes.app.R.string.medicine_type_tablets) },
                time = selectedTime,
                notes = notes,
                isInsulin = isInsulin,
                isActive = true,
                takenToday = false,
                isMissed = false,
                frequency = selectedFrequency,
                dosageTimes = dosageTimes,
                customDays = selectedDays.toList(),
                groupName = binding.editTextGroupName.text.toString().trim(),
                groupOrder = binding.editTextGroupOrder.text.toString().toIntOrNull() ?: 1,
                multipleDoses = binding.switchMultipleDoses.isChecked,
                doseTimes = selectedTimes.ifEmpty { listOf(selectedTime) },
                relatedMedicineIds = selectedRelatedMedicines.map { it.id },
                isPartOfGroup = selectedRelatedMedicines.isNotEmpty()
            )
        } catch (e: Exception) {
            android.util.Log.e("AddMedicine", "Error creating Medicine object", e)
            hideLoadingState()
            Toast.makeText(this, "Ошибка создания лекарства: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Сохраняем лекарство
        CoroutineScope(Dispatchers.IO + com.medicalnotes.app.utils.CrashReporter.getCoroutineExceptionHandler()).launch {
            try {
                android.util.Log.d("AddMedicine", "Starting medicine insertion...")
                val success = viewModel.insertMedicine(medicine)
                android.util.Log.d("AddMedicine", "Medicine insertion result: $success")
                
                if (success > 0) {
                    //  ИСПРАВЛЕНО: Планируем уведомление для нового лекарства
                    try {
                        android.util.Log.d("AddMedicine", "Планирование уведомления для нового лекарства: ${medicine.name}")
                        val updatedMedicine = medicine.copy(id = success)
                        val scheduler = com.medicalnotes.app.utils.NotificationScheduler(this@AddMedicineActivity)
                        scheduler.scheduleConsideringEdit(updatedMedicine, isEdit = false)
                        android.util.Log.d("AddMedicine", "✓ Уведомление запланировано для лекарства ID: $success")
                    } catch (e: Exception) {
                        android.util.Log.e("AddMedicine", "Ошибка планирования уведомления", e)
                    }
                    
                    // Если есть связанные лекарства, создаем группу
                    if (selectedRelatedMedicines.isNotEmpty()) {
                        try {
                            createMedicineGroup(medicine, selectedRelatedMedicines)
                        } catch (e: Exception) {
                            android.util.Log.e("AddMedicine", "Ошибка создания группы лекарств", e)
                        }
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        hideLoadingState()
                        Toast.makeText(this@AddMedicineActivity, "Лекарство добавлено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        hideLoadingState()
                        Toast.makeText(this@AddMedicineActivity, "Ошибка добавления лекарства", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AddMedicine", "Critical error during medicine insertion", e)
                CoroutineScope(Dispatchers.Main).launch {
                    hideLoadingState()
                    Toast.makeText(this@AddMedicineActivity, "Критическая ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun createMedicineGroup(newMedicine: Medicine, relatedMedicines: List<Medicine>) {
        val dataManager = DataManager(this)
        val groupName = "Группа ${newMedicine.name}"
        
        // Генерируем уникальный ID группы
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        val groupFrequency = newMedicine.frequency
        
        // Создаем метаданные группы
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = relatedMedicines.size + 1,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:${relatedMedicines.size + 1}".hashCode().toString()
        )
        
        // Обновляем новое лекарство с полными данными группы
        val updatedNewMedicine = newMedicine.copy(
            groupId = groupId,
            groupName = groupName,
            groupOrder = 1,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupValidationHash = groupMetadata.groupValidationHash,
            groupMetadata = groupMetadata
        )
        dataManager.updateMedicine(updatedNewMedicine)
        
        // Обновляем связанные лекарства
        relatedMedicines.forEachIndexed { index, medicine ->
            val updatedMedicine = medicine.copy(
                groupId = groupId,
                groupName = groupName,
                groupOrder = index + 2, // Новое лекарство имеет порядок 1
                groupStartDate = groupStartDate,
                groupFrequency = groupFrequency,
                groupValidationHash = groupMetadata.groupValidationHash,
                groupMetadata = groupMetadata
            )
            dataManager.updateMedicine(updatedMedicine)
        }
        
        android.util.Log.d("AddMedicineActivity", "Создана группа '$groupName' (ID: $groupId) с ${relatedMedicines.size + 1} лекарствами")
        android.util.Log.d("AddMedicineActivity", "Группа: startDate=$groupStartDate, frequency=$groupFrequency")
    }
    
    private fun showCreateGroupFromExistingDialog() {
        val dataManager = DataManager(this)
        val existingMedicines = dataManager.loadMedicines()
        
        if (existingMedicines.isEmpty()) {
            Toast.makeText(this, "Нет доступных лекарств для создания группы", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Создаем список названий лекарств для диалога
        val medicineNames = existingMedicines.map { 
            "${it.name} (${it.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))})" 
        }.toTypedArray()
        
        val selectedMedicines = mutableListOf<Medicine>()
        val checkedItems = BooleanArray(medicineNames.size) { false }
        
        AlertDialog.Builder(this)
            .setTitle("Создать группу из существующих лекарств")
            .setMessage("Выберите лекарства для объединения в группу:")
            .setMultiChoiceItems(medicineNames, checkedItems) { _, which, isChecked ->
                val selectedMedicine = existingMedicines[which]
                if (isChecked) {
                    if (!selectedMedicines.contains(selectedMedicine)) {
                        selectedMedicines.add(selectedMedicine)
                    }
                } else {
                    selectedMedicines.remove(selectedMedicine)
                }
            }
            .setPositiveButton("Создать группу") { _, _ ->
                if (selectedMedicines.isNotEmpty()) {
                    showGroupNameDialog(selectedMedicines)
                } else {
                    Toast.makeText(this, "Выберите хотя бы одно лекарство", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showGroupNameDialog(medicines: List<Medicine>) {
        val input = android.widget.EditText(this)
        input.hint = "Введите название группы"
        input.setText("Группа ${medicines.first().name}")
        
        AlertDialog.Builder(this)
            .setTitle("Название группы")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createGroupFromExistingMedicines(groupName, medicines)
                } else {
                    Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun createGroupFromExistingMedicines(groupName: String, medicines: List<Medicine>) {
        val dataManager = DataManager(this)
        
        // Генерируем уникальный ID группы
        val groupId = System.currentTimeMillis()
        val groupStartDate = System.currentTimeMillis()
        
        // Определяем частоту группы на основе первого лекарства
        val groupFrequency = medicines.firstOrNull()?.frequency ?: DosageFrequency.DAILY
        
        // Создаем метаданные группы
        val groupMetadata = GroupMetadata(
            groupId = groupId,
            groupName = groupName,
            groupStartDate = groupStartDate,
            groupFrequency = groupFrequency,
            groupSize = medicines.size,
            groupValidationHash = "$groupId:$groupName:$groupStartDate:$groupFrequency:${medicines.size}".hashCode().toString()
        )
        
        // Обновляем все выбранные лекарства
        medicines.forEachIndexed { index, medicine ->
            val updatedMedicine = medicine.copy(
                groupId = groupId,
                groupName = groupName,
                groupOrder = index + 1,
                groupStartDate = groupStartDate,
                groupFrequency = groupFrequency,
                groupValidationHash = groupMetadata.groupValidationHash,
                groupMetadata = groupMetadata
            )
            dataManager.updateMedicine(updatedMedicine)
        }
        
        Toast.makeText(this, "Группа '$groupName' создана с ${medicines.size} лекарствами", Toast.LENGTH_SHORT).show()
        android.util.Log.d("AddMedicineActivity", "Создана группа '$groupName' (ID: $groupId) с ${medicines.size} лекарствами")
        android.util.Log.d("AddMedicineActivity", "Группа: startDate=$groupStartDate, frequency=$groupFrequency")
    }
    
    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            android.util.Log.d("AddMedicineActivity", "Updating UI after language change")
            
            // Обновляем заголовок Activity
            title = getString(R.string.add_medicine)
            supportActionBar?.title = getString(R.string.add_medicine)
            
            // Обновляем заголовки полей
            updateFieldLabels()
            
            // Обновляем подсказки (hints)
            updateFieldHints()
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            // Обновляем текст в спиннерах и адаптерах
            updateSpinnerTexts()
            
            android.util.Log.d("AddMedicineActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating UI after language change", e)
        }
    }
    
    private fun updateFieldLabels() {
        try {
            // Обновляем заголовки полей (используем доступные элементы)
            // binding.textInputLayoutName.hint = getString(R.string.medicine_name)
            // binding.textInputLayoutQuantity.hint = getString(R.string.quantity)
            // binding.textInputLayoutNotes.hint = getString(R.string.notes)
            // binding.textInputLayoutType.hint = getString(R.string.medicine_type)
            
            // Обновляем заголовки секций (используем доступные элементы)
            // binding.textViewTimeSection.text = getString(R.string.time_section)
            // binding.textViewFrequencySection.text = getString(R.string.frequency_section)
            // binding.textViewGroupSection.text = getString(R.string.group_section)
            
            android.util.Log.d("AddMedicineActivity", "Field labels update completed")
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating field labels", e)
        }
    }
    
    private fun updateFieldHints() {
        try {
            // Обновляем подсказки в полях ввода
            binding.editTextName.hint = getString(R.string.enter_medicine_name)
            binding.editTextQuantity.hint = getString(R.string.enter_quantity)
            binding.editTextNotes.hint = getString(R.string.enter_notes)
            
            // Обновляем подсказки в TextInputLayout (если доступны)
            try {
                (binding.editTextName.parent as? com.google.android.material.textfield.TextInputLayout)?.hint = getString(R.string.medicine_name)
                (binding.editTextQuantity.parent as? com.google.android.material.textfield.TextInputLayout)?.hint = getString(R.string.quantity)
                (binding.editTextNotes.parent as? com.google.android.material.textfield.TextInputLayout)?.hint = getString(R.string.notes)
            } catch (e: Exception) {
                android.util.Log.w("AddMedicineActivity", "Could not update TextInputLayout hints", e)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating field hints", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок (используем доступные элементы)
            // binding.buttonSave.text = getString(R.string.save)
            // binding.buttonCancel.text = getString(R.string.cancel)
            // binding.buttonAddTime.text = getString(R.string.add_time)
            // binding.buttonCreateGroup.text = getString(R.string.create_group)
            // binding.buttonAddToGroup.text = getString(R.string.add_to_group)
            
            android.util.Log.d("AddMedicineActivity", "Button texts update completed")
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating button texts", e)
        }
    }
    
    private fun updateSpinnerTexts() {
        try {
            // Обновляем текст в спиннерах частоты
            updateFrequencyDisplay()
            
            // Обновляем отображение времени
            updateTimeDisplay()
            
            // ОБНОВЛЯЕМ СПИСОК ТИПОВ ЛЕКАРСТВ
            updateMedicineTypeDropdown()
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating spinner texts", e)
        }
    }
    
    private fun updateMedicineTypeDropdown() {
        try {
            // Создаем новый список типов лекарств с обновленными строками
            val medicineTypes = listOf(
                getString(com.medicalnotes.app.R.string.medicine_type_tablets),
                getString(com.medicalnotes.app.R.string.medicine_type_capsules), 
                getString(com.medicalnotes.app.R.string.medicine_type_injections),
                getString(com.medicalnotes.app.R.string.medicine_type_ozempic),
                getString(com.medicalnotes.app.R.string.medicine_type_mounjaro),
                getString(com.medicalnotes.app.R.string.medicine_type_insulin),
                getString(com.medicalnotes.app.R.string.medicine_type_drops),
                getString(com.medicalnotes.app.R.string.medicine_type_syrup),
                getString(com.medicalnotes.app.R.string.medicine_type_inhalations),
                getString(com.medicalnotes.app.R.string.medicine_type_ointments),
                getString(com.medicalnotes.app.R.string.medicine_type_gels),
                getString(com.medicalnotes.app.R.string.medicine_type_creams),
                getString(com.medicalnotes.app.R.string.medicine_type_suppositories),
                getString(com.medicalnotes.app.R.string.medicine_type_sprays),
                getString(com.medicalnotes.app.R.string.medicine_type_aerosols),
                getString(com.medicalnotes.app.R.string.medicine_type_powders),
                getString(com.medicalnotes.app.R.string.medicine_type_suspensions),
                getString(com.medicalnotes.app.R.string.medicine_type_emulsions),
                getString(com.medicalnotes.app.R.string.medicine_type_other)
            )
            
            // Создаем новый адаптер с обновленными данными
            val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medicineTypes)
            binding.autoCompleteMedicineType.setAdapter(adapter)
            
            // Сохраняем текущее выбранное значение
            val currentText = binding.autoCompleteMedicineType.text.toString()
            
            // Если текущий текст соответствует какому-то типу в новом списке, обновляем его
            val matchingType = medicineTypes.find { it == currentText }
            if (matchingType != null) {
                binding.autoCompleteMedicineType.setText(matchingType, false)
                selectedMedicineType = matchingType
            } else {
                // Если не найдено соответствие, устанавливаем значение по умолчанию
                val defaultType = getString(com.medicalnotes.app.R.string.medicine_type_tablets)
                binding.autoCompleteMedicineType.setText(defaultType, false)
                selectedMedicineType = defaultType
            }
            
            android.util.Log.d("AddMedicineActivity", "Medicine type dropdown updated successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineActivity", "Error updating medicine type dropdown", e)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        //  ДОБАВЛЕНО: Уведомляем сервис об окончании редактирования
        com.medicalnotes.app.service.OverdueCheckService.setEditingActive(false)
        android.util.Log.i("AddMedicine", "onDestroy: редактирование завершено")
    }
} 