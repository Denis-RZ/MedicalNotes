package com.medicalnotes.app.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.medicalnotes.app.R
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.receiver.NotificationButtonReceiver
import java.time.format.DateTimeFormatter

/**
 * Utility class for creating RemoteViews for medicine notification cards
 * Follows Android notification design patterns
 */
object NotificationCardRemoteViews {
    
    /**
     * Creates a RemoteViews for medicine notification
     */
    fun createMedicineNotificationView(
        context: Context,
        medicine: Medicine,
        isOverdue: Boolean = false
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_medicine_card_modern)
        
        // Set title and status
        val title = if (isOverdue) "${context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase)}: ${medicine.name}" else "${context.getString(com.medicalnotes.app.R.string.status_take)}: ${medicine.name}"
        remoteViews.setTextViewText(R.id.notification_title, title)
        
        val status = if (isOverdue) context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase) else context.getString(com.medicalnotes.app.R.string.status_active)
        remoteViews.setTextViewText(R.id.notification_status, status)
        
        // Set medicine details
        remoteViews.setTextViewText(R.id.medicine_name, medicine.name)
        remoteViews.setTextViewText(
            R.id.medicine_dosage,
            context.getString(com.medicalnotes.app.R.string.dosage_format, medicine.dosage)
        )
        val timeText = medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        remoteViews.setTextViewText(
            R.id.medicine_time,
            context.getString(com.medicalnotes.app.R.string.time_format, timeText)
        )
        
        // Set additional info
        remoteViews.setTextViewText(
            R.id.medicine_remaining,
            context.getString(com.medicalnotes.app.R.string.remaining_format, medicine.remainingQuantity)
        )
        
        val groupInfo = if (medicine.groupName.isNotEmpty()) {
            context.getString(com.medicalnotes.app.R.string.group_format, medicine.groupName)
        } else {
            context.getString(com.medicalnotes.app.R.string.group_name_placeholder)
        }
        remoteViews.setTextViewText(R.id.medicine_group, groupInfo)
        
        // Set icon background based on status
        val iconBackgroundRes = if (isOverdue) {
            R.drawable.notification_icon_background_overdue
        } else {
            R.drawable.notification_icon_background
        }
        remoteViews.setInt(R.id.notification_icon_container, "setBackgroundResource", iconBackgroundRes)
        
        // Set button click listeners
        setupButtonClickListeners(remoteViews, medicine.id, context)
        
        return remoteViews
    }
    
    /**
     * Creates a compact RemoteViews for small notifications
     */
    fun createCompactNotificationView(
        context: Context,
        medicine: Medicine,
        isOverdue: Boolean = false
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_medicine_card_modern)
        
        // Hide additional info for compact view
        remoteViews.setViewVisibility(R.id.additional_info_container, android.view.View.GONE)
        
        // Set basic info
        val title = if (isOverdue) "${context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase)}: ${medicine.name}" else "${context.getString(com.medicalnotes.app.R.string.status_take)}: ${medicine.name}"
        remoteViews.setTextViewText(R.id.notification_title, title)
        
        val status = if (isOverdue) context.getString(com.medicalnotes.app.R.string.status_overdue_uppercase) else context.getString(com.medicalnotes.app.R.string.status_active)
        remoteViews.setTextViewText(R.id.notification_status, status)
        
        remoteViews.setTextViewText(R.id.medicine_name, medicine.name)
        remoteViews.setTextViewText(
            R.id.medicine_dosage,
            context.getString(com.medicalnotes.app.R.string.dosage_format, medicine.dosage)
        )
        val timeText = medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        remoteViews.setTextViewText(
            R.id.medicine_time,
            context.getString(com.medicalnotes.app.R.string.time_format, timeText)
        )
        
        // Set icon background
        val iconBackgroundRes = if (isOverdue) {
            R.drawable.notification_icon_background_overdue
        } else {
            R.drawable.notification_icon_background
        }
        remoteViews.setInt(R.id.notification_icon_container, "setBackgroundResource", iconBackgroundRes)
        
        // Setup button listeners
        setupButtonClickListeners(remoteViews, medicine.id, context)
        
        return remoteViews
    }
    
    /**
     * Creates an expanded RemoteViews with additional information
     */
    fun createExpandedNotificationView(
        context: Context,
        medicine: Medicine,
        isOverdue: Boolean = false
    ): RemoteViews {
        val remoteViews = createMedicineNotificationView(context, medicine, isOverdue)
        
        // Show additional info
        remoteViews.setViewVisibility(R.id.additional_info_container, android.view.View.VISIBLE)
        
        return remoteViews
    }
    
    /**
     * Sets up click listeners for notification buttons
     */
    private fun setupButtonClickListeners(remoteViews: RemoteViews, medicineId: Long, context: Context) {
        // Create PendingIntents for button actions
        fun createButtonIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, NotificationButtonReceiver::class.java).apply {
                this.action = action
                putExtra("medicine_id", medicineId)
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        // Set click listeners for buttons
        remoteViews.setOnClickPendingIntent(
            R.id.button_taken,
            createButtonIntent(NotificationButtonReceiver.ACTION_BUTTON_TAKEN, 100)
        )
        
        remoteViews.setOnClickPendingIntent(
            R.id.button_snooze,
            createButtonIntent(NotificationButtonReceiver.ACTION_BUTTON_SNOOZE, 101)
        )
        
        remoteViews.setOnClickPendingIntent(
            R.id.button_skip,
            createButtonIntent(NotificationButtonReceiver.ACTION_BUTTON_SKIP, 102)
        )
    }
    
    /**
     * Updates the notification view with new medicine data
     */
    fun updateNotificationView(
        remoteViews: RemoteViews,
        medicine: Medicine,
        isOverdue: Boolean = false
    ) {
        val title = if (isOverdue)
            remoteViews.apply {}.let { // no-op to keep style
                // We don't have Context here; callers should rebuild via factory methods when locale changes
                "ПРОСРОЧЕНО: ${medicine.name}"
            }
        else
            "Примите: ${medicine.name}"
        remoteViews.setTextViewText(R.id.notification_title, title)
        
        val status = if (isOverdue) "ПРОСРОЧЕНО" else "Активно"
        remoteViews.setTextViewText(R.id.notification_status, status)
        
        remoteViews.setTextViewText(R.id.medicine_name, medicine.name)
        remoteViews.setTextViewText(R.id.medicine_dosage, "Дозировка: ${medicine.dosage}")
        val timeText2 = medicine.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        remoteViews.setTextViewText(R.id.medicine_time, "Время: ${timeText2}")
        remoteViews.setTextViewText(R.id.medicine_remaining, "Остаток: ${medicine.remainingQuantity} шт.")
        
        val groupInfo2 = if (medicine.groupName.isNotEmpty()) {
            "Группа: ${medicine.groupName}"
        } else {
            "Группа: не указана"
        }
        remoteViews.setTextViewText(R.id.medicine_group, groupInfo2)
    }
} 