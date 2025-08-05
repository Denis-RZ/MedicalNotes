package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.medicalnotes.app.adapters.MedicineGridAdapter
import com.medicalnotes.app.databinding.ActivityMedicineManagerBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatusHelper
import com.medicalnotes.app.viewmodels.MainViewModel

class MedicineManagerActivity : BaseActivity() {
    
    private lateinit var binding: ActivityMedicineManagerBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: MedicineGridAdapter
    
    private var currentFilter = MedicineFilter.ALL
    
    enum class MedicineFilter {
        ALL, ACTIVE, LOW_STOCK
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]
        
        setupViews()
        setupListeners()
        loadMedicines()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.medicine_manager_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настраиваем RecyclerView
        adapter = MedicineGridAdapter(
            onMedicineClick = { medicine -> editMedicine(medicine) },
            onDeleteClick = { medicine -> showDeleteDialog(medicine) },
            onEditClick = { medicine -> editMedicine(medicine) },
            onToggleClick = { medicine -> toggleMedicine(medicine) },
            onTakenClick = { medicine -> markMedicineAsTaken(medicine) }
        )
        
        binding.recyclerViewMedicines.apply {
            layoutManager = GridLayoutManager(this@MedicineManagerActivity, 2)
            adapter = this@MedicineManagerActivity.adapter
        }
        
        // Устанавливаем активный фильтр
        updateFilterButtons()
    }
    
    private fun setupListeners() {
        binding.buttonAddMedicine.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }
        
        binding.buttonFilterAll.setOnClickListener {
            setFilter(MedicineFilter.ALL)
        }
        
        binding.buttonFilterActive.setOnClickListener {
            setFilter(MedicineFilter.ACTIVE)
        }
        
        binding.buttonFilterLow.setOnClickListener {
            setFilter(MedicineFilter.LOW_STOCK)
        }
    }
    
    private fun setFilter(filter: MedicineFilter) {
        currentFilter = filter
        updateFilterButtons()
        loadMedicines()
    }
    
    private fun updateFilterButtons() {
        binding.buttonFilterAll.isSelected = currentFilter == MedicineFilter.ALL
        binding.buttonFilterActive.isSelected = currentFilter == MedicineFilter.ACTIVE
        binding.buttonFilterLow.isSelected = currentFilter == MedicineFilter.LOW_STOCK
    }
    
    private fun loadMedicines() {
        viewModel.loadAllMedicines()
        viewModel.allMedicines.observe(this) { medicines ->
            // Обновляем статус всех лекарств
            val updatedMedicines = medicines.map { MedicineStatusHelper.updateMedicineStatus(it) }
            
            val filteredMedicines = when (currentFilter) {
                MedicineFilter.ALL -> updatedMedicines
                MedicineFilter.ACTIVE -> updatedMedicines.filter { MedicineStatusHelper.isActiveForToday(it) }
                MedicineFilter.LOW_STOCK -> updatedMedicines.filter { it.remainingQuantity <= 5 && it.isActive }
            }
            
            android.util.Log.d("MedicineManager", "Filter: $currentFilter, Total medicines: ${updatedMedicines.size}, Filtered: ${filteredMedicines.size}")
            updatedMedicines.forEach { medicine ->
                val status = MedicineStatusHelper.getMedicineStatus(medicine)
                android.util.Log.d("MedicineManager", "Medicine: ${medicine.name}, Status: $status, Active: ${medicine.isActive}, Frequency: ${medicine.frequency}")
            }
            
            adapter.submitList(filteredMedicines)
            
            // Показываем/скрываем пустое состояние
            binding.layoutEmpty.visibility = if (filteredMedicines.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun editMedicine(medicine: Medicine) {
        android.util.Log.d("MedicineManager", "Edit medicine clicked: ${medicine.name}, id: ${medicine.id}, id type: ${medicine.id::class.java.simpleName}")
        
        // Проверяем, что ID не равен 0 или отрицательному значению
        if (medicine.id <= 0) {
            android.util.Log.e("MedicineManager", "Invalid medicine ID: ${medicine.id}")
            Toast.makeText(this, "Ошибка: неверный ID лекарства", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, EditMedicineActivity::class.java).apply {
            putExtra("medicine_id", medicine.id)
        }
        
        android.util.Log.d("MedicineManager", "Starting EditMedicineActivity with intent extras: ${intent.extras}")
        
        try {
            startActivity(intent)
            android.util.Log.d("MedicineManager", "EditMedicineActivity started successfully")
        } catch (e: Exception) {
            android.util.Log.e("MedicineManager", "Error starting EditMedicineActivity", e)
            Toast.makeText(this, "Ошибка открытия редактирования: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeleteDialog(medicine: Medicine) {
        AlertDialog.Builder(this)
            .setTitle("Удалить лекарство")
            .setMessage("Вы уверены, что хотите удалить \"${medicine.name}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteMedicine(medicine)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun deleteMedicine(medicine: Medicine) {
        viewModel.deleteMedicine(medicine.id)
        Toast.makeText(this, "Лекарство удалено", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleMedicine(medicine: Medicine) {
        val updatedMedicine = medicine.copy(isActive = !medicine.isActive)
        viewModel.updateMedicine(updatedMedicine)
        
        val status = if (updatedMedicine.isActive) "включено" else "отключено"
        Toast.makeText(this, "Лекарство $status", Toast.LENGTH_SHORT).show()
    }
    
    private fun markMedicineAsTaken(medicine: Medicine) {
        val updatedMedicine = MedicineStatusHelper.markAsTaken(medicine)
        viewModel.updateMedicine(updatedMedicine)
        
        Toast.makeText(this, "Лекарство \"${medicine.name}\" отмечено как принятое", Toast.LENGTH_SHORT).show()
        
        // Перезагружаем список, чтобы обновить отображение
        loadMedicines()
    }
    
    override fun onResume() {
        super.onResume()
        loadMedicines()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 