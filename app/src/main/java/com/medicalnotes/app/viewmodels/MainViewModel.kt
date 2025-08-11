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
import com.medicalnotes.app.utils.GroupFixer
import com.medicalnotes.app.utils.StatusManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val medicineRepository = MedicineRepository(application)
    private val customButtonRepository = CustomButtonRepository(application)
    
    private val _todayMedicines = MutableLiveData<List<Medicine>>()
    val todayMedicines: LiveData<List<Medicine>> = _todayMedicines
    
    private val _allMedicines = MutableLiveData<List<Medicine>>()
    val allMedicines: LiveData<List<Medicine>> = _allMedicines
    
    private val _customButtons = MutableLiveData<List<CustomButton>>()
    val customButtons: LiveData<List<CustomButton>> = _customButtons
    
    /**
     * УНИВЕРСАЛЬНЫЙ МЕТОД: Загружает лекарства для указанной даты
     * Используется как при загрузке приложения, так и при клике по календарю
     */
    fun loadMedicinesForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainViewModel", "🚀 === НАЧАЛО loadMedicinesForDate($date) ===")
                
                val allMedicines = medicineRepository.getAllMedicines()
                android.util.Log.d("MainViewModel", "Загружено всех лекарств: ${allMedicines.size}")

                // Применяем исправления групповых данных
                android.util.Log.d("MainViewModel", "Применяем GroupFixer для валидации групп...")
                val fixedMedicines = GroupFixer.fixGroupInconsistencies(allMedicines)

                // Если обнаружены изменения групповых полей — сохраняем
                var groupChanges = false
                for (i in allMedicines.indices) {
                    val original = allMedicines[i]
                    val fixed = fixedMedicines[i]
                    if (original.groupId != fixed.groupId || original.groupStartDate != fixed.groupStartDate) {
                        groupChanges = true
                        break
                    }
                }

                val medicinesForCalculation: List<com.medicalnotes.app.models.Medicine> = if (groupChanges) {
                    android.util.Log.d("MainViewModel", "Обнаружены изменения групп — сохраняем исправленные данные")
                    fixedMedicines.forEach { medicineRepository.updateMedicine(it) }
                    val reloaded = medicineRepository.getAllMedicines()
                    android.util.Log.d("MainViewModel", "Повторно загружено лекарств после сохранения: ${reloaded.size}")
                    reloaded
                } else {
                    android.util.Log.d("MainViewModel", "Изменений групп не обнаружено")
                    allMedicines
                }

                // Получаем активные лекарства для указанной даты (как в календаре)
                val activeMedicines = DosageCalculator.getActiveMedicinesForDateForCalendar(medicinesForCalculation, date)
                android.util.Log.d("MainViewModel", "Активных лекарств для $date: ${activeMedicines.size}")

                // Обновляем UI в главном потоке
                withContext(Dispatchers.Main) {
                    _todayMedicines.value = activeMedicines
                    android.util.Log.d("MainViewModel", "UI обновлен для даты $date: ${activeMedicines.size} лекарств")
                }

            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Ошибка при загрузке лекарств для даты $date", e)
                withContext(Dispatchers.Main) {
                    _todayMedicines.value = emptyList()
                }
            }
        }
    }
    
    fun loadTodayMedicines() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainViewModel", "🚀 === НАЧАЛО loadTodayMedicines() ===")
                android.util.Log.d("MainViewModel", "Текущий поток: ${Thread.currentThread().name}")
                
                val allMedicines = medicineRepository.getAllMedicines()
                android.util.Log.d("MainViewModel", "Загружено всех лекарств: ${allMedicines.size}")

                // Применяем исправления групповых данных
                android.util.Log.d("MainViewModel", "Применяем GroupFixer...")
                val fixedMedicines = GroupFixer.fixGroupInconsistencies(allMedicines)
                
                // Проверяем, были ли изменения в групповых данных
                android.util.Log.d("MainViewModel", "=== ПРОВЕРКА ИЗМЕНЕНИЙ ГРУПП ===")
                var groupChanges = false
                for (i in allMedicines.indices) {
                    val original = allMedicines[i]
                    val fixed = fixedMedicines[i]
                    if (original.groupId != fixed.groupId || original.groupStartDate != fixed.groupStartDate) {
                        android.util.Log.d("MainViewModel", "ИЗМЕНЕНИЕ ГРУППЫ: ${original.name}")
                        android.util.Log.d("MainViewModel", "  - groupId: ${original.groupId} -> ${fixed.groupId}")
                        android.util.Log.d("MainViewModel", "  - groupStartDate: ${original.groupStartDate} -> ${fixed.groupStartDate}")
                        groupChanges = true
                    }
                }

                if (groupChanges) {
                    android.util.Log.d("MainViewModel", "Обнаружены изменения групп, сохраняем исправленные данные...")
                    fixedMedicines.forEach { medicine ->
                        medicineRepository.updateMedicine(medicine)
                    }
                    android.util.Log.d("MainViewModel", "Исправленные групповые данные сохранены")
                } else {
                    android.util.Log.d("MainViewModel", "Изменений групп не обнаружено")
                }

                // Получаем обновленные данные после исправлений
                val updatedMedicines = medicineRepository.getAllMedicines()
                android.util.Log.d("MainViewModel", "Обновленных лекарств после исправлений: ${updatedMedicines.size}")

                // Получаем активные лекарства для сегодня
                val today = LocalDate.now()
                android.util.Log.d("MainViewModel", "Сегодняшняя дата: $today")
                
                // ИСПРАВЛЕНО: Для экрана «Лекарства на сегодня» показываем только те,
                // которые нужно принять сегодня И ещё не приняты (takenToday = false)
                val medicinesForToday = DosageCalculator.getActiveMedicinesForDate(updatedMedicines, today)
                android.util.Log.d("MainViewModel", "Активных (не принятых) лекарств на сегодня: ${medicinesForToday.size}")
                android.util.Log.d("MainViewModel", "Лекарств для отображения сегодня: ${medicinesForToday.size}")

                // Логируем каждое лекарство для отладки
                medicinesForToday.forEach { medicine ->
                    android.util.Log.d("MainViewModel", "ЛЕКАРСТВО ДЛЯ СЕГОДНЯ: ${medicine.name}")
                    android.util.Log.d("MainViewModel", "  - groupId: ${medicine.groupId}")
                    android.util.Log.d("MainViewModel", "  - groupStartDate: ${medicine.groupStartDate}")
                    android.util.Log.d("MainViewModel", "  - groupOrder: ${medicine.groupOrder}")
                    android.util.Log.d("MainViewModel", "  - takenToday: ${medicine.takenToday}")
                    android.util.Log.d("MainViewModel", "  - isOverdue: ${DosageCalculator.isMedicineOverdue(medicine)}")
                }

                // Обновляем UI в главном потоке
                withContext(Dispatchers.Main) {
                    android.util.Log.d("MainViewModel", "=== ОБНОВЛЕНИЕ UI ===")
                    android.util.Log.d("MainViewModel", "Лекарств для отображения: ${medicinesForToday.size}")
                    android.util.Log.d("MainViewModel", "Предыдущее значение LiveData: ${_todayMedicines.value?.size ?: 0} лекарств")
                    
                    _todayMedicines.value = medicinesForToday
                    
                    android.util.Log.d("MainViewModel", "Новое значение LiveData: ${_todayMedicines.value?.size ?: 0} лекарств")
                    android.util.Log.d("MainViewModel", "UI обновлен: ${medicinesForToday.size} лекарств")
                    
                    // Дополнительная проверка
                    medicinesForToday.forEach { medicine ->
                        android.util.Log.d("MainViewModel", "ОТОБРАЖАЕТСЯ: ${medicine.name} (takenToday: ${medicine.takenToday})")
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Ошибка при загрузке лекарств на сегодня", e)
                withContext(Dispatchers.Main) {
                    _todayMedicines.value = emptyList()
                }
            }
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
    
    /**
     * ИСПРАВЛЕНО: Используем StatusManager для отметки лекарства как принятого
     */
    fun markMedicineAsTaken(medicineId: Long) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "=== НАЧАЛО markMedicineAsTaken ===")
            android.util.Log.d("MainViewModel", "ID лекарства: $medicineId")
            
            // Получаем лекарство
            val medicine = medicineRepository.getMedicineById(medicineId)
            if (medicine != null) {
                // Используем StatusManager для отметки
                val updatedMedicine = StatusManager.markAsTaken(medicine)
                medicineRepository.updateMedicine(updatedMedicine)
                android.util.Log.d("MainViewModel", "Лекарство отмечено как принятое через StatusManager")
            }
            
            // Принудительно перезагружаем лекарства на сегодня
            loadMedicinesForDate(LocalDate.now())
            android.util.Log.d("MainViewModel", "Лекарства на сегодня перезагружены")
        }
    }
    
    fun markMedicineAsSkipped(medicineId: Long) {
        viewModelScope.launch {
            medicineRepository.markMedicineAsSkipped(medicineId)
            loadMedicinesForDate(LocalDate.now())
        }
    }
    
    fun markAllMedicinesAsTaken() {
        viewModelScope.launch {
            val todayMedicines = _todayMedicines.value ?: return@launch
            todayMedicines.forEach { medicine ->
                medicineRepository.markMedicineAsTaken(medicine.id)
            }
            loadMedicinesForDate(LocalDate.now())
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
                    android.util.Log.d("MainViewModel", "Вызываем loadMedicinesForDate")
                    loadMedicinesForDate(LocalDate.now())
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
            try {
                android.util.Log.d("MainViewModel", "=== ОБНОВЛЕНИЕ ЛЕКАРСТВА ===")
                android.util.Log.d("MainViewModel", "Лекарство: ${medicine.name}")
                android.util.Log.d("MainViewModel", "ID: ${medicine.id}")
                android.util.Log.d("MainViewModel", "takenToday: ${medicine.takenToday}")
                android.util.Log.d("MainViewModel", "lastTakenTime: ${medicine.lastTakenTime}")
                
                // Обновляем лекарство в репозитории
                val success = medicineRepository.updateMedicine(medicine)
                android.util.Log.d("MainViewModel", "Результат обновления в репозитории: $success")
                
                if (success) {
                    android.util.Log.d("MainViewModel", "✓ Лекарство успешно обновлено в репозитории")
                    
                    // Перезагружаем списки
                    android.util.Log.d("MainViewModel", "Перезагружаем список лекарств на сегодня...")
                    loadMedicinesForDate(LocalDate.now())
                    android.util.Log.d("MainViewModel", "✓ Список лекарств на сегодня перезагружен")
                    
                    android.util.Log.d("MainViewModel", "Перезагружаем полный список лекарств...")
                    loadAllMedicines()
                    android.util.Log.d("MainViewModel", "✓ Полный список лекарств перезагружен")
                    
                    // Проверяем, что лекарство действительно исчезло из списка на сегодня
                    val currentTodayMedicines = _todayMedicines.value ?: emptyList()
                    val medicineStillInList = currentTodayMedicines.any { it.id == medicine.id }
                    
                    if (medicineStillInList && medicine.takenToday) {
                        android.util.Log.w("MainViewModel", "⚠️ Лекарство все еще в списке на сегодня, хотя takenToday = true")
                        android.util.Log.w("MainViewModel", "Принудительно перезагружаем список...")
                        loadMedicinesForDate(LocalDate.now())
                    } else {
                        android.util.Log.d("MainViewModel", "✓ Лекарство корректно обработано")
                    }
                } else {
                    android.util.Log.e("MainViewModel", "❌ Ошибка обновления лекарства в репозитории")
                }
                
                android.util.Log.d("MainViewModel", "=== ОБНОВЛЕНИЕ ЛЕКАРСТВА ЗАВЕРШЕНО ===")
                
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "❌ Исключение при обновлении лекарства", e)
            }
        }
    }
} 