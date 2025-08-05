package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.medicalnotes.app.databinding.ActivityMedicineCardBinding
import com.medicalnotes.app.receiver.MedicineAlarmReceiver

class MedicineCardActivity : BaseActivity() {
    private lateinit var binding: ActivityMedicineCardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        binding = ActivityMedicineCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getLongExtra("medicine_id", -1L)
        val name = intent.getStringExtra("medicine_name") ?: getString(R.string.medicine)
        val time = intent.getStringExtra("medicine_time") ?: ""

        binding.title.text = getString(R.string.time_to_take_medicine)
        binding.subtitle.text = "$name — $time"

        binding.btnTaken.setOnClickListener { sendAction("ACTION_MEDICINE_TAKEN", id) }
        binding.btnSnooze.setOnClickListener { sendAction("ACTION_SNOOZE_10", id) }
        binding.btnSkip.setOnClickListener { sendAction("ACTION_MEDICINE_SKIP", id) }
    }

    override fun updateUIAfterLanguageChange() {
        super.updateUIAfterLanguageChange()
        
        try {
            android.util.Log.d("MedicineCardActivity", "Updating UI after language change")
            
            // Обновляем заголовки
            binding.title.text = getString(R.string.time_to_take_medicine)
            
            // Обновляем текст кнопок
            updateButtonTexts()
            
            // Обновляем подзаголовок с именем и временем лекарства
            val name = intent.getStringExtra("medicine_name") ?: getString(R.string.medicine)
            val time = intent.getStringExtra("medicine_time") ?: ""
            binding.subtitle.text = "$name — $time"
            
            android.util.Log.d("MedicineCardActivity", "UI updated successfully after language change")
            
        } catch (e: Exception) {
            android.util.Log.e("MedicineCardActivity", "Error updating UI after language change", e)
        }
    }
    
    private fun updateButtonTexts() {
        try {
            // Обновляем текст кнопок
            binding.btnTaken.text = getString(R.string.taken)
            binding.btnSnooze.text = getString(R.string.snooze)
            binding.btnSkip.text = getString(R.string.skip)
            
        } catch (e: Exception) {
            android.util.Log.e("MedicineCardActivity", "Error updating button texts", e)
        }
    }
    
    private fun sendAction(action: String, id: Long) {
        val i = Intent(this, MedicineAlarmReceiver::class.java).apply {
            this.action = action
            putExtra("medicine_id", id)
        }
        sendBroadcast(i)
        finish()
    }
} 