package com.medicalnotes.app.models

import com.medicalnotes.app.utils.LanguageManager

data class UserPreferences(
    val id: Int = 1,
    val buttonSize: ButtonSize = ButtonSize.LARGE,
    val buttonColor: ButtonColor = ButtonColor.PRIMARY,
    val useHighContrast: Boolean = false,
    val enableVibration: Boolean = true,
    val enableSound: Boolean = true,
    val emergencyContact: String = "",
    val isDiabetic: Boolean = true,
    val bloodSugarTarget: Int = 120,
    val bloodSugarUnit: String = "mg/dL",
    val isElderlyMode: Boolean = false,
    val largeTextEnabled: Boolean = false,
    val sonPhoneNumber: String = "",
    val emergencyPhoneNumber: String = "103",
    val autoBackupEnabled: Boolean = false,
    val voiceRemindersEnabled: Boolean = false,
    val language: LanguageManager.Language = LanguageManager.Language.ENGLISH,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 