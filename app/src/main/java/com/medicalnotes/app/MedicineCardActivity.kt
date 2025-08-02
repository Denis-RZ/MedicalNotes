package com.medicalnotes.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.medicalnotes.app.databinding.ActivityMedicineCardBinding
import com.medicalnotes.app.receiver.MedicineAlarmReceiver

class MedicineCardActivity : AppCompatActivity() {
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
        val name = intent.getStringExtra("medicine_name") ?: "Лекарство"
        val time = intent.getStringExtra("medicine_time") ?: ""

        binding.title.text = "Время принять лекарство"
        binding.subtitle.text = "$name — $time"

        binding.btnTaken.setOnClickListener { sendAction("ACTION_MEDICINE_TAKEN", id) }
        binding.btnSnooze.setOnClickListener { sendAction("ACTION_SNOOZE_10", id) }
        binding.btnSkip.setOnClickListener { sendAction("ACTION_MEDICINE_SKIP", id) }
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