package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.GroupInfo
import com.medicalnotes.app.databinding.ItemGroupBinding

class GroupAdapter(
    private val onGroupClick: (String) -> Unit,
    private val onRenameClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<GroupInfo, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(groupInfo: GroupInfo) {
            binding.textGroupName.text = groupInfo.name
            binding.textMedicineCount.text = "${groupInfo.medicineCount} лекарств"
            
            // Список лекарств в группе
            val medicineList = groupInfo.medicines
                .sortedBy { it.groupOrder }
                .joinToString(", ") { "${it.name} (№${it.groupOrder})" }
            binding.textMedicineList.text = medicineList
            
            // Обработчики кликов
            binding.root.setOnClickListener {
                onGroupClick(groupInfo.name)
            }
            
            binding.buttonRename.setOnClickListener {
                onRenameClick(groupInfo.name)
            }
            
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(groupInfo.name)
            }
        }
    }

    private class GroupDiffCallback : DiffUtil.ItemCallback<GroupInfo>() {
        override fun areItemsTheSame(oldItem: GroupInfo, newItem: GroupInfo): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: GroupInfo, newItem: GroupInfo): Boolean {
            return oldItem == newItem
        }
    }
} 