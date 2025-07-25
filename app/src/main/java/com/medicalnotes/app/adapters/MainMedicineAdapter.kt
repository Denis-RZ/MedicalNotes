package com.medicalnotes.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medicalnotes.app.databinding.ItemMedicineMainBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.utils.DosageCalculator
import com.medicalnotes.app.utils.MedicineStatus
import java.time.format.DateTimeFormatter

class MainMedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MainMedicineAdapter.MainMedicineViewHolder>(MainMedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainMedicineViewHolder {
        val binding = ItemMedicineMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MainMedicineViewHolder(binding, onMedicineClick)
    }

    override fun onBindViewHolder(holder: MainMedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MainMedicineViewHolder(
        private val binding: ItemMedicineMainBinding,
        private val onMedicineClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(medicine: Medicine) {
            binding.apply {
                textMedicineName.text = medicine.name
                textMedicineDosage.text = medicine.dosage
                
                // Получаем статус лекарства
                val medicineStatus = DosageCalculator.getMedicineStatus(medicine)
                
                // Отображаем схему приема с учетом статуса
                val dosageDescription = DosageCalculator.getDosageDescription(medicine)
                val groupInfo = if (medicine.groupName.isNotEmpty()) {
                    " (${medicine.groupName}, №${medicine.groupOrder})"
                } else {
                    ""
                }
                textMedicineTime.text = dosageDescription + groupInfo
                
                textMedicineQuantity.text = "Осталось: ${medicine.remainingQuantity} таблеток"
                
                // Показываем статус в зависимости от состояния лекарства
                when (medicineStatus) {
                    MedicineStatus.OVERDUE -> {
                        textMissedStatus.visibility = android.view.View.VISIBLE
                        textMissedStatus.text = "ПРОСРОЧЕНО"
                        textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                        textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.missed_background)
                        
                        // Красный фон для просроченных лекарств
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_red_light)
                        )
                        
                        // Мигание кнопки для просроченных лекарств
                        startButtonBlinkingAnimation(buttonTakeMedicine, true)
                        
                        // Уведомление для просроченных лекарств
                        val notificationManager = com.medicalnotes.app.utils.NotificationManager(binding.root.context)
                        notificationManager.showOverdueMedicineNotification(medicine)
                        
                        // Красная кнопка для просроченных лекарств
                        buttonTakeMedicine.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_red)
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
                        
                        // Зеленая кнопка для обычных лекарств
                        buttonTakeMedicine.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            root.context.getColor(com.medicalnotes.app.R.color.button_success)
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
                        
                        // Синяя кнопка для принятых лекарств
                        buttonTakeMedicine.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_blue)
                        )
                    }
                    else -> {
                        textMissedStatus.visibility = android.view.View.GONE
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.white)
                        )
                        
                        // Обычная кнопка
                        buttonTakeMedicine.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            root.context.getColor(com.medicalnotes.app.R.color.button_success)
                        )
                    }
                }
                
                if (medicine.notes.isNotEmpty()) {
                    textMedicineNotes.text = medicine.notes
                    textMedicineNotes.visibility = android.view.View.VISIBLE
                } else {
                    textMedicineNotes.visibility = android.view.View.GONE
                }
                
                // Цветовая индикация для инсулина
                if (medicine.isInsulin) {
                    cardMedicine.setCardBackgroundColor(
                        root.context.getColor(com.medicalnotes.app.R.color.medical_orange)
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
                
                buttonTakeMedicine.setOnClickListener {
                    onMedicineClick(medicine)
                }
                
                // Устанавливаем тег для мигания
                if (medicineStatus == MedicineStatus.OVERDUE) {
                    buttonTakeMedicine.tag = "overdue"
                } else {
                    buttonTakeMedicine.tag = null
                }
            }
        }
        
        private fun startButtonBlinkingAnimation(button: android.view.View, isOverdue: Boolean) {
            if (isOverdue) {
                button.alpha = 0.7f
                button.animate()
                    .alpha(1.0f)
                    .setDuration(800)
                    .withEndAction {
                        button.animate()
                            .alpha(0.7f)
                            .setDuration(800)
                            .withEndAction {
                                // Проверяем, что лекарство все еще просрочено
                                if (button.tag == "overdue") {
                                    startButtonBlinkingAnimation(button, true)
                                }
                            }
                            .start()
                    }
                    .start()
            }
        }
        


    }

    private class MainMedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
} 