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
import com.medicalnotes.app.service.OverdueCheckService
import com.medicalnotes.app.utils.UnifiedNotificationManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var todayMedicineAdapter: MainMedicineAdapter
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private var overdueCheckTimer: android.os.Handler? = null
    
    //  ДОБАВЛЕНО: Список для хранения логов
    private val logs = mutableListOf<String>()
    
    //  ДОБАВЛЕНО: Переменная для тестовой даты
    private var selectedTestDate: LocalDate = LocalDate.now()
    
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
                viewModel.loadMedicinesForDate(LocalDate.now())
                
                // Принудительно обновляем статусы
                checkOverdueMedicines()
                
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "Обработка результата EditMedicineActivity завершена")
            }
        }
    }
    
    // ИСПРАВЛЕНО: Обработка intent extras для уведомлений
    private fun handleNotificationIntent() {
        try {
            val takeMedicine = intent.getBooleanExtra("take_medicine", false)
            val showOverdueMedicines = intent.getBooleanExtra("show_overdue_medicines", false)
            val alarmNotification = intent.getBooleanExtra("alarm_notification", false)
            
            if (takeMedicine) {
                val overdueMedicineIds = intent.getParcelableArrayListExtra("overdue_medicines", Long::class.java)
                if (!overdueMedicineIds.isNullOrEmpty()) {
                    // Останавливаем звуки и вибрацию
                    OverdueCheckService.forceStopSoundAndVibration(this@MainActivity)
                    
                    // Помечаем лекарства как принятые
                    markOverdueMedicinesAsTaken(overdueMedicineIds)
                    
                    android.widget.Toast.makeText(this, "Лекарства помечены как принятые", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            if (showOverdueMedicines) {
                // Показываем список просроченных лекарств
                android.widget.Toast.makeText(this, "Показаны просроченные лекарства", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            // ИСПРАВЛЕНО: Обработка уведомления от AlarmManager
            if (alarmNotification) {
                android.util.Log.d("MainActivity", "Получено уведомление от AlarmManager")
                
                // Принудительно проверяем просроченные лекарства
                checkOverdueMedicines()
                
                // Показываем уведомление пользователю
                android.widget.Toast.makeText(this, "🚨 Проверьте просроченные лекарства!", android.widget.Toast.LENGTH_LONG).show()
                
                // Дополнительно запускаем сервис для показа уведомления
                OverdueCheckService.startService(this@MainActivity)
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error handling notification intent", e)
        }
    }
    
    // Помечаем просроченные лекарства как принятые
    private fun markOverdueMedicinesAsTaken(medicineIds: ArrayList<Long>) {
        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val dataManager = com.medicalnotes.app.utils.DataManager(this@MainActivity)
                val allMedicines = dataManager.loadMedicines()
                
                val updatedMedicines = allMedicines.map { medicine ->
                    if (medicineIds.contains(medicine.id)) {
                        medicine.copy(takenToday = true)
                    } else {
                        medicine
                    }
                }
                
                dataManager.saveMedicines(updatedMedicines)
                
                // Обновляем UI на главном потоке
                lifecycleScope.launch(Dispatchers.Main) {
                    loadTodayMedicines()
                    checkOverdueMedicines()
                }
            }
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error marking medicines as taken", e)
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
        
        // ИСПРАВЛЕНО: Инициализация binding перед setContentView
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("MainActivity", "Binding успешно инициализирован")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Критическая ошибка инициализации binding", e)
            
            // ДОБАВЛЕНО: Показываем диалог с информацией об ошибке
            showErrorDialog("Ошибка инициализации binding", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
            
            // Fallback - используем обычный setContentView
            setContentView(R.layout.activity_main)
            android.widget.Toast.makeText(this, "Ошибка инициализации интерфейса", android.widget.Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // Инициализация компонентов ПЕРЕД проверкой разрешений
        try {
            initializeComponents()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка инициализации компонентов", e)
            showErrorDialog("Ошибка инициализации компонентов", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
            finish()
            return
        }
        
        // ДОБАВЛЕНО: Диагностика UI состояния
        try {
            diagnoseUIState()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка диагностики UI", e)
            showErrorDialog("Ошибка диагностики UI", "Детали ошибки:\n${e.message}")
        }
        
        // ДОБАВЛЕНО: Проверка разрешений после инициализации UI
        try {
            checkAndRequestPermissions()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка проверки разрешений", e)
            showErrorDialog("Ошибка проверки разрешений", "Детали ошибки:\n${e.message}")
        }
        
        // Обработка входящих интентов
        try {
            handleNotificationIntent()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка обработки интентов", e)
            showErrorDialog("Ошибка обработки интентов", "Детали ошибки:\n${e.message}")
        }
        
        // ИСПРАВЛЕНО: Проверяем и сбрасываем некорректные статусы при запуске
        try {
            fixIncorrectTakenTodayStatus()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка исправления статусов", e)
        }
    }

    private fun initializeComponents() {
        try {
            // ИСПРАВЛЕНО: Проверка инициализации binding
            if (!::binding.isInitialized) {
                android.util.Log.e("MainActivity", "Binding не инициализирован в initializeComponents")
                android.widget.Toast.makeText(this, "Ошибка инициализации интерфейса", android.widget.Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            android.util.Log.d("MainActivity", "Binding проверен, продолжаем инициализацию")
            
            // Инициализация ViewModel и Repository с проверками
            try {
                viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                userPreferencesRepository = UserPreferencesRepository(this)
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "ViewModel and Repository initialized")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error initializing ViewModel/Repository", e)
                showErrorDialog("Ошибка инициализации ViewModel/Repository", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
                finish()
                return
            }

            // ИСПРАВЛЕНО: Инициализация UnifiedNotificationManager
            try {
                UnifiedNotificationManager.createNotificationChannels(this)
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "UnifiedNotificationManager initialized")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error initializing UnifiedNotificationManager", e)
                showErrorDialog("Ошибка инициализации уведомлений", "Детали ошибки:\n${e.message}")
            }

            // Настройка UI с дополнительными проверками
            try {
                setupViews()
                setupNavigationDrawer()
                setupButtons()
                observeData()
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "UI setup completed")
            } catch (e: Exception) {
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error in UI setup", e)
                showErrorDialog("Ошибка настройки UI", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
                finish()
                return
            }

            // Загрузка данных с проверками
            try {
                android.util.Log.d("MainActivity", "🚀 === НАЧАЛО ЗАГРУЗКИ ДАННЫХ ===")
                android.util.Log.d("MainActivity", "viewModel доступен: ${viewModel != null}")
                android.util.Log.d("MainActivity", "viewModel.todayMedicines доступен: ${viewModel.todayMedicines != null}")
                
                android.util.Log.d("MainActivity", "Вызываем viewModel.loadAllMedicines()")
                viewModel.loadAllMedicines()
                android.util.Log.d("MainActivity", "✅ viewModel.loadAllMedicines() завершен")
                
                android.util.Log.d("MainActivity", "Вызываем loadTodayMedicines()")
                viewModel.loadMedicinesForDate(LocalDate.now())
                android.util.Log.d("MainActivity", "✅ viewModel.loadMedicinesForDate() завершен")
                
                com.medicalnotes.app.utils.LogCollector.i("MainActivity", "Data loading completed")
                android.util.Log.d("MainActivity", "=== ЗАГРУЗКА ДАННЫХ ЗАВЕРШЕНА ===")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ ОШИБКА при загрузке данных", e)
                com.medicalnotes.app.utils.LogCollector.e("MainActivity", "Error loading data", e)
                showErrorDialog("Ошибка загрузки данных", "Детали ошибки:\n${e.message}")
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
            showErrorDialog("Критическая ошибка инициализации", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
            finish()
        }
    }

    private fun setupViews() {
        try {
            android.util.Log.d("MainActivity", "setupViews: Начало настройки UI")
            
            // Настройка toolbar с проверками
            try {
                android.util.Log.d("MainActivity", "setupViews: Настройка toolbar")
                setSupportActionBar(binding.toolbar)
                binding.toolbar.setTitle(getString(R.string.app_name))
                binding.toolbar.subtitle = VersionUtils.getShortVersionInfo(this)
                
                // ДОБАВЛЕНО: Настройка ActionBarDrawerToggle для работы с navigation drawer
                val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
                    this, 
                    binding.drawerLayout, 
                    binding.toolbar, 
                    R.string.navigation_drawer_open, 
                    R.string.navigation_drawer_close
                )
                binding.drawerLayout.addDrawerListener(toggle)
                toggle.syncState()
                
                // ДОБАВЛЕНО: Отладочная информация
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "setupViews: Setting toolbar title to: ${getString(R.string.app_name)}")
                com.medicalnotes.app.utils.LogCollector.d("MainActivity", "setupViews: Current toolbar title is: ${binding.toolbar.title}")
                
                // ДОБАВЛЕНО: Принудительное обновление toolbar
                binding.toolbar.invalidate()
                
                android.util.Log.d("MainActivity", "setupViews: Toolbar настроен успешно")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupViews: Ошибка настройки toolbar", e)
                showErrorDialog("Ошибка настройки toolbar", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
                throw e
            }
            
            // Настройка RecyclerView с проверками
            try {
                android.util.Log.d("MainActivity", "setupViews: Настройка RecyclerView")
                binding.recyclerViewTodayMedicines.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                todayMedicineAdapter = MainMedicineAdapter(
                    onMedicineClick = { medicine ->
                        try {
                            val intent = android.content.Intent(this, EditMedicineActivity::class.java)
                            intent.putExtra("medicine_id", medicine.id)
                            startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Ошибка открытия редактирования лекарства", e)
                            showErrorDialog("Ошибка открытия редактирования", "Детали ошибки:\n${e.message}")
                        }
                    },
                    onTakeMedicineClick = { medicine ->
                        try {
                            android.util.Log.d("MainActivity", "=== НАЖАТИЕ КНОПКИ 'ПРИНЯТЬ ЛЕКАРСТВО' ===")
                            android.util.Log.d("MainActivity", "Лекарство: ${medicine.name}")
                            android.util.Log.d("MainActivity", "ID: ${medicine.id}")
                            
                            // 1. Отмечаем лекарство как принятое
                            android.util.Log.d("MainActivity", "1. Отмечаем лекарство как принятое...")
                            val updatedMedicine = com.medicalnotes.app.utils.MedicineStatusHelper.markAsTaken(medicine)
                            viewModel.updateMedicine(updatedMedicine)
                            android.util.Log.d("MainActivity", "✓ Лекарство отмечено как принятое")
                            
                            // 2. Останавливаем звуки и вибрацию через OverdueCheckService
                            android.util.Log.d("MainActivity", "2. Останавливаем звуки и вибрацию...")
                            com.medicalnotes.app.service.OverdueCheckService.forceStopSoundAndVibration(this)
                            android.util.Log.d("MainActivity", "✓ Звуки и вибрация остановлены")
                            
                            // 3. Отменяем все уведомления через UnifiedNotificationManager
                            android.util.Log.d("MainActivity", "3. Отменяем уведомления...")
                            com.medicalnotes.app.utils.UnifiedNotificationManager.cancelAllNotifications(this)
                            android.util.Log.d("MainActivity", "✓ Все уведомления отменены")
                            
                            // 4. Отменяем уведомления для конкретного лекарства
                            com.medicalnotes.app.utils.UnifiedNotificationManager.cancelMedicineNotifications(this, medicine.id)
                            android.util.Log.d("MainActivity", "✓ Уведомления для лекарства отменены")
                            
                            // 5. Показываем подтверждение пользователю
                            android.widget.Toast.makeText(
                                this, 
                                "Лекарство \"${medicine.name}\" отмечено как принятое", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            android.util.Log.d("MainActivity", "✓ Пользователю показано подтверждение")
                            
                            // 6. Принудительно перезагружаем список лекарств
                            android.util.Log.d("MainActivity", "4. Перезагружаем список лекарств...")
                            viewModel.loadMedicinesForDate(LocalDate.now())
                            android.util.Log.d("MainActivity", "✓ Список лекарств перезагружен")
                            
                            // 7. Дополнительная проверка - убеждаемся, что лекарство исчезло из списка
                            viewModel.todayMedicines.observe(this) { medicines ->
                                val medicineStillInList = medicines.any { it.id == medicine.id }
                                if (medicineStillInList) {
                                    android.util.Log.w("MainActivity", "⚠️ Лекарство все еще в списке, принудительно обновляем...")
                                    // Принудительно обновляем UI
                                    todayMedicineAdapter?.notifyDataSetChanged()
                                } else {
                                    android.util.Log.d("MainActivity", "✓ Лекарство успешно исчезло из списка")
                                }
                            }
                            
                            android.util.Log.d("MainActivity", "=== ОБРАБОТКА НАЖАТИЯ ЗАВЕРШЕНА ===")
                            
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Ошибка отметки лекарства как принятого", e)
                            showErrorDialog("Ошибка отметки лекарства", "Детали ошибки:\n${e.message}")
                        }
                    },
                    onAddBackTodayClick = { medicine ->
                        try {
                            android.util.Log.d("MainActivity", "=== НАЖАТИЕ КНОПКИ 'ДОБАВИТЬ ОБРАТНО НА СЕГОДНЯ' ===")
                            android.util.Log.d("MainActivity", "Лекарство: ${medicine.name}")
                            android.util.Log.d("MainActivity", "ID: ${medicine.id}")
                            
                            // 1. Сбрасываем статус "принято сегодня"
                            android.util.Log.d("MainActivity", "1. Сбрасываем статус 'принято сегодня'...")
                            val updatedMedicine = medicine.copy(
                                takenToday = false,
                                lastTakenTime = 0L
                            )
                            viewModel.updateMedicine(updatedMedicine)
                            android.util.Log.d("MainActivity", "✓ Статус сброшен")
                            
                            // 2. Показываем подтверждение пользователю
                            android.widget.Toast.makeText(
                                this, 
                                "Лекарство \"${medicine.name}\" добавлено обратно на сегодня", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            android.util.Log.d("MainActivity", "✓ Пользователю показано подтверждение")
                            
                            // 3. Принудительно перезагружаем список лекарств
                            android.util.Log.d("MainActivity", "3. Перезагружаем список лекарств...")
                            viewModel.loadMedicinesForDate(LocalDate.now())
                            android.util.Log.d("MainActivity", "✓ Список лекарств перезагружен")
                            
                            android.util.Log.d("MainActivity", "=== ОБРАБОТКА НАЖАТИЯ ЗАВЕРШЕНА ===")
                            
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Ошибка добавления лекарства обратно на сегодня", e)
                            showErrorDialog("Ошибка добавления лекарства", "Детали ошибки:\n${e.message}")
                        }
                    }
                )
                binding.recyclerViewTodayMedicines.adapter = todayMedicineAdapter
                android.util.Log.d("MainActivity", "setupViews: RecyclerView настроен успешно")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupViews: Ошибка настройки RecyclerView", e)
                showErrorDialog("Ошибка настройки RecyclerView", "Детали ошибки:\n${e.message}")
                throw e
            }
            
            // ДОБАВЛЕНО: Настройка кнопки календаря для тестирования
            try {
                android.util.Log.d("MainActivity", "setupViews: Настройка кнопки календаря")
                binding.buttonSelectDate.setOnClickListener {
                    showDatePickerDialog()
                }
                
                // Обновляем отображение выбранной даты
                updateSelectedDateDisplay()
                
                android.util.Log.d("MainActivity", "setupViews: Кнопка календаря настроена успешно")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupViews: Ошибка настройки кнопки календаря", e)
                showErrorDialog("Ошибка настройки кнопки календаря", "Детали ошибки:\n${e.message}")
                throw e
            }
            
            android.util.Log.d("MainActivity", "setupViews: Все компоненты UI настроены успешно")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "setupViews: Критическая ошибка настройки UI", e)
            showErrorDialog("Критическая ошибка настройки UI", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
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
            android.util.Log.d("MainActivity", "setupButtons: Начало настройки кнопок")
            
            // Настройка FAB добавления лекарства
            try {
                binding.fabAddMedicine.setOnClickListener {
                    try {
                        android.util.Log.d("MainActivity", "FAB добавления лекарства нажата")
                        val intent = android.content.Intent(this, AddMedicineActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка при нажатии FAB добавления", e)
                        showErrorDialog("Ошибка FAB добавления", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
                    }
                }
                android.util.Log.d("MainActivity", "setupButtons: FAB добавления настроена")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupButtons: Ошибка настройки FAB добавления", e)
                showErrorDialog("Ошибка настройки FAB добавления", "Детали ошибки:\n${e.message}")
            }
            
            // Настройка кнопки очистки логов
            try {
                binding.buttonClearLogs.setOnClickListener {
                    try {
                        android.util.Log.d("MainActivity", "Кнопка очистки логов нажата")
                        clearLogs()
                        android.widget.Toast.makeText(this, "Логи очищены", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка при нажатии кнопки очистки логов", e)
                        showErrorDialog("Ошибка очистки логов", "Детали ошибки:\n${e.message}")
                    }
                }
                android.util.Log.d("MainActivity", "setupButtons: Кнопка очистки логов настроена")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupButtons: Ошибка настройки кнопки очистки логов", e)
                showErrorDialog("Ошибка настройки кнопки очистки логов", "Детали ошибки:\n${e.message}")
            }
            
            // Настройка кнопки копирования лога
            try {
                binding.buttonCopyLog.setOnClickListener {
                    try {
                        android.util.Log.d("MainActivity", "Кнопка копирования лога нажата")
                        copyLogToClipboard()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка при нажатии кнопки копирования лога", e)
                        showErrorDialog("Ошибка копирования лога", "Детали ошибки:\n${e.message}")
                    }
                }
                android.util.Log.d("MainActivity", "setupButtons: Кнопка копирования лога настроена")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "setupButtons: Ошибка настройки кнопки копирования лога", e)
                showErrorDialog("Ошибка настройки кнопки копирования лога", "Детали ошибки:\n${e.message}")
            }
            
            android.util.Log.d("MainActivity", "setupButtons: Все кнопки настроены успешно")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "setupButtons: Критическая ошибка настройки кнопок", e)
            showErrorDialog("Критическая ошибка настройки кнопок", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        // Меню не нужно - используется кнопка навигации в toolbar
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_check_permissions -> {
                // ДОБАВЛЕНО: Проверка разрешений по нажатию кнопки
                checkAndRequestPermissions()
                true
            }
            R.id.action_settings -> {
                // Открываем настройки
                val intent = android.content.Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeData() {
        try {
            android.util.Log.d("MainActivity", "🔍 === НАЧАЛО НАСТРОЙКИ НАБЛЮДАТЕЛЕЙ ===")
            android.util.Log.d("MainActivity", "viewModel доступен: ${viewModel != null}")
            android.util.Log.d("MainActivity", "viewModel.todayMedicines доступен: ${viewModel.todayMedicines != null}")
            
            // Наблюдаем за изменениями в списке лекарств
            viewModel.todayMedicines.observe(this) { medicines ->
                try {
                    android.util.Log.d("MainActivity", "📋 === ПОЛУЧЕНЫ ДАННЫЕ В НАБЛЮДАТЕЛЕ ===")
                    android.util.Log.d("MainActivity", "Количество лекарств: ${medicines.size}")
                    android.util.Log.d("MainActivity", "📋 MainActivity: Получено ${medicines.size} лекарств в наблюдателе")
                    addLog("📋 Получено ${medicines.size} лекарств в наблюдателе")
                    
                    if (medicines.isEmpty()) {
                        android.util.Log.d("MainActivity", "Список пуст - показываем пустое состояние")
                        binding.progressBarTodayMedicines.visibility = android.view.View.GONE
                        binding.recyclerViewTodayMedicines.visibility = android.view.View.GONE
                        // Показываем пустое состояние
                        showEmptyState()
                    } else {
                        android.util.Log.d("MainActivity", "Список не пуст - показываем лекарства")
                        binding.progressBarTodayMedicines.visibility = android.view.View.GONE
                        binding.recyclerViewTodayMedicines.visibility = android.view.View.VISIBLE
                        // Скрываем пустое состояние
                        showContentState()
                        
                        todayMedicineAdapter.submitList(medicines)
                        android.util.Log.d("MainActivity", "✅ Список лекарств обновлен в адаптере")
                    }
                    
                    android.util.Log.d("MainActivity", "observeData: UI обновлен успешно")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ observeData: Ошибка обновления UI", e)
                    showErrorDialog("Ошибка обновления UI", "Детали ошибки:\n${e.message}")
                }
            }
            
            android.util.Log.d("MainActivity", "✅ Наблюдатели настроены успешно")
            android.util.Log.d("MainActivity", "=== НАСТРОЙКА НАБЛЮДАТЕЛЕЙ ЗАВЕРШЕНА ===")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ observeData: Критическая ошибка настройки наблюдателей", e)
            showErrorDialog("Критическая ошибка настройки наблюдателей", "Детали ошибки:\n${e.message}\n\nСтек вызовов:\n${e.stackTraceToString()}")
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        
        // ДОБАВЛЕНО: Проверяем и восстанавливаем службу при возвращении в приложение
        try {
            android.util.Log.d("MainActivity", "Проверка службы при возвращении в приложение")
            com.medicalnotes.app.utils.ServiceStatusChecker.checkAndRestoreService(this@MainActivity)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка проверки службы", e)
        }
        
        // Обновляем список лекарств
        try {
            viewModel.loadAllMedicines()
            viewModel.loadMedicinesForDate(LocalDate.now())
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка обновления данных", e)
        }
        
        // Проверяем просроченные лекарства
        checkOverdueMedicines()
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
        
        // ИСПРАВЛЕНО: Агрессивная остановка всех звуков, вибрации и уведомлений
        addLog(" НАЧИНАЕМ ПОЛНУЮ ОСТАНОВКУ ЗВУКА, ВИБРАЦИИ И УВЕДОМЛЕНИЙ")
        try {
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_НАЖАТА", "Кнопка 'принял лекарство' нажата для: ${medicine.name} (ID: ${medicine.id})")
            addLog(" КНОПКА_НАЖАТА: Кнопка 'принял лекарство' нажата для: ${medicine.name}")
            
            // 1. Останавливаем все через OverdueCheckService
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ДЕЙСТВИЕ", "Останавливаем звуки и вибрацию через OverdueCheckService")
            addLog(" 1. ОСТАНАВЛИВАЕМ через OverdueCheckService")
            OverdueCheckService.forceStopSoundAndVibration(this@MainActivity)
            
            // 2. Останавливаем все через UnifiedNotificationManager
            addLog(" 2. ОСТАНАВЛИВАЕМ через UnifiedNotificationManager")
            UnifiedNotificationManager.cancelAllNotifications(this@MainActivity)
            
            // 3. Останавливаем вибрацию напрямую через системный сервис
            addLog(" 3. ОСТАНАВЛИВАЕМ вибрацию напрямую")
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            vibrator?.cancel()
            
            // 4. Отменяем уведомления через системный NotificationManager
            addLog(" 4. ОТМЕНЯЕМ уведомления через системный NotificationManager")
            val systemNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            systemNotificationManager?.cancelAll()
            
            // 5. Останавливаем через NotificationManager (для совместимости)
            addLog(" 5. ОСТАНАВЛИВАЕМ через старый NotificationManager")
            val notificationManager = com.medicalnotes.app.utils.NotificationManager(this@MainActivity)
            notificationManager.stopVibration()
            notificationManager.cancelOverdueNotification(medicine.id)
            notificationManager.cancelMedicineNotification(medicine.id)
            notificationManager.forceStopAllNotifications()
            
            // 6. Останавливаем периодическую проверку
            addLog(" 6. ОСТАНАВЛИВАЕМ периодическую проверку")
            stopPeriodicOverdueCheck()
            
            // 7. Дополнительно останавливаем звук через аудио менеджер
            addLog(" 7. ОСТАНАВЛИВАЕМ звук через AudioManager")
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            audioManager?.let { am ->
                // Временно отключаем звук уведомлений на 2 секунды
                val currentVolume = am.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION)
                am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)
                
                // Восстанавливаем через 2 секунды
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, currentVolume, 0)
                }, 2000)
            }
            
            com.medicalnotes.app.utils.LogCollector.d(" КНОПКА_ЗАВЕРШЕНА", "ВСЕ действия по остановке завершены для: ${medicine.name}")
            addLog(" ✅ ВСЕ ДЕЙСТВИЯ ПО ОСТАНОВКЕ ЗАВЕРШЕНЫ для: ${medicine.name}")
        } catch (e: Exception) {
            com.medicalnotes.app.utils.LogCollector.e(" КНОПКА_ОШИБКА", "Ошибка отмены уведомлений для: ${medicine.name}", e)
            addLog(" ❌ ОШИБКА остановки для: ${medicine.name} - ${e.message}")
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
        addLog("📋 MainActivity: viewModel доступен: ${viewModel != null}")
        addLog("📋 MainActivity: viewModel.todayMedicines доступен: ${viewModel.todayMedicines != null}")
        showLoadingState()
        addLog("Вызываем viewModel.loadMedicinesForDate(LocalDate.now())")
        android.util.Log.d("MainActivity", "🚀 MainActivity: Вызываем viewModel.loadMedicinesForDate(LocalDate.now())")
        try {
            // ИСПРАВЛЕНО: Используем универсальный метод с текущей датой
            viewModel.loadMedicinesForDate(LocalDate.now())
            addLog("viewModel.loadMedicinesForDate() вызван успешно")
            android.util.Log.d("MainActivity", "✅ MainActivity: viewModel.loadMedicinesForDate() вызван успешно")
        } catch (e: Exception) {
            addLog("❌ ОШИБКА при вызове viewModel.loadMedicinesForDate(): ${e.message}")
            android.util.Log.e("MainActivity", "❌ ОШИБКА при вызове viewModel.loadMedicinesForDate()", e)
        }
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
                
                // Создаем XML экспорт
                val xmlData = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    appendLine("<medicines>")
                    medicines.forEach { medicine ->
                        appendLine("  <medicine>")
                        appendLine("    <id>${medicine.id}</id>")
                        appendLine("    <name>${medicine.name}</name>")
                        appendLine("    <dosage>${medicine.dosage}</dosage>")
                        appendLine("    <quantity>${medicine.quantity}</quantity>")
                        appendLine("    <remainingQuantity>${medicine.remainingQuantity}</remainingQuantity>")
                        appendLine("    <medicineType>${medicine.medicineType}</medicineType>")
                        appendLine("    <time>${medicine.time}</time>")
                        appendLine("    <frequency>${medicine.frequency}</frequency>")
                        appendLine("    <startDate>${medicine.startDate}</startDate>")
                        appendLine("    <isActive>${medicine.isActive}</isActive>")
                        appendLine("    <takenToday>${medicine.takenToday}</takenToday>")
                        appendLine("    <lastTakenTime>${medicine.lastTakenTime}</lastTakenTime>")
                        appendLine("    <isMissed>${medicine.isMissed}</isMissed>")
                        appendLine("    <missedCount>${medicine.missedCount}</missedCount>")
                        appendLine("    <groupId>${medicine.groupId ?: ""}</groupId>")
                        appendLine("    <groupName>${medicine.groupName}</groupName>")
                        appendLine("    <groupOrder>${medicine.groupOrder}</groupOrder>")
                        appendLine("    <groupStartDate>${medicine.groupStartDate}</groupStartDate>")
                        appendLine("    <groupFrequency>${medicine.groupFrequency}</groupFrequency>")
                        appendLine("    <multipleDoses>${medicine.multipleDoses}</multipleDoses>")
                        appendLine("    <doseTimes>${medicine.doseTimes.joinToString(",")}</doseTimes>")
                        appendLine("    <customDays>${medicine.customDays.joinToString(",")}</customDays>")
                        appendLine("    <updatedAt>${medicine.updatedAt}</updatedAt>")
                        appendLine("  </medicine>")
                    }
                    appendLine("</medicines>")
                }
                
                val fileName = "medical_notes_data_${LocalDate.now()}.xml"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(xmlData)
                
                // Копируем XML в буфер обмена
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("MedicalNotes XML Data", xmlData)
                        clipboardManager.setPrimaryClip(clip)
                        
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "XML данные скопированы в буфер обмена и сохранены в $fileName",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        addLog("XML экспорт: $fileName (${xmlData.length} символов)")
                        addLog("XML данные скопированы в буфер обмена")
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Данные сохранены в $fileName, но ошибка копирования: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        addLog("Ошибка копирования XML: ${e.message}")
                    }
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
    
    // ДОБАВЛЕНО: Экспорт JSON данных для анализа
    private fun exportJsonData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val medicines = viewModel.allMedicines.value ?: emptyList()
                
                // Создаем JSON экспорт
                val jsonData = buildString {
                    appendLine("[")
                    medicines.forEachIndexed { index, medicine ->
                        appendLine("  {")
                        appendLine("    \"id\": ${medicine.id},")
                        appendLine("    \"name\": \"${medicine.name}\",")
                        appendLine("    \"dosage\": \"${medicine.dosage}\",")
                        appendLine("    \"quantity\": ${medicine.quantity},")
                        appendLine("    \"remainingQuantity\": ${medicine.remainingQuantity},")
                        appendLine("    \"medicineType\": \"${medicine.medicineType}\",")
                        appendLine("    \"time\": \"${medicine.time}\",")
                        appendLine("    \"frequency\": \"${medicine.frequency}\",")
                        appendLine("    \"startDate\": ${medicine.startDate},")
                        appendLine("    \"isActive\": ${medicine.isActive},")
                        appendLine("    \"takenToday\": ${medicine.takenToday},")
                        appendLine("    \"lastTakenTime\": ${medicine.lastTakenTime},")
                        appendLine("    \"isMissed\": ${medicine.isMissed},")
                        appendLine("    \"missedCount\": ${medicine.missedCount},")
                        appendLine("    \"groupId\": ${medicine.groupId ?: "null"},")
                        appendLine("    \"groupName\": \"${medicine.groupName}\",")
                        appendLine("    \"groupOrder\": ${medicine.groupOrder},")
                        appendLine("    \"groupStartDate\": ${medicine.groupStartDate},")
                        appendLine("    \"groupFrequency\": \"${medicine.groupFrequency}\",")
                        appendLine("    \"multipleDoses\": ${medicine.multipleDoses},")
                        appendLine("    \"doseTimes\": [${medicine.doseTimes.joinToString(",") { "\"$it\"" }}],")
                        appendLine("    \"customDays\": [${medicine.customDays.joinToString(",")}],")
                        appendLine("    \"updatedAt\": ${medicine.updatedAt}")
                        if (index < medicines.size - 1) {
                            appendLine("  },")
                        } else {
                            appendLine("  }")
                        }
                    }
                    appendLine("]")
                }
                
                val fileName = "medical_notes_data_${LocalDate.now()}.json"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(jsonData)
                
                // Копируем JSON в буфер обмена
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("MedicalNotes JSON Data", jsonData)
                        clipboardManager.setPrimaryClip(clip)
                        
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "JSON данные скопированы в буфер обмена и сохранены в $fileName",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        addLog("JSON экспорт: $fileName (${jsonData.length} символов)")
                        addLog("JSON данные скопированы в буфер обмена")
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Данные сохранены в $fileName, но ошибка копирования: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        addLog("Ошибка копирования JSON: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка экспорта JSON: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    addLog("Ошибка экспорта JSON: ${e.message}")
                }
            }
        }
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
                
                // ДОБАВЛЕНО: Детальная диагностика каждого лекарства
                medicines.forEach { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    val shouldTake = com.medicalnotes.app.utils.MedicineStatusHelper.shouldTakeToday(medicine)
                    
                    lifecycleScope.launch(Dispatchers.Main) { 
                        addLog("ПРОВЕРКА: ${medicine.name} - Статус: $status, Время: ${medicine.time}, Принято сегодня: ${medicine.takenToday}")
                        addLog("  - Должно принимать сегодня: $shouldTake")
                        addLog("  - Частота: ${medicine.frequency}")
                        addLog("  - Активно: ${medicine.isActive}")
                        addLog("  - Группа: ${medicine.groupName} (ID: ${medicine.groupId}, Порядок: ${medicine.groupOrder})")
                        addLog("  - Дата начала: ${medicine.startDate}")
                        addLog("  - Последний прием: ${medicine.lastTakenTime}")
                    }
                    
                    if (status == com.medicalnotes.app.utils.DosageCalculator.MedicineStatus.OVERDUE) {
                        lifecycleScope.launch(Dispatchers.Main) { 
                            addLog(" НАЙДЕНО ПРОСРОЧЕННОЕ: ${medicine.name} (принято сегодня: ${medicine.takenToday})")
                        }
                    }
                }
                
                val overdueMedicines = medicines.filter { medicine ->
                    val status = com.medicalnotes.app.utils.MedicineStatusHelper.getMedicineStatus(medicine)
                    val isOverdue = status == com.medicalnotes.app.utils.DosageCalculator.MedicineStatus.OVERDUE
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
        android.util.Log.d("MainActivity", "MainActivity уничтожается")
        
        // ИСПРАВЛЕНО: Принудительно запускаем службу при закрытии приложения
        try {
            android.util.Log.d("MainActivity", "Принудительный запуск службы при закрытии приложения")
            OverdueCheckService.startService(this@MainActivity)
            
            // Дополнительная проверка через 2 секунды
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    android.util.Log.d("MainActivity", "Повторная проверка службы через 2 секунды")
                    OverdueCheckService.startService(this@MainActivity)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Ошибка повторного запуска службы", e)
                }
            }, 2000)
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка запуска службы при закрытии", e)
        }
    }

    /**
     * ДОБАВЛЕНО: Проверка и запрос разрешений
     */
    private fun checkAndRequestPermissions() {
        android.util.Log.d("MainActivity", "🔐 НАЧАЛО ПРОВЕРКИ РАЗРЕШЕНИЙ В MainActivity")
        
        try {
            // Проверяем, что activity еще активна
            if (isFinishing || isDestroyed) {
                android.util.Log.w("MainActivity", "⚠️ Activity завершается, пропускаем проверку разрешений")
                return
            }
            
            android.util.Log.d("MainActivity", "📞 Вызываем PermissionManager.requestMissingPermissions")
            com.medicalnotes.app.utils.PermissionManager.requestMissingPermissions(this) { status ->
                android.util.Log.d("MainActivity", "📥 Получен результат от PermissionManager")
                android.util.Log.d("MainActivity", "  Все разрешено: ${status.isAllGranted()}")
                android.util.Log.d("MainActivity", "  Отсутствующие: ${status.missingPermissions}")
                android.util.Log.d("MainActivity", "  System Alert: ${status.systemAlertWindowGranted}")
                android.util.Log.d("MainActivity", "  Уведомления: ${status.notificationsEnabled}")
                android.util.Log.d("MainActivity", "  Батарея: ${status.batteryOptimizationIgnored}")
                
                // Проверяем, что activity еще активна перед обновлением UI
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        try {
                            if (status.isAllGranted()) {
                                android.util.Log.d("MainActivity", "✅ Все разрешения предоставлены - показываем успех")
                                showPermissionStatusMessage("Все разрешения настроены корректно", true)
                            } else {
                                android.util.Log.w("MainActivity", "⚠️ Не все разрешения предоставлены - показываем предупреждение")
                                showPermissionStatusMessage("Некоторые разрешения не настроены. Уведомления могут работать некорректно.", false)
                                
                                // Показываем кнопку для повторной проверки
                                showPermissionCheckButton()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Ошибка обновления UI разрешений", e)
                        }
                    }
                } else {
                    android.util.Log.w("MainActivity", "⚠️ Activity завершилась, пропускаем обновление UI")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ КРИТИЧЕСКАЯ ОШИБКА проверки разрешений", e)
        }
    }
    
    /**
     * Показывает сообщение о статусе разрешений
     */
    private fun showPermissionStatusMessage(message: String, isSuccess: Boolean) {
        try {
            // Проверяем, что binding инициализирован
            if (!::binding.isInitialized) {
                android.util.Log.w("MainActivity", "Binding не инициализирован, пропускаем показ сообщения")
                return
            }
            
            val snackbar = com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                message,
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            )
            
            if (isSuccess) {
                snackbar.setBackgroundTint(resources.getColor(com.google.android.material.R.color.design_default_color_primary, null))
            } else {
                snackbar.setBackgroundTint(resources.getColor(com.google.android.material.R.color.design_default_color_error, null))
                snackbar.setAction("Настройки") {
                    com.medicalnotes.app.utils.PermissionManager.openAppSettings(this@MainActivity)
                }
            }
            
            snackbar.show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка показа сообщения о разрешениях", e)
        }
    }
    
    /**
     * Показывает кнопку для проверки разрешений
     */
    private fun showPermissionCheckButton() {
        try {
            // Добавляем кнопку в меню или показываем в интерфейсе
            // Здесь можно добавить кнопку в toolbar или floating action button
            android.util.Log.d("MainActivity", "Кнопка проверки разрешений добавлена")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка добавления кнопки разрешений", e)
        }
    }
    
    /**
     * Обрабатывает результат запроса разрешений
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        android.util.Log.d("MainActivity", "📋 onRequestPermissionsResult вызван")
        android.util.Log.d("MainActivity", "  RequestCode: $requestCode")
        android.util.Log.d("MainActivity", "  Permissions: ${permissions.joinToString(", ")}")
        android.util.Log.d("MainActivity", "  GrantResults: ${grantResults.joinToString(", ")}")
        
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        com.medicalnotes.app.utils.PermissionManager.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        ) { status ->
            android.util.Log.d("MainActivity", "📥 Получен результат обработки разрешений")
            android.util.Log.d("MainActivity", "  Все разрешено: ${status.isAllGranted()}")
            android.util.Log.d("MainActivity", "  Отсутствующие: ${status.missingPermissions}")
            
            runOnUiThread {
                if (status.isAllGranted()) {
                    showPermissionStatusMessage("Все разрешения предоставлены!", true)
                } else {
                    showPermissionStatusMessage("Некоторые разрешения не предоставлены", false)
                }
            }
        }
    }

    /**
     * ДОБАВЛЕНО: Диагностический метод для проверки состояния UI
     */
    private fun diagnoseUIState() {
        try {
            android.util.Log.d("MainActivity", "=== ДИАГНОСТИКА UI ===")
            android.util.Log.d("MainActivity", "Binding инициализирован: ${::binding.isInitialized}")
            
            if (::binding.isInitialized) {
                android.util.Log.d("MainActivity", "Toolbar существует: ${binding.toolbar != null}")
                android.util.Log.d("MainActivity", "RecyclerView существует: ${binding.recyclerViewTodayMedicines != null}")
                android.util.Log.d("MainActivity", "ProgressBar существует: ${binding.progressBarTodayMedicines != null}")
                
                try {
                    android.util.Log.d("MainActivity", "Toolbar title: ${binding.toolbar.title}")
                    android.util.Log.d("MainActivity", "Toolbar subtitle: ${binding.toolbar.subtitle}")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Ошибка получения свойств toolbar", e)
                }
            }
            
            android.util.Log.d("MainActivity", "Activity finishing: $isFinishing")
            android.util.Log.d("MainActivity", "Activity destroyed: $isDestroyed")
            android.util.Log.d("MainActivity", "=== КОНЕЦ ДИАГНОСТИКИ ===")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка диагностики UI", e)
        }
    }

    /**
     * ДОБАВЛЕНО: Показывает диалог с информацией об ошибке
     */
    private fun showErrorDialog(title: String, message: String) {
        try {
            // Проверяем, что activity еще активна
            if (isFinishing || isDestroyed) {
                android.util.Log.w("MainActivity", "Activity завершается, пропускаем показ диалога ошибки")
                return
            }
            
            // Показываем диалог на главном потоке
            runOnUiThread {
                try {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Копировать") { _, _ ->
                            try {
                                // Копируем текст в буфер обмена
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Ошибка", message)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(this@MainActivity, "Текст скопирован в буфер обмена", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Ошибка копирования в буфер", e)
                            }
                        }
                        .setNegativeButton("Закрыть") { dialog, _ ->
                            try {
                                dialog.dismiss()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Ошибка закрытия диалога", e)
                            }
                        }
                        .setCancelable(false)
                        .show()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Ошибка показа диалога", e)
                    // Fallback - показываем Toast
                    android.widget.Toast.makeText(this@MainActivity, "$title: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Критическая ошибка показа диалога", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Показывает диалог выбора даты для тестирования
     */
    private fun showDatePickerDialog() {
        try {
            val currentYear = selectedTestDate.year
            val currentMonth = selectedTestDate.monthValue - 1 // Calendar использует 0-based месяцы
            val currentDay = selectedTestDate.dayOfMonth
            
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    try {
                        val newDate = LocalDate.of(year, month + 1, dayOfMonth)
                        selectedTestDate = newDate
                        updateSelectedDateDisplay()
                        
                        addLog("=== ИЗМЕНЕНИЕ ТЕСТОВОЙ ДАТЫ ===")
                        addLog("Новая тестовая дата: $newDate")
                        
                        // ИСПРАВЛЕНО: Используем универсальный метод из ViewModel
                        viewModel.loadMedicinesForDate(newDate)
                        
                        android.widget.Toast.makeText(
                            this,
                            "Тестовая дата изменена на: ${formatDate(newDate)}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Ошибка обработки выбранной даты", e)
                        showErrorDialog("Ошибка выбора даты", "Детали ошибки:\n${e.message}")
                    }
                },
                currentYear,
                currentMonth,
                currentDay
            )
            
            datePickerDialog.show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка показа диалога выбора даты", e)
            showErrorDialog("Ошибка календаря", "Детали ошибки:\n${e.message}")
        }
    }
    
    /**
     * ДОБАВЛЕНО: Обновляет отображение выбранной даты
     */
    private fun updateSelectedDateDisplay() {
        try {
            val displayText = when {
                selectedTestDate == LocalDate.now() -> "Сегодня"
                selectedTestDate == LocalDate.now().minusDays(1) -> "Вчера"
                selectedTestDate == LocalDate.now().plusDays(1) -> "Завтра"
                else -> formatDate(selectedTestDate)
            }
            
            binding.textSelectedDate.text = displayText
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка обновления отображения даты", e)
        }
    }
    
    /**
     * ДОБАВЛЕНО: Форматирует дату для отображения
     */
    private fun formatDate(date: LocalDate): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
            date.format(formatter)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка форматирования даты", e)
            date.toString()
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Функция для исправления некорректных статусов takenToday
     * Проверяет все лекарства и сбрасывает takenToday если lastTakenTime не сегодня
     */
    private fun fixIncorrectTakenTodayStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("MainActivity", "=== НАЧИНАЕМ ИСПРАВЛЕНИЕ СТАТУСОВ TAKEN_TODAY ===")
                
                val dataManager = com.medicalnotes.app.utils.DataManager(this@MainActivity)
                val allMedicines = dataManager.loadMedicines()
                val today = LocalDate.now()
                
                android.util.Log.d("MainActivity", "Всего лекарств для проверки: ${allMedicines.size}")
                android.util.Log.d("MainActivity", "Сегодняшняя дата: $today")
                
                var fixedCount = 0
                val medicinesToUpdate = mutableListOf<Medicine>()
                
                allMedicines.forEach { medicine ->
                    android.util.Log.d("MainActivity", "Проверяем лекарство: ${medicine.name}")
                    android.util.Log.d("MainActivity", "  - takenToday: ${medicine.takenToday}")
                    android.util.Log.d("MainActivity", "  - lastTakenTime: ${medicine.lastTakenTime}")
                    
                    if (medicine.takenToday && medicine.lastTakenTime > 0) {
                        val lastTakenDate = java.time.Instant.ofEpochMilli(medicine.lastTakenTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        
                        android.util.Log.d("MainActivity", "  - lastTakenDate: $lastTakenDate")
                        
                        if (lastTakenDate != today) {
                            android.util.Log.d("MainActivity", "  - НАЙДЕНА ПРОБЛЕМА: lastTakenDate != today")
                            android.util.Log.d("MainActivity", "  - Сбрасываем takenToday для лекарства: ${medicine.name}")
                            
                            val updatedMedicine = medicine.copy(
                                takenToday = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            medicinesToUpdate.add(updatedMedicine)
                            fixedCount++
                        } else {
                            android.util.Log.d("MainActivity", "  - Статус корректный")
                        }
                    } else if (medicine.takenToday && medicine.lastTakenTime <= 0) {
                        // Некорректная ситуация: takenToday = true, но lastTakenTime не установлен
                        android.util.Log.d("MainActivity", "  - НАЙДЕНА ПРОБЛЕМА: takenToday=true но lastTakenTime<=0")
                        
                        val updatedMedicine = medicine.copy(
                            takenToday = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        medicinesToUpdate.add(updatedMedicine)
                        fixedCount++
                    }
                }
                
                // Сохраняем исправления
                if (medicinesToUpdate.isNotEmpty()) {
                    android.util.Log.d("MainActivity", "Сохраняем ${medicinesToUpdate.size} исправленных лекарств")
                    
                    // Обновляем каждое лекарство
                    medicinesToUpdate.forEach { medicine ->
                        dataManager.updateMedicine(medicine)
                    }
                    
                    android.util.Log.d("MainActivity", "✅ ИСПРАВЛЕНО СТАТУСОВ: $fixedCount")
                    
                    // Перезагружаем лекарства на UI
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Исправлено $fixedCount некорректных статусов",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            
                            // Перезагружаем данные
                            viewModel.loadMedicinesForDate(today)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Ошибка обновления UI после исправления статусов", e)
                        }
                    }
                } else {
                    android.util.Log.d("MainActivity", "✅ Все статусы корректны, исправления не требуются")
                }
                
                android.util.Log.d("MainActivity", "=== ИСПРАВЛЕНИЕ СТАТУСОВ TAKEN_TODAY ЗАВЕРШЕНО ===")
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Ошибка при исправлении статусов takenToday", e)
                
                lifecycleScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "Ошибка при исправлении статусов: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}  