package com.medicalnotes.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.medicalnotes.app.databinding.ActivityDataBackupBinding
import com.medicalnotes.app.utils.DataExportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataBackupActivity : BaseActivity() {
    
    private lateinit var binding: ActivityDataBackupBinding
    private lateinit var dataExportManager: DataExportManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            performExport()
        } else {
            Toast.makeText(this, "Разрешение необходимо для экспорта данных", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настройка обработки кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Логика обработки кнопки "Назад"
                if (isEnabled) {
                    finish()
                }
            }
        })
        
        binding = ActivityDataBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        dataExportManager = DataExportManager(this)
        
        setupViews()
        setupListeners()
        loadBackupList()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.data_backup_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupListeners() {
        binding.buttonExport.setOnClickListener {
            checkPermissionsAndExport()
        }
        
        binding.buttonImport.setOnClickListener {
            showImportDialog()
        }
        
        binding.buttonAutoImport.setOnClickListener {
            performAutoImport()
        }
        
        binding.buttonRefresh.setOnClickListener {
            loadBackupList()
        }
    }
    
    private fun checkPermissionsAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                performExport()
            } else {
                showStoragePermissionDialog()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED) {
                performExport()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Разрешение на доступ к файлам")
            .setMessage("Для экспорта данных необходимо разрешить доступ к файлам. Перейти в настройки?")
            .setPositiveButton("Настройки") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun performExport() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonExport.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = dataExportManager.exportAllData()
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.buttonExport.isEnabled = true
                
                if (success) {
                    Toast.makeText(this@DataBackupActivity, "Данные успешно экспортированы", Toast.LENGTH_LONG).show()
                    loadBackupList()
                } else {
                    Toast.makeText(this@DataBackupActivity, "Ошибка при экспорте данных", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun performAutoImport() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonAutoImport.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = dataExportManager.autoImportLatestBackup()
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.buttonAutoImport.isEnabled = true
                
                if (success) {
                    Toast.makeText(this@DataBackupActivity, "Данные успешно импортированы", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@DataBackupActivity, "Резервные копии не найдены", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showImportDialog() {
        val backups = dataExportManager.getAvailableBackups()
        
        if (backups.isEmpty()) {
            Toast.makeText(this, "Резервные копии не найдены", Toast.LENGTH_LONG).show()
            return
        }
        
        val backupNames = backups.map { backup ->
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(backup.lastModified()))
            "${backup.name} ($date)"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Выберите резервную копию для импорта")
            .setItems(backupNames) { _, which ->
                importBackup(backups[which])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun importBackup(backupFolder: File) {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonImport.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = dataExportManager.importAllData(backupFolder)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.buttonImport.isEnabled = true
                
                if (success) {
                    Toast.makeText(this@DataBackupActivity, "Данные успешно импортированы", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@DataBackupActivity, "Ошибка при импорте данных", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun loadBackupList() {
        CoroutineScope(Dispatchers.IO).launch {
            val backups = dataExportManager.getAvailableBackups()
            
            withContext(Dispatchers.Main) {
                if (backups.isEmpty()) {
                    binding.textViewNoBackups.visibility = View.VISIBLE
                    binding.recyclerViewBackups.visibility = View.GONE
                } else {
                    binding.textViewNoBackups.visibility = View.GONE
                    binding.recyclerViewBackups.visibility = View.VISIBLE
                    // Здесь можно добавить адаптер для списка резервных копий
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 