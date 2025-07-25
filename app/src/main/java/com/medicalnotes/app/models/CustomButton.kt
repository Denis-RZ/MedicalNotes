package com.medicalnotes.app.models

data class CustomButton(
    val id: Long = 0,
    val name: String,
    val action: ButtonAction,
    val size: ButtonSize = ButtonSize.MEDIUM,
    val color: ButtonColor = ButtonColor.PRIMARY,
    val isVisible: Boolean = true,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ButtonAction {
    TAKE_MEDICINE,
    SKIP_MEDICINE,
    ADD_MEDICINE,
    CHECK_SUPPLY,
    EMERGENCY_CALL,
    SETTINGS,
    INSULIN_REMINDER,
    BLOOD_SUGAR_RECORD
}

enum class ButtonSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

enum class ButtonColor {
    PRIMARY,
    SECONDARY,
    SUCCESS,
    WARNING,
    DANGER,
    HIGH_CONTRAST
} 