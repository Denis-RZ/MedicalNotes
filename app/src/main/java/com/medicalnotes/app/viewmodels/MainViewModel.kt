package com.medicalnotes.app.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medicalnotes.app.models.CustomButton
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.MedicineRepository
import com.medicalnotes.app.repository.CustomButtonRepository
import com.medicalnotes.app.utils.DosageCalculator
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val medicineRepository = MedicineRepository(application)
    private val customButtonRepository = CustomButtonRepository(application)
    
    private val _todayMedicines = MutableLiveData<List<Medicine>>()
    val todayMedicines: LiveData<List<Medicine>> = _todayMedicines
    
    private val _allMedicines = MutableLiveData<List<Medicine>>()
    val allMedicines: LiveData<List<Medicine>> = _allMedicines
    
    private val _customButtons = MutableLiveData<List<CustomButton>>()
    val customButtons: LiveData<List<CustomButton>> = _customButtons
    
    fun loadTodayMedicines() {
        viewModelScope.launch {
            val allMedicines = medicineRepository.getAllMedicines()
            android.util.Log.d("MainViewModel", "=== ЗАГРУЗКА ЛЕКАРСТВ НА СЕГОДНЯ ===")
            android.util.Log.d("MainViewModel", "Всего лекарств в базе: ${allMedicines.size}")
            
            allMedicines.forEach { medicine ->
                android.util.Log.d("MainViewModel", "Лекарство: ${medicine.name}")
                android.util.Log.d("MainViewModel", "  - Активно: ${medicine.isActive}")
                android.util.Log.d("MainViewModel", "  - Частота: ${medicine.frequency}")
                android.util.Log.d("MainViewModel", "  - Дата начала: ${medicine.startDate}")
                android.util.Log.d("MainViewModel", "  - Последний прием: ${medicine.lastTakenTime}")
            }
            
            val today = LocalDate.now()
            android.util.Log.d("MainViewModel", "Сегодняшняя дата: $today")
            
            android.util.Log.d("MainViewModel", "=== ВЫЗОВ DosageCalculator.getActiveMedicinesForDate ===")
            val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
            android.util.Log.d("MainViewModel", "Лекарств на сегодня: ${todayMedicines.size}")
            
            //  ДОБАВЛЕНО: Подробное логирование для отладки
            if (todayMedicines.isEmpty()) {
                android.util.Log.d("MainViewModel", " СПИСОК ПУСТОЙ - проверяем каждое лекарство:")
                allMedicines.forEach { medicine ->
                    android.util.Log.d("MainViewModel", "Проверяем: ${medicine.name}")
                    android.util.Log.d("MainViewModel", "  - isActive: ${medicine.isActive}")
                    android.util.Log.d("MainViewModel", "  - shouldTakeMedicine: ${DosageCalculator.shouldTakeMedicine(medicine, today)}")
                    android.util.Log.d("MainViewModel", "  - takenToday: ${medicine.takenToday}")
                    android.util.Log.d("MainViewModel", "  - lastTakenTime: ${medicine.lastTakenTime}")
                    android.util.Log.d("MainViewModel", "  - startDate: ${medicine.startDate}")
                    android.util.Log.d("MainViewModel", "  - frequency: ${medicine.frequency}")
                }
            }
            
            todayMedicines.forEach { medicine ->
                android.util.Log.d("MainViewModel", "Сегодня: ${medicine.name}")
                android.util.Log.d("MainViewModel", "  - Время: ${medicine.time}")
                android.util.Log.d("MainViewModel", "  - Принято сегодня: ${medicine.takenToday}")
                android.util.Log.d("MainViewModel", "  - Дата начала: ${medicine.startDate}")
            }
            
            _todayMedicines.value = todayMedicines
        }
    }
    
    fun loadAllMedicines() {
        viewModelScope.launch {
            val medicines = medicineRepository.getAllMedicines()
            _allMedicines.value = medicines
        }
    }
    
    fun loadCustomButtons() {
        viewModelScope.launch {
            val buttons = customButtonRepository.getAllButtons()
            _customButtons.value = buttons.sortedBy { it.order }
        }
    }
    
    fun markMedicineAsTaken(medicineId: Long) {
        viewModelScope.launch {
            medicineRepository.markMedicineAsTaken(medicineId)
            loadTodayMedicines()
        }
    }
    
    fun markMedicineAsSkipped(medicineId: Long) {
        viewModelScope.launch {
            medicineRepository.markMedicineAsSkipped(medicineId)
            loadTodayMedicines()
        }
    }
    
    fun markAllMedicinesAsTaken() {
        viewModelScope.launch {
            val todayMedicines = _todayMedicines.value ?: return@launch
            todayMedicines.forEach { medicine ->
                medicineRepository.markMedicineAsTaken(medicine.id)
            }
            loadTodayMedicines()
        }
    }
    
    fun insertDefaultButtons(buttons: List<CustomButton>) {
        viewModelScope.launch {
            customButtonRepository.insertButtons(buttons)
            loadCustomButtons()
        }
    }
    
    fun updateButton(button: CustomButton) {
        viewModelScope.launch {
            customButtonRepository.updateButton(button)
            loadCustomButtons()
        }
    }
    
    fun deleteButton(buttonId: Long) {
        viewModelScope.launch {
            customButtonRepository.deleteButton(buttonId)
            loadCustomButtons()
        }
    }
    
    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch {
            medicineRepository.deleteMedicine(medicineId)
            loadTodayMedicines()
            loadAllMedicines()
        }
    }
    
    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Обновление лекарства: ${medicine.name}")
            android.util.Log.d("MainViewModel", "Статус принятия: lastTakenTime=${medicine.lastTakenTime}, takenToday=${medicine.takenToday}")
            
            medicineRepository.updateMedicine(medicine)
            loadTodayMedicines()
            loadAllMedicines()
        }
    }
} 