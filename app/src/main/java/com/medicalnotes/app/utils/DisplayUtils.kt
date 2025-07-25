package com.medicalnotes.app.utils

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.medicalnotes.app.models.UserPreferences

object DisplayUtils {
    
    fun applyElderlyMode(view: View, preferences: UserPreferences) {
        if (!preferences.isElderlyMode) return
        
        when (view) {
            is TextView -> applyElderlyModeToTextView(view, preferences)
            is MaterialButton -> applyElderlyModeToButton(view, preferences)
        }
    }
    
    private fun applyElderlyModeToTextView(textView: TextView, preferences: UserPreferences) {
        if (preferences.largeTextEnabled) {
            val currentSize = textView.textSize
            val newSize = currentSize * 1.5f // Увеличиваем на 50%
            textView.textSize = newSize / textView.resources.displayMetrics.density
        }
        
        if (preferences.useHighContrast) {
            textView.setTextColor(textView.context.getColor(android.R.color.black))
        }
    }
    
    private fun applyElderlyModeToButton(button: MaterialButton, preferences: UserPreferences) {
        if (preferences.largeTextEnabled) {
            val currentSize = button.textSize
            val newSize = currentSize * 1.3f // Увеличиваем на 30%
            button.textSize = newSize / button.resources.displayMetrics.density
            
            // Увеличиваем высоту кнопки
            val layoutParams = button.layoutParams
            if (layoutParams != null) {
                layoutParams.height = (button.resources.displayMetrics.density * 72).toInt() // 72dp
                button.layoutParams = layoutParams
            }
        }
        
        if (preferences.useHighContrast) {
            button.setTextColor(button.context.getColor(android.R.color.white))
            button.setBackgroundColor(button.context.getColor(android.R.color.black))
        }
    }
    
    fun getTextSize(context: Context, baseSize: Float, preferences: UserPreferences): Float {
        return if (preferences.isElderlyMode && preferences.largeTextEnabled) {
            baseSize * 1.5f
        } else {
            baseSize
        }
    }
    
    fun getButtonHeight(context: Context, baseHeight: Int, preferences: UserPreferences): Int {
        return if (preferences.isElderlyMode && preferences.largeTextEnabled) {
            (baseHeight * 1.5).toInt()
        } else {
            baseHeight
        }
    }
} 