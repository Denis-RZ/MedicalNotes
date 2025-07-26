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
import com.medicalnotes.app.repository.UserPreferencesRepository
import com.medicalnotes.app.utils.DisplayUtils
import com.medicalnotes.app.utils.NotificationManager
import com.medicalnotes.app.viewmodels.MainViewModel

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
        // Это будет реализовано позже
    }
    

    
    private fun showJournalDialog() {
        AlertDialog.Builder(this)
            .setTitle("ЖУРНАЛ ПРИЕМА ЛЕКАРСТВ")
            .setMessage("Функция журнала будет добавлена в следующей версии приложения.")
            .setPositiveButton("ПОНЯТНО", null)
            .show()
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