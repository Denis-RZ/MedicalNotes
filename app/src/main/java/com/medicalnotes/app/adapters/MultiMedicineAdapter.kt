package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineMultiMainBinding
import com.medicalnotes.app.models.Medicine
import java.time.format.DateTimeFormatter

data class MultiMedicineItem(
    val time: java.time.LocalTime,
    val medicines: List<Medicine>,
    val isOverdue: Boolean = false,
    val isTaken: Boolean = false
)

class MultiMedicineAdapter(
    private val onMultiMedicineClick: (MultiMedicineItem) -> Unit
) : ListAdapter<MultiMedicineItem, MultiMedicineAdapter.MultiMedicineViewHolder>(MultiMedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiMedicineViewHolder {
        val binding = ItemMedicineMultiMainBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MultiMedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MultiMedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MultiMedicineViewHolder(
        private val binding: ItemMedicineMultiMainBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var medicineAdapter: MedicineInMultiAdapter

        fun bind(item: MultiMedicineItem) {
            // Время приема
            binding.textMedicineTime.text = item.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            
            // Количество лекарств
            val count = item.medicines.size
            binding.textMedicineCount.text = "$count ${getMedicineWord(count)}"
            
            // Настройка адаптера для лекарств
            if (!::medicineAdapter.isInitialized) {
                medicineAdapter = MedicineInMultiAdapter()
                binding.recyclerViewMedicines.apply {
                    layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    adapter = medicineAdapter
                }
            }
            
            // Обновляем список лекарств
            medicineAdapter.submitList(item.medicines)
            
            // Статус группы
            when {
                item.isTaken -> {
                    binding.textStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_taken)
                    binding.textMissedStatus.visibility = android.view.View.GONE
                    binding.buttonTakeAll.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_taken_uppercase)
                    binding.buttonTakeAll.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_blue)
                    )
                }
                item.isOverdue -> {
                    binding.textStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_overdue)
                    binding.textMissedStatus.visibility = android.view.View.VISIBLE
                    binding.textMissedStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase)
                    binding.buttonTakeAll.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_take_uppercase) + " ВСЕ"
                    binding.buttonTakeAll.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                    )
                    
                    // Красный фон для просроченных
                    binding.cardMedicine.setCardBackgroundColor(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red_light)
                    )
                }
                else -> {
                    binding.textStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_active)
                    binding.textMissedStatus.visibility = android.view.View.GONE
                    binding.buttonTakeAll.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_take_uppercase) + " ВСЕ"
                    binding.buttonTakeAll.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.button_success)
                    )
                    
                    // Обычный фон
                    binding.cardMedicine.setCardBackgroundColor(
                        binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                    )
                }
            }
            
            // Обработчик клика
            binding.buttonTakeAll.setOnClickListener {
                onMultiMedicineClick(item)
            }
        }
        
        private fun getMedicineWord(count: Int): String {
            return when {
                count == 1 -> binding.root.context.getString(com.medicalnotes.app.R.string.preparation_count_one)
                count in 2..4 -> binding.root.context.getString(com.medicalnotes.app.R.string.preparation_count_few)
                else -> binding.root.context.getString(com.medicalnotes.app.R.string.preparation_count_many)
            }
        }
    }

    private class MultiMedicineDiffCallback : DiffUtil.ItemCallback<MultiMedicineItem>() {
        override fun areItemsTheSame(oldItem: MultiMedicineItem, newItem: MultiMedicineItem): Boolean {
            return oldItem.time == newItem.time
        }

        override fun areContentsTheSame(oldItem: MultiMedicineItem, newItem: MultiMedicineItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Адаптер для лекарств внутри мульти-карточки
class MedicineInMultiAdapter : ListAdapter<Medicine, MedicineInMultiAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = com.medicalnotes.app.databinding.ItemMedicineInMultiBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicineViewHolder(
        private val binding: com.medicalnotes.app.databinding.ItemMedicineInMultiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            // Название лекарства
            binding.textMedicineName.text = medicine.name
            
            // Дозировка
            binding.textMedicineDosage.text = medicine.dosage
            
            // Количество для приема
            binding.textMedicineQuantity.text = "1 ${medicine.medicineType.lowercase()}"
            
            // Оставшееся количество
            binding.textRemainingQuantity.text = "Осталось: ${medicine.remainingQuantity}"
            
            // Цветной индикатор (разные цвета для разных лекарств)
            val colors = listOf(
                android.graphics.Color.parseColor("#2196F3"), // Синий
                android.graphics.Color.parseColor("#FF9800"), // Оранжевый
                android.graphics.Color.parseColor("#4CAF50"), // Зеленый
                android.graphics.Color.parseColor("#9C27B0"), // Фиолетовый
                android.graphics.Color.parseColor("#F44336"), // Красный
                android.graphics.Color.parseColor("#00BCD4"), // Голубой
                android.graphics.Color.parseColor("#795548"), // Коричневый
                android.graphics.Color.parseColor("#607D8B")  // Серо-синий
            )
            val colorIndex = (medicine.id % colors.size).toInt()
            binding.viewColorIndicator.setBackgroundColor(colors[colorIndex])
            
            // Предупреждение о низком количестве (менее 5)
            if (medicine.remainingQuantity < 5) {
                binding.textLowStockWarning.visibility = android.view.View.VISIBLE
                binding.textRemainingQuantity.setTextColor(
                    binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                )
            } else {
                binding.textLowStockWarning.visibility = android.view.View.GONE
                binding.textRemainingQuantity.setTextColor(
                    binding.root.context.getColor(com.medicalnotes.app.R.color.dark_gray)
                )
            }
            
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