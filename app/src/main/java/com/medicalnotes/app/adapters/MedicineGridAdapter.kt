package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineGridBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatusHelper
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.utils.DataLocalizationHelper

class MedicineGridAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onDeleteClick: (Medicine) -> Unit,
    private val onEditClick: (Medicine) -> Unit,
    private val onToggleClick: (Medicine) -> Unit,
    private val onTakenClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MedicineGridAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicineViewHolder(binding, onMedicineClick, onDeleteClick, onEditClick, onToggleClick, onTakenClick)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicineViewHolder(
        private val binding: ItemMedicineGridBinding,
        private val onMedicineClick: (Medicine) -> Unit,
        private val onDeleteClick: (Medicine) -> Unit,
        private val onEditClick: (Medicine) -> Unit,
        private val onToggleClick: (Medicine) -> Unit,
        private val onTakenClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            // Локализуем данные лекарства для текущего языка
            val localizedMedicine = DataLocalizationHelper.localizeMedicineData(medicine, binding.root.context)
            
            binding.apply {
                // Обновляем статус лекарства
                val updatedMedicine = MedicineStatusHelper.updateMedicineStatus(localizedMedicine)
                val status = MedicineStatusHelper.getMedicineStatus(updatedMedicine)
                
                // Основная информация - ограничиваем длину названия
                textMedicineName.text = if (updatedMedicine.name.length > 15) {
                    updatedMedicine.name.take(12) + "..."
                } else {
                    updatedMedicine.name
                }
                
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
                // Ограничиваем длину текста для предотвращения обрезания
                textMedicineDosage.text = if (fullDosageText.length > 25) {
                    fullDosageText.take(22) + "..."
                } else {
                    fullDosageText
                }
                
                // Время приема
                val timeText = if (updatedMedicine.multipleDoses && updatedMedicine.doseTimes.isNotEmpty()) {
                    val times = updatedMedicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
                    times.joinToString(", ")
                } else {
                    updatedMedicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                }
                binding.textMedicineTime.text = timeText
                
                // Количество - показываем только количество таблеток, не время
                textMedicineQuantity.text = "${updatedMedicine.remainingQuantity} ${updatedMedicine.medicineType.lowercase()}"
                
                // Статус в зависимости от состояния
                when (status) {
                    MedicineStatus.UPCOMING -> {
                        textStatus.text = "СЕГОДНЯ"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_light_background)
                    }
                    MedicineStatus.OVERDUE -> {
                        textStatus.text = "ПРОСРОЧЕНО"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.missed_light_background)
                    }
                    MedicineStatus.TAKEN_TODAY -> {
                        textStatus.text = "ПРИНЯТО"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_light_background)
                    }
                    MedicineStatus.NOT_TODAY -> {
                        textStatus.text = "НЕ АКТИВНО"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_light_background)
                    }
                }
                
                // Индикатор низкого запаса
                if (medicine.remainingQuantity <= 5) {
                    binding.textLowStock.visibility = android.view.View.VISIBLE
                    binding.textLowStock.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.low_stock_compact_background)
                } else {
                    binding.textLowStock.visibility = android.view.View.GONE
                }
                
                // Кнопки в зависимости от статуса
                when (status) {
                    MedicineStatus.UPCOMING -> {
                        binding.buttonToggle.text = "Принял"
                        binding.buttonToggle.visibility = android.view.View.VISIBLE
                        binding.buttonEdit.visibility = android.view.View.VISIBLE
                        binding.buttonDelete.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.OVERDUE -> {
                        binding.buttonToggle.text = "Принял"
                        binding.buttonToggle.visibility = android.view.View.VISIBLE
                        binding.buttonEdit.visibility = android.view.View.VISIBLE
                        binding.buttonDelete.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.TAKEN_TODAY -> {
                        binding.buttonToggle.text = "Отменить"
                        binding.buttonToggle.visibility = android.view.View.VISIBLE
                        binding.buttonEdit.visibility = android.view.View.VISIBLE
                        binding.buttonDelete.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.NOT_TODAY -> {
                        binding.buttonToggle.text = "Отключить"
                        binding.buttonToggle.visibility = android.view.View.VISIBLE
                        binding.buttonEdit.visibility = android.view.View.VISIBLE
                        binding.buttonDelete.visibility = android.view.View.VISIBLE
                    }
                }
                
                // Цвет количества при низком запасе
                if (medicine.remainingQuantity <= 5) {
                    textMedicineQuantity.setTextColor(
                        root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                    )
                } else {
                    textMedicineQuantity.setTextColor(
                        root.context.getColor(com.medicalnotes.app.R.color.black)
                    )
                }
                
                // Обработчики событий
                root.setOnClickListener { onMedicineClick(updatedMedicine) }
                binding.buttonEdit.setOnClickListener { 
                    android.util.Log.d("MedicineGridAdapter", "Edit button clicked for: ${updatedMedicine.name}")
                    onEditClick(updatedMedicine) 
                }
                
                // Обработчик кнопки удаления
                binding.buttonDelete.setOnClickListener {
                    android.util.Log.d("MedicineGridAdapter", "Delete button clicked for: ${updatedMedicine.name}")
                    onDeleteClick(updatedMedicine)
                }
                
                // Обработчик кнопки принятия/отмены
                binding.buttonToggle.setOnClickListener { 
                    when (status) {
                        MedicineStatus.UPCOMING, MedicineStatus.OVERDUE -> {
                            android.util.Log.d("MedicineGridAdapter", "Taken button clicked for: ${updatedMedicine.name}")
                            onTakenClick(updatedMedicine)
                        }
                        MedicineStatus.TAKEN_TODAY -> {
                            android.util.Log.d("MedicineGridAdapter", "Cancel button clicked for: ${updatedMedicine.name}")
                            onToggleClick(updatedMedicine)
                        }
                        else -> {
                            android.util.Log.d("MedicineGridAdapter", "Toggle button clicked for: ${updatedMedicine.name}")
                            onToggleClick(updatedMedicine)
                        }
                    }
                }
            }
        }
    }
}

class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
    override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
        return oldItem == newItem
    }
} 