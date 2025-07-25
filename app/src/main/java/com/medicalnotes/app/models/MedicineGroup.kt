package com.medicalnotes.app.models

data class MedicineGroup(
    val id: Long = 0,
    val name: String,
    val frequency: DosageFrequency,
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 