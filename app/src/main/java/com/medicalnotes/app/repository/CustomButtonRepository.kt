package com.medicalnotes.app.repository

import android.content.Context
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.utils.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomButtonRepository(context: Context) {
    
    private val dataManager = DataManager(context)
    
    suspend fun getAllButtons(): List<CustomButton> = withContext(Dispatchers.IO) {
        return@withContext dataManager.loadCustomButtons()
    }
    
    suspend fun getButtonById(id: Long): CustomButton? = withContext(Dispatchers.IO) {
        return@withContext dataManager.loadCustomButtons().find { it.id == id }
    }
    
    suspend fun insertButton(button: CustomButton): Long = withContext(Dispatchers.IO) {
        val success = dataManager.addCustomButton(button)
        return@withContext if (success) button.id else -1L
    }
    
    suspend fun insertButtons(buttons: List<CustomButton>) = withContext(Dispatchers.IO) {
        dataManager.saveCustomButtons(buttons)
    }
    
    suspend fun updateButton(button: CustomButton) = withContext(Dispatchers.IO) {
        dataManager.updateCustomButton(button)
    }
    
    suspend fun deleteButton(buttonId: Long) = withContext(Dispatchers.IO) {
        dataManager.deleteCustomButton(buttonId)
    }
    
    suspend fun updateButtonOrder(buttons: List<CustomButton>) = withContext(Dispatchers.IO) {
        buttons.forEachIndexed { index, button ->
            dataManager.updateCustomButton(button.copy(order = index))
        }
    }
} 