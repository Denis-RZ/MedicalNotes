package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineModernBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
// import com.medicalnotes.app.utils.MedicineStatus // УДАЛЕНОHelper
// import com.medicalnotes.app.utils.MedicineStatus // УДАЛЕНО
import java.time.format.DateTimeFormatter

class ModernMedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit
) : ListAdapter<Medicine, ModernMedicineAdapter.ModernMedicineViewHolder>(ModernMedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModernMedicineViewHolder {
        val binding = ItemMedicineModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModernMedicineViewHolder(binding, onMedicineClick)
    }

    override fun onBindViewHolder(holder: ModernMedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ModernMedicineViewHolder(
        private val binding: ItemMedicineModernBinding,
        private val onMedicineClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            binding.apply {
                // Обновляем статус лекарства
                val updatedMedicine = medicine
                val status = DosageCalculator.getMedicineStatus(updatedMedicine)
                
                // Название лекарства
                textMedicineName.text = updatedMedicine.name
                
                // Время приема
                val timeText = if (updatedMedicine.multipleDoses && updatedMedicine.doseTimes.isNotEmpty()) {
                    val times = updatedMedicine.doseTimes.map { time -> 
                        time.format(DateTimeFormatter.ofPattern("HH:mm")) 
                    }
                    times.joinToString(", ")
                } else {
                    updatedMedicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                }
                textMedicineTime.text = timeText
                
                // Дозировка с схемой приема
                val dosageDescription = DosageCalculator.getDosageDescription(updatedMedicine, binding.root.context)
                val groupInfo = if (updatedMedicine.groupName.isNotEmpty()) {
                    " (${updatedMedicine.groupName}, №${updatedMedicine.groupOrder})"
                } else {
                    ""
                }
                val fullDosageText = if (updatedMedicine.dosage.isNotEmpty()) {
                    "$dosageDescription - ${updatedMedicine.dosage}$groupInfo"
                } else {
                    dosageDescription + groupInfo
                }
                textMedicineDosage.text = fullDosageText
                
                // Количество
                textMedicineQuantity.text = "Осталось: ${updatedMedicine.remainingQuantity} ${updatedMedicine.medicineType.lowercase()}"
                
                // Статус в зависимости от состояния
                when (status) {
                    DosageCalculator.MedicineStatus.UPCOMING -> {
                        textStatus.text = root.context.getString(com.medicalnotes.app.R.string.status_today_uppercase)
                        textStatus.background = root.context.getDrawable(
                            com.medicalnotes.app.R.drawable.status_active_badge
                        )
                    }
                    DosageCalculator.MedicineStatus.OVERDUE -> {
                        textStatus.text = root.context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase)
                        textStatus.background = root.context.getDrawable(
                            com.medicalnotes.app.R.drawable.missed_light_background
                        )
                    }
                    DosageCalculator.MedicineStatus.TAKEN_TODAY -> {
                        textStatus.text = root.context.getString(com.medicalnotes.app.R.string.status_taken_uppercase)
                        textStatus.background = root.context.getDrawable(
                            com.medicalnotes.app.R.drawable.status_light_background
                        )
                    }
                    DosageCalculator.MedicineStatus.NOT_TODAY -> {
                        textStatus.text = root.context.getString(com.medicalnotes.app.R.string.status_disable)
                        textStatus.background = root.context.getDrawable(
                            com.medicalnotes.app.R.drawable.status_light_background
                        )
                    }
                }
                
                // Обработчик нажатия на карточку
                root.setOnClickListener {
                    onMedicineClick(updatedMedicine)
                }
                
                // Обработчик нажатия на кнопку "ПРИНЯЛ"
                buttonTakeMedicine.setOnClickListener {
                    onMedicineClick(updatedMedicine)
                }
            }
        }
    }
}

class ModernMedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
    override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
        return oldItem == newItem
    }
} 