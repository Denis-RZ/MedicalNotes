# Анализ проблем с уведомлениями о просроченных лекарствах

## Текущие проблемы
1. **Визуальное уведомление не появляется поверх других приложений**
2. **Звук и вибрация срабатывают только один раз вместо повторения каждые 5 секунд**

## Анализ кода

### OverdueCheckService.kt - текущая конфигурация:
```kotlin
// Канал уведомлений
val overdueChannel = NotificationChannel(
    CHANNEL_ID_OVERDUE,
    "Просроченные лекарства",
    NotificationManager.IMPORTANCE_HIGH
).apply {
    description = "Уведомления о просроченных лекарствах"
    enableVibration(true)
    enableLights(true)
    setShowBadge(true)
    setBypassDnd(true)
    setSound(Settings.System.DEFAULT_NOTIFICATION_URI, null)
    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
}

// Основное уведомление
val notification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
    .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
    .setContentText("У вас $overdueCount просроченных лекарств")
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setAutoCancel(false)
    .setOngoing(true)
    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
    .setVibrate(longArrayOf(0, 500, 200, 500))
    .setLights(0xFF0000FF.toInt(), 3000, 3000)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setFullScreenIntent(pendingIntent, true)
    .setTimeoutAfter(0)
    .build()
```

## Возможные причины проблем

### 1. Проблема с визуальным отображением

**Возможные причины:**
- Настройки устройства блокируют уведомления
- Battery optimization отключает фоновые процессы
- Do Not Disturb режим блокирует уведомления
- Производитель устройства имеет дополнительные ограничения

**Решения:**
```kotlin
// Добавить дополнительные флаги для принудительного отображения
.setFullScreenIntent(pendingIntent, true)
.setPriority(NotificationCompat.PRIORITY_MAX)
.setCategory(NotificationCompat.CATEGORY_ALARM)
.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
```

### 2. Проблема с повторением звука/вибрации

**Возможные причины:**
- Система Android ограничивает повторяющиеся уведомления
- Battery optimization останавливает повторения
- Неправильная логика в Runnable

**Текущая логика повторения:**
```kotlin
private val soundVibrationRunnable = object : Runnable {
    override fun run() {
        if (isOverdueNotificationActive) {
            playNotificationSound(this@OverdueCheckService)
            startVibration(this@OverdueCheckService)
            handler.postDelayed(this, 5000) // Повтор каждые 5 секунд
        }
    }
}
```

## Предлагаемые исправления

### Исправление 1: Усиление визуального отображения
```kotlin
// В showOverdueNotification()
val notification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
    .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
    .setContentText("У вас $overdueCount просроченных лекарств")
    .setStyle(NotificationCompat.BigTextStyle()
        .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: $medicineNames\n\nПожалуйста, примите их как можно скорее!"))
    .setSmallIcon(R.drawable.ic_medicine)
    .setContentIntent(pendingIntent)
    .setPriority(NotificationCompat.PRIORITY_MAX)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setAutoCancel(false)
    .setOngoing(true)
    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
    .setVibrate(longArrayOf(0, 500, 200, 500))
    .setLights(0xFF0000FF.toInt(), 3000, 3000)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setFullScreenIntent(pendingIntent, true)
    .setTimeoutAfter(0)
    .addAction(R.drawable.ic_check, "Принял лекарство", takeMedicineIntent)
    .build()
```

### Исправление 2: Улучшение логики повторения
```kotlin
// Использовать AlarmManager для более надежного повторения
private fun scheduleRepeatingAlarm() {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(this, MedicineAlarmReceiver::class.java).apply {
        action = "REPEAT_OVERDUE_NOTIFICATION"
    }
    val pendingIntent = PendingIntent.getBroadcast(
        this, 
        OVERDUE_REPEAT_REQUEST_CODE, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + 5000,
        5000, // 5 секунд
        pendingIntent
    )
}
```

### Исправление 3: Добавление системного уведомления
```kotlin
// Создать системное уведомление с высоким приоритетом
private fun createSystemAlert() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "system_alert",
            "System Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setBypassDnd(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    val notification = NotificationCompat.Builder(this, "system_alert")
        .setContentTitle("🚨 ВНИМАНИЕ!")
        .setContentText("У вас просроченные лекарства")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
        .setVibrate(longArrayOf(0, 1000, 500, 1000))
        .setLights(0xFFFF0000.toInt(), 1000, 1000)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setFullScreenIntent(pendingIntent, true)
        .build()
    
    notificationManager.notify(SYSTEM_ALERT_ID, notification)
}
```

## Настройки устройства для проверки

### 1. Настройки уведомлений
- Settings > Apps > MedicalNotes > Notifications
- ✅ Show notifications: ON
- ✅ Override Do Not Disturb: ON
- ✅ Importance: High/Urgent

### 2. Battery optimization
- Settings > Apps > MedicalNotes > Battery
- ✅ Background activity: ON
- ✅ Battery optimization: OFF

### 3. Do Not Disturb
- Settings > Sound & vibration > Do Not Disturb
- ✅ Add MedicalNotes to "Apps that can interrupt"

### 4. Device-specific settings
**Samsung:**
- Settings > Apps > MedicalNotes > Battery > Allow background activity

**Xiaomi:**
- Settings > Apps > MedicalNotes > Battery saver > No restrictions

**Huawei:**
- Settings > Apps > MedicalNotes > Battery > Launch > Allow

**OnePlus:**
- Settings > Apps > MedicalNotes > Battery > Background activity > Allow

## Тестирование

### Шаги для тестирования:
1. Установить APK на устройство
2. Добавить лекарство с временем через 1 минуту
3. Закрыть приложение
4. Подождать 1 минуту
5. Проверить:
   - Появляется ли визуальное уведомление
   - Повторяется ли звук каждые 5 секунд
   - Повторяется ли вибрация каждые 5 секунд

### Ожидаемое поведение:
- Визуальное уведомление должно появиться поверх всех приложений
- Звук и вибрация должны повторяться каждые 5 секунд
- Уведомление должно оставаться до нажатия "Принял лекарство"

## Следующие шаги

1. **Применить исправления** в OverdueCheckService.kt
2. **Пересобрать APK** с новыми изменениями
3. **Протестировать** на реальном устройстве
4. **Проверить настройки** устройства согласно рекомендациям
5. **Сравнить поведение** с ожидаемым результатом

Если проблемы сохраняются, возможно потребуется:
- Использовать Foreground Service вместо обычного Service
- Добавить Wake Lock для предотвращения сна устройства
- Использовать WorkManager для более надежного планирования
- Добавить логирование для отладки поведения 