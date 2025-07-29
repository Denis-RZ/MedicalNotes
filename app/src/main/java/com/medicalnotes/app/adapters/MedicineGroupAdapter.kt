package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineGroupMainBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatusHelper
import java.time.format.DateTimeFormatter

data class MedicineGroup(
    val timeGroupId: Long,
    val timeGroupName: String,
    val time: java.time.LocalTime,
    val medicines: List<Medicine>,
    val isOverdue: Boolean = false,
    val isTaken: Boolean = false
)

class MedicineGroupAdapter(
    private val onGroupClick: (MedicineGroup) -> Unit
) : ListAdapter<MedicineGroup, MedicineGroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemMedicineGroupMainBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemMedicineGroupMainBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var medicineAdapter: MedicineInGroupAdapter

        fun bind(group: MedicineGroup) {
            // Заголовок группы
            binding.textGroupName.text = group.timeGroupName
            binding.textGroupTime.text = group.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            
            // Количество лекарств
            binding.textGroupStatus.text = "${group.medicines.size} ${getMedicineWord(group.medicines.size)}"
            
            // Настройка адаптера для лекарств в группе
            if (!::medicineAdapter.isInitialized) {
                medicineAdapter = MedicineInGroupAdapter()
                binding.recyclerViewMedicines.apply {
                    layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    adapter = medicineAdapter
                }
            }
            
            // Обновляем список лекарств
            medicineAdapter.submitList(group.medicines)
            
            // Статус группы
            when {
                group.isTaken -> {
                    binding.textGroupMissedStatus.visibility = android.view.View.GONE
                    binding.buttonTakeGroup.text = "ПРИНЯТО"
                    binding.buttonTakeGroup.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_blue)
                    )
                }
                group.isOverdue -> {
                    binding.textGroupMissedStatus.visibility = android.view.View.VISIBLE
                    binding.textGroupMissedStatus.text = "ПРОСРОЧЕНО"
                    binding.buttonTakeGroup.text = "ПРИНЯТЬ ВСЕ"
                    binding.buttonTakeGroup.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                    )
                    
                    // Красный фон для просроченных групп
                    binding.cardMedicine.setCardBackgroundColor(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red_light)
                    )
                }
                else -> {
                    binding.textGroupMissedStatus.visibility = android.view.View.GONE
                    binding.buttonTakeGroup.text = "ПРИНЯТЬ ВСЕ"
                    binding.buttonTakeGroup.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.button_success)
                    )
                    
                    // Обычный фон
                    binding.cardMedicine.setCardBackgroundColor(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                    )
                }
            }
            
            // Обработчик клика
            binding.buttonTakeGroup.setOnClickListener {
                onGroupClick(group)
            }
        }
        
        private fun getMedicineWord(count: Int): String {
            return when {
                count == 1 -> "лекарство"
                count in 2..4 -> "лекарства"
                else -> "лекарств"
            }
        }
    }

    private class GroupDiffCallback : DiffUtil.ItemCallback<MedicineGroup>() {
        override fun areItemsTheSame(oldItem: MedicineGroup, newItem: MedicineGroup): Boolean {
            return oldItem.timeGroupId == newItem.timeGroupId
        }

        override fun areContentsTheSame(oldItem: MedicineGroup, newItem: MedicineGroup): Boolean {
            return oldItem == newItem
        }
    }
}

// Адаптер для лекарств внутри группы
class MedicineInGroupAdapter : ListAdapter<Medicine, MedicineInGroupAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = com.medicalnotes.app.databinding.ItemMedicineInGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicineViewHolder(
        private val binding: com.medicalnotes.app.databinding.ItemMedicineInGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            binding.textMedicineName.text = medicine.name
            binding.textMedicineDosage.text = medicine.dosage
            binding.textMedicineQuantity.text = "${medicine.remainingQuantity} ${medicine.medicineType.lowercase()}"
            
            // Иконка типа лекарства
            val iconRes = when (medicine.medicineType) {
                "Уколы" -> android.R.drawable.ic_menu_edit
                "Капли" -> android.R.drawable.ic_menu_view
                "Сироп" -> android.R.drawable.ic_menu_help
                "Ингаляции" -> android.R.drawable.ic_menu_send
                "Мази" -> android.R.drawable.ic_menu_edit
                "Свечи" -> android.R.drawable.ic_menu_help
                else -> android.R.drawable.ic_menu_help
            }
            binding.imageMedicineType.setImageResource(iconRes)
            
            // Статус принятия
            binding.imageTakenStatus.visibility = if (medicine.takenToday) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    private class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
} 