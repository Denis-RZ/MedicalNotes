package com.medicalnotes.app.utils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.medicalnotes.app.R
import com.medicalnotes.app.models.ButtonAction
import com.medicalnotes.app.models.ButtonColor
import com.medicalnotes.app.models.ButtonSize
import com.medicalnotes.app.models.CustomButton

class ButtonManager(private val context: Context) {
    
    fun createButton(
        customButton: CustomButton,
        onActionClick: (ButtonAction) -> Unit
    ): MaterialButton {
        return MaterialButton(context).apply {
            text = customButton.name
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            
            // Применяем размер
            applyButtonSize(customButton.size)
            
            // Применяем цвет
            applyButtonColor(customButton.color)
            
            // Устанавливаем обработчик нажатия
            setOnClickListener {
                onActionClick(customButton.action)
            }
        }
    }
    
    private fun MaterialButton.applyButtonSize(size: ButtonSize) {
        when (size) {
            ButtonSize.SMALL -> {
                minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height_small)
                textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_small).toFloat()
            }
            ButtonSize.MEDIUM -> {
                minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height_medium)
                textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_medium).toFloat()
            }
            ButtonSize.LARGE -> {
                minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height_large)
                textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_large).toFloat()
            }
            ButtonSize.EXTRA_LARGE -> {
                minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height_extra_large)
                textSize = context.resources.getDimensionPixelSize(R.dimen.text_size_extra_large).toFloat()
            }
        }
    }
    
    private fun MaterialButton.applyButtonColor(color: ButtonColor) {
        when (color) {
            ButtonColor.PRIMARY -> {
                setBackgroundColor(context.getColor(R.color.button_primary))
                setTextColor(context.getColor(R.color.white))
            }
            ButtonColor.SECONDARY -> {
                setBackgroundColor(context.getColor(R.color.button_secondary))
                setTextColor(context.getColor(R.color.white))
            }
            ButtonColor.SUCCESS -> {
                setBackgroundColor(context.getColor(R.color.button_success))
                setTextColor(context.getColor(R.color.white))
            }
            ButtonColor.WARNING -> {
                setBackgroundColor(context.getColor(R.color.button_warning))
                setTextColor(context.getColor(R.color.black))
            }
            ButtonColor.DANGER -> {
                setBackgroundColor(context.getColor(R.color.button_danger))
                setTextColor(context.getColor(R.color.white))
            }
            ButtonColor.HIGH_CONTRAST -> {
                setBackgroundColor(context.getColor(R.color.high_contrast_black))
                setTextColor(context.getColor(R.color.high_contrast_yellow))
            }
        }
    }
    
    fun getButtonStyle(size: ButtonSize): Int {
        return when (size) {
            ButtonSize.SMALL -> R.style.Widget_MedicalNotes_Button
            ButtonSize.MEDIUM -> R.style.Widget_MedicalNotes_Button
            ButtonSize.LARGE -> R.style.Widget_MedicalNotes_Button_Large
            ButtonSize.EXTRA_LARGE -> R.style.Widget_MedicalNotes_Button_ExtraLarge
        }
    }
    
    fun getButtonColorResource(color: ButtonColor): Int {
        return when (color) {
            ButtonColor.PRIMARY -> R.color.button_primary
            ButtonColor.SECONDARY -> R.color.button_secondary
            ButtonColor.SUCCESS -> R.color.button_success
            ButtonColor.WARNING -> R.color.button_warning
            ButtonColor.DANGER -> R.color.button_danger
            ButtonColor.HIGH_CONTRAST -> R.color.high_contrast_black
        }
    }
} 