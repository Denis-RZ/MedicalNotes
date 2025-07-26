package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.medicalnotes.app.databinding.ActivityEditMedicineBinding
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

class EditMedicineActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditMedicineBinding
    private lateinit var viewModel: AddMedicineViewModel
    private var medicineId: Long = 0
    
    // Переменные для хранения данных
    private var selectedTime: LocalTime? = null
    private var selectedFrequency: DosageFrequency = DosageFrequency.DAILY
    private var selectedMedicineType: String = "Таблетки"
    private val selectedDays = mutableSetOf<Int>()
    private var allGroups = mutableListOf<String>() // Добавляем список всех групп
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[AddMedicineViewModel::class.java]
        
        medicineId = intent.getLongExtra("medicine_id", 0L)
        android.util.Log.d("EditMedicine", "onCreate: medicineId = $medicineId")
        
        if (medicineId == 0L) {
            android.util.Log.e("EditMedicine", "Invalid medicine ID: $medicineId")
            Toast.makeText(this, "Ошибка: не указан ID лекарства", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            setupToolbar()
            setupListeners()
            
            // Добавляем небольшую задержку для инициализации DataManager
            binding.root.post {
                loadGroups() // Загружаем группы при инициализации
                loadMedicine()
            }
        } catch (e: Exception) {
            android.util.Log.e("EditMedicine", "Error in onCreate", e)
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Редактировать лекарство"
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupListeners() {
        android.util.Log.d("EditMedicine", "Setting up listeners...")
        
        // Настройка AutoCompleteTextView для типа лекарства
        setupMedicineTypeField()
        
        // Кнопка выбора времени
        binding.buttonTime.setOnClickListener {
            android.util.Log.d("EditMedicine", "Time button clicked")
            showTimePicker()
        }
        
        binding.autoCompleteMedicineType.setOnClickListener {
            android.util.Log.d("EditMedicine", "Medicine type field clicked")
            showMedicineTypeDialog()
        }
        
        // Альтернативный способ обработки клика для AutoCompleteTextView
        binding.autoCompleteMedicineType.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                android.util.Log.d("EditMedicine", "Medicine type field touched")
                showMedicineTypeDialog()
                return@setOnTouchListener true
            }
            false
        }
        
        binding.buttonWeekDays.setOnClickListener {
            android.util.Log.d("EditMedicine", "Week days button clicked")
            showWeekDaysDialog()
        }
        
        binding.buttonChangeGroup.setOnClickListener {
            android.util.Log.d("EditMedicine", "Change group button clicked")
            showGroupSelectionDialog()
        }
        
        binding.buttonSave.setOnClickListener {
            android.util.Log.d("EditMedicine", "Save button clicked")
            updateMedicine()
        }
        
        binding.buttonCancel.setOnClickListener {
            android.util.Log.d("EditMedicine", "Cancel button clicked")
            finish()
        }
        
        android.util.Log.d("EditMedicine", "All listeners set up successfully")
    }
    
    private fun setupMedicineTypeField() {
        val medicineTypes = arrayOf(
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
        
        // Отключаем автоматическое отображение dropdown
        binding.autoCompleteMedicineType.threshold = Int.MAX_VALUE
        
        android.util.Log.d("EditMedicine", "Medicine type field setup completed")
    }
    
    private fun loadGroups() {
        try {
            android.util.Log.d("EditMedicine", "Loading groups...")
            viewModel.getAllMedicines { medicines ->
                android.util.Log.d("EditMedicine", "getAllMedicines callback received, medicines count: ${medicines.size}")
                val existingGroups = mutableSetOf<String>()
                medicines.forEach { medicine ->
                    if (medicine.groupName.isNotEmpty()) {
                        existingGroups.add(medicine.groupName)
                    }
                }
                allGroups.clear()
                allGroups.add("Без группы") // Заменяем пустую строку на понятное описание
                allGroups.addAll(existingGroups.sorted())
                android.util.Log.d("EditMedicine", "Groups loaded: $allGroups")
            }
        } catch (e: Exception) {
            android.util.Log.e("EditMedicine", "Error in loadGroups", e)
            // Не завершаем активность, просто логируем ошибку
        }
    }
    
    private fun loadMedicine() {
        android.util.Log.d("EditMedicine", "Loading medicine with ID: $medicineId")
        try {
            viewModel.getMedicineById(medicineId) { medicine ->
                android.util.Log.d("EditMedicine", "getMedicineById callback received, medicine: ${medicine?.name ?: "null"}")
                if (medicine != null) {
                    android.util.Log.d("EditMedicine", "Medicine loaded successfully: ${medicine.name}")
                    populateFields(medicine)
                } else {
                    android.util.Log.e("EditMedicine", "Medicine not found with ID: $medicineId")
                    
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
            android.util.Log.e("EditMedicine", "Error in loadMedicine", e)
            Toast.makeText(this, "Ошибка загрузки лекарства: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun populateFields(medicine: Medicine) {
        try {
            android.util.Log.d("EditMedicine", "Populating fields for medicine: ${medicine.name}")
            
            binding.editTextName.setText(medicine.name)
            binding.editTextDosage.setText(medicine.dosage)
            binding.editTextQuantity.setText(medicine.remainingQuantity.toString())
            binding.editTextNotes.setText(medicine.notes)
            binding.checkBoxInsulin.isChecked = medicine.isInsulin
            
            selectedTime = medicine.time
            android.util.Log.d("EditMedicine", "Setting selectedTime from medicine: ${medicine.time}")
            selectedFrequency = medicine.frequency
            selectedMedicineType = medicine.medicineType
            android.util.Log.d("EditMedicine", "Setting selectedMedicineType from medicine: ${medicine.medicineType}")
            selectedDays.clear()
            selectedDays.addAll(medicine.customDays)
            
            // Загружаем групповые данные
            binding.editTextGroupName.setText(medicine.groupName)
            binding.editTextGroupOrder.setText(medicine.groupOrder.toString())
            
            // Показываем/скрываем группировку и дни недели в зависимости от частоты
            val isEveryOtherDay = medicine.frequency == DosageFrequency.EVERY_OTHER_DAY
            val isCustom = medicine.frequency == DosageFrequency.CUSTOM
            binding.layoutGrouping.visibility = if (isEveryOtherDay) View.VISIBLE else View.GONE
            binding.layoutWeekDays.visibility = if (isCustom) View.VISIBLE else View.GONE
            
            // Обновляем отображение времени, типа лекарства и дней недели
            updateTimeDisplay()
            binding.autoCompleteMedicineType.setText(selectedMedicineType)
            updateWeekDaysDisplay()
            
            // Логируем загруженное лекарство для отладки
            android.util.Log.d("EditMedicine", "Fields populated successfully: name=${medicine.name}, dosage=${medicine.dosage}, " +
                "remainingQuantity=${medicine.remainingQuantity}, " +
                "frequency=${medicine.frequency}, groupName=${medicine.groupName}, groupOrder=${medicine.groupOrder}")
                
        } catch (e: Exception) {
            android.util.Log.e("EditMedicine", "Error in populateFields", e)
            Toast.makeText(this, "Ошибка заполнения полей: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showTimePicker() {
        val currentTime = selectedTime ?: LocalTime.of(8, 0)
        android.util.Log.d("EditMedicine", "Opening time picker with current time: $currentTime")
        CustomTimePickerDialog(this, currentTime) { time ->
            android.util.Log.d("EditMedicine", "Time selected: $time")
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
        android.util.Log.d("EditMedicine", "Updated time display: $timeText (selectedTime: $selectedTime)")
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
    
    private fun showMedicineTypeDialog() {
        android.util.Log.d("EditMedicine", "showMedicineTypeDialog called")
        android.util.Log.d("EditMedicine", "Current selectedMedicineType: $selectedMedicineType")
        
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
        android.util.Log.d("EditMedicine", "Current index in medicineTypes: $currentIndex")
        
        try {
            AlertDialog.Builder(this)
                .setTitle("Выберите тип лекарства")
                .setSingleChoiceItems(medicineTypes.toTypedArray(), currentIndex) { _, which ->
                    android.util.Log.d("EditMedicine", "Medicine type selected: ${medicineTypes[which]}")
                    selectedMedicineType = medicineTypes[which]
                    binding.autoCompleteMedicineType.setText(selectedMedicineType)
                    
                    // Автоматически отмечаем чекбокс инсулина для соответствующих типов
                    binding.checkBoxInsulin.isChecked = selectedMedicineType == "Инсулин" || 
                                                       selectedMedicineType == "Оземпик" || 
                                                       selectedMedicineType == "Мунджаро"
                    android.util.Log.d("EditMedicine", "CheckBox insulin set to: ${binding.checkBoxInsulin.isChecked}")
                }
                .setPositiveButton("OK", null)
                .setNegativeButton("Отмена", null)
                .show()
            
            android.util.Log.d("EditMedicine", "Medicine type dialog shown successfully")
        } catch (e: Exception) {
            android.util.Log.e("EditMedicine", "Error showing medicine type dialog", e)
            Toast.makeText(this, "Ошибка открытия диалога типа лекарства: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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

        // Создаем ID группы, если указано название
        val groupId = if (groupName.isNotEmpty()) {
            System.currentTimeMillis() // Простой способ генерации ID
        } else {
            null
        }

        viewModel.getMedicineById(medicineId) { originalMedicine ->
            if (originalMedicine != null) {
                val saveTime = selectedTime ?: LocalTime.of(8, 0)
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
                    startDate = originalMedicine.startDate,
                    multipleDoses = false,
                    dosesPerDay = 1,
                    doseTimes = listOf(saveTime),
                    groupId = groupId,
                    groupName = groupName,
                    groupOrder = groupOrder,
                    lastTakenTime = 0,
                    takenToday = false,
                    takenAt = 0,
                    isMissed = false,
                    missedCount = 0,
                    updatedAt = System.currentTimeMillis()
                )
                
                // Логируем обновленное лекарство для отладки
                android.util.Log.d("EditMedicine", "=== ОБНОВЛЕНИЕ ЛЕКАРСТВА ===")
                android.util.Log.d("EditMedicine", "Название: ${updatedMedicine.name}")
                android.util.Log.d("EditMedicine", "Время: ${updatedMedicine.time}")
                android.util.Log.d("EditMedicine", "Частота: ${updatedMedicine.frequency}")
                android.util.Log.d("EditMedicine", "Количество: ${updatedMedicine.remainingQuantity}")
                android.util.Log.d("EditMedicine", "СБРОШЕН СТАТУС ПРИНЯТИЯ:")
                android.util.Log.d("EditMedicine", "  - lastTakenTime: ${updatedMedicine.lastTakenTime}")
                android.util.Log.d("EditMedicine", "  - takenToday: ${updatedMedicine.takenToday}")
                android.util.Log.d("EditMedicine", "  - takenAt: ${updatedMedicine.takenAt}")
                
                // Сохраняем в корутине
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = viewModel.updateMedicine(updatedMedicine)
                        
                        // Синхронизируем группы, если лекарство добавлено в группу
                        if (success && groupName.isNotEmpty()) {
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
                                        CoroutineScope(Dispatchers.IO).launch {
                                            viewModel.updateMedicine(updatedGroupMedicine)
                                        }
                                    }
                                }
                            }
                        }
                        
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                Toast.makeText(this@EditMedicineActivity, 
                                    "Лекарство обновлено", Toast.LENGTH_SHORT).show()
                                
                                finish()
                            } else {
                                Toast.makeText(this@EditMedicineActivity, 
                                    "Ошибка обновления", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@EditMedicineActivity, 
                                "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_medicine, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_duplicate -> {
                duplicateMedicine()
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
                CoroutineScope(Dispatchers.IO).launch {
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
    
    private fun duplicateMedicine() {
        viewModel.getMedicineById(medicineId) { originalMedicine ->
            if (originalMedicine != null) {
                val duplicatedMedicine = originalMedicine.copy(
                    id = 0, // Новый ID будет назначен автоматически
                    name = "${originalMedicine.name} (копия)",
                    remainingQuantity = originalMedicine.quantity // Сбрасываем остаток к исходному количеству
                )
                
                CoroutineScope(Dispatchers.IO).launch {
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