package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineElderlyBinding
import com.medicalnotes.app.models.Medicine
import java.time.format.DateTimeFormatter

class ElderlyMedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onSkipClick: (Medicine) -> Unit
) : ListAdapter<Medicine, ElderlyMedicineAdapter.ElderlyMedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElderlyMedicineViewHolder {
        val binding = ItemMedicineElderlyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ElderlyMedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ElderlyMedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ElderlyMedicineViewHolder(
        private val binding: ItemMedicineElderlyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine) {
            // Название лекарства
            binding.textMedicineName.text = medicine.name

            // Время приема
            val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
                val times = medicine.doseTimes.map { it.format(DateTimeFormatter.ofPattern("HH:mm")) }
                "⏰ ${times.joinToString(", ")}"
            } else {
                "⏰ ${medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }
            binding.textMedicineTime.text = timeText

            // Дозировка с схемой приема
            val dosageDescription = com.medicalnotes.app.utils.DosageCalculator.getDosageDescription(medicine)
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
            binding.textMedicineDosage.text = fullDosageText

            // Количество таблеток
            binding.textMedicineQuantity.text = "📦 Осталось: ${medicine.remainingQuantity} ${medicine.medicineType.lowercase()}"

            // Заметки (показываем только если есть)
            if (medicine.notes.isNotEmpty()) {
                binding.textMedicineNotes.text = "💡 ${medicine.notes}"
                binding.textMedicineNotes.visibility = android.view.View.VISIBLE
            } else {
                binding.textMedicineNotes.visibility = android.view.View.GONE
            }

            // Статус пропуска
            if (medicine.isMissed) {
                binding.textMissedStatus.visibility = android.view.View.VISIBLE
            } else {
                binding.textMissedStatus.visibility = android.view.View.GONE
            }

            // Кнопка принятия лекарства
            binding.buttonTakeMedicine.setOnClickListener {
                onMedicineClick(medicine)
            }

            // Кнопка пропуска (показываем только если лекарство не принято)
            if (!medicine.takenToday) {
                binding.buttonSkipMedicine.visibility = android.view.View.VISIBLE
                binding.buttonSkipMedicine.setOnClickListener {
                    onSkipClick(medicine)
                }
            } else {
                binding.buttonSkipMedicine.visibility = android.view.View.GONE
            }

            // Изменяем текст кнопки в зависимости от статуса
            if (medicine.takenToday) {
                binding.buttonTakeMedicine.text = "✅ ПРИНЯТО"
                binding.buttonTakeMedicine.isEnabled = false
            } else {
                binding.buttonTakeMedicine.text = "✅ ПРИНЯТЬ ЛЕКАРСТВО"
                binding.buttonTakeMedicine.isEnabled = true
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