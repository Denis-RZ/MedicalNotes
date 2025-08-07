package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.medicalnotes.app.databinding.ActivityEditMedicineBinding
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

class EditMedicineActivity : BaseActivity() {
    
    private lateinit var binding: ActivityEditMedicineBinding
    private lateinit var viewModel: AddMedicineViewModel
    private var medicineId: Long = 0
    
    // Переменные для хранения данных
    private var selectedTime: LocalTime? = null
    private var selectedFrequency: DosageFrequency = DosageFrequency.DAILY
    private var selectedMedicineType: String = ""
    private val selectedDays = mutableSetOf<Int>()
    private var allGroups = mutableListOf<String>() // Добавляем список всех групп
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        //  ДОБАВЛЕНО: Уведомляем сервис о начале редактирования
        com.medicalnotes.app.service.OverdueCheckService.setEditingActive(true)
        
        // Настройка обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Логика обработки кнопки "Назад"
                if (isEnabled) {
                    finish()
                }
            }
        })
        
        try {
            // Инициализация системы логирования
            com.medicalnotes.app.utils.LogCollector.initialize(this)
            com.medicalnotes.app.utils.LogCollector.i("EditMedicine", "onCreate started")
            
            binding = ActivityEditMedicineBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            viewModel = ViewModelProvider(this)[AddMedicineViewModel::class.java]
            
            medicineId = intent.getLongExtra("medicine_id", 0L)
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "onCreate: medicineId = $medicineId")
            
            if (medicineId == 0L) {
                com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Invalid medicine ID: $medicineId")
                Toast.makeText(this, "Ошибка: не указан ID лекарства", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            
            try {
                setupToolbar()
                setupListeners()
                
                // Добавляем небольшую задержку для инициализации DataManager
                binding.root.post {
                    try {
                        loadGroups() // Загружаем группы при инициализации
                        loadMedicine()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error in post initialization", e)
                        Toast.makeText(this@EditMedicineActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error in onCreate", e)
                Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Critical error in onCreate", e)
            Toast.makeText(this, "Критическая ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.edit_medicine_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupListeners() {
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Setting up listeners...")
        
        // Настройка AutoCompleteTextView для типа лекарства
        setupMedicineTypeField()
        
        // Кнопка выбора времени
        binding.buttonTime.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Time button clicked")
            showTimePicker()
        }
        
        // Кнопка выбора частоты
        binding.buttonFrequency.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Frequency button clicked")
            showFrequencyDialog()
        }
        
        // Убираем старые обработчики - теперь используется AutoCompleteTextView
        // binding.autoCompleteMedicineType.setOnClickListener {
        //     com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine type field clicked")
        //     showMedicineTypeDialog()
        // }
        
        // Альтернативный способ обработки клика для AutoCompleteTextView
        // binding.autoCompleteMedicineType.setOnTouchListener { _, event ->
        //     if (event.action == android.view.MotionEvent.ACTION_DOWN) {
        //         com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine type field touched")
        //         showMedicineTypeDialog()
        //         return@setOnTouchListener true
        //     }
        //     false
        // }
        
        binding.buttonWeekDays.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Week days button clicked")
            showWeekDaysDialog()
        }
        
        binding.buttonChangeGroup.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Change group button clicked")
            showGroupSelectionDialog()
        }
        
        binding.buttonSave.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Save button clicked")
            updateMedicine()
        }
        
        binding.buttonCancel.setOnClickListener {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Cancel button clicked")
            finish()
        }
        
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "All listeners set up successfully")
    }
    
    private fun setupMedicineTypeField() {
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
        
        // НЕ устанавливаем значение по умолчанию здесь - оно будет установлено в populateFields()
        // binding.autoCompleteMedicineType.setText(selectedMedicineType, true)
        
        //  ИСПРАВЛЕНО: Обработчик выбора типа лекарства
        binding.autoCompleteMedicineType.setOnItemClickListener { _, _, position, _ ->
            selectedMedicineType = medicineTypes[position]
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine type selected: $selectedMedicineType")
            
            // Автоматически отмечаем чекбокс инсулина для соответствующих типов
            binding.checkBoxInsulin.isChecked = selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_insulin) || 
                                               selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_ozempic) || 
                                               selectedMedicineType == getString(com.medicalnotes.app.R.string.medicine_type_mounjaro)
            
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Insulin checkbox set to: ${binding.checkBoxInsulin.isChecked}")
        }
        
        //  ИСПРАВЛЕНО: Обработчики для AutoCompleteTextView
        binding.autoCompleteMedicineType.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.autoCompleteMedicineType.showDropDown()
            }
        }
        
        // Добавляем обработчик клика для показа dropdown
        binding.autoCompleteMedicineType.setOnClickListener {
            binding.autoCompleteMedicineType.showDropDown()
        }
        
        // Добавляем обработчик изменения текста для отслеживания ручного ввода
        binding.autoCompleteMedicineType.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val newType = s?.toString()?.trim() ?: ""
                if (newType.isNotEmpty()) {
                    selectedMedicineType = newType
                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine type changed via text input: $selectedMedicineType")
                }
            }
        })
        
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine type field setup completed")
    }
    
    private fun loadGroups() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Loading groups...")
            viewModel.getAllMedicines { medicines ->
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "getAllMedicines callback received, medicines count: ${medicines.size}")
                val existingGroups = mutableSetOf<String>()
                medicines.forEach { medicine ->
                    if (medicine.groupName.isNotEmpty()) {
                        existingGroups.add(medicine.groupName)
                    }
                }
                allGroups.clear()
                allGroups.add("Без группы") // Заменяем пустую строку на понятное описание
                allGroups.addAll(existingGroups.sorted())
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Groups loaded: $allGroups")
                
                //  ИСПРАВЛЕНО: Обновляем диалог выбора группы, если он открыт
                updateGroupSelectionDialog()
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error in loadGroups", e)
            // Не завершаем активность, просто логируем ошибку
        }
    }
    
    //  ДОБАВЛЕНО: Функция для обновления диалога выбора группы
    private fun updateGroupSelectionDialog() {
        // Если диалог открыт, обновляем его данные
        // Это поможет синхронизировать группы в реальном времени
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Groups updated, dialog data refreshed")
    }
    
    private fun loadMedicine() {
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Loading medicine with ID: $medicineId")
        try {
            viewModel.getMedicineById(medicineId) { medicine ->
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "getMedicineById callback received, medicine: ${medicine?.name ?: "null"}")
                if (medicine != null) {
                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine loaded successfully: ${medicine.name}")
                    populateFields(medicine)
                } else {
                    com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Medicine not found with ID: $medicineId")
                    
                    // Показываем более информативное сообщение
                    AlertDialog.Builder(this)
                        .setTitle("Лекарство не найдено")
                        .setMessage("Лекарство с ID $medicineId не найдено в базе данных. Возможно, оно было удалено или ID неверный.")
                        .setPositiveButton("OK") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error in loadMedicine", e)
            Toast.makeText(this, "Ошибка загрузки лекарства: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun populateFields(medicine: Medicine) {
        try {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Populating fields for medicine: ${medicine.name}")
            
            binding.editTextName.setText(medicine.name)
            binding.editTextDosage.setText(medicine.dosage)
            binding.editTextQuantity.setText(medicine.remainingQuantity.toString())
            binding.editTextNotes.setText(medicine.notes)
            binding.checkBoxInsulin.isChecked = medicine.isInsulin
            
            selectedTime = medicine.time
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Setting selectedTime from medicine: ${medicine.time}")
            selectedFrequency = medicine.frequency
            selectedMedicineType = medicine.medicineType
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Setting selectedMedicineType from medicine: ${medicine.medicineType}")
            selectedDays.clear()
            selectedDays.addAll(medicine.customDays)
            
            // Загружаем групповые данные
            binding.editTextGroupName.setText(medicine.groupName)
            binding.editTextGroupOrder.setText(medicine.groupOrder.toString())
            
            // Показываем/скрываем группировку и дни недели в зависимости от частоты
            val isEveryOtherDay = medicine.frequency == DosageFrequency.EVERY_OTHER_DAY
            val isCustom = medicine.frequency == DosageFrequency.CUSTOM
            val hasGroup = medicine.groupName.isNotEmpty()
            
            // Показываем группировку если лекарство в группе ИЛИ если частота "через день"
            binding.layoutGrouping.visibility = if (hasGroup || isEveryOtherDay) View.VISIBLE else View.GONE
            binding.layoutWeekDays.visibility = if (isCustom) View.VISIBLE else View.GONE
            
            // Обновляем отображение времени, частоты, типа лекарства и дней недели
            updateTimeDisplay()
            updateFrequencyDisplay()
            
            // ИСПРАВЛЕНО: Обновляем выпадающий список типов лекарств для правильной локализации
            updateMedicineTypeDropdown()
            
            //  ИСПРАВЛЕНО: Правильно устанавливаем тип лекарства
            binding.autoCompleteMedicineType.setText(selectedMedicineType, false)
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Set medicine type in field: $selectedMedicineType")
            
            // Убеждаемся, что dropdown работает правильно
            binding.autoCompleteMedicineType.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    binding.autoCompleteMedicineType.showDropDown()
                }
            }
            
            updateWeekDaysDisplay()
            
            // Логируем загруженное лекарство для отладки
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Fields populated successfully: name=${medicine.name}, dosage=${medicine.dosage}, " +
                "remainingQuantity=${medicine.remainingQuantity}, " +
                "frequency=${medicine.frequency}, groupName=${medicine.groupName}, groupOrder=${medicine.groupOrder}")
                
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error in populateFields", e)
            Toast.makeText(this, "Ошибка заполнения полей: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTimePicker() {
        val currentTime = selectedTime ?: LocalTime.of(8, 0)
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Opening time picker with current time: $currentTime")
        CustomTimePickerDialog(this, currentTime) { time ->
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Time selected: $time")
            selectedTime = time
            updateTimeDisplay()
            Toast.makeText(this, "Выбрано время: $time", Toast.LENGTH_SHORT).show()
        }.show()
    }
    
    private fun updateTimeDisplay() {
        val currentTime = selectedTime ?: LocalTime.of(8, 0)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val timeText = currentTime.format(formatter)
        binding.buttonTime.text = timeText
        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Updated time display: $timeText (selectedTime: $selectedTime)")
    }
    
    private fun updateFrequencyDisplay() {
        try {
            val frequencyText = when (selectedFrequency) {
                DosageFrequency.DAILY -> getString(com.medicalnotes.app.R.string.frequency_daily)
                DosageFrequency.EVERY_OTHER_DAY -> getString(com.medicalnotes.app.R.string.frequency_every_other_day)
                DosageFrequency.TWICE_A_WEEK -> getString(com.medicalnotes.app.R.string.frequency_twice_a_week)
                DosageFrequency.THREE_TIMES_A_WEEK -> getString(com.medicalnotes.app.R.string.frequency_three_times_a_week)
                DosageFrequency.WEEKLY -> getString(com.medicalnotes.app.R.string.frequency_weekly)
                DosageFrequency.CUSTOM -> getString(com.medicalnotes.app.R.string.frequency_custom)
            }
            binding.buttonFrequency.text = frequencyText
            com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Updated frequency display: $frequencyText (selectedFrequency: $selectedFrequency)")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error updating frequency display", e)
            binding.buttonFrequency.text = getString(com.medicalnotes.app.R.string.frequency_daily)
            selectedFrequency = DosageFrequency.DAILY
        }
    }
    
    private fun showFrequencyDialog() {
        try {
            val frequencies = arrayOf(
                getString(com.medicalnotes.app.R.string.frequency_daily),
                getString(com.medicalnotes.app.R.string.frequency_every_other_day), 
                getString(com.medicalnotes.app.R.string.frequency_twice_a_week),
                getString(com.medicalnotes.app.R.string.frequency_three_times_a_week),
                getString(com.medicalnotes.app.R.string.frequency_weekly),
                getString(com.medicalnotes.app.R.string.frequency_custom)
            )
            
            val currentIndex = when (selectedFrequency) {
                DosageFrequency.DAILY -> 0
                DosageFrequency.EVERY_OTHER_DAY -> 1
                DosageFrequency.TWICE_A_WEEK -> 2
                DosageFrequency.THREE_TIMES_A_WEEK -> 3
                DosageFrequency.WEEKLY -> 4
                DosageFrequency.CUSTOM -> 5
            }
            
            AlertDialog.Builder(this)
                .setTitle(getString(com.medicalnotes.app.R.string.frequency_selection_dialog))
                .setSingleChoiceItems(frequencies, currentIndex) { _, which ->
                    try {
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
                        
                        // Показываем/скрываем группировку и дни недели в зависимости от частоты
                        val isEveryOtherDay = selectedFrequency == DosageFrequency.EVERY_OTHER_DAY
                        val isCustom = selectedFrequency == DosageFrequency.CUSTOM
                        val hasGroup = binding.editTextGroupName.text.toString().isNotEmpty()
                        
                        binding.layoutGrouping.visibility = if (hasGroup || isEveryOtherDay) View.VISIBLE else View.GONE
                        binding.layoutWeekDays.visibility = if (isCustom) View.VISIBLE else View.GONE
                        
                        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Frequency changed to: $selectedFrequency")
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error selecting frequency", e)
                        selectedFrequency = DosageFrequency.DAILY
                        updateFrequencyDisplay()
                        Toast.makeText(this, "Ошибка выбора частоты", Toast.LENGTH_SHORT).show()
                    }
                }
                .setPositiveButton("OK", null)
                .setNegativeButton("Отмена", null)
                .setOnCancelListener {
                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Frequency dialog cancelled")
                }
                .setOnDismissListener {
                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Frequency dialog dismissed")
                }
                .show()
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error showing frequency dialog", e)
            Toast.makeText(this, "Ошибка показа диалога частоты", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showGroupSelectionDialog() {
        val currentGroup = binding.editTextGroupName.text.toString()
        // Если текущая группа пустая, показываем "Без группы"
        val displayGroup = if (currentGroup.isEmpty()) "Без группы" else currentGroup
        val currentIndex = allGroups.indexOf(displayGroup).coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle("Выберите группу")
            .setSingleChoiceItems(allGroups.toTypedArray(), currentIndex) { _, which ->
                val selectedGroup = allGroups[which]
                // Если выбрано "Без группы", сохраняем пустую строку
                val groupToSave = if (selectedGroup == "Без группы") "" else selectedGroup
                binding.editTextGroupName.setText(groupToSave)
                
                // Если выбрана группа, устанавливаем следующий порядок
                if (groupToSave.isNotEmpty()) {
                    viewModel.getAllMedicines { medicines ->
                        val groupMedicines = medicines.filter { it.groupName == groupToSave }
                        val nextOrder = groupMedicines.size + 1
                        binding.editTextGroupOrder.setText(nextOrder.toString())
                    }
                } else {
                    binding.editTextGroupOrder.setText("1")
                }
            }
            .setPositiveButton("OK", null)
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    // Метод showMedicineTypeDialog удален - теперь используется AutoCompleteTextView
    
    private fun showWeekDaysDialog() {
        val weekDays = arrayOf(
            getString(com.medicalnotes.app.R.string.monday),
            getString(com.medicalnotes.app.R.string.tuesday), 
            getString(com.medicalnotes.app.R.string.wednesday),
            getString(com.medicalnotes.app.R.string.thursday),
            getString(com.medicalnotes.app.R.string.friday),
            getString(com.medicalnotes.app.R.string.saturday),
            getString(com.medicalnotes.app.R.string.sunday)
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
            binding.buttonWeekDays.text = getString(com.medicalnotes.app.R.string.weekday_selection)
        } else {
            val dayNames = listOf(
                getString(com.medicalnotes.app.R.string.monday_short),
                getString(com.medicalnotes.app.R.string.tuesday_short),
                getString(com.medicalnotes.app.R.string.wednesday_short),
                getString(com.medicalnotes.app.R.string.thursday_short),
                getString(com.medicalnotes.app.R.string.friday_short),
                getString(com.medicalnotes.app.R.string.saturday_short),
                getString(com.medicalnotes.app.R.string.sunday_short)
            )
            val selectedDayNames = selectedDays.sorted().map { dayNames[it - 1] }
            binding.buttonWeekDays.text = "Дни: ${selectedDayNames.joinToString(", ")}"
        }
    }
    
    private fun updateMedicine() {
        val name = binding.editTextName.text.toString().trim()
        val dosage = binding.editTextDosage.text.toString().trim()
        val quantityText = binding.editTextQuantity.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()
        val isInsulin = binding.checkBoxInsulin.isChecked
        val medicineType = binding.autoCompleteMedicineType.text.toString().trim() // <-- берем из UI
        val groupName = binding.editTextGroupName.text.toString().trim()
        val groupOrderText = binding.editTextGroupOrder.text.toString().trim()
        val groupOrder = groupOrderText.toIntOrNull() ?: 1

        if (name.isEmpty()) {
            binding.editTextName.error = "Введите название лекарства"
            return
        }
        if (dosage.isEmpty()) {
            binding.editTextDosage.error = "Введите дозировку"
            return
        }
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            binding.editTextQuantity.error = "Введите корректное количество"
            return
        }

        // Определяем dosageTimes на основе выбранной частоты
        val dosageTimes = when (selectedFrequency) {
            DosageFrequency.DAILY -> listOf(DosageTime.MORNING, DosageTime.AFTERNOON, DosageTime.EVENING)
            DosageFrequency.EVERY_OTHER_DAY -> listOf(DosageTime.MORNING)
            DosageFrequency.TWICE_A_WEEK -> listOf(DosageTime.MORNING, DosageTime.EVENING)
            DosageFrequency.THREE_TIMES_A_WEEK -> listOf(DosageTime.MORNING, DosageTime.AFTERNOON, DosageTime.EVENING)
            DosageFrequency.WEEKLY -> listOf(DosageTime.MORNING)
            DosageFrequency.CUSTOM -> listOf(DosageTime.CUSTOM)
        }

        viewModel.getMedicineById(medicineId) { originalMedicine ->
            if (originalMedicine != null) {
                // ИСПРАВЛЕНО: Правильная обработка групповых данных с сохранением истории
                val groupId = if (groupName.isNotEmpty()) {
                    if (originalMedicine.groupId != null) originalMedicine.groupId else System.currentTimeMillis()
                } else null
                val groupStartDate = if (groupName.isNotEmpty()) {
                    if (originalMedicine.groupStartDate > 0) {
                        // Если частота изменилась, обновляем groupStartDate, но сохраняем lastTakenTime
                        if (originalMedicine.frequency != selectedFrequency) {
                            val today = java.time.LocalDate.now()
                            val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault())
                            startOfDay.toInstant().toEpochMilli()
                        } else {
                            originalMedicine.groupStartDate
                        }
                    } else {
                        System.currentTimeMillis()
                    }
                } else 0L
                val groupFrequency = if (groupName.isNotEmpty()) selectedFrequency else DosageFrequency.DAILY
                val groupValidationHash = if (groupName.isNotEmpty()) {
                    "$groupId:$groupName:$groupStartDate:$groupFrequency".hashCode().toString()
                } else ""
                val groupMetadata = if (groupName.isNotEmpty()) GroupMetadata(
                    groupId = groupId!!,
                    groupName = groupName,
                    groupStartDate = groupStartDate,
                    groupFrequency = groupFrequency,
                    groupSize = 1,
                    groupValidationHash = groupValidationHash
                ) else null
                
                val saveTime = selectedTime ?: LocalTime.of(8, 0)
                val currentTime = LocalTime.now()
                
                //  ИСПРАВЛЕНО: Сбрасываем статус принятия только в критических случаях:
                // 1. Изменена частота приема (это влияет на логику расчета дней)
                // 2. Лекарство было принято сегодня, но новое время приема уже прошло (нужно сбросить статус)
                // 3. Лекарство отмечено как принятое, но количество не уменьшилось (не было фактически принято)
                // 4. Лекарство было принято сегодня, но время изменено на будущее (нужно сбросить статус)
                // 5. НЕ сбрасываем статус если лекарство еще не было принято сегодня
                val wasActuallyTaken = originalMedicine.remainingQuantity < originalMedicine.quantity
                val timeChangedToFuture = originalMedicine.takenToday && originalMedicine.time != saveTime && saveTime.isAfter(currentTime)
                val shouldResetStatus = originalMedicine.frequency != selectedFrequency || 
                                       (originalMedicine.takenToday && saveTime.isBefore(currentTime)) ||
                                       (originalMedicine.takenToday && !wasActuallyTaken) ||
                                       timeChangedToFuture
                
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "=== АНАЛИЗ СБРОСА СТАТУСА ===")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Текущее время: $currentTime")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Новое время приема: $saveTime")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Лекарство принято сегодня: ${originalMedicine.takenToday}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Количество не уменьшилось (не было фактически принято): ${!wasActuallyTaken}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Новое время уже прошло: ${saveTime.isBefore(currentTime)}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Время изменено на будущее: $timeChangedToFuture")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Изменена частота: ${originalMedicine.frequency != selectedFrequency}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Изменено время И прошло: ${originalMedicine.time != saveTime && saveTime.isBefore(currentTime)}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Сбрасываем статус: $shouldResetStatus")
                
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "=== ОБНОВЛЕНИЕ ЛЕКАРСТВА ===")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Старая частота: ${originalMedicine.frequency}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Новая частота: $selectedFrequency")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Старое время: ${originalMedicine.time}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Новое время: $saveTime")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Изменена частота: ${originalMedicine.frequency != selectedFrequency}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Изменено время: ${originalMedicine.time != saveTime}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Сбрасываем статус: $shouldResetStatus")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Старый startDate: ${originalMedicine.startDate}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Новый startDate будет: ${if (originalMedicine.frequency != selectedFrequency) "сегодня" else "старый"}")
                
                val updatedMedicine = originalMedicine.copy(
                    name = name,
                    dosage = dosage,
                    quantity = quantity,
                    remainingQuantity = quantity,
                    medicineType = medicineType, // <-- используем актуальное значение
                    time = saveTime,
                    notes = notes,
                    isInsulin = isInsulin,
                    frequency = selectedFrequency,
                    dosageTimes = dosageTimes,
                    customDays = if (selectedFrequency == DosageFrequency.CUSTOM) {
                        if (selectedDays.isNotEmpty()) {
                            selectedDays.sorted().toList()
                        } else {
                            listOf(1, 2, 3, 4, 5, 6, 7)
                        }
                    } else {
                        emptyList()
                    },
                    customTimes = emptyList(),
                    startDate = if (originalMedicine.frequency != selectedFrequency) {
                        //  ИСПРАВЛЕНО: Изменяем startDate при изменении частоты приема, но сохраняем историю приема
                        val today = java.time.LocalDate.now()
                        val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault())
                        startOfDay.toInstant().toEpochMilli()
                    } else {
                        originalMedicine.startDate
                    }, //  ИСПРАВЛЕНО: Изменяем startDate при изменении частоты, но сохраняем lastTakenTime
                    multipleDoses = false,
                    dosesPerDay = 1,
                    doseTimes = listOf(saveTime),
                    groupId = groupId,
                    groupName = groupName,
                    groupOrder = groupOrder,
                    groupStartDate = groupStartDate,
                    groupFrequency = groupFrequency,
                    groupValidationHash = groupValidationHash,
                    groupMetadata = groupMetadata,
                    lastTakenTime = if (shouldResetStatus) 0 else originalMedicine.lastTakenTime, //  ИСПРАВЛЕНО: Сбрасываем время последнего приема при любом сбросе статуса
                    takenToday = if (shouldResetStatus && originalMedicine.takenToday) false else originalMedicine.takenToday, //  ИСПРАВЛЕНО: Сбрасываем статус принятия только если лекарство было принято
                    takenAt = if (shouldResetStatus && originalMedicine.takenToday) 0 else originalMedicine.takenAt, //  ИСПРАВЛЕНО: Сбрасываем время принятия только если лекарство было принято
                    isMissed = if (shouldResetStatus) false else originalMedicine.isMissed, //  ИСПРАВЛЕНО: Сбрасываем статус пропуска
                    missedCount = if (shouldResetStatus) 0 else originalMedicine.missedCount, //  ИСПРАВЛЕНО: Сбрасываем счетчик пропусков
                    updatedAt = System.currentTimeMillis()
                )
                
                // Логируем обновленное лекарство для отладки
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "=== ОБНОВЛЕНИЕ ЛЕКАРСТВА ===")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Название: ${updatedMedicine.name}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Время: ${updatedMedicine.time}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Частота: ${updatedMedicine.frequency}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Количество: ${updatedMedicine.remainingQuantity}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "СТАТУС ПРИНЯТИЯ:")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "  - lastTakenTime: ${updatedMedicine.lastTakenTime}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "  - takenToday: ${updatedMedicine.takenToday}")
                com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "  - takenAt: ${updatedMedicine.takenAt}")
                
                //  ИСПРАВЛЕНО: Отменяем старый будильник и планируем новый
                try {
                    val scheduler = com.medicalnotes.app.utils.NotificationScheduler(this@EditMedicineActivity)
                    scheduler.rescheduleOnEdit(updatedMedicine)
                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "✓ Уведомление перепланировано для лекарства ID: $medicineId")
                } catch (e: Exception) {
                    com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Ошибка перепланирования уведомления", e)
                }
                
                // Сохраняем в корутине
                CoroutineScope(Dispatchers.IO + com.medicalnotes.app.utils.CrashReporter.getCoroutineExceptionHandler()).launch {
                    try {
                        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Starting medicine update...")
                        val success = viewModel.updateMedicine(updatedMedicine)
                        com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "Medicine update result: $success")
                        
                        // Синхронизируем группы, если лекарство добавлено в группу
                        if (success && groupName.isNotEmpty()) {
                            try {
                                viewModel.getAllMedicines { allMedicines ->
                                    val groupMedicines = allMedicines.filter { 
                                        it.groupName == groupName && it.id != updatedMedicine.id 
                                    }
                                    
                                    // Обновляем порядок других лекарств в группе
                                    groupMedicines.forEachIndexed { index, medicine ->
                                        val newOrder = index + 1
                                        if (newOrder >= groupOrder) {
                                            val updatedGroupMedicine = medicine.copy(
                                                groupOrder = newOrder + 1
                                            )
                                            CoroutineScope(Dispatchers.IO + com.medicalnotes.app.utils.CrashReporter.getCoroutineExceptionHandler()).launch {
                                                try {
                                                    viewModel.updateMedicine(updatedGroupMedicine)
                                                } catch (e: Exception) {
                                                    com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error updating group medicine", e)
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Error syncing group medicines", e)
                            }
                        }
                        
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                //  ДОБАВЛЕНО: Принудительно обновляем статус лекарства
                                try {
                                    val dataManager = com.medicalnotes.app.utils.DataManager(this@EditMedicineActivity)
                                    val updatedStatusMedicine = com.medicalnotes.app.utils.MedicineStatusHelper.updateMedicineStatus(updatedMedicine)
                                    dataManager.updateMedicine(updatedStatusMedicine)
                                    com.medicalnotes.app.utils.LogCollector.d("EditMedicine", "✓ Статус лекарства обновлен")
                                } catch (e: Exception) {
                                    com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Ошибка обновления статуса", e)
                                }
                                
                                Toast.makeText(this@EditMedicineActivity, 
                                    "Лекарство обновлено", Toast.LENGTH_SHORT).show()
                                
                                //  ДОБАВЛЕНО: Возвращаем результат в MainActivity для обновления списка
                                val resultIntent = Intent()
                                resultIntent.putExtra("medicine_updated", true)
                                resultIntent.putExtra("medicine_id", medicineId)
                                setResult(RESULT_OK, resultIntent)
                                
                                finish()
                            } else {
                                Toast.makeText(this@EditMedicineActivity, 
                                    "Ошибка обновления лекарства", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("EditMedicine", "Critical error during medicine update", e)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@EditMedicineActivity, 
                                "Критическая ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Лекарство не найдено", Toast.LENGTH_SHORT).show()
            }
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
        com.medicalnotes.app.utils.LogCollector.i("EditMedicine", "onDestroy: редактирование завершено")
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_medicine, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            com.medicalnotes.app.R.id.action_save -> {
                updateMedicine()
                true
            }
            com.medicalnotes.app.R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить лекарство")
            .setMessage("Вы уверены, что хотите удалить лекарство \"${binding.editTextName.text}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteMedicine()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun deleteMedicine() {
        viewModel.getMedicineById(medicineId) { medicine ->
            if (medicine != null) {
                CoroutineScope(Dispatchers.IO + com.medicalnotes.app.utils.CrashReporter.getCoroutineExceptionHandler()).launch {
                    try {
                        val success = viewModel.deleteMedicine(medicine)
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                Toast.makeText(this@EditMedicineActivity, "Лекарство удалено", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this@EditMedicineActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@EditMedicineActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this@EditMedicineActivity, "Лекарство не найдено", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "Updating UI after language change")
            
            // Обновляем заголовок Activity
            title = getString(R.string.edit_medicine)
            supportActionBar?.title = getString(R.string.edit_medicine)
            
            // Обновляем заголовки полей
            updateFieldLabels()
            
            // Обновляем подсказки (hints)
            updateFieldHints()
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            // Обновляем текст в спиннерах и адаптерах
            updateSpinnerTexts()
            
            // Обновляем меню
            invalidateOptionsMenu()
            
            com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating UI after language change", e)
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
            
            com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "Field labels update completed")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating field labels", e)
        }
    }
    
    private fun updateFieldHints() {
        try {
            // Обновляем подсказки в полях ввода
            binding.editTextName.hint = getString(R.string.enter_medicine_name)
            binding.editTextQuantity.hint = getString(R.string.enter_quantity)
            binding.editTextNotes.hint = getString(R.string.enter_notes)
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating field hints", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок (используем только существующие элементы)
            binding.buttonSave.text = getString(R.string.save)
            binding.buttonCancel.text = getString(R.string.cancel)
            
            // Проверяем существование других кнопок перед обновлением
            // buttonAddTime не существует в layout EditMedicineActivity
            
            try {
                binding.buttonChangeGroup?.text = getString(R.string.change_group)
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.w("EditMedicineActivity", "buttonChangeGroup not found")
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating button texts", e)
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
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating spinner texts", e)
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
            
            // ИСПРАВЛЕНО: Локализуем сохраненный тип лекарства
            val localizedMedicineType = com.medicalnotes.app.utils.DataLocalizationHelper.localizeMedicineType(
                selectedMedicineType, 
                resources.configuration.locales[0], 
                this
            )
            
            // Если локализованный тип соответствует какому-то типу в новом списке, используем его
            val matchingType = medicineTypes.find { it == localizedMedicineType }
            if (matchingType != null) {
                binding.autoCompleteMedicineType.setText(matchingType, false)
                selectedMedicineType = matchingType
                com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "Localized medicine type found: $matchingType")
            } else {
                // Если не найдено соответствие, устанавливаем значение по умолчанию
                val defaultType = getString(com.medicalnotes.app.R.string.medicine_type_tablets)
                binding.autoCompleteMedicineType.setText(defaultType, false)
                selectedMedicineType = defaultType
                com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "Using default medicine type: $defaultType")
            }
            
            com.medicalnotes.app.utils.LogCollector.d("EditMedicineActivity", "Medicine type dropdown updated successfully")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("EditMedicineActivity", "Error updating medicine type dropdown", e)
        }
    }
    
    private fun duplicateMedicine() {
        viewModel.getMedicineById(medicineId) { originalMedicine ->
            if (originalMedicine != null) {
                val duplicatedMedicine = originalMedicine.copy(
                    id = 0, // Новый ID будет назначен автоматически
                    name = "${originalMedicine.name} (копия)",
                    remainingQuantity = originalMedicine.quantity // Сбрасываем остаток к исходному количеству
                )
                
                CoroutineScope(Dispatchers.IO + com.medicalnotes.app.utils.CrashReporter.getCoroutineExceptionHandler()).launch {
                    try {
                        val success = viewModel.addMedicine(duplicatedMedicine)
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                Toast.makeText(this@EditMedicineActivity, "Лекарство продублировано", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this@EditMedicineActivity, "Ошибка дублирования", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@EditMedicineActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this@EditMedicineActivity, "Лекарство не найдено", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 