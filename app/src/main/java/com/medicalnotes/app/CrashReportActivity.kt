package com.medicalnotes.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.medicalnotes.app.databinding.ActivityCrashReportBinding
import com.medicalnotes.app.utils.CrashReporter
import com.medicalnotes.app.utils.LogCollector

class CrashReportActivity : BaseActivity() {
    
    private lateinit var binding: ActivityCrashReportBinding
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CrashReportActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        loadCrashReport()
    }
    
    private fun setupViews() {
        // Настройка toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.crash_report_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Настройка кнопок
        binding.buttonCopy.setOnClickListener {
            copyReportToClipboard()
        }
        
        binding.buttonShare.setOnClickListener {
            shareReport()
        }
        
        binding.buttonClear.setOnClickListener {
            clearCrashReport()
        }
        
        binding.buttonRefresh.setOnClickListener {
            loadCrashReport()
        }
    }
    
    private fun loadCrashReport() {
        try {
            val crashLog = CrashReporter.getCrashLog(this)
            val appLogs = LogCollector.getLogsForCrashReport()
            
            val report = buildString {
                if (crashLog != null) {
                    appendLine("=== ОТЧЕТ О КРАШЕ ===")
                    appendLine(crashLog)
                    appendLine()
                }
                
                appendLine("=== ЛОГИ ПРИЛОЖЕНИЯ ===")
                appendLine(appLogs)
            }
            
            binding.textViewReport.text = report
            
            if (crashLog != null) {
                binding.textViewStatus.text = "Найдены данные о краше"
                binding.textViewStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                binding.textViewStatus.text = "Данные о краше не найдены"
                binding.textViewStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            
        } catch (e: Exception) {
            binding.textViewReport.text = "Ошибка загрузки отчета: ${e.message}"
            binding.textViewStatus.text = "Ошибка загрузки"
            binding.textViewStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }
    
    private fun copyReportToClipboard() {
        try {
            val report = binding.textViewReport.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Отчет об ошибке", report)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(this, "Отчет скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка копирования: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareReport() {
        try {
            val report = binding.textViewReport.text.toString()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Отчет об ошибке Medical Notes")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            
            startActivity(Intent.createChooser(intent, "Отправить отчет"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отправки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun clearCrashReport() {
        try {
            CrashReporter.clearCrashLog(this)
            LogCollector.clearLogs()
            
            binding.textViewReport.text = "Отчет очищен"
            binding.textViewStatus.text = "Данные очищены"
            binding.textViewStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            
            Toast.makeText(this, "Отчет очищен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка очистки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 