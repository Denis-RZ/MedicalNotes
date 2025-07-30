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
            // Инициализация системы логирования
            com.medicalnotes.app.utils.LogCollector.initialize(this)
            com.medicalnotes.app.utils.LogCollector.i("MainActivity", "onCreate started")
            
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Инициализация ViewModel и Repository с проверками
            try {
                viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                userPreferencesRepository = UserPreferencesRepository(this)
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "ViewModel and Repository initialized")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error initializing ViewModel/Repository", e)
                android.widget.Toast.makeText(this, "Ошибка инициализации данных", android.widget.Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Настройка UI с дополнительными проверками
            try {
                setupViews()
                setupButtons()
                observeData()
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "UI setup completed")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in UI setup", e)
                android.widget.Toast.makeText(this, "Ошибка настройки интерфейса", android.widget.Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Загрузка данных с проверками
            try {
                viewModel.loadAllMedicines()
                loadTodayMedicines()
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "Data loading completed")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error loading data", e)
                android.widget.Toast.makeText(this, "Ошибка загрузки данных", android.widget.Toast.LENGTH_LONG).show()
            }
            
            // Запуск сервисов с проверками
            try {
                // ✅ ДОБАВЛЕНО: Автоматическая проверка просроченных лекарств
                checkOverdueMedicines()
                
                // ✅ ДОБАВЛЕНО: Периодическая проверка каждые 30 секунд
                startPeriodicOverdueCheck()
                
                // ✅ ДОБАВЛЕНО: Запуск сервиса уведомлений для обеспечения работы в фоне
                startNotificationService()
                
                // ✅ ДОБАВЛЕНО: Запуск сервиса проверки просроченных лекарств
                startOverdueCheckService()
                
                // ✅ ДОБАВЛЕНО: Проверка и восстановление уведомлений при запуске
                checkAndRestoreNotifications()
                
                // ✅ ДОБАВЛЕНО: Проверка разрешений для надежной работы уведомлений
                checkNotificationPermissions()
                
                // ✅ ДОБАВЛЕНО: Проверка разрешений для показа окон поверх других приложений
                checkOverlayPermissions()
                
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "Services started successfully")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error starting services", e)
                android.widget.Toast.makeText(this, "Ошибка запуска сервисов", android.widget.Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Critical error in onCreate", e)
            android.widget.Toast.makeText(this, "Критическая ошибка инициализации: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupViews() {
        try {
            // Настройка toolbar с проверками
            try {
                setSupportActionBar(binding.toolbar)
                binding.toolbar.subtitle = VersionUtils.getShortVersionInfo(this)
                
                // ВАЖНО: Настройка кнопки меню (гамбургер) - ИСПРАВЛЕНО
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
                
                // Настройка навигации для MaterialToolbar
                binding.toolbar.setNavigationOnClickListener {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Кнопка навигации нажата - открываем drawer")
                    try {
                        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Drawer уже открыт - закрываем")
                            binding.drawerLayout.closeDrawer(GravityCompat.START)
                        } else {
                            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Drawer закрыт - открываем")
                            binding.drawerLayout.openDrawer(GravityCompat.START)
                        }
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка при работе с drawer", e)
                    }
                }
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Toolbar настроен, кнопка навигации установлена")
                
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting version info", e)
                binding.toolbar.subtitle = "v?.?"
            }
            
            // Настройка Navigation Drawer с проверками
            try {
                setupNavigationDrawer()
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up navigation drawer", e)
            }
            
            // Настройка RecyclerView с проверками
            try {
                todayMedicineAdapter = MainMedicineAdapter(
                    onMedicineClick = { medicine -> 
                        try {
                            takeMedicine(medicine) 
                        } catch (e: Exception) {
                            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in medicine click", e)
                            android.widget.Toast.makeText(this, "Ошибка при приеме лекарства", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                binding.recyclerViewTodayMedicines.apply {
                    layoutManager = LinearLayoutManager(this@MainActivity)
                    adapter = todayMedicineAdapter
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up RecyclerView", e)
            }

            // Настройка кнопки повтора с проверками
            try {
                binding.buttonRetry.setOnClickListener {
                    try {
                        loadTodayMedicines()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in retry button click", e)
                        android.widget.Toast.makeText(this, "Ошибка при повторной загрузке", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up retry button", e)
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Critical error in setupViews", e)
            throw e
        }
    }

    private fun setupNavigationDrawer() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Настройка Navigation Drawer")
            
            // Проверяем, что NavigationView существует
            if (binding.navigationView == null) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "NavigationView не найден!")
                return
            }
            
            // Настройка NavigationView
            binding.navigationView.setNavigationItemSelectedListener { menuItem ->
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Выбран пункт меню: ${menuItem.title}")
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
                        com.medicalnotes.app.utils.LogCollector.w("MainActivity", "Неизвестный пункт меню: ${menuItem.itemId}")
                        false
                    }
                }
            }
            
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Navigation Drawer настроен успешно")
            
            // Добавляем обработчик закрытия drawer при нажатии вне его области
            binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
                override fun onDrawerOpened(drawerView: android.view.View) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Drawer открыт")
                }
                override fun onDrawerClosed(drawerView: android.view.View) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Drawer закрыт")
                }
                override fun onDrawerStateChanged(newState: Int) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Состояние drawer изменилось: $newState")
                }
            })
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up navigation drawer", e)
        }
    }

    private fun setupButtons() {
        try {
            // Настройка FAB с проверками
            try {
                binding.fabAddMedicine.setOnClickListener {
                    try {
                        val intent = android.content.Intent(this, AddMedicineActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error starting AddMedicineActivity", e)
                        android.widget.Toast.makeText(this, "Ошибка открытия добавления лекарства", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up FAB", e)
            }

            // Настройка кнопки очистки логов с проверками
            try {
                binding.buttonClearLogs.setOnClickListener {
                    try {
                        clearLogs()
                        android.widget.Toast.makeText(this, "Логи очищены", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error clearing logs", e)
                        android.widget.Toast.makeText(this, "Ошибка очистки логов", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up clear logs button", e)
            }
            
            // Настройка кнопки копирования лога с проверками
            try {
                binding.buttonCopyLog.setOnClickListener {
                    try {
                        copyLogToClipboard()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error copying log", e)
                        android.widget.Toast.makeText(this, "Ошибка копирования лога", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up copy log button", e)
            }
            
            // ✅ ДОБАВЛЕНО: Кнопка диагностики
            try {
                binding.buttonDiagnostic.setOnClickListener {
                    try {
                        performDiagnostic()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error performing diagnostic", e)
                        android.widget.Toast.makeText(this, "Ошибка диагностики", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up diagnostic button", e)
            }
            
            // ✅ ДОБАВЛЕНО: Кнопка исправления проблем
            try {
                binding.buttonFixIssues.setOnClickListener {
                    try {
                        fixIssues()
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error fixing issues", e)
                        android.widget.Toast.makeText(this, "Ошибка исправления проблем", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error setting up fix issues button", e)
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Critical error in setupButtons", e)
            throw e
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        // Меню не нужно - используется кнопка навигации в toolbar
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "onOptionsItemSelected: ${item.itemId}")
        return when (item.itemId) {
            android.R.id.home -> {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Нажата кнопка home - открываем drawer")
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
                                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error processing medicine ${medicine.name}", e)
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
                            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error with grouping logic", e)
                            // Fallback - показываем обычные карточки
                            val activeMedicines = DosageCalculator.getActiveMedicinesForDate(medicines, LocalDate.now())
                            binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                            todayMedicineAdapter.submitList(activeMedicines)
                            addLog("Ошибка группировки, показываем обычные карточки")
                        }
                    }
                } catch (e: Exception) {
                    com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in medicines observer", e)
                    showErrorState("Ошибка обработки данных: ${e.message}")
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in observeData", e)
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
            
            // ✅ ДОБАВЛЕНО: Обработка результата от EditMedicineActivity
            handleEditMedicineResult()
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in onResume", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ ДОБАВЛЕНО: Обработка результата от EditMedicineActivity
     */
    private fun handleEditMedicineResult() {
        try {
            // Проверяем, был ли результат от EditMedicineActivity
            val medicineUpdated = intent.getBooleanExtra("medicine_updated", false)
            val medicineId = intent.getLongExtra("medicine_id", -1L)
            
            if (medicineUpdated && medicineId != -1L) {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Получен результат от EditMedicineActivity для лекарства ID: $medicineId")
                
                // АГРЕССИВНО останавливаем все уведомления для этого лекарства
                val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
                notificationManager.forceCancelAllNotificationsForMedicine(medicineId)
                
                // Принудительно обновляем статусы
                checkOverdueMedicines()
                
                // Очищаем результат
                intent.removeExtra("medicine_updated")
                intent.removeExtra("medicine_id")
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Обработка результата EditMedicineActivity завершена")
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка обработки результата EditMedicineActivity", e)
        }
    }

    private fun takeMedicine(medicine: Medicine) {
        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПРИЕМ ЛЕКАРСТВА ===")
        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Лекарство: ${medicine.name} (ID: ${medicine.id})")
        
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
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_НАЖАТА", "Кнопка 'принял лекарство' нажата для: ${medicine.name} (ID: ${medicine.id})")
            addLog("🔇 КНОПКА_НАЖАТА: Кнопка 'принял лекарство' нажата для: ${medicine.name}")
            
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
            addLog("🔇 NotificationManager создан")
            
            // Принудительно останавливаем все вибрации и звуки
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем stopVibration() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ stopVibration() для: ${medicine.name}")
            notificationManager.stopVibration()
            addLog("🔇 stopVibration() выполнен")
            
            // ✅ ДОБАВЛЕНО: Проверяем результат stopVibration
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ПРОВЕРКА", "Проверяем результат stopVibration для: ${medicine.name}")
            addLog("🔇 ПРОВЕРЯЕМ результат stopVibration для: ${medicine.name}")
            
            // Отменяем конкретное уведомление для этого лекарства
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем cancelOverdueNotification() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ cancelOverdueNotification() для: ${medicine.name}")
            notificationManager.cancelOverdueNotification(medicine.id)
            addLog("🔇 cancelOverdueNotification() выполнен")
            
            // ✅ ДОБАВЛЕНО: Проверяем результат cancelOverdueNotification
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ПРОВЕРКА", "Проверяем результат cancelOverdueNotification для: ${medicine.name}")
            addLog("🔇 ПРОВЕРЯЕМ результат cancelOverdueNotification для: ${medicine.name}")
            
            // Дополнительно отменяем все уведомления для этого лекарства
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ДЕЙСТВИЕ", "Вызываем cancelMedicineNotification() для: ${medicine.name}")
            addLog("🔇 ВЫЗЫВАЕМ cancelMedicineNotification() для: ${medicine.name}")
            notificationManager.cancelMedicineNotification(medicine.id)
            addLog("🔇 cancelMedicineNotification() выполнен")
            
            // ✅ ДОБАВЛЕНО: Останавливаем периодическую проверку просроченных лекарств
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Останавливаем периодическую проверку для: ${medicine.name}")
            addLog("🔇 ОСТАНАВЛИВАЕМ периодическую проверку для: ${medicine.name}")
            stopPeriodicOverdueCheck()
            addLog("🔇 Периодическая проверка остановлена")
            
            // ✅ ДОБАВЛЕНО: Принудительная остановка через AudioManager
            try {
                com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_AUDIO", "Принудительно останавливаем звук через AudioManager для: ${medicine.name}")
                addLog("🔇 ПРИНУДИТЕЛЬНО останавливаем звук через AudioManager для: ${medicine.name}")
                
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_AUDIO", "Оригинальная громкость: $originalVolume")
                addLog("🔇 Оригинальная громкость уведомлений: $originalVolume")
                
                // Временно отключаем звук уведомлений
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_AUDIO", "Звук уведомлений отключен")
                addLog("🔇 Звук уведомлений временно отключен")
                
                // Восстанавливаем через 200мс
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, originalVolume, 0)
                    com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_AUDIO", "Громкость восстановлена: $originalVolume")
                    addLog("🔇 Громкость уведомлений восстановлена: $originalVolume")
                }, 200)
                
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("🔇 КНОПКА_AUDIO", "Ошибка AudioManager для: ${medicine.name}", e)
                addLog("❌ ОШИБКА AudioManager для: ${medicine.name} - ${e.message}")
            }
            
            com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ЗАВЕРШЕНА", "Все действия по остановке завершены для: ${medicine.name}")
            addLog("🔇 ВСЕ ДЕЙСТВИЯ ПО ОСТАНОВКЕ ЗАВЕРШЕНЫ для: ${medicine.name}")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("🔇 КНОПКА_ОШИБКА", "Ошибка отмены уведомлений для: ${medicine.name}", e)
            addLog("❌ ОШИБКА отмены уведомлений для: ${medicine.name} - ${e.message}")
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ ИСПРАВЛЕНО: Уменьшаем количество таблеток на 1
                val newRemainingQuantity = (medicine.remainingQuantity - 1).coerceAtLeast(0)
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== УМЕНЬШЕНИЕ КОЛИЧЕСТВА ===")
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Лекарство: ${medicine.name}")
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Старое количество: ${medicine.remainingQuantity}")
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Новое количество: $newRemainingQuantity")
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
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Обновляем лекарство: takenToday=true, isMissed=false")
                addLog("ОБНОВЛЯЕМ лекарство: takenToday=true, isMissed=false")
                viewModel.updateMedicine(updatedMedicine)
                
                // ✅ ДОБАВЛЕНО: Предупреждение о заканчивающихся таблетках
                if (newRemainingQuantity <= 5 && newRemainingQuantity > 0) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "⚠️ ВНИМАНИЕ: Заканчиваются таблетки ${medicine.name}")
                    addLog("⚠️ ВНИМАНИЕ: Заканчиваются таблетки ${medicine.name} (осталось: $newRemainingQuantity)")
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "⚠️ Заканчиваются ${medicine.medicineType.lowercase()} ${medicine.name} (осталось: $newRemainingQuantity)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (newRemainingQuantity == 0) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "🚨 КРИТИЧНО: Таблетки ${medicine.name} закончились!")
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
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Лекарство успешно обновлено")
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
                        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Адаптер обновлен")
                        addLog("✓ Адаптер обновлен")
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка обновления адаптера", e)
                        addLog("❌ ОШИБКА обновления адаптера: ${e.message}")
                    }
                    
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Список лекарств обновлен")
                    addLog("✓ Список лекарств обновлен")
                    
                    // ✅ ДОБАВЛЕНО: Логирование завершения обновления
                    addLog("=== ОБНОВЛЕНИЕ ЗАВЕРШЕНО ===")
                    addLog("Лекарство: ${medicine.name} (ID: ${medicine.id})")
                    addLog("Статус: takenToday = true")
                    addLog("Время обновления: ${LocalDateTime.now()}")
                    
                    // ✅ ДОБАВЛЕНО: Перезапускаем периодическую проверку через 10 секунд
                    com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Перезапускаем периодическую проверку через 10 секунд")
                    addLog("🔇 ПЕРЕЗАПУСКАЕМ периодическую проверку через 10 секунд")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startPeriodicOverdueCheck()
                        com.medicalnotes.app.utils.LogCollector.d("🔇 КНОПКА_ПЕРИОДИЧЕСКАЯ", "Периодическая проверка перезапущена")
                        addLog("🔇 Периодическая проверка перезапущена")
                    }, 10000) // Увеличиваем задержку до 10 секунд
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка обновления лекарства", e)
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
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error scrolling logs", e)
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error adding log", e)
        }
    }

    private fun clearLogs() {
        try {
            // ✅ ДОБАВЛЕНО: Очищаем список логов
            logs.clear()
            binding.textViewLogs.text = ""
            addLog("=== ЛОГИ ОЧИЩЕНЫ ===")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error clearing logs", e)
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
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error toggling elderly mode", e)
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
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error toggling logs visibility", e)
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
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== КОПИРОВАНИЕ ЛОГА ===")
            
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val logText = logs.joinToString("\n")
            
            val clip = android.content.ClipData.newPlainText("MedicalNotes Log", logText)
            clipboardManager.setPrimaryClip(clip)
            
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Лог скопирован в буфер обмена")
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Размер лога: ${logText.length} символов")
            
            android.widget.Toast.makeText(
                this,
                "Лог скопирован в буфер обмена (${logText.length} символов)",
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            addLog("=== ЛОГ СКОПИРОВАН ===")
            addLog("Размер: ${logText.length} символов")
            addLog("Время: ${LocalDateTime.now()}")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error copying log to clipboard", e)
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
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
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
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки просроченных лекарств", e)
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
                        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПЕРИОДИЧЕСКАЯ ПРОВЕРКА ПРОСРОЧЕННЫХ ===")
                        checkOverdueMedicines()
                        
                        // Планируем следующую проверку через 30 секунд
                        overdueCheckTimer?.postDelayed(this, 30000) // 30 секунд
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка периодической проверки", e)
                    }
                }
            }
            
            // Запускаем первую проверку через 30 секунд
            overdueCheckTimer?.postDelayed(checkRunnable, 30000)
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Периодическая проверка запущена (каждые 30 секунд)")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка запуска периодической проверки", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Остановка периодической проверки
    private fun stopPeriodicOverdueCheck() {
        try {
            overdueCheckTimer?.removeCallbacksAndMessages(null)
            overdueCheckTimer = null
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Периодическая проверка остановлена")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка остановки периодической проверки", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Запуск сервиса уведомлений
    private fun startNotificationService() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Запуск сервиса уведомлений")
            com.medicalnotes.app.service.NotificationService.startService(this)
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Сервис уведомлений запущен")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка запуска сервиса уведомлений", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Запуск сервиса проверки просроченных лекарств
    private fun startOverdueCheckService() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Запуск сервиса проверки просроченных лекарств")
            addLog("🚀 Запуск сервиса проверки просроченных лекарств")
            
            com.medicalnotes.app.service.OverdueCheckService.startService(this)
            
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Сервис проверки просроченных лекарств запущен")
            addLog("✅ Сервис проверки просроченных лекарств запущен")
            
            // Проверяем, что сервис действительно запущен
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val isServiceRunning = isServiceRunning(com.medicalnotes.app.service.OverdueCheckService::class.java)
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Проверка сервиса: $isServiceRunning")
                    addLog("🔍 Проверка сервиса: $isServiceRunning")
                } catch (e: Exception) {
                    com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки сервиса", e)
                }
            }, 2000)
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка запуска сервиса проверки просроченных лекарств", e)
            addLog("❌ Ошибка запуска сервиса: ${e.message}")
        }
    }
    
    // ✅ ДОБАВЛЕНО: Проверка, запущен ли сервис
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // ✅ ИСПРАВЛЕНО: Используем современный подход вместо deprecated getRunningServices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Для Android 8.0+ используем getRunningServices с ограничением
                val runningServices = activityManager.getRunningServices(100)
                runningServices.any { it.service.className == serviceClass.name }
            } else {
                // Для старых версий используем альтернативный подход
                val packageManager = packageManager
                val intent = android.content.Intent(this, serviceClass)
                val resolveInfo = packageManager.queryIntentServices(intent, 0)
                resolveInfo.isNotEmpty()
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error checking service status", e)
            false
        }
    }
    
    // ✅ ДОБАВЛЕНО: Проверка и восстановление уведомлений
    private fun checkAndRestoreNotifications() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Проверка и восстановление уведомлений")
            val restorationManager = com.medicalnotes.app.utils.NotificationRestorationManager(this)
            restorationManager.checkAndRestoreNotifications()
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Проверка и восстановление уведомлений завершено")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки и восстановления уведомлений", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Проверка разрешений для уведомлений
    private fun checkNotificationPermissions() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Проверка разрешений для уведомлений")
            val batteryHelper = com.medicalnotes.app.utils.BatteryOptimizationHelper(this)
            batteryHelper.checkAndRequestNotificationPermissions()
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Проверка разрешений для уведомлений завершена")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки разрешений для уведомлений", e)
        }
    }
    
    // ✅ ДОБАВЛЕНО: Проверка разрешений для показа окон поверх других приложений
    private fun checkOverlayPermissions() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Проверка разрешений для показа окон поверх других приложений")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Нет разрешения на показ окон поверх других приложений")
                    addLog("⚠️ Нужно разрешение на показ окон поверх других приложений")
                    
                    // Показываем диалог с объяснением
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Разрешение необходимо")
                        .setMessage("Для показа уведомлений поверх всех приложений нужно разрешение. Нажмите 'Настройки' для перехода к настройкам.")
                        .setPositiveButton("Настройки") { _, _ ->
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                                addLog("🔧 Открыто окно настроек разрешений")
                            } catch (e: Exception) {
                                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка открытия настроек разрешений", e)
                                addLog("❌ Ошибка открытия настроек: ${e.message}")
                            }
                        }
                        .setNegativeButton("Позже") { _, _ ->
                            addLog("⏰ Настройка разрешений отложена")
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Разрешение на показ окон поверх других приложений есть")
                    addLog("✅ Разрешение на показ окон есть")
                }
            } else {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Android версия ниже 6.0, разрешение не требуется")
                addLog("✅ Android версия ниже 6.0, разрешение не требуется")
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки разрешений для показа окон", e)
            addLog("❌ Ошибка проверки разрешений: ${e.message}")
        }
    }
    
    // ✅ ДОБАВЛЕНО: Метод диагностики
    private fun performDiagnostic() {
        try {
            addLog("🔍 ЗАПУСК ДИАГНОСТИКИ...")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val diagnostic = com.medicalnotes.app.utils.DiagnosticUtils.performFullDiagnostic(this@MainActivity)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        addLog("🔍 ДИАГНОСТИКА ЗАВЕРШЕНА")
                        addLog("📋 Результат:")
                        addLog(diagnostic)
                        
                        // Сохраняем отчет в файл
                        val saveResult = com.medicalnotes.app.utils.DiagnosticUtils.saveDiagnosticReport(this@MainActivity, diagnostic)
                        addLog("💾 $saveResult")
                        
                        android.widget.Toast.makeText(this@MainActivity, "Диагностика завершена", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        addLog("❌ ОШИБКА ДИАГНОСТИКИ: ${e.message}")
                        android.widget.Toast.makeText(this@MainActivity, "Ошибка диагностики: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in performDiagnostic", e)
            addLog("❌ КРИТИЧЕСКАЯ ОШИБКА ДИАГНОСТИКИ: ${e.message}")
        }
    }
    
    // ✅ ДОБАВЛЕНО: Метод исправления проблем
    private fun fixIssues() {
        try {
            addLog("🔧 ЗАПУСК ИСПРАВЛЕНИЯ ПРОБЛЕМ...")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fixResult = com.medicalnotes.app.utils.DiagnosticUtils.fixCommonIssues(this@MainActivity)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        addLog("🔧 ИСПРАВЛЕНИЕ ЗАВЕРШЕНО")
                        addLog("📋 Результат:")
                        addLog(fixResult)
                        
                        android.widget.Toast.makeText(this@MainActivity, "Исправление завершено", android.widget.Toast.LENGTH_LONG).show()
                        
                        // Перезагружаем данные после исправления
                        try {
                            viewModel.loadAllMedicines()
                            loadTodayMedicines()
                            addLog("🔄 Данные перезагружены после исправления")
                        } catch (e: Exception) {
                            addLog("❌ ОШИБКА ПЕРЕЗАГРУЗКИ: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        addLog("❌ ОШИБКА ИСПРАВЛЕНИЯ: ${e.message}")
                        android.widget.Toast.makeText(this@MainActivity, "Ошибка исправления: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in fixIssues", e)
            addLog("❌ КРИТИЧЕСКАЯ ОШИБКА ИСПРАВЛЕНИЯ: ${e.message}")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Останавливаем периодическую проверку
            stopPeriodicOverdueCheck()
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in onDestroy", e)
        }
    }
}  