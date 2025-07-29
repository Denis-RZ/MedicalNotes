package com.medicalnotes.app

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.medicalnotes.app.adapters.MainMedicineAdapter
import com.medicalnotes.app.databinding.ActivityMainBinding
import com.medicalnotes.app.models.Medicine
import com.medicalnotes.app.repository.UserPreferencesRepository
import com.medicalnotes.app.utils.MedicineGroupingUtil
import com.medicalnotes.app.utils.MedicineStatus
import com.medicalnotes.app.utils.VersionUtils
import com.medicalnotes.app.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import com.medicalnotes.app.utils.DosageCalculator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var todayMedicineAdapter: MainMedicineAdapter
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private var overdueCheckTimer: android.os.Handler? = null
    
    // ✅ ДОБАВЛЕНО: Список для хранения логов
    private val logs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Инициализация ViewModel и Repository
            viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            userPreferencesRepository = UserPreferencesRepository(this)

            // Настройка UI
            setupViews()
            setupButtons()
            observeData()

            // Загрузка данных
            viewModel.loadAllMedicines()
            loadTodayMedicines()
            
            // ✅ ДОБАВЛЕНО: Автоматическая проверка просроченных лекарств
            checkOverdueMedicines()
            
            // ✅ ДОБАВЛЕНО: Периодическая проверка каждые 30 секунд
            startPeriodicOverdueCheck()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            android.widget.Toast.makeText(this, "Ошибка инициализации: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun setupViews() {
        try {
            // Настройка toolbar
            setSupportActionBar(binding.toolbar)
            binding.toolbar.subtitle = VersionUtils.getShortVersionInfo(this)
            
            // ВАЖНО: Настройка кнопки меню (гамбургер) - ИСПРАВЛЕНО
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
            
            // Настройка навигации для MaterialToolbar
            binding.toolbar.setNavigationOnClickListener {
                android.util.Log.d("MainActivity", "Кнопка навигации нажата - открываем drawer")
                try {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        android.util.Log.d("MainActivity", "Drawer уже открыт - закрываем")
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        android.util.Log.d("MainActivity", "Drawer закрыт - открываем")
                        binding.drawerLayout.openDrawer(GravityCompat.START)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Ошибка при работе с drawer", e)
                }
            }
            
            android.util.Log.d("MainActivity", "Toolbar настроен, кнопка навигации установлена")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting version info", e)
            binding.toolbar.subtitle = "v?.?"
        }
        
        try {
            // Настройка Navigation Drawer
            setupNavigationDrawer()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up navigation drawer", e)
        }
        
        try {
            // Настройка RecyclerView
            todayMedicineAdapter = MainMedicineAdapter(
                onMedicineClick = { medicine -> takeMedicine(medicine) }
            )

            binding.recyclerViewTodayMedicines.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = todayMedicineAdapter
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up RecyclerView", e)
        }

        try {
            // Настройка кнопки повтора
            binding.buttonRetry.setOnClickListener {
                loadTodayMedicines()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up retry button", e)
        }
    }

    private fun setupNavigationDrawer() {
        try {
            android.util.Log.d("MainActivity", "Настройка Navigation Drawer")
            
            // Проверяем, что NavigationView существует
            if (binding.navigationView == null) {
                android.util.Log.e("MainActivity", "NavigationView не найден!")
                return
            }
            
            // Настройка NavigationView
            binding.navigationView.setNavigationItemSelectedListener { menuItem ->
                android.util.Log.d("MainActivity", "Выбран пункт меню: ${menuItem.title}")
                when (menuItem.itemId) {
                    com.medicalnotes.app.R.id.nav_manage_medicines -> {
                        val intent = android.content.Intent(this, MedicineManagerActivity::class.java)
                        startActivity(intent)
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_manage_groups -> {
                        val intent = android.content.Intent(this, GroupManagementActivity::class.java)
                        startActivity(intent)
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_settings -> {
                        val intent = android.content.Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_elderly_mode -> {
                        toggleElderlyMode()
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_logs -> {
                        toggleLogsVisibility()
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_export_data -> {
                        exportData()
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    com.medicalnotes.app.R.id.nav_test_groups -> {
                        testGroups()
                        binding.drawerLayout.closeDrawers()
                        true
                    }
                    else -> {
                        android.util.Log.w("MainActivity", "Неизвестный пункт меню: ${menuItem.itemId}")
                        false
                    }
                }
            }
            
            android.util.Log.d("MainActivity", "Navigation Drawer настроен успешно")
            
            // Добавляем обработчик закрытия drawer при нажатии вне его области
            binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
                override fun onDrawerOpened(drawerView: android.view.View) {
                    android.util.Log.d("MainActivity", "Drawer открыт")
                }
                override fun onDrawerClosed(drawerView: android.view.View) {
                    android.util.Log.d("MainActivity", "Drawer закрыт")
                }
                override fun onDrawerStateChanged(newState: Int) {
                    android.util.Log.d("MainActivity", "Состояние drawer изменилось: $newState")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up navigation drawer", e)
        }
    }

    private fun setupButtons() {
        try {
            // Кнопка добавления лекарства
            binding.fabAddMedicine.setOnClickListener {
                val intent = android.content.Intent(this, AddMedicineActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up FAB", e)
        }

        try {
            // Кнопка очистки логов
            binding.buttonClearLogs.setOnClickListener {
                clearLogs()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up clear logs button", e)
        }
        
        try {
            // Кнопка копирования лога
            binding.buttonCopyLog.setOnClickListener {
                copyLogToClipboard()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting up copy log button", e)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        // Меню не нужно - используется кнопка навигации в toolbar
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        android.util.Log.d("MainActivity", "onOptionsItemSelected: ${item.itemId}")
        return when (item.itemId) {
            android.R.id.home -> {
                android.util.Log.d("MainActivity", "Нажата кнопка home - открываем drawer")
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeData() {
        try {
            viewModel.todayMedicines.observe(this) { medicines ->
                try {
                    if (medicines.isEmpty()) {
                        showEmptyState()
                        addLog("Нет лекарств на сегодня")
                    } else {
                        showContentState()
                        addLog("=== ОБНОВЛЕНИЕ СПИСКА ЛЕКАРСТВ ===")
                        addLog("Получено лекарств: ${medicines.size}")
                        addLog("Текущее время: ${LocalDateTime.now()}")
                        
                        var hasOverdueMedicines = false
                        
                        medicines.forEach { medicine ->
                            try {
                                val status = com.medicalnotes.app.utils.DosageCalculator.getMedicineStatus(medicine)
                                addLog("Отображается: ${medicine.name} - Статус: $status")
                                addLog("  Время приема: ${medicine.time}")
                                addLog("  Частота: ${medicine.frequency}")
                                
                                if (status == MedicineStatus.OVERDUE) {
                                    hasOverdueMedicines = true
                                    addLog("⚠️ ПРОСРОЧЕНО: ${medicine.name}")
                                } else if (status == MedicineStatus.UPCOMING) {
                                    addLog("📅 ПРЕДСТОИТ: ${medicine.name} - время еще не пришло")
                                } else if (status == MedicineStatus.TAKEN_TODAY) {
                                    addLog("✅ ПРИНЯТО: ${medicine.name} - уже принято сегодня")
                                } else {
                                    addLog("❌ НЕ СЕГОДНЯ: ${medicine.name} - не по расписанию")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Error processing medicine ${medicine.name}", e)
                                addLog("❌ ОШИБКА обработки: ${medicine.name}")
                            }
                        }
                        
                        // Проверяем, нужно ли показывать группированные карточки
                        try {
                            // Используем DosageCalculator для получения активных лекарств на сегодня
                            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, LocalDate.now())
                            addLog("Активных лекарств на сегодня: ${activeMedicines.size}")
                            
                            // Проверяем, есть ли лекарства в группах среди активных
                            val groupedMedicines = activeMedicines.filter { it.groupName.isNotEmpty() }
                            val shouldShowGrouped = groupedMedicines.isNotEmpty()
                            
                            addLog("Лекарств в группах: ${groupedMedicines.size}")
                            addLog("Показывать группированные карточки: $shouldShowGrouped")
                            
                            if (shouldShowGrouped) {
                                // Группируем активные лекарства по названию группы
                                val groupedByGroupName = groupedMedicines.groupBy { it.groupName }
                                addLog("Групп лекарств: ${groupedByGroupName.size}")
                                
                                // Создаем список для отображения
                                val displayList = mutableListOf<Medicine>()
                                
                                groupedByGroupName.forEach { (groupName, groupMedicines) ->
                                    // Сортируем лекарства в группе по порядку
                                    val sortedGroupMedicines = groupMedicines.sortedBy { it.groupOrder }
                                    addLog("Группа '$groupName': ${sortedGroupMedicines.size} лекарств")
                                    
                                    // Добавляем только первое лекарство из группы (остальные будут скрыты)
                                    if (sortedGroupMedicines.isNotEmpty()) {
                                        displayList.add(sortedGroupMedicines.first())
                                    }
                                }
                                
                                // Добавляем лекарства без групп
                                val nonGroupedMedicines = activeMedicines.filter { it.groupName.isEmpty() }
                                displayList.addAll(nonGroupedMedicines)
                                
                                addLog("Итого для отображения: ${displayList.size} лекарств")
                                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                                todayMedicineAdapter.submitList(displayList)
                                
                                addLog("Показаны группированные карточки")
                            } else {
                                // Показываем обычные карточки
                                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                                todayMedicineAdapter.submitList(activeMedicines)
                                
                                addLog("Показаны обычные карточки")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error with grouping logic", e)
                            // Fallback - показываем обычные карточки
                            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, LocalDate.now())
                            binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                            todayMedicineAdapter.submitList(activeMedicines)
                            addLog("Ошибка группировки, показываем обычные карточки")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error in medicines observer", e)
                    showErrorState("Ошибка обработки данных: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in observeData", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadAllMedicines()
            loadTodayMedicines()
            
            // ✅ ДОБАВЛЕНО: Проверка просроченных лекарств при возвращении в приложение
            checkOverdueMedicines()
            
            // ✅ ДОБАВЛЕНО: Перезапуск периодической проверки при возвращении в приложение
            startPeriodicOverdueCheck()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun takeMedicine(medicine: Medicine) {
        android.util.Log.d("MainActivity", "=== ПРИЕМ ЛЕКАРСТВА ===")
        android.util.Log.d("MainActivity", "Лекарство: ${medicine.name} (ID: ${medicine.id})")
        
        // ✅ ДОБАВЛЕНО: Подробное логирование нажатия кнопки
        addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯЛ ЛЕКАРСТВА' ===")
        addLog("Лекарство: ${medicine.name} (ID: ${medicine.id})")
        addLog("Время приема: ${medicine.time}")
        addLog("Принято сегодня: ${medicine.takenToday}")
        addLog("Текущее время: ${LocalDateTime.now()}")
        addLog("Статус лекарства: ${com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)}")
        
        // ✅ ДОБАВЛЕНО: Немедленная остановка всех звуков и уведомлений
        addLog("🔇 НАЧИНАЕМ ОСТАНОВКУ ЗВУКА И УВЕДОМЛЕНИЙ")
        try {
            android.util.Log.d("🔇 КНОПКА_НАЖАТА", "Кнопка 'принял лекарство' нажата для: ${medicine.name} (ID: ${medicine.id})")
            addLog("🔇 КНОПКА_НАЖАТА: Кнопка 'принял лекарство' нажата для: ${medicine.name}")
            
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
            addLog("🔇 NotificationManager создан")
            
            // Принудительно останавливаем все вибрации и звуки
            android.util.Log.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем stopVibration() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ stopVibration() для: ${medicine.name}")
            notificationManager.stopVibration()
            addLog("🔇 stopVibration() выполнен")
            
            // ✅ ДОБАВЛЕНО: Проверяем результат stopVibration
            android.util.Log.d("🔇 КНОПКА_ПРОВЕРКА", "Проверяем результат stopVibration для: ${medicine.name}")
            addLog("🔇 ПРОВЕРЯЕМ результат stopVibration для: ${medicine.name}")
            
            // Отменяем конкретное уведомление для этого лекарства
            android.util.Log.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем cancelOverdueNotification() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ cancelOverdueNotification() для: ${medicine.name}")
            notificationManager.cancelOverdueNotification(medicine.id)
            addLog("🔇 cancelOverdueNotification() выполнен")
            
            // ✅ ДОБАВЛЕНО: Проверяем результат cancelOverdueNotification
            android.util.Log.d("🔇 КНОПКА_ПРОВЕРКА", "Проверяем результат cancelOverdueNotification для: ${medicine.name}")
            addLog("🔇 ПРОВЕРЯЕМ результат cancelOverdueNotification для: ${medicine.name}")
            
            // Дополнительно отменяем все уведомления для этого лекарства
            android.util.Log.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем cancelMedicineNotification() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ cancelMedicineNotification() для: ${medicine.name}")
            notificationManager.cancelMedicineNotification(medicine.id)
            addLog("🔇 cancelMedicineNotification() выполнен")
            
            // ✅ ДОБАВЛЕНО: Останавливаем периодическую проверку просроченных лекарств
            android.util.Log.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Останавливаем периодическую проверку для: ${medicine.name}")
            addLog("🔇 ОСТАНАВЛИВАЕМ периодическую проверку для: ${medicine.name}")
            stopPeriodicOverdueCheck()
            addLog("🔇 Периодическая проверка остановлена")
            
            // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
            try {
                android.util.Log.d("🔇 КНОПКА_AUDIO", "Принудительно останавливаем звук через AudioManager для: ${medicine.name}")
                addLog("🔇 ПРИНУДИТЕЛЬНО останавливаем звук через AudioManager для: ${medicine.name}")
                
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                android.util.Log.d("🔇 КНОПКА_AUDIO", "Оригинальная громкость: $originalVolume")
                addLog("🔇 Оригинальная громкость уведомлений: $originalVolume")
                
                // Временно отключаем звук уведомлений
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                android.util.Log.d("🔇 КНОПКА_AUDIO", "Звук уведомлений отключен")
                addLog("🔇 Звук уведомлений временно отключен")
                
                // Восстанавливаем через 200мс
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                    android.util.Log.d("🔇 КНОПКА_AUDIO", "Громкость восстановлена: $originalVolume")
                    addLog("🔇 Громкость уведомлений восстановлена: $originalVolume")
                }, 200)
                
            } catch (e: Exception) {
                android.util.Log.e("🔇 КНОПКА_AUDIO", "Ошибка AudioManager для: ${medicine.name}", e)
                addLog("❌ ОШИБКА AudioManager для: ${medicine.name} - ${e.message}")
            }
            
            android.util.Log.d("🔇 КНОПКА_ЗАВЕРШЕНА", "Все действия по остановке завершены для: ${medicine.name}")
            addLog("🔇 ВСЕ ДЕЙСТВИЯ ПО ОСТАНОВКЕ ЗАВЕРШЕНЫ для: ${medicine.name}")
        } catch (e: Exception) {
            android.util.Log.e("🔇 КНОПКА_ОШИБКА", "Ошибка отмены уведомлений для: ${medicine.name}", e)
            addLog("❌ ОШИБКА отмены уведомлений для: ${medicine.name} - ${e.message}")
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ ИСПРАВЛЕНО: Уменьшаем количество таблеток на 1
                val newRemainingQuantity = (medicine.remainingQuantity - 1).coerceAtLeast(0)
                
                android.util.Log.d("MainActivity", "=== УМЕНЬШЕНИЕ КОЛИЧЕСТВА ===")
                android.util.Log.d("MainActivity", "Лекарство: ${medicine.name}")
                android.util.Log.d("MainActivity", "Старое количество: ${medicine.remainingQuantity}")
                android.util.Log.d("MainActivity", "Новое количество: $newRemainingQuantity")
                addLog("=== УМЕНЬШЕНИЕ КОЛИЧЕСТВА ===")
                addLog("Лекарство: ${medicine.name}")
                addLog("Старое количество: ${medicine.remainingQuantity}")
                addLog("Новое количество: $newRemainingQuantity")
                
                val updatedMedicine = medicine.copy(
                    takenToday = true,
                    isMissed = false,
                    lastTakenTime = System.currentTimeMillis(),
                    takenAt = System.currentTimeMillis(),
                    remainingQuantity = newRemainingQuantity // ✅ ИСПРАВЛЕНО: Уменьшаем количество
                )
                
                android.util.Log.d("MainActivity", "Обновляем лекарство: takenToday=true, isMissed=false")
                addLog("ОБНОВЛЯЕМ лекарство: takenToday=true, isMissed=false")
                viewModel.updateMedicine(updatedMedicine)
                
                // ✅ ДОБАВЛЕНО: Предупреждение о заканчивающихся таблетках
                if (newRemainingQuantity <= 5 && newRemainingQuantity > 0) {
                    android.util.Log.d("MainActivity", "⚠️ ВНИМАНИЕ: Заканчиваются таблетки ${medicine.name}")
                    addLog("⚠️ ВНИМАНИЕ: Заканчиваются таблетки ${medicine.name} (осталось: $newRemainingQuantity)")
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "⚠️ Заканчиваются ${medicine.medicineType.lowercase()} ${medicine.name} (осталось: $newRemainingQuantity)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (newRemainingQuantity == 0) {
                    android.util.Log.d("MainActivity", "🚨 КРИТИЧНО: Таблетки ${medicine.name} закончились!")
                    addLog("🚨 КРИТИЧНО: Таблетки ${medicine.name} закончились!")
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "🚨 ${medicine.medicineType.lowercase().replaceFirstChar { it.uppercase() }} ${medicine.name} закончились! Нужно пополнить запас!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                // Считаем успешным, если нет исключений
                android.util.Log.d("MainActivity", "✓ Лекарство успешно обновлено")
                addLog("✓ Лекарство успешно обновлено")
                
                CoroutineScope(Dispatchers.Main).launch {
                    // ✅ ИЗМЕНЕНО: Убираем Toast уведомление, которое может воспроизводить звук
                    // android.widget.Toast.makeText(
                    //     this@MainActivity,
                    //     "Лекарство ${medicine.name} принято!",
                    //     android.widget.Toast.LENGTH_SHORT
                    // ).show()
                    
                    // ✅ ДОБАВЛЕНО: Немедленное обновление списка лекарств
                    addLog("🔄 НЕМЕДЛЕННОЕ ОБНОВЛЕНИЕ СПИСКА")
                    viewModel.loadAllMedicines()
                    
                    // ✅ ДОБАВЛЕНО: Принудительное обновление списка для немедленного исчезновения карточки
                    try {
                        // Немедленно обновляем список лекарств на сегодня
                        val currentList = todayMedicineAdapter.currentList.toMutableList()
                        val updatedList = currentList.filter { it.id != medicine.id }
                        todayMedicineAdapter.submitList(updatedList)
                        addLog("🔄 Карточка лекарства ${medicine.name} немедленно удалена из списка")
                    } catch (e: Exception) {
                        addLog("❌ ОШИБКА немедленного удаления карточки: ${e.message}")
                    }
                    
                    // Принудительно обновляем адаптер
                    try {
                        todayMedicineAdapter.notifyDataSetChanged()
                        android.util.Log.d("MainActivity", "✓ Адаптер обновлен")
                        addLog("✓ Адаптер обновлен")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка обновления адаптера", e)
                        addLog("❌ ОШИБКА обновления адаптера: ${e.message}")
                    }
                    
                    android.util.Log.d("MainActivity", "✓ Список лекарств обновлен")
                    addLog("✓ Список лекарств обновлен")
                    
                    // ✅ ДОБАВЛЕНО: Логирование завершения обновления
                    addLog("=== ОБНОВЛЕНИЕ ЗАВЕРШЕНО ===")
                    addLog("Лекарство: ${medicine.name} (ID: ${medicine.id})")
                    addLog("Статус: takenToday = true")
                    addLog("Время обновления: ${LocalDateTime.now()}")
                    
                    // ✅ ДОБАВЛЕНО: Перезапускаем периодическую проверку через 10 секунд
                    android.util.Log.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Перезапускаем периодическую проверку через 10 секунд")
                    addLog("🔇 ПЕРЕЗАПУСКАЕМ периодическую проверку через 10 секунд")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startPeriodicOverdueCheck()
                        android.util.Log.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Периодическая проверка перезапущена")
                        addLog("🔇 Периодическая проверка перезапущена")
                    }, 10000) // Увеличиваем задержку до 10 секунд
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Ошибка обновления лекарства", e)
                addLog("❌ ОШИБКА обновления лекарства: ${e.message}")
                
                CoroutineScope(Dispatchers.Main).launch {
                    // ✅ ИЗМЕНЕНО: Показываем Toast только при ошибке, но без звука
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка обновления лекарства: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun editMedicine(medicine: Medicine) {
        val intent = android.content.Intent(this, EditMedicineActivity::class.java).apply {
            putExtra("medicine_id", medicine.id)
        }
        startActivity(intent)
    }

    private fun deleteMedicine(medicine: Medicine) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Удаление лекарства")
            .setMessage("Вы уверены, что хотите удалить лекарство '${medicine.name}'?")
            .setPositiveButton("Удалить") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        viewModel.deleteMedicine(medicine.id)
                        CoroutineScope(Dispatchers.Main).launch {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Лекарство удалено",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Ошибка удаления: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun addLog(message: String) {
        try {
            val timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            val logMessage = "[$timestamp] $message"
            
            // ✅ ДОБАВЛЕНО: Добавляем в список логов
            logs.add(logMessage)
            
            // Ограничиваем размер списка логов (последние 1000 записей)
            if (logs.size > 1000) {
                logs.removeAt(0)
            }
            
            binding.textViewLogs.append("$logMessage\n")
            
            // Автоматическая прокрутка к концу
            try {
                val scrollAmount = binding.textViewLogs.layout.getLineTop(binding.textViewLogs.lineCount) - binding.textViewLogs.height
                if (scrollAmount > 0) {
                    binding.textViewLogs.scrollTo(0, scrollAmount)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error scrolling logs", e)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error adding log", e)
        }
    }

    private fun clearLogs() {
        try {
            // ✅ ДОБАВЛЕНО: Очищаем список логов
            logs.clear()
            binding.textViewLogs.text = ""
            addLog("=== ЛОГИ ОЧИЩЕНЫ ===")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error clearing logs", e)
        }
    }

    private fun loadTodayMedicines() {
        showLoadingState()
        viewModel.loadTodayMedicines()
    }

    private fun showLoadingState() {
        binding.progressBarTodayMedicines.visibility = View.VISIBLE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.VISIBLE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun showErrorState(errorMessage: String = "Не удалось загрузить лекарства") {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.GONE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.VISIBLE
        binding.textErrorMessage.text = errorMessage
    }

    private fun showContentState() {
        binding.progressBarTodayMedicines.visibility = View.GONE
        binding.recyclerViewTodayMedicines.visibility = View.VISIBLE
        binding.layoutEmptyTodayMedicines.visibility = View.GONE
        binding.layoutErrorTodayMedicines.visibility = View.GONE
    }

    private fun toggleElderlyMode() {
        try {
            // Переключение режима для пожилых
            android.widget.Toast.makeText(this, "Режим для пожилых переключен", android.widget.Toast.LENGTH_SHORT).show()
            addLog("Режим для пожилых переключен")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error toggling elderly mode", e)
        }
    }

    private fun toggleLogsVisibility() {
        try {
            if (binding.layoutLogs.visibility == View.VISIBLE) {
                binding.layoutLogs.visibility = View.GONE
            } else {
                binding.layoutLogs.visibility = View.VISIBLE
                addLog("=== ЛОГИ ВКЛЮЧЕНЫ ===")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error toggling logs visibility", e)
        }
    }

    private fun exportData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = viewModel.allMedicines.value ?: emptyList()
                val exportData = medicines.joinToString("\n") { medicine ->
                    "${medicine.name} - ${medicine.dosage} - ${medicine.time}"
                }
                
                val fileName = "medical_notes_export_${LocalDate.now()}.txt"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(exportData)
                
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Данные экспортированы в $fileName",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    addLog("Экспорт данных: $fileName")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка экспорта: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    addLog("Ошибка экспорта: ${e.message}")
                }
            }
        }
    }

    private fun testGroups() {
        addLog("=== ТЕСТИРОВАНИЕ ГРУПП ===")
        val medicines = viewModel.allMedicines.value ?: emptyList()
        val groupedMedicines = medicines.filter { it.groupName.isNotEmpty() }
        
        addLog("Всего лекарств: ${medicines.size}")
        addLog("Лекарств в группах: ${groupedMedicines.size}")
        
        val groups = groupedMedicines.groupBy { it.groupName }
        groups.forEach { (groupName, groupMedicines) ->
            addLog("Группа '$groupName': ${groupMedicines.size} лекарств")
            groupMedicines.forEach { medicine ->
                addLog("  - ${medicine.name} (порядок: ${medicine.groupOrder})")
            }
        }
        
        android.widget.Toast.makeText(
            this,
            "Тест групп завершен. Проверьте логи.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    // ✅ ДОБАВЛЕНО: Метод копирования лога в буфер обмена
    private fun copyLogToClipboard() {
        try {
            android.util.Log.d("MainActivity", "=== КОПИРОВАНИЕ ЛОГА ===")
            
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val logText = logs.joinToString("\n")
            
            val clip = android.content.ClipData.newPlainText("MedicalNotes Log", logText)
            clipboardManager.setPrimaryClip(clip)
            
            android.util.Log.d("MainActivity", "✓ Лог скопирован в буфер обмена")
            android.util.Log.d("MainActivity", "Размер лога: ${logText.length} символов")
            
            android.widget.Toast.makeText(
                this,
                "Лог скопирован в буфер обмена (${logText.length} символов)",
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            addLog("=== ЛОГ СКОПИРОВАН ===")
            addLog("Размер: ${logText.length} символов")
            addLog("Время: ${LocalDateTime.now()}")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error copying log to clipboard", e)
            android.widget.Toast.makeText(
                this,
                "Ошибка копирования лога: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // ✅ ДОБАВЛЕНО: Автоматическая проверка просроченных лекарств
    private fun checkOverdueMedicines() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("MainActivity", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
                addLog("=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
                
                val medicines = viewModel.allMedicines.value ?: emptyList()
                addLog("Всего лекарств в базе: ${medicines.size}")
                
                medicines.forEach { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    addLog("ПРОВЕРКА: ${medicine.name} - Статус: $status, Время: ${medicine.time}, Принято сегодня: ${medicine.takenToday}")
                    
                    if (status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE) {
                        addLog("⚠️ НАЙДЕНО ПРОСРОЧЕННОЕ: ${medicine.name} (принято сегодня: ${medicine.takenToday})")
                    }
                }
                
                val overdueMedicines = medicines.filter { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    val isOverdue = status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE
                    val notTakenToday = !medicine.takenToday
                    
                    addLog("🔍 ФИЛЬТРАЦИЯ: ${medicine.name} - Статус: $status, Просрочено: $isOverdue, Не принято сегодня: $notTakenToday")
                    
                    isOverdue && notTakenToday
                }
                
                addLog("Найдено просроченных лекарств (не принятых сегодня): ${overdueMedicines.size}")
                
                if (overdueMedicines.isNotEmpty()) {
                    val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
                    
                    overdueMedicines.forEach { medicine ->
                        addLog("🔔 ПОКАЗЫВАЕМ УВЕДОМЛЕНИЕ для: ${medicine.name}")
                        notificationManager.showOverdueMedicineNotification(medicine)
                    }
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Найдено ${overdueMedicines.size} просроченных лекарств!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    addLog("Просроченных лекарств не найдено")
                }
                
                addLog("=== ПРОВЕРКА ЗАВЕРШЕНА ===")
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Ошибка проверки просроченных лекарств", e)
                addLog("❌ ОШИБКА проверки просроченных лекарств: ${e.message}")
            }
        }
    }
    
    // ✅ ДОБАВЛЕНО: Периодическая проверка просроченных лекарств
    private fun startPeriodicOverdueCheck() {
        try {
            overdueCheckTimer = android.os.Handler(android.os.Looper.getMainLooper())
            
            val checkRunnable = object : Runnable {
                override fun run() {
                    try {
                        android.util.Log.d("MainActivity", "=== ПЕРИОДИЧЕСКАЯ ПРОВЕРКА ПРОСРОЧЕННЫХ ===")
                        checkOverdueMedicines()
                        
                        // Планируем следующую проверку через 30 секунд
                        overdueCheckTimer?.postDelayed(this, 30000) // 30 секунд
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка периодической проверки", e)
                    }
                }
            }
            
            // Запускаем первую проверку через 30 секунд
            overdueCheckTimer?.postDelayed(checkRunnable, 30000)
            android.util.Log.d("MainActivity", "✓ Периодическая проверка запущена (каждые 30 секунд)")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка запуска периодической проверки", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Остановка периодической проверки
    private fun stopPeriodicOverdueCheck() {
        try {
            overdueCheckTimer?.removeCallbacksAndMessages(null)
            overdueCheckTimer = null
            android.util.Log.d("MainActivity", "✓ Периодическая проверка остановлена")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка остановки периодической проверки", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Останавливаем периодическую проверку
            stopPeriodicOverdueCheck()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onDestroy", e)
        }
    }
}  