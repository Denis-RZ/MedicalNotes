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
            binding.apply {
                // Обновляем статус лекарства
                val updatedMedicine = MedicineStatusHelper.updateMedicineStatus(medicine)
                val status = MedicineStatusHelper.getMedicineStatus(updatedMedicine)
                
                // Основная информация
                textMedicineName.text = updatedMedicine.name
                
                // Дозировка с схемой приема
                val dosageDescription = DosageCalculator.getDosageDescription(updatedMedicine)
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
                
                // Время приема
                val timeText = if (updatedMedicine.multipleDoses && updatedMedicine.doseTimes.isNotEmpty()) {
                    val times = updatedMedicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
                    times.joinToString(", ")
                } else {
                    updatedMedicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                }
                textMedicineSchedule.text = timeText
                
                // Количество
                textMedicineQuantity.text = "${updatedMedicine.remainingQuantity} ${updatedMedicine.medicineType.lowercase()}"
                
                // Статус в зависимости от состояния
                when (status) {
                    MedicineStatus.UPCOMING -> {
                        textStatus.text = "СЕГОДНЯ"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_green))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        root.setCardBackgroundColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                    }
                    MedicineStatus.OVERDUE -> {
                        textStatus.text = "ПРОСРОЧЕНО"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        root.setCardBackgroundColor(root.context.getColor(com.medicalnotes.app.R.color.medical_red))
                    }
                    MedicineStatus.TAKEN_TODAY -> {
                        textStatus.text = "ПРИНЯТО"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_blue))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        root.setCardBackgroundColor(root.context.getColor(com.medicalnotes.app.R.color.light_gray))
                    }
                    MedicineStatus.NOT_TODAY -> {
                        textStatus.text = "НЕ СЕГОДНЯ"
                        textStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.gray))
                        textStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        root.setCardBackgroundColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                    }
                }
                
                // Индикатор низкого запаса
                if (medicine.remainingQuantity <= 5) {
                    textLowStock.visibility = android.view.View.VISIBLE
                } else {
                    textLowStock.visibility = android.view.View.GONE
                }
                
                // Кнопки в зависимости от статуса
                when (status) {
                    MedicineStatus.UPCOMING -> {
                        buttonToggle.text = "Принял"
                        buttonToggle.visibility = android.view.View.VISIBLE
                        buttonEdit.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.OVERDUE -> {
                        buttonToggle.text = "Принял"
                        buttonToggle.visibility = android.view.View.VISIBLE
                        buttonEdit.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.TAKEN_TODAY -> {
                        buttonToggle.text = "Отменить"
                        buttonToggle.visibility = android.view.View.VISIBLE
                        buttonEdit.visibility = android.view.View.VISIBLE
                    }
                    MedicineStatus.NOT_TODAY -> {
                        buttonToggle.text = "Отключить"
                        buttonToggle.visibility = android.view.View.VISIBLE
                        buttonEdit.visibility = android.view.View.VISIBLE
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
                buttonDelete.setOnClickListener { onDeleteClick(updatedMedicine) }
                buttonEdit.setOnClickListener { 
                    android.util.Log.d("MedicineGridAdapter", "Edit button clicked for: ${updatedMedicine.name}")
                    onEditClick(updatedMedicine) 
                }
                
                // Обработчик кнопки принятия/отмены
                buttonToggle.setOnClickListener { 
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