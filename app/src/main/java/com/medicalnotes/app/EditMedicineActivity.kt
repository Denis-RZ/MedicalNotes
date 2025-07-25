package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
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

class EditMedicineActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddMedicineBinding
    private lateinit var viewModel: AddMedicineViewModel
    private var selectedTime: LocalTime? = null
    private var selectedFrequency = DosageFrequency.DAILY
    private var selectedTimes = mutableListOf<LocalTime>()
    private var medicineId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        medicineId = intent.getLongExtra("medicine_id", -1)
        if (medicineId == -1L) {
            Toast.makeText(this, "Ошибка: лекарство не найдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[AddMedicineViewModel::class.java]
        
        setupViews()
        setupListeners()
        loadMedicine()
    }
    
    private fun setupViews() {
        // Устанавливаем заголовок
        supportActionBar?.title = "Редактировать лекарство"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Изменяем текст кнопки сохранения
        binding.buttonSave.text = "Сохранить изменения"
    }
    
    private fun setupListeners() {
        binding.buttonTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.buttonFrequency.setOnClickListener {
            showFrequencyDialog()
        }
        
        // Показываем/скрываем группировку в зависимости от выбранной частоты
        binding.buttonFrequency.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val isEveryOtherDay = selectedFrequency == DosageFrequency.EVERY_OTHER_DAY
                binding.layoutGrouping.visibility = if (isEveryOtherDay) View.VISIBLE else View.GONE
                
                // Если выбрали "через день", показываем поля группы
                // Диалог выбора группы будет показан при нажатии кнопки "Изменить группу"
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
        
        binding.buttonSave.setOnClickListener {
            updateMedicine()
        }
        
        binding.buttonCancel.setOnClickListener {
            finish()
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
    }
    
    private fun loadMedicine() {
        android.util.Log.d("EditMedicine", "Loading medicine with ID: $medicineId")
        viewModel.getMedicineById(medicineId) { medicine ->
            if (medicine != null) {
                android.util.Log.d("EditMedicine", "Medicine loaded: ${medicine.name}")
                populateFields(medicine)
            } else {
                android.util.Log.e("EditMedicine", "Medicine not found with ID: $medicineId")
                Toast.makeText(this, "Лекарство не найдено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun populateFields(medicine: Medicine) {
        binding.editTextName.setText(medicine.name)
        binding.editTextDosage.setText(medicine.dosage)
        binding.editTextQuantity.setText(medicine.quantity.toString())
        binding.editTextNotes.setText(medicine.notes)
        binding.checkBoxInsulin.isChecked = medicine.isInsulin
        
        selectedTime = medicine.time
        android.util.Log.d("EditMedicine", "Setting selectedTime from medicine: ${medicine.time}")
        Toast.makeText(this, "Загружено время: ${medicine.time}", Toast.LENGTH_SHORT).show()
        selectedFrequency = medicine.frequency
        selectedTimes.clear()
        selectedTimes.addAll(medicine.doseTimes)
        
        binding.switchMultipleDoses.isChecked = medicine.multipleDoses
        
        // Загружаем групповые данные
        binding.editTextGroupName.setText(medicine.groupName)
        binding.editTextGroupOrder.setText(medicine.groupOrder.toString())
        
        // Показываем/скрываем группировку в зависимости от частоты
        val isEveryOtherDay = medicine.frequency == DosageFrequency.EVERY_OTHER_DAY
        binding.layoutGrouping.visibility = if (isEveryOtherDay) View.VISIBLE else View.GONE
        
        updateTimeDisplay()
        updateFrequencyDisplay()
        updateMultipleTimesDisplay()
        
        // Логируем загруженное лекарство для отладки
        android.util.Log.d("EditMedicine", "Loaded medicine: name=${medicine.name}, dosage=${medicine.dosage}, " +
            "quantity=${medicine.quantity}, remainingQuantity=${medicine.remainingQuantity}, " +
            "frequency=${medicine.frequency}, dosageTimes=${medicine.dosageTimes}, " +
            "customDays=${medicine.customDays}, customTimes=${medicine.customTimes}, " +
            "multipleDoses=${medicine.multipleDoses}, doseTimes=${medicine.doseTimes}")
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
    
    private fun showTimePickerForMultiple() {
        android.util.Log.d("EditMedicine", "Opening multiple time picker")
        CustomTimePickerDialog(this, LocalTime.of(8, 0)) { time ->
            android.util.Log.d("EditMedicine", "Multiple time selected: $time")
            if (!selectedTimes.contains(time)) {
                selectedTimes.add(time)
                selectedTimes.sort()
                updateMultipleTimesDisplay()
                android.util.Log.d("EditMedicine", "Updated selectedTimes: $selectedTimes")
            }
        }.show()
    }
    
    private fun updateMultipleTimesDisplay() {
        if (selectedTimes.isNotEmpty()) {
            val timesText = selectedTimes.joinToString(", ") { 
                it.format(DateTimeFormatter.ofPattern("HH:mm")) 
            }
            binding.buttonAddTime.text = "Добавить время ($timesText)"
        }
    }
    
    private fun updateMedicine() {
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
        val currentTime = selectedTime ?: LocalTime.of(8, 0)
        val doseTimes = if (binding.switchMultipleDoses.isChecked && selectedTimes.isNotEmpty()) {
            selectedTimes
        } else {
            listOf(currentTime)
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
            listOf(1, 2, 3, 4, 5, 6, 7) // Все дни недели по умолчанию
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
        
        // Обновляем лекарство
        viewModel.getMedicineById(medicineId) { originalMedicine ->
            if (originalMedicine != null) {
                val saveTime = selectedTime ?: LocalTime.of(8, 0)
                android.util.Log.d("EditMedicine", "Saving medicine with selectedTime: $saveTime")
                Toast.makeText(this, "Сохраняем время: $saveTime", Toast.LENGTH_SHORT).show()
                val updatedMedicine = originalMedicine.copy(
                    name = name,
                    dosage = dosage,
                    quantity = quantity,
                    // Сохраняем remainingQuantity из оригинального лекарства
                    remainingQuantity = originalMedicine.remainingQuantity,
                    time = saveTime,
                    notes = notes,
                    isInsulin = isInsulin,
                    frequency = selectedFrequency,
                    dosageTimes = dosageTimes,
                    customDays = customDays,
                    customTimes = customTimes,
                    // Сохраняем startDate из оригинального лекарства
                    startDate = originalMedicine.startDate,
                    multipleDoses = binding.switchMultipleDoses.isChecked,
                    dosesPerDay = doseTimes.size,
                    doseTimes = doseTimes,
                    groupId = groupId,
                    groupName = groupName,
                    groupOrder = groupOrder,
                    // СБРАСЫВАЕМ СТАТУС ПРИНЯТИЯ ПОСЛЕ РЕДАКТИРОВАНИЯ
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
                android.util.Log.d("EditMedicine", "СБРОШЕН СТАТУС ПРИНЯТИЯ:")
                android.util.Log.d("EditMedicine", "  - lastTakenTime: ${updatedMedicine.lastTakenTime}")
                android.util.Log.d("EditMedicine", "  - takenToday: ${updatedMedicine.takenToday}")
                android.util.Log.d("EditMedicine", "  - takenAt: ${updatedMedicine.takenAt}")
                
                // Сохраняем в корутине
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = viewModel.updateMedicine(updatedMedicine)
                        
                        // Переключаемся на главный поток для UI операций
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                // Планируем уведомление для обновленного лекарства
                                val intent = Intent(this@EditMedicineActivity, NotificationService::class.java).apply {
                                    action = "SCHEDULE_MEDICINE"
                                    putExtra("medicine_id", medicineId)
                                }
                                startService(intent)
                                
                                Toast.makeText(this@EditMedicineActivity, "Лекарство обновлено", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this@EditMedicineActivity, "Ошибка обновления лекарства", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        // Переключаемся на главный поток для показа ошибки
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@EditMedicineActivity, "Ошибка обновления: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Лекарство не найдено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 