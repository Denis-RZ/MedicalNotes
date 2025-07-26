package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemGroupBinding
import com.medicalnotes.app.models.Medicine

/**
 * Адаптер для отображения групп лекарств
 */
class GroupAdapter(
    private val onGroupClick: (String) -> Unit,
    private val onAddToGroup: (String) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groups: List<GroupData> = emptyList()

    data class GroupData(
        val name: String,
        val medicines: List<Medicine>,
        val totalMedicines: Int
    )

    fun updateGroups(medicines: List<Medicine>) {
        val groupedMedicines = medicines.groupBy { it.groupName }
            .filter { it.key.isNotEmpty() }
            .map { (groupName, groupMedicines) ->
                GroupData(
                    name = groupName,
                    medicines = groupMedicines.sortedBy { it.groupOrder },
                    totalMedicines = groupMedicines.size
                )
            }
            .sortedBy { it.name }
        
        groups = groupedMedicines
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(groupData: GroupData) {
            binding.apply {
                textGroupName.text = groupData.name
                textMedicineCount.text = "${groupData.totalMedicines} лекарств"
                
                // Отображаем первые 3 лекарства в группе
                val medicineNames = groupData.medicines
                    .take(3)
                    .joinToString(", ") { it.name }
                
                if (groupData.totalMedicines > 3) {
                    textMedicineList.text = "$medicineNames и еще ${groupData.totalMedicines - 3}"
                } else {
                    textMedicineList.text = medicineNames
                }

                // Обработчики кликов
                root.setOnClickListener {
                    onGroupClick(groupData.name)
                }

                buttonAddMedicine.setOnClickListener {
                    onAddToGroup(groupData.name)
                }
            }
        }
    }
} 