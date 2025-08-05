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
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import com.medicalnotes.app.utils.DosageCalculator

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var todayMedicineAdapter: MainMedicineAdapter
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private var overdueCheckTimer: android.os.Handler? = null
    
    //  ДОБАВЛЕНО: Список для хранения логов
    private val logs = mutableListOf<String>()
    
    //  ДОБАВЛЕНО: Обработчик результата от EditMedicineActivity
    private val editMedicineLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val medicineUpdated = result.data?.getBooleanExtra("medicine_updated", false) ?: false
            val medicineId = result.data?.getLongExtra("medicine_id", -1L) ?: -1L
            
            if (medicineUpdated && medicineId != -1L) {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Получен результат от EditMedicineActivity для лекарства ID: $medicineId")
                
                // АГРЕССИВНО останавливаем все уведомления для этого лекарства
                val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
                notificationManager.forceCancelAllNotificationsForMedicine(medicineId)
                
                //  ИСПРАВЛЕНО: Принудительно перезагружаем лекарства на сегодня
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Перезагружаем лекарства на сегодня после редактирования")
                loadTodayMedicines()
                
                // Принудительно обновляем статусы
                checkOverdueMedicines()
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Обработка результата EditMedicineActivity завершена")
            }
        }
    }
    
    //  ДОБАВЛЕНО: Тестовая функция для проверки CrashReporter
    private fun testCrashReporter() {
        try {
            android.widget.Toast.makeText(this, "Тестирование CrashReporter...", android.widget.Toast.LENGTH_SHORT).show()
            
            // Создаем тестовую ошибку
            val testException = RuntimeException("Тестовая ошибка для проверки CrashReporter")
            testException.printStackTrace()
            
            // Показываем диалог с тестовой ошибкой
            com.medicalnotes.app.utils.CrashReporter.showSimpleErrorDialog(
                this, 
                getString(R.string.test_crash_reporter), 
                testException
            )
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка тестирования CrashReporter", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            //  ДОБАВЛЕНО: Проверка инициализации CrashReporter
            try {
                com.medicalnotes.app.utils.CrashReporter.initialize(this)
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", " CrashReporter проверен")
                
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", " Ошибка проверки CrashReporter", e)
                e.printStackTrace()
            }
            
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
                android.widget.Toast.makeText(this, getString(R.string.error_initialization), android.widget.Toast.LENGTH_LONG).show()
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
                android.widget.Toast.makeText(this, getString(R.string.error_ui_setup), android.widget.Toast.LENGTH_LONG).show()
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
                android.widget.Toast.makeText(this, getString(R.string.error_loading_data), android.widget.Toast.LENGTH_LONG).show()
            }
            
            //  ИСПРАВЛЕНО: Запуск сервисов в фоновом потоке для предотвращения ANR
            try {
                // Запускаем сервисы в фоновом потоке, чтобы не блокировать UI
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        //  ДОБАВЛЕНО: Автоматическая проверка просроченных лекарств
                        checkOverdueMedicines()
                        
                        //  ДОБАВЛЕНО: Запуск сервиса уведомлений для обеспечения работы в фоне
                        startNotificationService()
                        
                        //  ДОБАВЛЕНО: Запуск сервиса проверки просроченных лекарств
                        startOverdueCheckService()
                        
                        //  ДОБАВЛЕНО: Проверка и восстановление уведомлений при запуске
                        checkAndRestoreNotifications()
                        
                        //  ДОБАВЛЕНО: Проверка разрешений для надежной работы уведомлений
                        checkNotificationPermissions()
                        
                        //  ИСПРАВЛЕНО: Проверка разрешений для показа окон поверх других приложений на главном потоке
                        lifecycleScope.launch(Dispatchers.Main) {
                            checkOverlayPermissions()
                        }
                        
                        com.medicalnotes.app.utils.LogCollector.i("MainActivity", "Services started successfully in background")
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error starting services in background", e)
                    }
                }
                
                //  ИСПРАВЛЕНО: Периодическая проверка каждые 2 минуты (запускается на главном потоке, но операции в фоне)
                startPeriodicOverdueCheck()
                
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "Services startup initiated")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error initiating services", e)
                android.widget.Toast.makeText(this, getString(R.string.error_starting_services), android.widget.Toast.LENGTH_LONG).show()
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
                binding.toolbar.setTitle(getString(R.string.app_name))
                binding.toolbar.subtitle = VersionUtils.getShortVersionInfo(this)
                
                // ДОБАВЛЕНО: Отладочная информация
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "setupViews: Setting toolbar title to: ${getString(R.string.app_name)}")
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "setupViews: Current toolbar title is: ${binding.toolbar.title}")
                
                // ДОБАВЛЕНО: Принудительное обновление toolbar
                binding.toolbar.invalidate()
                
                // ДОБАВЛЕНО: Устанавливаем заголовок после полной инициализации view
                binding.toolbar.post {
                    binding.toolbar.setTitle(getString(R.string.app_name))
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "setupViews: Post-set toolbar title to: ${getString(R.string.app_name)}")
                }
                
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
            
            // NavigationView всегда существует в layout, проверка не нужна
            
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
            
            //  ДОБАВЛЕНО: Кнопка диагностики
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
            
            //  ДОБАВЛЕНО: Кнопка исправления проблем
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
                    //  ДОБАВЛЕНО: Подробное логирование для отладки
                    addLog("=== ОБСЕРВЕР ЛЕКАРСТВ НА СЕГОДНЯ ===")
                    addLog("Получено лекарств из ViewModel: ${medicines.size}")
                    addLog("Время обновления: ${LocalDateTime.now()}")
                    
                    // Логируем каждое лекарство для отладки
                    medicines.forEach { medicine ->
                        addLog("  - ${medicine.name} (ID: ${medicine.id})")
                    }
                    
                    if (medicines.isEmpty()) {
                        showEmptyState()
                        addLog("Нет лекарств на сегодня")
                    } else {
                        showContentState()
                        addLog("=== ОБНОВЛЕНИЕ СПИСКА ЛЕКАРСТВ ===")
                        addLog("Получено лекарств: ${medicines.size}")
                        addLog("Текущее время: ${LocalDateTime.now()}")
                        
                        // Отслеживаем просроченные лекарства для логирования
                        var overdueCount = 0
                        
                        medicines.forEach { medicine ->
                            try {
                                val status = com.medicalnotes.app.utils.DosageCalculator.getMedicineStatus(medicine)
                                addLog("Отображается: ${medicine.name} - Статус: $status")
                                addLog("  Время приема: ${medicine.time}")
                                addLog("  Частота: ${medicine.frequency}")
                                
                                if (status == MedicineStatus.OVERDUE) {
                                    overdueCount++
                                    addLog(" ПРОСРОЧЕНО: ${medicine.name}")
                                } else if (status == MedicineStatus.UPCOMING) {
                                    addLog("📅 ПРЕДСТОИТ: ${medicine.name} - время еще не пришло")
                                } else if (status == MedicineStatus.TAKEN_TODAY) {
                                    addLog(" ПРИНЯТО: ${medicine.name} - уже принято сегодня")
                                } else {
                                    addLog(" НЕ СЕГОДНЯ: ${medicine.name} - не по расписанию")
                                }
                            } catch (e: Exception) {
                                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error processing medicine ${medicine.name}", e)
                                addLog(" ОШИБКА обработки: ${medicine.name}")
                            }
                        }
                        
                        // Логируем итоговую статистику
                        addLog("📊 ИТОГО: просроченных лекарств: $overdueCount")
                        
                        // Проверяем, нужно ли показывать группированные карточки
                        try {
                            //  ИСПРАВЛЕНО: medicines уже отфильтрованы в MainViewModel
                            // НЕ вызываем DosageCalculator.getActiveMedicinesForDate() повторно!
                            addLog("=== ОБРАБОТКА УЖЕ ОТФИЛЬТРОВАННЫХ ДАННЫХ ===")
                            addLog("Входные данные (уже отфильтрованы): ${medicines.size} лекарств")
                            medicines.forEach { medicine ->
                                addLog("  - ${medicine.name}: активен=${medicine.isActive}, принято=${medicine.takenToday}")
                            }
                            
                            // Проверяем, есть ли лекарства в группах среди активных
                            val groupedMedicines = medicines.filter { it.groupName.isNotEmpty() }
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
                                    
                                    // ИСПРАВЛЕНО: Показываем только те лекарства, которые должны приниматься сегодня
                                    // В группе "через день" только одно лекарство должно приниматься в день
                                    val today = java.time.LocalDate.now()
                                    addLog("=== ПОВТОРНАЯ ФИЛЬТРАЦИЯ ГРУППОВЫХ ЛЕКАРСТВ ===")
                                    addLog("Группа: $groupName")
                                    addLog("Сегодня: $today")
                                    
                                    val medicinesForToday = sortedGroupMedicines.filter { medicine ->
                                        addLog("Проверяем: ${medicine.name}")
                                        addLog("  - groupId: ${medicine.groupId}")
                                        addLog("  - groupName: ${medicine.groupName}")
                                        addLog("  - groupOrder: ${medicine.groupOrder}")
                                        addLog("  - groupStartDate: ${medicine.groupStartDate}")
                                        addLog("  - groupFrequency: ${medicine.groupFrequency}")
                                        addLog("  - frequency: ${medicine.frequency}")
                                        
                                        val shouldTake = com.medicalnotes.app.utils.DosageCalculator.shouldTakeMedicine(medicine, today)
                                        addLog("  - shouldTakeMedicine: $shouldTake")
                                        
                                        shouldTake
                                    }
                                    
                                    addLog("В группе '$groupName' на сегодня: ${medicinesForToday.size} лекарств")
                                    medicinesForToday.forEach { medicine ->
                                        addLog("  - ${medicine.name} (groupOrder=${medicine.groupOrder})")
                                    }
                                    
                                    // Добавляем только лекарства, которые должны приниматься сегодня
                                    displayList.addAll(medicinesForToday)
                                }
                                
                                // Добавляем лекарства без групп
                                val nonGroupedMedicines = medicines.filter { it.groupName.isEmpty() }
                                displayList.addAll(nonGroupedMedicines)
                                
                                addLog("Итого для отображения: ${displayList.size} лекарств")
                                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                                todayMedicineAdapter.submitList(displayList)
                                
                                addLog("Показаны группированные карточки")
                            } else {
                                // Показываем обычные карточки
                                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                                todayMedicineAdapter.submitList(medicines)
                                
                                addLog("Показаны обычные карточки")
                            }
                        } catch (e: Exception) {
                            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error with grouping logic", e)
                            // Fallback - показываем обычные карточки
                            binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                            todayMedicineAdapter.submitList(medicines)
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
            
            //  ДОБАВЛЕНО: Проверка просроченных лекарств при возвращении в приложение
            checkOverdueMedicines()
            
            //  ДОБАВЛЕНО: Перезапуск периодической проверки при возвращении в приложение
            startPeriodicOverdueCheck()
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in onResume", e)
            android.widget.Toast.makeText(this, "Ошибка обновления данных", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    


    private fun takeMedicine(medicine: Medicine) {
        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПРИЕМ ЛЕКАРСТВА ===")
        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Лекарство: ${medicine.name} (ID: ${medicine.id})")
        
        //  ДОБАВЛЕНО: Подробное логирование нажатия кнопки
        addLog("=== НАЖАТИЕ КНОПКИ 'ПРИНЯЛ ЛЕКАРСТВА' ===")
        addLog("Лекарство: ${medicine.name} (ID: ${medicine.id})")
        addLog("Время приема: ${medicine.time}")
        addLog("Принято сегодня: ${medicine.takenToday}")
        addLog("Текущее время: ${LocalDateTime.now()}")
        addLog("Статус лекарства: ${com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)}")
        
        //  ДОБАВЛЕНО: Немедленная остановка всех звуков и уведомлений
        addLog(" НАЧИНАЕМ ОСТАНОВКУ ЗВУКА И УВЕДОМЛЕНИЙ")
        try {
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_НАЖАТА", "Кнопка 'принял лекарство' нажата для: ${medicine.name} (ID: ${medicine.id})")
            addLog(" КНОПКА_НАЖАТА: Кнопка 'принял лекарство' нажата для: ${medicine.name}")
            
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
            addLog(" NotificationManager создан")
            
            // ИСПРАВЛЕНО: Простая остановка вибрации и уведомлений
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ДЕЙСТВИЕ", "Останавливаем вибрацию для: ${medicine.name}")
            addLog(" ОСТАНАВЛИВАЕМ вибрацию для: ${medicine.name}")
            notificationManager.stopVibration()
            
            // Отменяем уведомления для этого лекарства
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ДЕЙСТВИЕ", "Отменяем уведомления для: ${medicine.name}")
            addLog(" ОТМЕНЯЕМ уведомления для: ${medicine.name}")
            notificationManager.cancelOverdueNotification(medicine.id)
            notificationManager.cancelMedicineNotification(medicine.id)
            
            // Останавливаем периодическую проверку
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ПЕРИОДИЧЕСКАЯ", "Останавливаем периодическую проверку для: ${medicine.name}")
            addLog(" ОСТАНАВЛИВАЕМ периодическую проверку для: ${medicine.name}")
            stopPeriodicOverdueCheck()
            
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ЗАВЕРШЕНА", "Все действия по остановке завершены для: ${medicine.name}")
            addLog(" ВСЕ ДЕЙСТВИЯ ПО ОСТАНОВКЕ ЗАВЕРШЕНЫ для: ${medicine.name}")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e(" КНОПКА_ОШИБКА", "Ошибка отмены уведомлений для: ${medicine.name}", e)
            addLog(" ОШИБКА отмены уведомлений для: ${medicine.name} - ${e.message}")
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //  ИСПРАВЛЕНО: Уменьшаем количество таблеток на 1
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
                    remainingQuantity = newRemainingQuantity //  ИСПРАВЛЕНО: Уменьшаем количество
                )
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Обновляем лекарство: takenToday=true, isMissed=false")
                addLog("ОБНОВЛЯЕМ лекарство: takenToday=true, isMissed=false")
                viewModel.updateMedicine(updatedMedicine)
                
                //  ДОБАВЛЕНО: Предупреждение о заканчивающихся таблетках
                if (newRemainingQuantity <= 5 && newRemainingQuantity > 0) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", getString(com.medicalnotes.app.R.string.warning_low_supply, medicine.name))
                    addLog(getString(com.medicalnotes.app.R.string.warning_low_supply, medicine.name) + " (осталось: $newRemainingQuantity)")
                    
                    lifecycleScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            " Заканчиваются ${medicine.medicineType.lowercase()} ${medicine.name} (осталось: $newRemainingQuantity)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (newRemainingQuantity == 0) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", getString(com.medicalnotes.app.R.string.critical_no_supply, medicine.name))
                    addLog(getString(com.medicalnotes.app.R.string.critical_no_supply, medicine.name))
                    
                    lifecycleScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            " ${medicine.medicineType.lowercase().replaceFirstChar { it.uppercase() }} ${medicine.name} закончились! Нужно пополнить запас!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                // Считаем успешным, если нет исключений
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", getString(com.medicalnotes.app.R.string.medicine_successfully_updated))
                addLog(getString(com.medicalnotes.app.R.string.medicine_successfully_updated))
                
                lifecycleScope.launch(Dispatchers.Main) {
                    //  ИСПРАВЛЕНО: Правильное обновление списка лекарств на сегодня
                    addLog("ИСПРАВЛЕНО: Правильное обновление списка лекарств на сегодня")
                    
                    // Принудительно очищаем UI перед обновлением
                    todayMedicineAdapter.submitList(emptyList())
                    addLog("UI очищен перед обновлением")
                    
                    // Небольшая задержка для синхронизации
                    kotlinx.coroutines.delay(100)
                    
                    // Перезагружаем все данные
                    viewModel.loadAllMedicines()
                    
                    // Перезагружаем лекарства на сегодня
                    loadTodayMedicines()
                    
                    addLog("Список лекарств на сегодня перезагружен")
                    
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", getString(com.medicalnotes.app.R.string.medicine_list_updated))
                    addLog(getString(com.medicalnotes.app.R.string.medicine_list_updated))
                    
                    //  ДОБАВЛЕНО: Логирование завершения обновления
                    addLog(getString(com.medicalnotes.app.R.string.update_completed))
                    addLog(getString(com.medicalnotes.app.R.string.medicine_info, medicine.name, medicine.id))
                    addLog(getString(com.medicalnotes.app.R.string.status_taken_today))
                    addLog(getString(com.medicalnotes.app.R.string.update_time, LocalDateTime.now().toString()))
                    
                    //  ДОБАВЛЕНО: Перезапускаем периодическую проверку через 10 секунд
                    com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ПЕРИОДИЧЕСКАЯ", getString(com.medicalnotes.app.R.string.restarting_periodic_check))
                    addLog(getString(com.medicalnotes.app.R.string.restarting_periodic_check))
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startPeriodicOverdueCheck()
                        com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ПЕРИОДИЧЕСКАЯ", getString(com.medicalnotes.app.R.string.periodic_check_restarted))
                        addLog(getString(com.medicalnotes.app.R.string.periodic_check_restarted))
                    }, 10000) // Увеличиваем задержку до 10 секунд
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", getString(com.medicalnotes.app.R.string.error_updating_medicine, e.message ?: ""), e)
                addLog(getString(com.medicalnotes.app.R.string.error_updating_medicine, e.message ?: ""))
                
                lifecycleScope.launch(Dispatchers.Main) {
                    //  ИЗМЕНЕНО: Показываем Toast только при ошибке, но без звука
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        getString(com.medicalnotes.app.R.string.error_updating_medicine, e.message ?: ""),
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
        editMedicineLauncher.launch(intent)
    }

    private fun deleteMedicine(medicine: Medicine) {
        addLog("=== НАЧАЛО УДАЛЕНИЯ ЛЕКАРСТВА ===")
        addLog("Лекарство для удаления: ${medicine.name} (ID: ${medicine.id})")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Удаление лекарства")
            .setMessage("Вы уверены, что хотите удалить лекарство '${medicine.name}'?")
            .setPositiveButton("Удалить") { _, _ ->
                addLog("Пользователь подтвердил удаление")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        addLog("Вызываем viewModel.deleteMedicine")
                        viewModel.deleteMedicine(medicine.id)
                        addLog("viewModel.deleteMedicine завершен")
                        
                        lifecycleScope.launch(Dispatchers.Main) {
                            addLog("Показываем уведомление об успешном удалении")
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Лекарство удалено",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            
                            // Принудительно обновляем список
                            addLog("Принудительно обновляем список лекарств")
                            loadTodayMedicines()
                        }
                    } catch (e: Exception) {
                        addLog("ОШИБКА при удалении: ${e.message}")
                        lifecycleScope.launch(Dispatchers.Main) {
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
            //  ИСПРАВЛЕНО: Проверяем, что Activity не уничтожена и binding существует
            if (isDestroyed || isFinishing || !::binding.isInitialized) {
                return
            }
            
            val timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            val logMessage = "[$timestamp] $message"
            
            //  ДОБАВЛЕНО: Добавляем в список логов
            logs.add(logMessage)
            
            // Ограничиваем размер списка логов (последние 1000 записей)
            if (logs.size > 1000) {
                logs.removeAt(0)
            }
            
            //  ИСПРАВЛЕНО: Безопасное обновление UI на главном потоке
            runOnUiThread {
                try {
                    if (!isDestroyed && !isFinishing && ::binding.isInitialized) {
                        binding.textViewLogs.append("$logMessage\n")
                        
                        //  ИСПРАВЛЕНО: Безопасная прокрутка с post
                        binding.textViewLogs.post {
                            try {
                                if (!isDestroyed && binding.textViewLogs.layout != null) {
                                    val layout = binding.textViewLogs.layout
                                    val lineCount = binding.textViewLogs.lineCount
                                    if (lineCount > 0) {
                                        val scrollAmount = layout.getLineTop(lineCount) - binding.textViewLogs.height
                                        if (scrollAmount > 0) {
                                            binding.textViewLogs.scrollTo(0, scrollAmount)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Игнорируем ошибки прокрутки - они не критичны
                            }
                        }
                    }
                } catch (e: Exception) {
                    com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error updating logs UI", e)
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error adding log", e)
        }
    }

    private fun clearLogs() {
        try {
            //  ДОБАВЛЕНО: Очищаем список логов
            logs.clear()
            binding.textViewLogs.text = ""
            addLog("=== ЛОГИ ОЧИЩЕНЫ ===")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error clearing logs", e)
        }
    }

    private fun loadTodayMedicines() {
        addLog("=== ВЫЗОВ loadTodayMedicines() ===")
        showLoadingState()
        addLog("Вызываем viewModel.loadTodayMedicines()")
        viewModel.loadTodayMedicines()
        addLog("viewModel.loadTodayMedicines() вызван")
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val medicines = viewModel.allMedicines.value ?: emptyList()
                val exportData = medicines.joinToString("\n") { medicine ->
                    "${medicine.name} - ${medicine.dosage} - ${medicine.time}"
                }
                
                val fileName = "medical_notes_export_${LocalDate.now()}.txt"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(exportData)
                
                lifecycleScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Данные экспортированы в $fileName",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    addLog("Экспорт данных: $fileName")
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
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
    
    //  ДОБАВЛЕНО: Метод копирования лога в буфер обмена
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
    
    //  ДОБАВЛЕНО: Автоматическая проверка просроченных лекарств
    private fun checkOverdueMedicines() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===")
                lifecycleScope.launch(Dispatchers.Main) { addLog("=== ПРОВЕРКА ПРОСРОЧЕННЫХ ЛЕКАРСТВ ===") }
                
                //  ИСПРАВЛЕНО: Загружаем данные, если они еще не загружены
                var medicines = viewModel.allMedicines.value ?: emptyList()
                if (medicines.isEmpty()) {
                    // Пробуем загрузить данные напрямую
                    try {
                        val dataManager = com.medicalnotes.app.utils.DataManager(this@MainActivity)
                        medicines = dataManager.loadMedicines()
                        lifecycleScope.launch(Dispatchers.Main) { 
                            addLog("Данные загружены напрямую из DataManager: ${medicines.size} лекарств")
                        }
                    } catch (e: Exception) {
                        lifecycleScope.launch(Dispatchers.Main) { 
                            addLog(" Ошибка загрузки данных: ${e.message}")
                        }
                        return@launch
                    }
                }
                
                lifecycleScope.launch(Dispatchers.Main) { addLog("Всего лекарств в базе: ${medicines.size}") }
                
                medicines.forEach { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    lifecycleScope.launch(Dispatchers.Main) { 
                        addLog("ПРОВЕРКА: ${medicine.name} - Статус: $status, Время: ${medicine.time}, Принято сегодня: ${medicine.takenToday}")
                    }
                    
                    if (status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE) {
                        lifecycleScope.launch(Dispatchers.Main) { 
                            addLog(" НАЙДЕНО ПРОСРОЧЕННОЕ: ${medicine.name} (принято сегодня: ${medicine.takenToday})")
                        }
                    }
                }
                
                val overdueMedicines = medicines.filter { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    val isOverdue = status == com.medicalnotes.app.utils.MedicineStatus.OVERDUE
                    val notTakenToday = !medicine.takenToday
                    
                    lifecycleScope.launch(Dispatchers.Main) { 
                        addLog("🔍 ФИЛЬТРАЦИЯ: ${medicine.name} - Статус: $status, Просрочено: $isOverdue, Не принято сегодня: $notTakenToday")
                    }
                    
                    isOverdue && notTakenToday
                }
                
                lifecycleScope.launch(Dispatchers.Main) { 
                    addLog("Найдено просроченных лекарств (не принятых сегодня): ${overdueMedicines.size}")
                }
                
                if (overdueMedicines.isNotEmpty()) {
                    // ИСПРАВЛЕНО: Убираем дублирование уведомлений - OverdueCheckService уже создает уведомления
                    addLog(" Найдено ${overdueMedicines.size} просроченных лекарств - уведомления создаются сервисом")
                    
                    lifecycleScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Найдено ${overdueMedicines.size} просроченных лекарств!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) { addLog("Просроченных лекарств не найдено") }
                }
                
                lifecycleScope.launch(Dispatchers.Main) { addLog("=== ПРОВЕРКА ЗАВЕРШЕНА ===") }
                
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки просроченных лекарств", e)
                lifecycleScope.launch(Dispatchers.Main) { 
                    addLog(" ОШИБКА проверки просроченных лекарств: ${e.message}")
                }
            }
        }
    }
    
    //  ИСПРАВЛЕНО: Периодическая проверка просроченных лекарств (оптимизировано для предотвращения ANR)
    private fun startPeriodicOverdueCheck() {
        try {
            overdueCheckTimer = android.os.Handler(android.os.Looper.getMainLooper())
            
            val checkRunnable = object : Runnable {
                override fun run() {
                    try {
                        com.medicalnotes.app.utils.LogCollector.d("MainActivity", "=== ПЕРИОДИЧЕСКАЯ ПРОВЕРКА ПРОСРОЧЕННЫХ ===")
                        
                        //  ИСПРАВЛЕНО: Переносим тяжелую операцию в фоновый поток
                        lifecycleScope.launch(Dispatchers.IO) {
                            checkOverdueMedicines()
                        }
                        
                        //  ИСПРАВЛЕНО: Увеличиваем интервал до 2 минут для предотвращения ANR
                        overdueCheckTimer?.postDelayed(this, 120000) // 2 минуты вместо 30 секунд
                    } catch (e: Exception) {
                        com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка периодической проверки", e)
                    }
                }
            }
            
            // Запускаем первую проверку через 2 минуты
            overdueCheckTimer?.postDelayed(checkRunnable, 120000)
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Периодическая проверка запущена (каждые 2 минуты)")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка запуска периодической проверки", e)
        }
    }
    
    //  ДОБАВЛЕНО: Остановка периодической проверки
    private fun stopPeriodicOverdueCheck() {
        try {
            overdueCheckTimer?.removeCallbacksAndMessages(null)
            overdueCheckTimer = null
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Периодическая проверка остановлена")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка остановки периодической проверки", e)
        }
    }
    
    //  ДОБАВЛЕНО: Запуск сервиса уведомлений
    private fun startNotificationService() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Запуск сервиса уведомлений")
            com.medicalnotes.app.service.NotificationService.startService(this)
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Сервис уведомлений запущен")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка запуска сервиса уведомлений", e)
        }
    }
    
    //  ДОБАВЛЕНО: Запуск сервиса проверки просроченных лекарств
    private fun startOverdueCheckService() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Запуск сервиса проверки просроченных лекарств")
            addLog(" Запуск сервиса проверки просроченных лекарств")
            
            com.medicalnotes.app.service.OverdueCheckService.startService(this)
            
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Сервис проверки просроченных лекарств запущен")
            addLog(" Сервис проверки просроченных лекарств запущен")
            
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
            addLog(" Ошибка запуска сервиса: ${e.message}")
        }
    }
    
    //  ДОБАВЛЕНО: Проверка, запущен ли сервис
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return try {
            //  ИСПРАВЛЕНО: Используем современный подход без deprecated методов
            val packageManager = packageManager
            val intent = android.content.Intent(this, serviceClass)
            val resolveInfo = packageManager.queryIntentServices(intent, 0)
            
            // Проверяем, есть ли сервис в списке зарегистрированных
            if (resolveInfo.isNotEmpty()) {
                // Для более точной проверки используем альтернативный подход
                val serviceName = serviceClass.name
                
                // Проверяем через ActivityManager только если необходимо
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    @Suppress("DEPRECATION")
                    val runningServices = activityManager.getRunningServices(100)
                    runningServices.any { it.service.className == serviceName }
                } else {
                    // Для старых версий считаем, что сервис может быть запущен
                    true
                }
            } else {
                false
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error checking service status", e)
            false
        }
    }
    
    //  ДОБАВЛЕНО: Проверка и восстановление уведомлений
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
    
    //  ДОБАВЛЕНО: Проверка разрешений для уведомлений
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
    
    //  ДОБАВЛЕНО: Проверка разрешений для показа окон поверх других приложений
    private fun checkOverlayPermissions() {
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Проверка разрешений для показа окон поверх других приложений")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Нет разрешения на показ окон поверх других приложений")
                    addLog(" Нужно разрешение на показ окон поверх других приложений")
                    
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
                                addLog(" Открыто окно настроек разрешений")
                            } catch (e: Exception) {
                                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка открытия настроек разрешений", e)
                                addLog(" Ошибка открытия настроек: ${e.message}")
                            }
                        }
                        .setNegativeButton("Позже") { _, _ ->
                            addLog(" Настройка разрешений отложена")
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    com.medicalnotes.app.utils.LogCollector.d("MainActivity", "✓ Разрешение на показ окон поверх других приложений есть")
                    addLog(" Разрешение на показ окон есть")
                }
            } else {
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Android версия ниже 6.0, разрешение не требуется")
                addLog(" Android версия ниже 6.0, разрешение не требуется")
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Ошибка проверки разрешений для показа окон", e)
            addLog(" Ошибка проверки разрешений: ${e.message}")
        }
    }
    
    //  ДОБАВЛЕНО: Метод диагностики
    private fun performDiagnostic() {
        try {
            addLog("🔍 ЗАПУСК ДИАГНОСТИКИ...")
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val diagnostic = com.medicalnotes.app.utils.DiagnosticUtils.performFullDiagnostic(this@MainActivity)
                    
                    lifecycleScope.launch(Dispatchers.Main) {
                        addLog("🔍 ДИАГНОСТИКА ЗАВЕРШЕНА")
                        addLog(" Результат:")
                        addLog(diagnostic)
                        
                        // Сохраняем отчет в файл
                        val saveResult = com.medicalnotes.app.utils.DiagnosticUtils.saveDiagnosticReport(this@MainActivity, diagnostic)
                        addLog("💾 $saveResult")
                        
                        android.widget.Toast.makeText(this@MainActivity, "Диагностика завершена", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        addLog(" ОШИБКА ДИАГНОСТИКИ: ${e.message}")
                        android.widget.Toast.makeText(this@MainActivity, "Ошибка диагностики: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in performDiagnostic", e)
            addLog(" КРИТИЧЕСКАЯ ОШИБКА ДИАГНОСТИКИ: ${e.message}")
        }
    }
    
    //  ДОБАВЛЕНО: Метод исправления проблем
    private fun fixIssues() {
        try {
            addLog(" ЗАПУСК ИСПРАВЛЕНИЯ ПРОБЛЕМ...")
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val fixResult = com.medicalnotes.app.utils.DiagnosticUtils.fixCommonIssues(this@MainActivity)
                    
                    lifecycleScope.launch(Dispatchers.Main) {
                        addLog(" ИСПРАВЛЕНИЕ ЗАВЕРШЕНО")
                        addLog(" Результат:")
                        addLog(fixResult)
                        
                        android.widget.Toast.makeText(this@MainActivity, "Исправление завершено", android.widget.Toast.LENGTH_LONG).show()
                        
                        // Перезагружаем данные после исправления
                        try {
                            viewModel.loadAllMedicines()
                            loadTodayMedicines()
                            addLog("🔄 Данные перезагружены после исправления")
                        } catch (e: Exception) {
                            addLog(" ОШИБКА ПЕРЕЗАГРУЗКИ: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        addLog(" ОШИБКА ИСПРАВЛЕНИЯ: ${e.message}")
                        android.widget.Toast.makeText(this@MainActivity, "Ошибка исправления: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in fixIssues", e)
            addLog(" КРИТИЧЕСКАЯ ОШИБКА ИСПРАВЛЕНИЯ: ${e.message}")
        }
    }
    
    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Updating UI after language change")
            
            // Обновляем заголовок Activity
            title = getString(R.string.app_name)
            supportActionBar?.title = getString(R.string.app_name)
            
            // ВАЖНО: Явно обновляем заголовок toolbar
            binding.toolbar.setTitle(getString(R.string.app_name))
            
            // ДОБАВЛЕНО: Отладочная информация
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "updateUIAfterLanguageChange: Setting toolbar title to: ${getString(R.string.app_name)}")
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "updateUIAfterLanguageChange: Current toolbar title is: ${binding.toolbar.title}")
            
            // ДОБАВЛЕНО: Принудительное обновление toolbar
            binding.toolbar.invalidate()
            
            // ДОБАВЛЕНО: Устанавливаем заголовок после обновления view
            binding.toolbar.post {
                binding.toolbar.setTitle(getString(R.string.app_name))
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "updateUIAfterLanguageChange: Post-set toolbar title to: ${getString(R.string.app_name)}")
            }
            
            // Обновляем подзаголовок с версией
            binding.toolbar.subtitle = VersionUtils.getShortVersionInfo(this)
            
            // Обновляем текст в Navigation Drawer
            updateNavigationDrawerText()
            
            // Обновляем адаптеры
            todayMedicineAdapter.updateLanguage()
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            // Обновляем меню
            invalidateOptionsMenu()
            
            com.medicalnotes.app.utils.LogCollector.d("MainActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error updating UI after language change", e)
        }
    }
    
    private fun updateNavigationDrawerText() {
        try {
            // Обновляем заголовки в Navigation Drawer (если есть header)
            // ID nav_header_title и nav_header_subtitle не определены в ресурсах
            // try {
            //     binding.navigationView.getHeaderView(0)?.findViewById<android.widget.TextView>(R.id.nav_header_title)?.text = getString(R.string.app_name)
            //     binding.navigationView.getHeaderView(0)?.findViewById<android.widget.TextView>(R.id.nav_header_subtitle)?.text = getString(R.string.app_description)
            // } catch (e: Exception) {
            //     com.medicalnotes.app.utils.LogCollector.w("MainActivity", "Navigation header not found")
            // }
            
            // Обновляем пункты меню (если они есть)
            try {
                // Проверяем существование каждого пункта меню перед обновлением
                val navMenu = binding.navigationView.menu
                
                // Обновляем только существующие пункты меню
                navMenu.findItem(R.id.nav_manage_medicines)?.let { item ->
                    item.title = getString(R.string.medicines)
                }
                navMenu.findItem(R.id.nav_manage_groups)?.let { item ->
                    item.title = getString(R.string.groups)
                }
                navMenu.findItem(R.id.nav_settings)?.let { item ->
                    item.title = getString(R.string.settings)
                }
                navMenu.findItem(R.id.nav_elderly_mode)?.let { item ->
                    item.title = getString(R.string.elderly_mode)
                }
                navMenu.findItem(R.id.nav_logs)?.let { item ->
                    item.title = getString(R.string.system_logs)
                }
                navMenu.findItem(R.id.nav_export_data)?.let { item ->
                    item.title = getString(R.string.export_data)
                }
                navMenu.findItem(R.id.nav_test_groups)?.let { item ->
                    item.title = getString(R.string.test_groups)
                }
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.w("MainActivity", "Navigation menu items not found")
            }
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error updating navigation drawer text", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок, если они есть
            // FAB обычно не имеет текста, но можно обновить contentDescription
            binding.fabAddMedicine.contentDescription = getString(R.string.add_medicine)
            
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error updating button texts", e)
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