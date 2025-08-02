package com.medicalnotes.app.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.SeekBar
import android.widget.TextView
import com.medicalnotes.app.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CustomTimePickerDialog(
    context: Context,
    private val initialTime: LocalTime,
    private val onTimeSelected: (LocalTime) -> Unit
) : Dialog(context) {

    private lateinit var binding: android.view.View
    private var selectedHours = initialTime.hour
    private var selectedMinutes = initialTime.minute

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        binding = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)
        setContentView(binding)
        
        setupViews()
        setupListeners()
        updateTimeDisplay()
    }

    private fun setupViews() {
        // Настраиваем ползунки
        findViewById<SeekBar>(R.id.seekBarHours).apply {
            progress = selectedHours
            max = 23
        }
        
        findViewById<SeekBar>(R.id.seekBarMinutes).apply {
            progress = selectedMinutes
            max = 59
        }
    }

    private fun setupListeners() {
        // Ползунок часов
        findViewById<SeekBar>(R.id.seekBarHours).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedHours = progress
                    updateTimeDisplay()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        // Ползунок минут
        findViewById<SeekBar>(R.id.seekBarMinutes).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedMinutes = progress
                    updateTimeDisplay()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        // Кнопка отмены
        findViewById<android.widget.Button>(R.id.buttonCancel).setOnClickListener {
            dismiss()
        }

        // Кнопка ОК
        findViewById<android.widget.Button>(R.id.buttonOk).setOnClickListener {
            val selectedTime = LocalTime.of(selectedHours, selectedMinutes)
            onTimeSelected(selectedTime)
            dismiss()
        }
    }

    private fun updateTimeDisplay() {
        // Обновляем отображение часов
        findViewById<TextView>(R.id.textHours).text = selectedHours.toString()
        
        // Обновляем отображение минут с ведущим нулем
        findViewById<TextView>(R.id.textMinutes).text = 
            String.format("%02d", selectedMinutes)
        
        // Обновляем общее время
        val selectedTime = LocalTime.of(selectedHours, selectedMinutes)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        findViewById<TextView>(R.id.textSelectedTime).text = 
            selectedTime.format(formatter)
    }
} 