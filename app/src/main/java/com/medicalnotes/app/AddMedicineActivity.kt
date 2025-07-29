package com.medicalnotes.app

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.databinding.ActivityAddMedicineBinding
import com.medicalnotes.app.models.DosageFrequency
import com.medicalnotes.app.models.DosageTime
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.service.NotificationService
import com.medicalnotes.app.utils.CustomTimePickerDialog
import com.medicalnotes.app.viewmodels.AddMedicineViewModel
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AddMedicineActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddMedicineBinding
    private lateinit var viewModel: AddMedicineViewModel
    private var selectedTime: LocalTime = LocalTime.of(8, 0)
    private var selectedFrequency = DosageFrequency.DAILY
    private var selectedTimes = mutableListOf<LocalTime>()
    private var selectedMedicineType = "Таблетки"
    private var selectedDays = mutableSetOf<Int>() // Дни недели (1=понедельник, 7=воскресенье)
    private var selectedRelatedMedicines = mutableListOf<Medicine>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[AddMedicineViewModel::class.java]
        
        setupViews()
        setupListeners()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Добавить лекарство"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настраиваем AutoCompleteTextView для типов лекарств
        setupMedicineTypeDropdown()
        
        // Устанавливаем время по умолчанию
        updateTimeDisplay()
        updateFrequencyDisplay()
    }
    
    private fun setupMedicineTypeDropdown() {
        val medicineTypes = listOf(
            "Таблетки",
            "Капсулы", 
            "Уколы (инъекции)",
            "Оземпик",
            "Мунджаро",
            "Инсулин",
            "Капли",
            "Сироп",
            "Ингаляции",
            "Мази",
            "Гели",
            "Кремы",
            "Свечи",
            "Спреи",
            "Аэрозоли",
            "Порошки",
            "Суспензии",
            "Эмульсии",
            "Другое"
        )
        
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, medicineTypes)
        binding.autoCompleteMedicineType.setAdapter(adapter)
        
        // ✅ ИСПРАВЛЕНО: Правильно устанавливаем значение по умолчанию
        binding.autoCompleteMedicineType.setText(selectedMedicineType, false)
        android.util.Log.d("AddMedicine", "Set default medicine type: $selectedMedicineType")
        
        // ✅ ИСПРАВЛЕНО: Обработчик выбора типа лекарства
        binding.autoCompleteMedicineType.setOnItemClickListener { _, _, position, _ ->
            selectedMedicineType = medicineTypes[position]
            android.util.Log.d("AddMedicine", "Medicine type selected: $selectedMedicineType")
            
            // Автоматически отмечаем чекбокс инсулина для соответствующих типов
            binding.checkBoxInsulin.isChecked = selectedMedicineType == "Инсулин" || 
                                               selectedMedicineType == "Оземпик" || 
                                               selectedMedicineType == "Мунджаро"
            
            android.util.Log.d("AddMedicine", "Insulin checkbox set to: ${binding.checkBoxInsulin.isChecked}")
        }
        
        // ✅ ИСПРАВЛЕНО: Обработчики для AutoCompleteTextView
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
        binding.buttonFrequency.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val isEveryOtherDay = selectedFrequency == DosageFrequency.EVERY_OTHER_DAY
                val isCustom = selectedFrequency == DosageFrequency.CUSTOM
                
                binding.layoutGrouping.visibility = if (isEveryOtherDay) View.VISIBLE else View.GONE
                binding.layoutWeekDays.visibility = if (isCustom) View.VISIBLE else View.GONE
                
                // Если выбрали "через день", показываем поля группы
                // Если выбрали "по расписанию", показываем выбор дней недели
            }
        })
        

        
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
        val frequencyText = when (selectedFrequency) {
            DosageFrequency.DAILY -> "Каждый день"
            DosageFrequency.EVERY_OTHER_DAY -> "Через день"
            DosageFrequency.TWICE_A_WEEK -> "2 раза в неделю"
            DosageFrequency.THREE_TIMES_A_WEEK -> "3 раза в неделю"
            DosageFrequency.WEEKLY -> "Раз в неделю"
            DosageFrequency.CUSTOM -> "По расписанию"
        }
        binding.buttonFrequency.text = frequencyText
    }
    
    private fun showFrequencyDialog() {
        val frequencies = arrayOf(
            "Каждый день",
            "Через день", 
            "2 раза в неделю",
            "3 раза в неделю",
            "Раз в неделю",
            "По расписанию"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Выберите схему приема")
            .setItems(frequencies) { _, which ->
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
            .show()
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
        if (selectedDays.isEmpty()) {
            binding.buttonWeekDays.text = "Выбрать дни"
        } else {
            val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            val selectedDayNames = selectedDays.sorted().map { dayNames[it - 1] }
            binding.buttonWeekDays.text = "Дни: ${selectedDayNames.joinToString(", ")}"
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
        
        // ✅ ИСПРАВЛЕНО: Определяем dosageTimes на основе выбранной частоты
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
        
        // Создаем лекарство
        val medicine = Medicine(
            id = 0,
            name = name,
            dosage = dosage,
            quantity = quantity,
            remainingQuantity = quantity,
            medicineType = selectedMedicineType,
            time = selectedTime,
            notes = notes,
            isInsulin = isInsulin,
            isActive = true,
            takenToday = false,
            isMissed = false,
            frequency = selectedFrequency, // ✅ ИСПРАВЛЕНО: Добавляем частоту
            dosageTimes = dosageTimes, // ✅ ИСПРАВЛЕНО: Добавляем времена приема
            customDays = selectedDays.toList(),
            groupName = binding.editTextGroupName.text.toString().trim(),
            groupOrder = binding.editTextGroupOrder.text.toString().toIntOrNull() ?: 1,
            multipleDoses = binding.switchMultipleDoses.isChecked,
            doseTimes = selectedTimes,
            relatedMedicineIds = selectedRelatedMedicines.map { it.id },
            isPartOfGroup = selectedRelatedMedicines.isNotEmpty()
        )
        
        // Сохраняем лекарство
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = viewModel.insertMedicine(medicine)
                
                if (success > 0) {
                    // Если есть связанные лекарства, создаем группу
                    if (selectedRelatedMedicines.isNotEmpty()) {
                        createMedicineGroup(medicine, selectedRelatedMedicines)
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        hideLoadingState()
                        Toast.makeText(this@AddMedicineActivity, "Лекарство добавлено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        hideLoadingState()
                        Toast.makeText(this@AddMedicineActivity, "Ошибка добавления", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    hideLoadingState()
                    Toast.makeText(this@AddMedicineActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun createMedicineGroup(newMedicine: Medicine, relatedMedicines: List<Medicine>) {
        val dataManager = DataManager(this)
        val groupName = "Группа ${newMedicine.name}"
        
        // Обновляем новое лекарство с названием группы
        val updatedNewMedicine = newMedicine.copy(
            groupName = groupName,
            groupOrder = 1
        )
        dataManager.updateMedicine(updatedNewMedicine)
        
        // Обновляем связанные лекарства
        relatedMedicines.forEachIndexed { index, medicine ->
            val updatedMedicine = medicine.copy(
                groupName = groupName,
                groupOrder = index + 2 // Новое лекарство имеет порядок 1
            )
            dataManager.updateMedicine(updatedMedicine)
        }
        
        android.util.Log.d("AddMedicineActivity", "Создана группа '$groupName' с ${relatedMedicines.size + 1} лекарствами")
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
        
        // Обновляем все выбранные лекарства
        medicines.forEachIndexed { index, medicine ->
            val updatedMedicine = medicine.copy(
                groupName = groupName,
                groupOrder = index + 1
            )
            dataManager.updateMedicine(updatedMedicine)
        }
        
        Toast.makeText(this, "Группа '$groupName' создана с ${medicines.size} лекарствами", Toast.LENGTH_SHORT).show()
        android.util.Log.d("AddMedicineActivity", "Создана группа '$groupName' с ${medicines.size} лекарствами")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 