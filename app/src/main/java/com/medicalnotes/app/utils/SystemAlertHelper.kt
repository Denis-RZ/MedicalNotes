package com.medicalnotes.app.utils

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine

/**
 * Helper class for showing system alert windows for overdue medicine notifications
 * This provides an alternative way to show notifications on top of everything
 */
class SystemAlertHelper(private val context: Context) {
    
    private var windowManager: WindowManager? = null
    private var alertView: View? = null
    private var isShowing = false
    
    companion object {
        private const val TAG = "SystemAlertHelper"
    }
    
    /**
     * Show a system alert window for overdue medicines
     */
    fun showOverdueAlert(overdueMedicines: List<Medicine>) {
        try {
            if (isShowing) {
                LogCollector.d(TAG, "Alert already showing, updating content")
                updateAlertContent(overdueMedicines)
                return
            }
            
            // ИСПРАВЛЕНО: Убираем блокирующую проверку - overlay должен показываться ВСЕГДА при просроченных лекарствах
            // Overlay-окно критически важно для просроченных лекарств и должно показываться независимо от других уведомлений
            LogCollector.d(TAG, "Forcing system alert for critical overdue medicines")
            
            if (!hasSystemAlertPermission()) {
                LogCollector.w(TAG, "No SYSTEM_ALERT_WINDOW permission - trying fallback methods")
                
                // ДОБАВЛЕНО: Fallback через полноэкранное уведомление при отсутствии overlay разрешения
                try {
                    val notificationManager = com.medicalnotes.app.utils.NotificationManager(context)
                    val firstMedicine = overdueMedicines.firstOrNull()
                    if (firstMedicine != null) {
                        notificationManager.showMedicineCardNotification(firstMedicine, true)
                        LogCollector.d(TAG, "Fallback: показано полноэкранное уведомление")
                    }
                } catch (e: Exception) {
                    LogCollector.e(TAG, "Fallback failed", e)
                }
                
                // Все же пытаемся запросить разрешение для будущего использования
                requestPermission()
                return
            }
            
            LogCollector.d(TAG, "Showing system alert for ${overdueMedicines.size} overdue medicines")
            
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            // Create the alert view
            alertView = LayoutInflater.from(context).inflate(R.layout.notification_medicine_card_modern, null)
            
            // Set up the content
            setupAlertContent(overdueMedicines)
            
            // Set up window parameters
            val params = WindowManager.LayoutParams().apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP
                y = 100 // Offset from top
            }
            
            // Add the view to window manager
            windowManager?.addView(alertView, params)
            isShowing = true
            
            LogCollector.d(TAG, "System alert window shown successfully")
            
        } catch (e: Exception) {
            LogCollector.e(TAG, "Error showing system alert", e)
        }
    }
    
    /**
     * Hide the system alert window
     */
    fun hideAlert() {
        try {
            if (!isShowing || alertView == null || windowManager == null) {
                return
            }
            
            LogCollector.d(TAG, "Hiding system alert window")
            
            windowManager?.removeView(alertView)
            alertView = null
            isShowing = false
            
            LogCollector.d(TAG, "System alert window hidden successfully")
            
        } catch (e: Exception) {
            LogCollector.e(TAG, "Error hiding system alert", e)
        }
    }
    
    /**
     * Update the content of the existing alert
     */
    private fun updateAlertContent(overdueMedicines: List<Medicine>) {
        try {
            alertView?.let { view ->
                setupAlertContent(overdueMedicines)
            }
        } catch (e: Exception) {
            LogCollector.e(TAG, "Error updating alert content", e)
        }
    }
    
    /**
     * Set up the content of the alert view
     */
    private fun setupAlertContent(overdueMedicines: List<Medicine>) {
        try {
            alertView?.let { view ->
                val titleText = view.findViewById<TextView>(R.id.notification_title)
                val medicineNameText = view.findViewById<TextView>(R.id.medicine_name)
                val takeButton = view.findViewById<Button>(R.id.button_taken)
                val skipButton = view.findViewById<Button>(R.id.button_skip)
                
                val medicineNames = overdueMedicines.joinToString(", ") { it.name }
                val overdueCount = overdueMedicines.size
                
                titleText?.text = "🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!"
                medicineNameText?.text = "У вас $overdueCount просроченных лекарств:\n$medicineNames"
                
                // Set up take medicine button
                takeButton?.setOnClickListener {
                    LogCollector.d(TAG, "Take medicine button clicked")
                    
                    // Mark medicines as taken
                    markMedicinesAsTaken(overdueMedicines)
                    
                    // Hide the alert
                    hideAlert()
                    
                    // Stop sound and vibration
                    com.medicalnotes.app.service.OverdueCheckService.forceStopSoundAndVibration(context)
                }
                
                // Set up skip button as close button
                skipButton?.setOnClickListener {
                    LogCollector.d(TAG, "Skip button clicked")
                    hideAlert()
                }
                
                LogCollector.d(TAG, "Alert content set up for $overdueCount medicines")
            }
        } catch (e: Exception) {
            LogCollector.e(TAG, "Error setting up alert content", e)
        }
    }
    
    /**
     * Mark medicines as taken
     */
    private fun markMedicinesAsTaken(medicines: List<Medicine>) {
        try {
            val dataManager = DataManager(context)
            val allMedicines = dataManager.loadMedicines()
            
            val updatedMedicines = allMedicines.map { medicine ->
                if (medicines.any { it.id == medicine.id }) {
                    medicine.copy(takenToday = true)
                } else {
                    medicine
                }
            }
            
            dataManager.saveMedicines(updatedMedicines)
            LogCollector.d(TAG, "Marked ${medicines.size} medicines as taken")
            
        } catch (e: Exception) {
            LogCollector.e(TAG, "Error marking medicines as taken", e)
        }
    }
    
    /**
     * Check if we have system alert window permission
     */
    private fun hasSystemAlertPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true // Permission granted by default on older versions
        }
    }
    
    /**
     * Request system alert window permission
     */
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
} 