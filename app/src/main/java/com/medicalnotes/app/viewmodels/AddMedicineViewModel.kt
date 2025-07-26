package com.medicalnotes.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.MedicineRepository
import kotlinx.coroutines.launch

class AddMedicineViewModel(application: Application) : AndroidViewModel(application) {
    
    private val medicineRepository = MedicineRepository(application)
    
    suspend fun insertMedicine(medicine: Medicine): Long {
        return medicineRepository.insertMedicine(medicine)
    }
    
    suspend fun updateMedicine(medicine: Medicine): Boolean {
        return medicineRepository.updateMedicine(medicine)
    }
    
    fun getMedicineById(id: Long, callback: (Medicine?) -> Unit) {
        viewModelScope.launch {
            android.util.Log.d("AddMedicineViewModel", "Getting medicine by ID: $id")
            val medicine = medicineRepository.getMedicineById(id)
            android.util.Log.d("AddMedicineViewModel", "Medicine found: ${medicine?.name ?: "null"}")
            callback(medicine)
        }
    }
    
    fun getAllMedicines(callback: (List<Medicine>) -> Unit) {
        viewModelScope.launch {
            android.util.Log.d("AddMedicineViewModel", "Getting all medicines")
            val medicines = medicineRepository.getAllMedicines()
            android.util.Log.d("AddMedicineViewModel", "Found ${medicines.size} medicines")
            callback(medicines)
        }
    }
    
    suspend fun deleteMedicine(medicine: Medicine): Boolean {
        return medicineRepository.deleteMedicine(medicine.id)
    }
    
    suspend fun addMedicine(medicine: Medicine): Boolean {
        return try {
            val id = medicineRepository.insertMedicine(medicine)
            id > 0
        } catch (e: Exception) {
            android.util.Log.e("AddMedicineViewModel", "Error adding medicine", e)
            false
        }
    }
} 