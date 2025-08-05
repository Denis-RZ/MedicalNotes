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

    /**
     * Обновляет язык в адаптере и перерисовывает все элементы
     */
    fun updateLanguage() {
        android.util.Log.d("GroupAdapter", "Updating language in adapter")
        notifyDataSetChanged()
    }

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
                textMedicineCount.text = root.context.resources.getQuantityString(
                    com.medicalnotes.app.R.plurals.medicines_count,
                    groupData.totalMedicines,
                    groupData.totalMedicines
                )
                
                // Отображаем первые 3 лекарства в группе
                val medicineNames = groupData.medicines
                    .take(3)
                    .joinToString(", ") { it.name }
                
                if (groupData.totalMedicines > 3) {
                    textMedicineList.text = root.context.getString(
                        com.medicalnotes.app.R.string.and_more,
                        medicineNames,
                        groupData.totalMedicines - 3
                    )
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