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
    
    fun applyLargeText(rootView: View) {
        applyToAllTextViews(rootView) { textView ->
            val currentSize = textView.textSize
            val newSize = currentSize * 1.3f
            textView.textSize = newSize / textView.resources.displayMetrics.density
        }
    }
    
    fun applyHighContrast(rootView: View) {
        applyToAllViews(rootView) { view ->
            when (view) {
                is TextView -> {
                    view.setTextColor(view.context.getColor(android.R.color.black))
                    view.setBackgroundColor(view.context.getColor(android.R.color.white))
                }
                is MaterialButton -> {
                    view.setTextColor(view.context.getColor(android.R.color.white))
                    view.setBackgroundColor(view.context.getColor(android.R.color.black))
                }
            }
        }
    }
    
    fun applySimpleInterface(rootView: View) {
        applyToAllViews(rootView) { view ->
            when (view) {
                is MaterialButton -> {
                    // Увеличиваем размер кнопок
                    val layoutParams = view.layoutParams
                    if (layoutParams != null) {
                        layoutParams.height = (view.resources.displayMetrics.density * 80).toInt() // 80dp
                        view.layoutParams = layoutParams
                    }
                    
                    // Увеличиваем отступы
                    view.setPadding(
                        (view.paddingLeft * 1.5).toInt(),
                        (view.paddingTop * 1.5).toInt(),
                        (view.paddingRight * 1.5).toInt(),
                        (view.paddingBottom * 1.5).toInt()
                    )
                }
            }
        }
    }
    
    private fun applyToAllTextViews(view: View, action: (TextView) -> Unit) {
        if (view is TextView) {
            action(view)
        }
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToAllTextViews(view.getChildAt(i), action)
            }
        }
    }
    
    private fun applyToAllViews(view: View, action: (View) -> Unit) {
        action(view)
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToAllViews(view.getChildAt(i), action)
            }
        }
    }
} 