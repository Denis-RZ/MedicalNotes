package com.medicalnotes.app

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
        // Настройка RecyclerView
        groupAdapter = GroupAdapter(
            onGroupClick = { groupName ->
                showGroupDetails(groupName)
            },
            onRenameClick = { groupName ->
                showRenameGroupDialog(groupName)
            },
            onDeleteClick = { groupName ->
                showDeleteGroupDialog(groupName)
            }
        )
        
        binding.recyclerViewGroups.apply {
            layoutManager = LinearLayoutManager(this@GroupManagementActivity)
            adapter = groupAdapter
        }
        
        // Кнопка "Назад"
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadGroups() {
        CoroutineScope(Dispatchers.IO).launch {
            val groups = dataManager.getExistingGroups()
            val groupDetails = groups.map { groupName ->
                val medicines = dataManager.loadMedicines().filter { it.groupName == groupName }
                GroupInfo(
                    name = groupName,
                    medicineCount = medicines.size,
                    medicines = medicines
                )
            }
            
            withContext(Dispatchers.Main) {
                groupAdapter.submitList(groupDetails)
                updateEmptyState(groupDetails.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewGroups.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showGroupDetails(groupName: String) {
        val medicines = dataManager.loadMedicines().filter { it.groupName == groupName }
        val details = medicines.joinToString("\n") { 
            "• ${it.name} (№${it.groupOrder})"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Группа: $groupName")
            .setMessage("Лекарства в группе:\n$details")
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
}

data class GroupInfo(
    val name: String,
    val medicineCount: Int,
    val medicines: List<Medicine>
) 