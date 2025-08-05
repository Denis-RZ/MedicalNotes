package com.medicalnotes.app.utils

import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Утилита для работы с датами и исправления проблем с определением текущей даты
 */
object DateUtils {
    
    private const val TAG = "DateUtils"
    
    /**
     * Получает текущую дату с учетом часового пояса устройства
     */
    fun getCurrentDate(): LocalDate {
        val systemDate = LocalDate.now()
        val timezoneDate = ZonedDateTime.now().toLocalDate()
        
        Log.d(TAG, "Системная дата: $systemDate")
        Log.d(TAG, "Дата с учетом часового пояса: $timezoneDate")
        
        // Если даты различаются, используем дату с учетом часового пояса
        return if (systemDate != timezoneDate) {
            Log.w(TAG, "⚠️ Обнаружено расхождение в датах, используем дату с учетом часового пояса")
            timezoneDate
        } else {
            systemDate
        }
    }
    
    /**
     * Проверяет корректность определения даты
     */
    fun checkDateConsistency(): Boolean {
        val systemDate = LocalDate.now()
        val timezoneDate = ZonedDateTime.now().toLocalDate()
        val calendarDate = Calendar.getInstance().let { 
            LocalDate.of(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        
        Log.d(TAG, "=== ПРОВЕРКА СОГЛАСОВАННОСТИ ДАТ ===")
        Log.d(TAG, "Системная дата (LocalDate.now()): $systemDate")
        Log.d(TAG, "Дата с часовым поясом (ZonedDateTime): $timezoneDate")
        Log.d(TAG, "Дата из Calendar: $calendarDate")
        
        val allSame = systemDate == timezoneDate && timezoneDate == calendarDate
        
        if (!allSame) {
            Log.w(TAG, "⚠️ ОБНАРУЖЕНО РАСХОЖДЕНИЕ В ДАТАХ!")
            Log.w(TAG, "Это может вызывать проблемы с отображением лекарств")
        } else {
            Log.d(TAG, "✅ Все методы определения даты согласованы")
        }
        
        return allSame
    }
    
    /**
     * Получает часовой пояс устройства
     */
    fun getDeviceTimeZone(): ZoneId {
        return ZoneId.systemDefault()
    }
    
    /**
     * Проверяет, не изменилась ли дата во время работы приложения
     */
    fun checkDateChange(previousDate: LocalDate): Boolean {
        val currentDate = getCurrentDate()
        val changed = previousDate != currentDate
        
        if (changed) {
            Log.w(TAG, "⚠️ ДАТА ИЗМЕНИЛАСЬ: $previousDate -> $currentDate")
            Log.w(TAG, "Это может вызывать проблемы с отображением лекарств")
        }
        
        return changed
    }
    
    /**
     * Исправляет проблемы с определением даты
     */
    fun fixDateIssues(): LocalDate {
        Log.d(TAG, "=== ИСПРАВЛЕНИЕ ПРОБЛЕМ С ДАТОЙ ===")
        
        // Проверяем согласованность
        val isConsistent = checkDateConsistency()
        
        if (!isConsistent) {
            Log.w(TAG, "Применяем исправления для проблем с датой")
            
            // Используем наиболее надежный метод
            val fixedDate = ZonedDateTime.now().toLocalDate()
            Log.d(TAG, "Исправленная дата: $fixedDate")
            
            return fixedDate
        }
        
        return getCurrentDate()
    }
} 