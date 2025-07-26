package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemGroupMedicineBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import java.time.format.DateTimeFormatter

/**
 * Адаптер для отображения лекарств внутри группы
 */
class GroupMedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onEditClick: (Medicine) -> Unit,
    private val onMoveUp: (Medicine) -> Unit,
    private val onMoveDown: (Medicine) -> Unit,
    private val onRemoveFromGroup: (Medicine) -> Unit
) : RecyclerView.Adapter<GroupMedicineAdapter.GroupMedicineViewHolder>() {

    private var medicines: List<Medicine> = emptyList()

    fun updateMedicines(newMedicines: List<Medicine>) {
        medicines = newMedicines.sortedBy { it.groupOrder }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMedicineViewHolder {
        val binding = ItemGroupMedicineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupMedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupMedicineViewHolder, position: Int) {
        holder.bind(medicines[position], position)
    }

    override fun getItemCount(): Int = medicines.size

    inner class GroupMedicineViewHolder(
        private val binding: ItemGroupMedicineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicine: Medicine, position: Int) {
            binding.apply {
                // Основная информация
                textMedicineName.text = medicine.name
                textMedicineDosage.text = medicine.dosage
                
                // Время приема
                val timeText = if (medicine.multipleDoses && medicine.doseTimes.isNotEmpty()) {
                    val times = medicine.doseTimes.map { 
                        it.format(DateTimeFormatter.ofPattern("HH:mm")) 
                    }
                    times.joinToString(", ")
                } else {
                    medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                }
                textMedicineTime.text = timeText
                
                // Порядок в группе
                textOrder.text = "${medicine.groupOrder}"
                
                // Количество остатка
                textRemainingQuantity.text = "${medicine.remainingQuantity} ${medicine.medicineType}"
                
                // Статус лекарства
                val status = DosageCalculator.getMedicineStatus(medicine)
                textStatus.text = status.toString()
                
                // Настройка цвета статуса
                val statusColor = when (status.toString()) {
                    "СЕГОДНЯ" -> android.graphics.Color.parseColor("#4CAF50")
                    "ПРОПУЩЕНО" -> android.graphics.Color.parseColor("#F44336")
                    "ЗАВТРА" -> android.graphics.Color.parseColor("#FF9800")
                    else -> android.graphics.Color.parseColor("#757575")
                }
                textStatus.setTextColor(statusColor)
                
                // Кнопки управления порядком
                buttonMoveUp.isEnabled = position > 0
                buttonMoveDown.isEnabled = position < medicines.size - 1
                
                // Обработчики кликов
                root.setOnClickListener {
                    onMedicineClick(medicine)
                }
                
                buttonEdit.setOnClickListener {
                    onEditClick(medicine)
                }
                
                buttonMoveUp.setOnClickListener {
                    onMoveUp(medicine)
                }
                
                buttonMoveDown.setOnClickListener {
                    onMoveDown(medicine)
                }
                
                buttonRemove.setOnClickListener {
                    onRemoveFromGroup(medicine)
                }
            }
        }
    }
} 