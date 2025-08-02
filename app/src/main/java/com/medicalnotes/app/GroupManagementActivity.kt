package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.GroupAdapter
import com.medicalnotes.app.databinding.ActivityGroupManagementBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DataManager
import com.medicalnotes.app.utils.MedicineGroupingUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGroupManagementBinding
    private lateinit var dataManager: DataManager
    private lateinit var groupAdapter: GroupAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataManager = DataManager(this)
        
        setupViews()
        loadGroups()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Управление группами"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настройка RecyclerView
        groupAdapter = GroupAdapter(
            onGroupClick = { groupName ->
                showGroupDetails(groupName)
            },
            onAddToGroup = { groupName ->
                showAddToGroupDialog(groupName)
            }
        )
        
        binding.recyclerViewGroups.apply {
            layoutManager = LinearLayoutManager(this@GroupManagementActivity)
            adapter = groupAdapter
        }
        
        // Кнопка "Создать группу"
        binding.buttonCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
        
        // Кнопка "Автогруппировка"
        binding.buttonAutoGroup.setOnClickListener {
            performAutoGrouping()
        }
        
        // Кнопка "Очистить группы"
        binding.buttonClearGroups.setOnClickListener {
            showClearGroupsDialog()
        }
    }
    
    private fun loadGroups() {
        CoroutineScope(Dispatchers.IO).launch {
            val medicines = dataManager.loadMedicines()
            
            withContext(Dispatchers.Main) {
                groupAdapter.updateGroups(medicines)
                updateEmptyState(medicines.none { it.groupName.isNotEmpty() })
                updateStatistics(medicines)
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewGroups.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun updateStatistics(medicines: List<Medicine>) {
        val statistics = MedicineGroupingUtil.getGroupStatistics(medicines)
        
        binding.textTotalGroups.text = statistics.totalGroups.toString()
        binding.textTotalMedicines.text = statistics.totalMedicinesInGroups.toString()
        binding.textAveragePerGroup.text = "%.1f".format(statistics.averageMedicinesPerGroup)
    }
    
    private fun showGroupDetails(groupName: String) {
        val medicines = dataManager.loadMedicines().filter { it.groupName == groupName }
        val details = medicines.sortedBy { it.groupOrder }.joinToString("\n") { 
            "• ${it.name} (№${it.groupOrder}) - ${it.remainingQuantity} шт."
        }
        
        AlertDialog.Builder(this)
            .setTitle("Группа: $groupName")
            .setMessage("Лекарства в группе:\n$details")
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showCreateGroupDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Введите название группы"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Создать новую группу")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createGroup(groupName)
                } else {
                    Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showAddToGroupDialog(groupName: String) {
        //  ИСПРАВЛЕНО: Используем параметр groupName для предварительного выбора группы
        val medicines = dataManager.loadMedicines().filter { it.groupName.isEmpty() }
        if (medicines.isEmpty()) {
            Toast.makeText(this, "Нет лекарств без группы", Toast.LENGTH_SHORT).show()
            return
        }
        
        val groups = dataManager.getExistingGroups()
        if (groups.isEmpty()) {
            Toast.makeText(this, "Сначала создайте группу", Toast.LENGTH_SHORT).show()
            return
        }
        
        val medicineNames = medicines.map { it.name }.toTypedArray()
        val groupNames = groups.toTypedArray()
        
        //  ИСПРАВЛЕНО: Предварительно выбираем группу, если она передана
        val initialGroupIndex = if (groupName.isNotEmpty()) {
            groups.indexOf(groupName).takeIf { it >= 0 } ?: -1
        } else {
            -1
        }
        
        var selectedMedicine: Medicine? = null
        var selectedGroup: String? = if (initialGroupIndex >= 0) groupName else null
        
        AlertDialog.Builder(this)
            .setTitle("Добавить лекарство в группу")
            .setSingleChoiceItems(medicineNames, -1) { _, which ->
                selectedMedicine = medicines[which]
            }
            .setPositiveButton("Выбрать группу") { _, _ ->
                if (selectedMedicine != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Выберите группу")
                        .setSingleChoiceItems(groupNames, initialGroupIndex) { _, which ->
                            selectedGroup = groups[which]
                        }
                        .setPositiveButton("Добавить") { _, _ ->
                            if (selectedGroup != null) {
                                addMedicineToGroup(selectedMedicine!!, selectedGroup!!)
                            }
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showRemoveFromGroupDialog() {
        val medicines = dataManager.loadMedicines().filter { it.groupName.isNotEmpty() }
        if (medicines.isEmpty()) {
            Toast.makeText(this, "Нет лекарств в группах", Toast.LENGTH_SHORT).show()
            return
        }
        
        val medicineNames = medicines.map { "${it.name} (${it.groupName})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Убрать лекарство из группы")
            .setSingleChoiceItems(medicineNames, -1) { _, which ->
                removeMedicineFromGroup(medicines[which])
            }
            .setPositiveButton("Убрать") { _, _ ->
                // Действие уже выполнено в onItemClick
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showEditGroupDialog(groupName: String) {
        val medicines = dataManager.loadMedicines().filter { it.groupName == groupName }
        val medicineNames = medicines.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Редактировать группу: $groupName")
            .setItems(medicineNames) { _, which ->
                editMedicine(medicines[which])
            }
            .setPositiveButton("Закрыть", null)
            .show()
    }
    
    private fun showRenameGroupDialog(oldGroupName: String) {
        val input = android.widget.EditText(this).apply {
            setText(oldGroupName)
            selectAll()
        }
        
        AlertDialog.Builder(this)
            .setTitle("Переименовать группу")
            .setView(input)
            .setPositiveButton("Переименовать") { _, _ ->
                val newGroupName = input.text.toString().trim()
                if (newGroupName.isNotEmpty() && newGroupName != oldGroupName) {
                    renameGroup(oldGroupName, newGroupName)
                } else if (newGroupName.isEmpty()) {
                    Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showDeleteGroupDialog(groupName: String) {
        val medicines = dataManager.loadMedicines().filter { it.groupName == groupName }
        
        AlertDialog.Builder(this)
            .setTitle("Удалить группу")
            .setMessage("Группа '$groupName' содержит ${medicines.size} лекарств.\n\n" +
                    "При удалении группы все лекарства останутся, но будут убраны из группы.\n\n" +
                    "Продолжить?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteGroup(groupName)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun createGroup(groupName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Проверяем, что группа не существует
                val existingGroups = dataManager.getExistingGroups()
                if (existingGroups.contains(groupName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Группа '$groupName' уже существует", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Группа '$groupName' создана", Toast.LENGTH_SHORT).show()
                    loadGroups()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка создания группы: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun addMedicineToGroup(medicine: Medicine, groupName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val groupMedicines = medicines.filter { it.groupName == groupName }
                val newOrder = if (groupMedicines.isNotEmpty()) groupMedicines.maxOf { it.groupOrder } + 1 else 1
                
                val updatedMedicines = medicines.map { med ->
                    if (med.id == medicine.id) {
                        med.copy(
                            groupName = groupName,
                            groupOrder = newOrder
                        )
                    } else {
                        med
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "${medicine.name} добавлен в группу '$groupName'", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка добавления в группу", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun removeMedicineFromGroup(medicine: Medicine) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val updatedMedicines = medicines.map { med ->
                    if (med.id == medicine.id) {
                        med.copy(
                            groupName = "",
                            groupOrder = 0
                        )
                    } else {
                        med
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "${medicine.name} убран из группы", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка удаления из группы", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun editMedicine(medicine: Medicine) {
        val intent = Intent(this, EditMedicineActivity::class.java).apply {
            putExtra("medicine_id", medicine.id)
        }
        startActivity(intent)
    }
    
    private fun updateMedicineQuantity(medicine: Medicine, newQuantity: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val updatedMedicines = medicines.map { med ->
                    if (med.id == medicine.id) {
                        med.copy(remainingQuantity = newQuantity)
                    } else {
                        med
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Количество ${medicine.name} изменено на $newQuantity", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка изменения количества", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateMedicineOrder(medicine: Medicine, newOrder: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val groupMedicines = medicines.filter { it.groupName == medicine.groupName }
                
                // Проверяем, что новый порядок в допустимых пределах
                val maxOrder = groupMedicines.size
                val finalOrder = newOrder.coerceIn(1, maxOrder)
                
                val updatedMedicines = medicines.map { med ->
                    if (med.id == medicine.id) {
                        med.copy(groupOrder = finalOrder)
                    } else {
                        med
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Порядок ${medicine.name} изменен на №$finalOrder", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка изменения порядка", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun renameGroup(oldName: String, newName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val updatedMedicines = medicines.map { medicine ->
                    if (medicine.groupName == oldName) {
                        medicine.copy(groupName = newName)
                    } else {
                        medicine
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Группа переименована", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка переименования", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun deleteGroup(groupName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = dataManager.loadMedicines().toMutableList()
                val updatedMedicines = medicines.map { medicine ->
                    if (medicine.groupName == groupName) {
                        medicine.copy(
                            groupId = null,
                            groupName = "",
                            groupOrder = 0
                        )
                    } else {
                        medicine
                    }
                }
                
                val success = dataManager.saveMedicines(updatedMedicines)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Группа удалена", Toast.LENGTH_SHORT).show()
                        loadGroups()
                    } else {
                        Toast.makeText(this@GroupManagementActivity, 
                            "Ошибка удаления", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupManagementActivity, 
                        "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun performAutoGrouping() {
        CoroutineScope(Dispatchers.IO).launch {
            val medicines = dataManager.loadMedicines()
            val updatedMedicines = MedicineGroupingUtil.autoGroupByTime(medicines)
            
            // Сохраняем обновленные лекарства
            updatedMedicines.forEach { medicine ->
                dataManager.updateMedicine(medicine)
            }
            
            withContext(Dispatchers.Main) {
                loadGroups()
                Toast.makeText(this@GroupManagementActivity, 
                    "Автогруппировка завершена", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showClearGroupsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Очистить все группы")
            .setMessage("Все лекарства будут удалены из групп. Продолжить?")
            .setPositiveButton("Очистить") { _, _ ->
                clearAllGroups()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun clearAllGroups() {
        CoroutineScope(Dispatchers.IO).launch {
            val medicines = dataManager.loadMedicines()
            val updatedMedicines = medicines.map { medicine ->
                medicine.copy(groupName = "", groupOrder = 0)
            }
            
            // Сохраняем обновленные лекарства
            updatedMedicines.forEach { medicine ->
                dataManager.updateMedicine(medicine)
            }
            
            withContext(Dispatchers.Main) {
                loadGroups()
                Toast.makeText(this@GroupManagementActivity, 
                    "Все группы очищены", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadGroups()
    }
}

data class GroupInfo(
    val name: String,
    val medicineCount: Int,
    val medicines: List<Medicine>
) 