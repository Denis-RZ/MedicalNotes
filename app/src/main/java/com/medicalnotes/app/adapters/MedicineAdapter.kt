package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatus
import java.time.format.DateTimeFormatter

class MedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onSkipClick: (Medicine) -> Unit,
    private val onEditClick: (Medicine) -> Unit,
    private val onDeleteClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MedicineViewHolder(binding, onMedicineClick, onSkipClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicineViewHolder(
        private val binding: ItemMedicineBinding,
        private val onMedicineClick: (Medicine) -> Unit,
        private val onSkipClick: (Medicine) -> Unit,
        private val onEditClick: (Medicine) -> Unit,
        private val onDeleteClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(medicine: Medicine) {
            binding.apply {
                textMedicineName.text = medicine.name
                textMedicineDosage.text = medicine.dosage
                
                // Добавляем схему приема к дозировке
                val dosageDescription = DosageCalculator.getDosageDescription(medicine)
                val groupInfo = if (medicine.groupName.isNotEmpty()) {
                    " (${medicine.groupName}, №${medicine.groupOrder})"
                } else {
                    ""
                }
                val fullDosageText = if (medicine.dosage.isNotEmpty()) {
                    "$dosageDescription - ${medicine.dosage}$groupInfo"
                } else {
                    dosageDescription + groupInfo
                }
                textMedicineDosage.text = fullDosageText
                
                // Получаем статус лекарства
                val medicineStatus = DosageCalculator.getMedicineStatus(medicine)
                
                // Отображаем время приема
                val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
                    val times = medicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
                    times.joinToString(", ")
                } else {
                    medicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                }
                textMedicineTime.text = timeText
                
                textMedicineQuantity.text = "Осталось: ${medicine.remainingQuantity} ${medicine.medicineType.lowercase()}"
                
                // Показываем статус в зависимости от состояния лекарства
                when (medicineStatus) {
                    MedicineStatus.OVERDUE -> {
                        textMissedStatus.visibility = android.view.View.VISIBLE
                        textMissedStatus.text = "ПРОСРОЧЕНО"
                        textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_red))
                        textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.missed_background)
                        
                        // Мигание для просроченных лекарств
                        startBlinkingAnimation(textMissedStatus, true)
                        
                        // Красный фон для просроченных лекарств
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_red_light)
                        )
                    }
                    MedicineStatus.UPCOMING -> {
                        textMissedStatus.visibility = android.view.View.VISIBLE
                        textMissedStatus.text = "СЕГОДНЯ"
                        textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_green))
                        textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        
                        // Обычный фон
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.white)
                        )
                    }
                    MedicineStatus.TAKEN_TODAY -> {
                        textMissedStatus.visibility = android.view.View.VISIBLE
                        textMissedStatus.text = "ПРИНЯТО"
                        textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_blue))
                        textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                        
                        // Обычный фон
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.white)
                        )
                    }
                    else -> {
                        textMissedStatus.visibility = android.view.View.GONE
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.white)
                        )
                    }
                }
                
                if (medicine.notes.isNotEmpty()) {
                    binding.textMedicineNotes.text = medicine.notes
                    binding.textMedicineNotes.visibility = android.view.View.VISIBLE
                } else {
                    binding.textMedicineNotes.visibility = android.view.View.GONE
                }
                
                // Цветовая индикация для инсулина
                if (medicine.isInsulin) {
                    cardMedicine.setCardBackgroundColor(
                        root.context.getColor(com.medicalnotes.app.R.color.medical_orange)
                    )
                } else {
                    cardMedicine.setCardBackgroundColor(
                        root.context.getColor(com.medicalnotes.app.R.color.white)
                    )
                }
                
                // Индикация низкого запаса
                if (medicine.remainingQuantity <= 5) {
                    textMedicineQuantity.setTextColor(
                        root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                    )
                } else {
                    textMedicineQuantity.setTextColor(
                        root.context.getColor(com.medicalnotes.app.R.color.black)
                    )
                }
                
                binding.buttonTakeMedicine.setOnClickListener {
                    onMedicineClick(medicine)
                }
                
                binding.buttonSkipMedicine.setOnClickListener {
                    onSkipClick(medicine)
                }
                
                binding.buttonEditMedicine.setOnClickListener {
                    onEditClick(medicine)
                }
                
                binding.buttonDeleteMedicine.setOnClickListener {
                    onDeleteClick(medicine)
                }
                
                // Устанавливаем тег для мигания
                if (medicine.isMissed) {
                    textMissedStatus.tag = "missed"
                } else {
                    textMissedStatus.tag = null
                }
            }
        }
        
        private fun startBlinkingAnimation(view: android.view.View, isMissed: Boolean) {
            if (isMissed) {
                view.alpha = 0.5f
                view.animate()
                    .alpha(1.0f)
                    .setDuration(500)
                    .withEndAction {
                        view.animate()
                            .alpha(0.5f)
                            .setDuration(500)
                            .withEndAction {
                                // Проверяем, что лекарство все еще пропущено
                                if (view.tag == "missed") {
                                    startBlinkingAnimation(view, true)
                                }
                            }
                            .start()
                    }
                    .start()
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