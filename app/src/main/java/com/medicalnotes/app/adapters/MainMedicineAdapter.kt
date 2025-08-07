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
import com.medicalnotes.app.utils.DataLocalizationHelper

class MainMedicineAdapter(
    private val onMedicineClick: (Medicine) -> Unit,
    private val onTakeMedicineClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MainMedicineAdapter.MainMedicineViewHolder>(MainMedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainMedicineViewHolder {
        val binding = ItemMedicineMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MainMedicineViewHolder(binding, onMedicineClick, onTakeMedicineClick)
    }

    override fun onBindViewHolder(holder: MainMedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MainMedicineViewHolder(
        private val binding: ItemMedicineMainBinding,
        private val onMedicineClick: (Medicine) -> Unit,
        private val onTakeMedicineClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(medicine: Medicine) {
            try {
                // Локализуем данные лекарства для текущего языка
                val localizedMedicine = DataLocalizationHelper.localizeMedicineData(medicine, binding.root.context)
                
                binding.apply {
                    // Устанавливаем название лекарства
                    textMedicineName.text = localizedMedicine.name
                    
                    textMedicineDosage.text = localizedMedicine.dosage
                    
                    // Добавляем схему приема к дозировке
                    val dosageDescription = DosageCalculator.getDosageDescription(localizedMedicine, binding.root.context)
                    val groupInfo = if (localizedMedicine.groupName.isNotEmpty()) {
                        " (${localizedMedicine.groupName}, №${localizedMedicine.groupOrder})"
                    } else {
                        ""
                    }
                    val fullDosageText = if (localizedMedicine.dosage.isNotEmpty()) {
                        "$dosageDescription - ${localizedMedicine.dosage}$groupInfo"
                    } else {
                        dosageDescription + groupInfo
                    }
                    textMedicineDosage.text = fullDosageText
                    
                    // Получаем статус лекарства
                    val medicineStatus = DosageCalculator.getMedicineStatus(localizedMedicine)
                    
                    // Отображаем время приема
                    val timeText = if (localizedMedicine.multipleDoses && localizedMedicine.doseTimes.isNotEmpty()) {
                        val times = localizedMedicine.doseTimes.map { it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
                        times.joinToString(", ")
                    } else {
                        localizedMedicine.time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    textMedicineTime.text = timeText
                    
                    // Исправляем дублирование "Осталось:"
                    textMedicineQuantity.text = "${localizedMedicine.remainingQuantity} ${localizedMedicine.medicineType.lowercase()}"
                    
                    // Показываем статус в зависимости от состояния лекарства
                    when (medicineStatus) {
                        MedicineStatus.OVERDUE -> {
                            //  СРОЧНО: НОВЫЙ ДИЗАЙН ДЛЯ ПРОСРОЧЕННЫХ ЛЕКАРСТВ
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.missed_status)
                            textMissedStatus.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.white))
                            textMissedStatus.background = binding.root.context.getDrawable(com.medicalnotes.app.R.drawable.missed_background)
                            textMissedStatus.textSize = 14f
                            textMissedStatus.setPadding(24, 12, 24, 12)
                            
                            //  СРОЧНО: Скрываем статус "АКТИВНО" для просроченных
                            binding.textStatus.visibility = android.view.View.GONE
                            
                            //  СРОЧНО: Красный фон карточки с границей
                            binding.viewCardBackground.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_background)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                                )
                            )
                            cardMedicine.setStrokeWidth(6)
                            
                            //  СРОЧНО: Темно-красный текст для лучшей видимости
                            textMedicineName.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineTime.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineDosage.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineQuantity.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            textMedicineNotes.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red))
                            
                            //  СРОЧНО: Показываем иконку предупреждения
                            binding.textWarningIcon.visibility = android.view.View.VISIBLE
                            
                            //  СРОЧНО: Желтая кнопка с красным текстом
                            buttonTakeMedicine.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.urgent_button)
                            )
                            buttonTakeMedicine.setTextColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.overdue_red)
                                )
                            )
                            buttonTakeMedicine.text = binding.root.context.getString(com.medicalnotes.app.R.string.button_take_medicine)
                            buttonTakeMedicine.textSize = 14f
                            buttonTakeMedicine.isEnabled = true
                            
                            //  СРОЧНО: Убираем красный фон кнопки
                            buttonTakeMedicine.backgroundTintList = null
                            
                            // Мигание кнопки для просроченных лекарств
                            startButtonBlinkingAnimation(buttonTakeMedicine, true)
                            
                            //  ИЗМЕНЕНО: Уведомления теперь управляются автоматически в MainActivity
                            // Уведомление для просроченных лекарств показывается только визуально
                            android.util.Log.d("MainMedicineAdapter", "Просроченное лекарство отображается: ${localizedMedicine.name}")
                        }
                        MedicineStatus.UPCOMING -> {
                            //  Скрываем иконку предупреждения для обычных лекарств
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "АКТИВНО" для обычных лекарств
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_today_uppercase)
                            textMissedStatus.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.medical_green))
                            textMissedStatus.background = binding.root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для обычного отображения
                            textMedicineName.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            // Обычный фон
                            binding.viewCardBackground.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  ИСПРАВЛЕНО: Включаем кнопку и устанавливаем правильный текст для обычных лекарств
                            buttonTakeMedicine.text = binding.root.context.getString(com.medicalnotes.app.R.string.button_take_medicine)
                            buttonTakeMedicine.isEnabled = true
                            buttonTakeMedicine.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                        MedicineStatus.TAKEN_TODAY -> {
                            //  Скрываем иконку предупреждения для принятых лекарств
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "ПРИНЯТО" для принятых лекарств
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            binding.textStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.taken_status)
                            
                            textMissedStatus.visibility = android.view.View.VISIBLE
                            textMissedStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.taken_status)
                            textMissedStatus.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.medical_blue))
                            textMissedStatus.background = binding.root.context.getDrawable(com.medicalnotes.app.R.drawable.status_background)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для принятых лекарств
                            textMedicineName.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            // Обычный фон
                            binding.viewCardBackground.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  ИСПРАВЛЕНО: Отключаем кнопку и меняем текст для принятых лекарств
                            buttonTakeMedicine.text = binding.root.context.getString(com.medicalnotes.app.R.string.button_medicine_taken)
                            buttonTakeMedicine.isEnabled = false
                            buttonTakeMedicine.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                        else -> {
                            //  Скрываем иконку предупреждения для остальных случаев
                            binding.textWarningIcon.visibility = android.view.View.GONE
                            
                            //  Показываем статус "АКТИВНО" для остальных случаев
                            binding.textStatus.visibility = android.view.View.VISIBLE
                            binding.textStatus.text = binding.root.context.getString(com.medicalnotes.app.R.string.status_active_uppercase)
                            
                            textMissedStatus.visibility = android.view.View.GONE
                            binding.viewCardBackground.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            cardMedicine.setStrokeColor(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.gray_medium)
                                )
                            )
                            cardMedicine.setStrokeWidth(1)
                            
                            //  ДОБАВЛЕНО: Черный цвет текста для остальных случаев
                            textMedicineName.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineTime.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineDosage.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineQuantity.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            textMedicineNotes.setTextColor(binding.root.context.getColor(com.medicalnotes.app.R.color.black))
                            
                            //  ИСПРАВЛЕНО: Включаем кнопку и устанавливаем правильный текст для не принятых лекарств
                            buttonTakeMedicine.text = binding.root.context.getString(com.medicalnotes.app.R.string.button_take_medicine)
                            buttonTakeMedicine.isEnabled = true
                            buttonTakeMedicine.setBackgroundColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.medicine_success)
                            )
                            buttonTakeMedicine.setTextColor(
                                binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                            )
                            buttonTakeMedicine.setIconTint(
                                android.content.res.ColorStateList.valueOf(
                                    binding.root.context.getColor(com.medicalnotes.app.R.color.white)
                                )
                            )
                        }
                    }
                    
                    if (localizedMedicine.notes.isNotEmpty()) {
                        textMedicineNotes.text = localizedMedicine.notes
                        textMedicineNotes.visibility = android.view.View.VISIBLE
                    } else {
                        textMedicineNotes.visibility = android.view.View.GONE
                    }
                    
                    // DEBUG: Показываем групповую информацию для отладки
                    binding.debugGroupInfo.visibility = android.view.View.VISIBLE
                    binding.debugGroupId.text = "Group ID: ${localizedMedicine.groupId}"
                    binding.debugGroupName.text = "Group Name: ${localizedMedicine.groupName}"
                    binding.debugGroupOrder.text = "Group Order: ${localizedMedicine.groupOrder}"
                    binding.debugGroupStartDate.text = "Group Start: ${localizedMedicine.groupStartDate}"
                    binding.debugGroupFrequency.text = "Group Freq: ${localizedMedicine.groupFrequency}"
                    binding.debugIsValidGroup.text = "Valid Group: ${localizedMedicine.isValidGroup()}"
                    
                    //  ИЗМЕНЕНО: Цветовая индикация для инсулина (только если не просрочено)
                    if (localizedMedicine.isInsulin && medicineStatus != MedicineStatus.OVERDUE) {
                        cardMedicine.setCardBackgroundColor(
                            binding.root.context.getColor(com.medicalnotes.app.R.color.medical_orange)
                        )
                    }
                    
                    //  ИЗМЕНЕНО: Индикация низкого запаса (только если не просрочено)
                    if (localizedMedicine.remainingQuantity <= 5 && medicineStatus != MedicineStatus.OVERDUE) {
                        textMedicineQuantity.setTextColor(
                            binding.root.context.getColor(com.medicalnotes.app.R.color.medical_red)
                        )
                    } else if (medicineStatus != MedicineStatus.OVERDUE) {
                        textMedicineQuantity.setTextColor(
                            binding.root.context.getColor(com.medicalnotes.app.R.color.black)
                        )
                    }
                    // Для просроченных лекарств цвет уже установлен выше (белый)
                    
                    buttonTakeMedicine.setOnClickListener {
                        onTakeMedicineClick(localizedMedicine)
                    }
                    
                    // ДОБАВЛЕНО: Клик на карточку для редактирования
                    binding.root.setOnClickListener {
                        onMedicineClick(localizedMedicine)
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

    fun updateLanguage() {
        android.util.Log.d("MainMedicineAdapter", "Updating language in adapter")
        notifyDataSetChanged()
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