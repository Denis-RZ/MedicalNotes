package com.medicalnotes.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.MedicineAdapter
import com.medicalnotes.app.databinding.ActivityNotificationManagerBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.service.NotificationService
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NotificationManagerActivity : BaseActivity() {
    
    private lateinit var binding: ActivityNotificationManagerBinding
    private lateinit var dataManager: DataManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var medicineAdapter: MedicineAdapter
    
    private var allMedicines = listOf<Medicine>()
    private var filteredMedicines = listOf<Medicine>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        notificationManager = NotificationManager(this)
        
        setupViews()
        setupRecyclerView()
        setupListeners()
        loadMedicines()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.notification_manager_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupRecyclerView() {
        medicineAdapter = MedicineAdapter(
            onMedicineClick = { medicine ->
                showMedicineEditDialog(medicine)
            },
            onSkipClick = { medicine ->
                // В менеджере уведомлений пропуск не нужен, поэтому просто показываем диалог
                showMedicineEditDialog(medicine)
            },
            onEditClick = { medicine ->
                showMedicineEditDialog(medicine)
            },
            onDeleteClick = { medicine ->
                // В менеджере уведомлений удаление не нужно
                showMedicineEditDialog(medicine)
            }
        )
        
        binding.recyclerViewMedicines.apply {
            layoutManager = LinearLayoutManager(this@NotificationManagerActivity)
            adapter = medicineAdapter
        }
    }
    
    private fun setupListeners() {
        // Поиск
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterMedicines(s.toString())
            }
        })
        
        // Фильтры
        binding.buttonFilterAll.setOnClickListener {
            binding.buttonFilterAll.isSelected = true
            binding.buttonFilterActive.isSelected = false
            binding.buttonFilterInactive.isSelected = false
            filterMedicines(binding.editTextSearch.text.toString())
        }
        
        binding.buttonFilterActive.setOnClickListener {
            binding.buttonFilterAll.isSelected = false
            binding.buttonFilterActive.isSelected = true
            binding.buttonFilterInactive.isSelected = false
            filterMedicines(binding.editTextSearch.text.toString())
        }
        
        binding.buttonFilterInactive.setOnClickListener {
            binding.buttonFilterAll.isSelected = false
            binding.buttonFilterActive.isSelected = false
            binding.buttonFilterInactive.isSelected = true
            filterMedicines(binding.editTextSearch.text.toString())
        }
        
        // Действия
        binding.buttonTestNotification.setOnClickListener {
            showTestNotificationDialog()
        }
        
        binding.buttonCancelAllNotifications.setOnClickListener {
            showCancelAllNotificationsDialog()
        }
        
        binding.buttonRefresh.setOnClickListener {
            loadMedicines()
        }
        
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadMedicines() {
        CoroutineScope(Dispatchers.Main).launch {
            allMedicines = dataManager.loadMedicines()
            filterMedicines(binding.editTextSearch.text.toString())
            updateStatistics()
        }
    }
    
    private fun filterMedicines(searchQuery: String) {
        var filtered = allMedicines
        
        // Применяем фильтр по статусу
        when {
            binding.buttonFilterActive.isSelected -> {
                filtered = filtered.filter { it.isActive }
            }
            binding.buttonFilterInactive.isSelected -> {
                filtered = filtered.filter { !it.isActive }
            }
        }
        
        // Применяем поиск
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { medicine ->
                medicine.name.contains(searchQuery, ignoreCase = true) ||
                medicine.dosage.contains(searchQuery, ignoreCase = true) ||
                medicine.notes.contains(searchQuery, ignoreCase = true)
            }
        }
        
        filteredMedicines = filtered
        medicineAdapter.submitList(filteredMedicines)
        
        // Обновляем текст "не найдено"
        binding.textNoMedicines.visibility = 
            if (filteredMedicines.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
    
    private fun updateStatistics() {
        val activeCount = allMedicines.count { it.isActive }
        val totalCount = allMedicines.size
        val lowStockCount = allMedicines.count { it.isActive && it.remainingQuantity <= 5 }
        
        binding.textStatistics.text = "Всего: $totalCount | Активных: $activeCount | Низкий запас: $lowStockCount"
    }
    
    private fun showMedicineEditDialog(medicine: Medicine) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_medicine_notification, null)
        
        // Находим элементы в диалоге
        val editTextName = dialogView.findViewById<android.widget.EditText>(R.id.editTextName)
        val editTextDosage = dialogView.findViewById<android.widget.EditText>(R.id.editTextDosage)
        val buttonTime = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonTime)
        val switchActive = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchActive)
        val editTextQuantity = dialogView.findViewById<android.widget.EditText>(R.id.editTextQuantity)
        val editTextNotes = dialogView.findViewById<android.widget.EditText>(R.id.editTextNotes)
        
        // Заполняем данными
        editTextName.setText(medicine.name)
        editTextDosage.setText(medicine.dosage)
        buttonTime.text = medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        switchActive.isChecked = medicine.isActive
        editTextQuantity.setText(medicine.remainingQuantity.toString())
        editTextNotes.setText(medicine.notes)
        
        var selectedTime = medicine.time
        
        // Обработчик выбора времени
        buttonTime.setOnClickListener {
            val timePickerDialog = android.app.TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedTime = LocalTime.of(hourOfDay, minute)
                    buttonTime.text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                },
                selectedTime.hour,
                selectedTime.minute,
                true
            )
            timePickerDialog.show()
        }
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать лекарство")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedMedicine = medicine.copy(
                    name = editTextName.text.toString(),
                    dosage = editTextDosage.text.toString(),
                    time = selectedTime,
                    isActive = switchActive.isChecked,
                    remainingQuantity = editTextQuantity.text.toString().toIntOrNull() ?: medicine.remainingQuantity,
                    notes = editTextNotes.text.toString(),
                    updatedAt = System.currentTimeMillis()
                )
                
                CoroutineScope(Dispatchers.Main).launch {
                    val success = dataManager.updateMedicine(updatedMedicine)
                    if (success) {
                        // Обновляем уведомления
                        val intent = Intent(this@NotificationManagerActivity, NotificationService::class.java).apply {
                            action = if (updatedMedicine.isActive) "SCHEDULE_MEDICINE" else "CANCEL_MEDICINE"
                            putExtra("medicine_id", updatedMedicine.id)
                        }
                        startService(intent)
                        
                        Toast.makeText(this@NotificationManagerActivity, "Лекарство обновлено", Toast.LENGTH_SHORT).show()
                        loadMedicines()
                    } else {
                        Toast.makeText(this@NotificationManagerActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить") { _, _ ->
                showDeleteMedicineDialog(medicine)
            }
            .show()
    }
    
    private fun showDeleteMedicineDialog(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Удалить лекарство")
            .setMessage("Удалить '${medicine.name}'? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    val success = dataManager.deleteMedicine(medicine.id)
                    if (success) {
                        // Отменяем уведомления
                        val intent = Intent(this@NotificationManagerActivity, NotificationService::class.java).apply {
                            action = "CANCEL_MEDICINE"
                            putExtra("medicine_id", medicine.id)
                        }
                        startService(intent)
                        
                        Toast.makeText(this@NotificationManagerActivity, "Лекарство удалено", Toast.LENGTH_SHORT).show()
                        loadMedicines()
                    } else {
                        Toast.makeText(this@NotificationManagerActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showTestNotificationDialog() {
        val medicines = allMedicines.filter { it.isActive }
        if (medicines.isEmpty()) {
            Toast.makeText(this, "Нет активных лекарств для тестирования", Toast.LENGTH_SHORT).show()
            return
        }
        
        val medicineNames = medicines.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Тест уведомления")
            .setItems(medicineNames) { _, which ->
                val selectedMedicine = medicines[which]
                notificationManager.showMedicineReminder(selectedMedicine)
                Toast.makeText(this, "Тестовое уведомление отправлено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showCancelAllNotificationsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Отменить все уведомления")
            .setMessage("Отменить все активные уведомления?")
            .setPositiveButton("Отменить все") { _, _ ->
                notificationManager.cancelAllNotifications()
                Toast.makeText(this, "Все уведомления отменены", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 