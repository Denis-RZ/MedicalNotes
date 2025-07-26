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
        // Устанавливаем заголовок
        supportActionBar?.title = "Добавить лекарство"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Устанавливаем время по умолчанию
        updateTimeDisplay()
        updateFrequencyDisplay()
    }
    
    private fun setupListeners() {
        binding.buttonTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.buttonFrequency.setOnClickListener {
            showFrequencyDialog()
        }
        
        binding.buttonMedicineType.setOnClickListener {
            showMedicineTypeDialog()
        }
        
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
        
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.buttonCustomize.setOnClickListener {
            startActivity(Intent(this, ButtonCustomizationActivity::class.java))
        }
        
        binding.buttonManageGroups.setOnClickListener {
            startActivity(Intent(this, GroupManagementActivity::class.java))
        }
        
        binding.buttonTimeGroup.setOnClickListener {
            showTimeGroupDialog()
        }
        
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
    
    private fun showMedicineTypeDialog() {
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
        val currentIndex = medicineTypes.indexOf(selectedMedicineType).coerceAtLeast(0)
        
        AlertDialog.Builder(this)
            .setTitle("Выберите тип лекарства")
            .setSingleChoiceItems(medicineTypes.toTypedArray(), currentIndex) { _, which ->
                selectedMedicineType = medicineTypes[which]
                binding.buttonMedicineType.text = selectedMedicineType
                
                // Автоматически отмечаем чекбокс инсулина для соответствующих типов
                binding.checkBoxInsulin.isChecked = selectedMedicineType == "Инсулин" || 
                                                   selectedMedicineType == "Оземпик" || 
                                                   selectedMedicineType == "Мунджаро"
            }
            .setPositiveButton("OK", null)
            .setNegativeButton("Отмена", null)
            .show()
    }
    
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
        // Пока что просто показываем сообщение
        Toast.makeText(this, "Функция выбора связанных лекарств будет добавлена позже", Toast.LENGTH_LONG).show()
    }
    
    private fun updateSelectedMedicinesDisplay() {
        // Пока что скрываем
        binding.textSelectedMedicines.visibility = View.GONE
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
        // Здесь можно добавить адаптер для RecyclerView
        // Пока просто обновляем текст кнопки
        if (selectedTimes.isNotEmpty()) {
            val timesText = selectedTimes.joinToString(", ") { 
                it.format(DateTimeFormatter.ofPattern("HH:mm")) 
            }
            binding.buttonAddTime.text = "Добавить время ($timesText)"
        }
    }
    
    private fun saveMedicine() {
        val name = binding.editTextName.text.toString().trim()
        val dosage = binding.editTextDosage.text.toString().trim()
        val quantityText = binding.editTextQuantity.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()
        val isInsulin = binding.checkBoxInsulin.isChecked
        
        // Валидация
        if (name.isEmpty()) {
            binding.editTextName.error = "Введите название лекарства"
            return
        }
        
        if (dosage.isEmpty()) {
            binding.editTextDosage.error = "Введите дозировку"
            return
        }
        
        if (quantityText.isEmpty()) {
            binding.editTextQuantity.error = "Введите количество"
            return
        }
        
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            binding.editTextQuantity.error = "Введите корректное количество"
            return
        }
        
        // Определяем времена приема
        val doseTimes = if (binding.switchMultipleDoses.isChecked && selectedTimes.isNotEmpty()) {
            selectedTimes
        } else {
            listOf(selectedTime)
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
        
        // Определяем customDays для пользовательской схемы
        val customDays = if (selectedFrequency == DosageFrequency.CUSTOM) {
            if (selectedDays.isNotEmpty()) {
                selectedDays.sorted().toList()
            } else {
                listOf(1, 2, 3, 4, 5, 6, 7) // Все дни недели по умолчанию
            }
        } else {
            emptyList()
        }
        
        // Определяем customTimes для пользовательской схемы
        val customTimes = if (selectedFrequency == DosageFrequency.CUSTOM) {
            doseTimes
        } else {
            emptyList()
        }
        

        
        // Получаем данные группы
        val groupName = binding.editTextGroupName.text.toString().trim()
        val groupOrderText = binding.editTextGroupOrder.text.toString().trim()
        val groupOrder = groupOrderText.toIntOrNull() ?: 1
        
        // Создаем ID группы, если указано название
        val groupId = if (groupName.isNotEmpty()) {
            System.currentTimeMillis() // Простой способ генерации ID
        } else {
            null
        }
        
        // Создаем лекарство
        val medicine = Medicine(
            name = name,
            dosage = dosage,
            quantity = quantity,
            remainingQuantity = quantity,
            medicineType = selectedMedicineType,
            time = selectedTime,
            notes = notes,
            isInsulin = isInsulin,
            frequency = selectedFrequency,
            dosageTimes = dosageTimes,
            customDays = customDays,
            customTimes = customTimes,
            startDate = System.currentTimeMillis(),
            multipleDoses = binding.switchMultipleDoses.isChecked,
            dosesPerDay = doseTimes.size,
            doseTimes = doseTimes,
            groupId = groupId,
            groupName = groupName,
            groupOrder = groupOrder,
            relatedMedicineIds = selectedRelatedMedicines.map { it.id },
            isPartOfGroup = selectedRelatedMedicines.isNotEmpty()
        )
        
        // Логируем созданное лекарство для отладки
        android.util.Log.d("AddMedicine", "Creating medicine: name=${medicine.name}, dosage=${medicine.dosage}, " +
            "quantity=${medicine.quantity}, frequency=${medicine.frequency}, " +
            "dosageTimes=${medicine.dosageTimes}, customDays=${medicine.customDays}, " +
            "customTimes=${medicine.customTimes}, multipleDoses=${medicine.multipleDoses}, " +
            "doseTimes=${medicine.doseTimes}")
        
        // Сохраняем в корутине
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicineId = viewModel.insertMedicine(medicine)
                
                // Переключаемся на главный поток для UI операций
                CoroutineScope(Dispatchers.Main).launch {
                    if (medicineId > 0) {
                        // Планируем уведомление для нового лекарства
                        val intent = Intent(this@AddMedicineActivity, NotificationService::class.java).apply {
                            action = "SCHEDULE_MEDICINE"
                            putExtra("medicine_id", medicineId)
                        }
                        startService(intent)
                        
                        Toast.makeText(this@AddMedicineActivity, "Лекарство добавлено", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMedicineActivity, "Ошибка сохранения лекарства", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Переключаемся на главный поток для показа ошибки
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@AddMedicineActivity, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 