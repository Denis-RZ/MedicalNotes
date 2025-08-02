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
            try {
                binding.apply {
                    // Устанавливаем название лекарства
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
                    
                    // Исправляем дублирование "Осталось:"
                    textMedicineQuantity.text = "${medicine.remainingQuantity} ${medicine.medicineType.lowercase()}"
                    
                    // Показываем статус в зависимости от состояния лекарства
                    when (medicineStatus) {
                        MedicineStatus.OVERDUE -> {
                            //  СРОЧНО: НОВЫЙ ДИЗАЙН ДЛЯ ПРОСРОЧЕННЫХ ЛЕКАРСТВ
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = " ПРОСРОЧЕНО!"
                            textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.white))
                            textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.missed_background)
                            textMissedStatus.textSize = 14f
                            textMissedStatus.setPadding(24, 12, 24, 12)
                            
                            //  СРОЧНО: Скрываем статус "АКТИВНО" для просроченных
                            binding.textStatus.visibility = android.view.View.GONE
                            
                            //  СРОЧНО: Красный фон карточки с границей
                            binding.viewCardBackground.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.overdue_background)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                                )
                            )
                            cardMedicine.setStrokeWidth(6)
                            
                            //  СРОЧНО: Темно-красный текст для лучшей видимости
                            textMedicineName.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineTime.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineDosage.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineQuantity.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineNotes.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            
                            //  СРОЧНО: Показываем иконку предупреждения
                            binding.textWarningIcon.visibility = android.view.View.VISIBLE
                            
                            //  СРОЧНО: Желтая кнопка с красным текстом
                            buttonTakeMedicine.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.urgent_button)
                            )
                            buttonTakeMedicine.setTextColor(
                                root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                                )
                            )
                            buttonTakeMedicine.text = "ПРИНЯТЬ"
                            buttonTakeMedicine.textSize = 14f
                            
                            //  СРОЧНО: Убираем красный фон кнопки
                            buttonTakeMedicine.backgroundTintList = null
                            
                            // Мигание кнопки для просроченных лекарств
                            startButtonBlinkingAnimation(buttonTakeMedicine, true)
                            
                            //  ИЗМЕНЕНО: Уведомления теперь управляются автоматически в MainActivity
                            // Уведомление для просроченных лекарств показывается только визуально
                            android.util.Log.d("MainMedicineAdapter", "Просроченное лекарство отображается: ${medicine.name}")
                        }
                        MedicineStatus.UPCOMING -> {
                            //  Скрываем иконку предупреждения для обычных лекарств
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "АКТИВНО" для обычных лекарств
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = "СЕГОДНЯ"
                            textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_green))
                            textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для обычного отображения
                            textMedicineName.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            // Обычный фон
                            binding.viewCardBackground.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  УЛУЧШЕНО: Зеленая кнопка с белым текстом для обычных лекарств
                            buttonTakeMedicine.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                        MedicineStatus.TAKEN_TODAY -> {
                            //  Скрываем иконку предупреждения для принятых лекарств
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "ПРИНЯТО" для принятых лекарств
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            binding.textStatus.text = "ПРИНЯТО"
                            
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = "ПРИНЯТО"
                            textMissedStatus.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.medical_blue))
                            textMissedStatus.background = root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для принятых лекарств
                            textMedicineName.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            // Обычный фон
                            binding.viewCardBackground.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  УЛУЧШЕНО: Зеленая кнопка с белым текстом для принятых лекарств
                            buttonTakeMedicine.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                        else -> {
                            //  Скрываем иконку предупреждения для остальных случаев
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "АКТИВНО" для остальных случаев
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            binding.textStatus.text = "АКТИВНО"
                            
                            textMissedStatus.visibility = android.view.View.GONE
                            binding.viewCardBackground.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для остальных случаев
                            textMedicineName.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            //  УЛУЧШЕНО: Зеленая кнопка с белым текстом для остальных случаев
                            buttonTakeMedicine.setBackgroundColor(
                                root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                    }
                    
                    if (medicine.notes.isNotEmpty()) {
                        textMedicineNotes.text = medicine.notes
                        textMedicineNotes.visibility = android.view.View.VISIBLE
                    } else {
                        textMedicineNotes.visibility = android.view.View.GONE
                    }
                    
                    //  ИЗМЕНЕНО: Цветовая индикация для инсулина (только если не просрочено)
                    if (medicine.isInsulin && medicineStatus != MedicineStatus.OVERDUE) {
                        cardMedicine.setCardBackgroundColor(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_orange)
                        )
                    }
                    
                    //  ИЗМЕНЕНО: Индикация низкого запаса (только если не просрочено)
                    if (medicine.remainingQuantity <= 5 && medicineStatus != MedicineStatus.OVERDUE) {
                        textMedicineQuantity.setTextColor(
                            root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                        )
                    } else if (medicineStatus != MedicineStatus.OVERDUE) {
                        textMedicineQuantity.setTextColor(
                            root.context.getColor(com.medicalnotes.app.R.color.black)
                        )
                    }
                    // Для просроченных лекарств цвет уже установлен выше (белый)
                    
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
            } catch (e: Exception) {
                android.util.Log.e("MainMedicineAdapter", "Error binding medicine data", e)
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