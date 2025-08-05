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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            android.util.Log.d("MainViewModel", "=== НАЧАЛО loadTodayMedicines() С ВАЛИДАЦИЕЙ ГРУПП ===")
            
            val allMedicines = medicineRepository.getAllMedicines()
            android.util.Log.d("MainViewModel", "=== ЗАГРУЗКА ЛЕКАРСТВ НА СЕГОДНЯ ===")
            android.util.Log.d("MainViewModel", "Всего лекарств в базе: ${allMedicines.size}")
            
            val today = com.medicalnotes.app.utils.DateUtils.getCurrentDate()
            android.util.Log.d("MainViewModel", "Сегодняшняя дата: $today")
            
            // ВАЛИДАЦИЯ И ИСПРАВЛЕНИЕ ГРУПП
            android.util.Log.d("MainViewModel", "=== ВАЛИДАЦИЯ ГРУПП ===")
            val groupIds = allMedicines.mapNotNull { it.groupId }.distinct()
            android.util.Log.d("MainViewModel", "Найдено групп: ${groupIds.size}")
            
            groupIds.forEach { groupId ->
                android.util.Log.d("MainViewModel", "Проверяем группу $groupId")
                val groupMedicines = allMedicines.filter { it.groupId == groupId }
                android.util.Log.d("MainViewModel", "  - Лекарств в группе: ${groupMedicines.size}")
                
                // Проверяем валидность группы
                val firstMedicine = groupMedicines.first()
                val isValid = firstMedicine.isGroupConsistent(groupMedicines)
                android.util.Log.d("MainViewModel", "  - Группа валидна: $isValid")
                
                if (!isValid) {
                    android.util.Log.w("MainViewModel", "  - Группа $groupId невалидна, исправляем...")
                    // Здесь можно добавить вызов DataManager для исправления группы
                    // dataManager.fixGroupInconsistencies(groupId)
                }
            }
            
            allMedicines.forEach { medicine ->
                android.util.Log.d("MainViewModel", "Лекарство: ${medicine.name}")
                android.util.Log.d("MainViewModel", "  - Активно: ${medicine.isActive}")
                android.util.Log.d("MainViewModel", "  - Частота: ${medicine.frequency}")
                android.util.Log.d("MainViewModel", "  - Дата начала: ${medicine.startDate}")
                android.util.Log.d("MainViewModel", "  - Последний прием: ${medicine.lastTakenTime}")
                android.util.Log.d("MainViewModel", "  - takenToday: ${medicine.takenToday}")
                android.util.Log.d("MainViewModel", "  - groupId: ${medicine.groupId}")
                android.util.Log.d("MainViewModel", "  - groupName: ${medicine.groupName}")
                android.util.Log.d("MainViewModel", "  - groupOrder: ${medicine.groupOrder}")
                android.util.Log.d("MainViewModel", "  - groupStartDate: ${medicine.groupStartDate}")
                android.util.Log.d("MainViewModel", "  - groupFrequency: ${medicine.groupFrequency}")
                android.util.Log.d("MainViewModel", "  - isValidGroup(): ${medicine.isValidGroup()}")
            }
            
            android.util.Log.d("MainViewModel", "=== ВЫЗОВ DosageCalculator.getActiveMedicinesForDate ===")
            val todayMedicines = DosageCalculator.getActiveMedicinesForDate(allMedicines, today)
            android.util.Log.d("MainViewModel", "Лекарств на сегодня: ${todayMedicines.size}")
            
            //  ДОБАВЛЕНО: Подробное логирование для отладки
            if (todayMedicines.isEmpty()) {
                android.util.Log.d("MainViewModel", " СПИСОК ПУСТОЙ - проверяем каждое лекарство:")
                allMedicines.forEach { medicine ->
                    android.util.Log.d("MainViewModel", "Проверяем: ${medicine.name}")
                    android.util.Log.d("MainViewModel", "  - isActive: ${medicine.isActive}")
                    android.util.Log.d("MainViewModel", "  - shouldTakeMedicine: ${DosageCalculator.shouldTakeMedicine(medicine, today, allMedicines)}")
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
            
            android.util.Log.d("MainViewModel", "=== УСТАНОВКА ЗНАЧЕНИЯ В LiveData ===")
            android.util.Log.d("MainViewModel", "Устанавливаем значение: ${todayMedicines.size} лекарств")
            _todayMedicines.postValue(todayMedicines)
            android.util.Log.d("MainViewModel", "=== ЗНАЧЕНИЕ УСТАНОВЛЕНО ===")
        }
    }
    
    fun loadAllMedicines() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val medicines = medicineRepository.getAllMedicines()
            _allMedicines.postValue(medicines)
        }
    }
    
    fun loadCustomButtons() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val buttons = customButtonRepository.getAllButtons()
            _customButtons.postValue(buttons.sortedBy { it.order })
        }
    }
    
    fun markMedicineAsTaken(medicineId: Long) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "=== НАЧАЛО markMedicineAsTaken ===")
            android.util.Log.d("MainViewModel", "ID лекарства: $medicineId")
            
            // Принудительно очищаем кэш перед обновлением
            _todayMedicines.postValue(emptyList())
            android.util.Log.d("MainViewModel", "Кэш очищен")
            
            // Обновляем данные
            medicineRepository.markMedicineAsTaken(medicineId)
            android.util.Log.d("MainViewModel", "Данные обновлены в repository")
            
            // Принудительно перезагружаем лекарства на сегодня
            loadTodayMedicines()
            android.util.Log.d("MainViewModel", "Лекарства на сегодня перезагружены")
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
        android.util.Log.d("MainViewModel", "=== НАЧАЛО УДАЛЕНИЯ ЛЕКАРСТВА ===")
        android.util.Log.d("MainViewModel", "ID лекарства для удаления: $medicineId")
        viewModelScope.launch {
            try {
                android.util.Log.d("MainViewModel", "Вызываем medicineRepository.deleteMedicine")
                val success = medicineRepository.deleteMedicine(medicineId)
                android.util.Log.d("MainViewModel", "Результат удаления из repository: $success")
                
                if (success) {
                    android.util.Log.d("MainViewModel", "Лекарство успешно удалено из файла, обновляем списки")
                    android.util.Log.d("MainViewModel", "Вызываем loadTodayMedicines")
                    loadTodayMedicines()
                    android.util.Log.d("MainViewModel", "Вызываем loadAllMedicines")
                    loadAllMedicines()
                    android.util.Log.d("MainViewModel", "Списки обновлены")
                } else {
                    android.util.Log.e("MainViewModel", "Ошибка при удалении лекарства с ID: $medicineId - repository вернул false")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Исключение при удалении лекарства", e)
            }
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