# 🔔 АНАЛИЗ ДИАЛОГОВЫХ ОКОН И УВЕДОМЛЕНИЙ ПРИ ПРОСРОЧКЕ ЛЕКАРСТВ

## 📋 ОБЗОР СИСТЕМЫ УВЕДОМЛЕНИЙ

### 🎯 **Типы уведомлений при просрочке:**

1. **Обычные уведомления** (NotificationService) - в назначенное время
2. **Уведомления о просрочке** (OverdueCheckService) - для просроченных лекарств
3. **Системные алерты** - как запасной вариант
4. **Toast сообщения** - краткие уведомления

## 🔔 ДЕТАЛЬНЫЙ АНАЛИЗ УВЕДОМЛЕНИЙ

### **1. ОБЫЧНЫЕ УВЕДОМЛЕНИЯ (NotificationService)**

#### **Когда появляются:**
- В назначенное время приема лекарства
- Лекарство еще не принято

#### **Лейаут:** `notification_medicine_card.xml`
```xml
<!-- Заголовок -->
<TextView
    android:text="@string/time_to_take_medicine_exclamation"
    android:textStyle="bold"
    android:gravity="center" />

<!-- Карточка лекарства -->
<LinearLayout>
    <!-- Название лекарства -->
    <TextView android:id="@+id/textMedicineName" />
    
    <!-- Дозировка -->
    <TextView android:id="@+id/textDosage" />
    
    <!-- Время приема -->
    <TextView android:id="@+id/textTime" />
    
    <!-- Кнопки действий -->
    <LinearLayout>
        <Button android:id="@+id/buttonTaken" 
                android:text="✅ Выпил" />
        <Button android:id="@+id/buttonSkip" 
                android:text="⏭ Пропустить" />
    </LinearLayout>
</LinearLayout>

<!-- Дополнительная информация -->
<TextView android:id="@+id/textAdditionalInfo" 
          android:text="@string/click_taken_to_mark" />
```

#### **Современный лейаут:** `notification_medicine_card_modern.xml`
```xml
<!-- Header с иконкой и заголовком -->
<LinearLayout>
    <!-- Иконка лекарства -->
    <FrameLayout android:id="@+id/notification_icon_container">
        <ImageView android:id="@+id/notification_icon" 
                   android:src="@drawable/ic_pill" />
    </FrameLayout>
    
    <!-- Заголовок и статус -->
    <LinearLayout>
        <TextView android:id="@+id/notification_title" 
                  android:text="@string/take_medicine" />
        <TextView android:id="@+id/notification_status" 
                  android:text="@string/active" />
    </LinearLayout>
</LinearLayout>

<!-- Карточка лекарства -->
<LinearLayout android:id="@+id/notification_card">
    <!-- Название лекарства -->
    <TextView android:id="@+id/medicine_name" />
    
    <!-- Детали лекарства -->
    <LinearLayout>
        <TextView android:id="@+id/medicine_dosage" />
        <TextView android:id="@+id/medicine_time" />
    </LinearLayout>
    
    <!-- Дополнительная информация (скрыта по умолчанию) -->
    <LinearLayout android:id="@+id/additional_info_container" 
                  android:visibility="gone">
        <TextView android:id="@+id/medicine_remaining" />
        <TextView android:id="@+id/medicine_group" />
    </LinearLayout>
</LinearLayout>

<!-- Кнопки действий -->
<LinearLayout>
    <Button android:id="@+id/button_taken" 
            android:text="✅ Принял" />
    <Button android:id="@+id/button_snooze" 
            android:text="⏰ Отложить" />
    <Button android:id="@+id/button_skip" 
            android:text="⏭ Пропустить" />
</LinearLayout>
```

### **2. УВЕДОМЛЕНИЯ О ПРОСРОЧКЕ (OverdueCheckService)**

#### **Когда появляются:**
- Лекарство просрочено (прошло больше 15 минут после времени приема)
- Периодически каждые 5 минут
- Повторяющиеся звук и вибрация каждые 5 секунд

#### **Код создания уведомления:**
```kotlin
private fun showOverdueNotification(overdueMedicines: List<Medicine>) {
    val notification = NotificationCompat.Builder(this, CHANNEL_ID_OVERDUE)
        .setContentTitle("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА!")
        .setContentText("У вас $overdueCount просроченных лекарств")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("🚨 ПРОСРОЧЕННЫЕ ЛЕКАРСТВА: $medicineNames\n\nПожалуйста, примите их как можно скорее!"))
        .setSmallIcon(R.drawable.ic_medicine)
        .setPriority(NotificationCompat.PRIORITY_MAX) // Максимальный приоритет
        .setCategory(NotificationCompat.CATEGORY_ALARM) // Категория будильника
        .setAutoCancel(false) // Не закрывать автоматически
        .setOngoing(true) // Постоянное уведомление
        .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
        .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // Интенсивная вибрация
        .setLights(0xFF0000FF.toInt(), 3000, 3000) // Мигание красным
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // На экране блокировки
        .setFullScreenIntent(pendingIntent, true) // Поверх всего
        .setTimeoutAfter(0) // Не скрывать автоматически
        .addAction(R.drawable.ic_check, "Принял лекарство", takeMedicinePendingIntent)
        .build()
}
```

#### **Особенности:**
- **Максимальный приоритет** - обходит Do Not Disturb
- **Категория будильника** - для приоритета
- **Постоянное уведомление** - не закрывается автоматически
- **Показывается на экране блокировки**
- **Повторяющиеся звук и вибрация**
- **Кнопка "Принял лекарство"** для быстрого действия

### **3. СИСТЕМНЫЕ АЛЕРТЫ (запасной вариант)**

#### **Когда появляются:**
- Если обычное уведомление не показалось
- При ошибках в системе уведомлений

#### **Код создания:**
```kotlin
private fun showSystemAlert(overdueMedicines: List<Medicine>) {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("show_overdue_medicines", true)
    }
    
    val pendingIntent = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    // Системный алерт поверх всего
    val alertIntent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("system_alert", true)
        putExtra("overdue_medicines", ArrayList(overdueMedicines.map { it.id }))
    }
}
```

### **4. TOAST СООБЩЕНИЯ**

#### **В MainActivity.kt:**
```kotlin
// При получении уведомления от AlarmManager
android.widget.Toast.makeText(this, "🚨 Проверьте просроченные лекарства!", 
    android.widget.Toast.LENGTH_LONG).show()

// При отметке лекарств как принятых
android.widget.Toast.makeText(this, "Лекарства помечены как принятые", 
    android.widget.Toast.LENGTH_SHORT).show()

// При показе просроченных лекарств
android.widget.Toast.makeText(this, "Показаны просроченные лекарства", 
    android.widget.Toast.LENGTH_SHORT).show()
```

## 🎨 ВИЗУАЛЬНЫЕ ЭЛЕМЕНТЫ

### **Цвета и стили:**
- **Красный цвет** для просроченных лекарств
- **Эмодзи** для привлечения внимания (🚨, ✅, ⏭, ⏰)
- **Жирный шрифт** для заголовков
- **Закругленные углы** для карточек
- **Тени** для глубины

### **Иконки:**
- `ic_medicine` - основная иконка лекарства
- `ic_pill` - иконка таблетки
- `ic_check` - иконка галочки для "Принял"
- `ic_time` - иконка времени

## 🔧 НАСТРОЙКИ КАНАЛОВ УВЕДОМЛЕНИЙ

### **Канал для фонового сервиса:**
```kotlin
CHANNEL_ID = "overdue_check_service"
IMPORTANCE_LOW
```

### **Канал для просроченных лекарств:**
```kotlin
CHANNEL_ID_OVERDUE = "overdue_medicines"
IMPORTANCE_HIGH
enableVibration(true)
setBypassDnd(true) // Обходит Do Not Disturb
lockscreenVisibility = VISIBILITY_PUBLIC // Показывать на экране блокировки
```

## 🎵 ЗВУКИ И ВИБРАЦИЯ

### **Обычные уведомления:**
- Стандартный звук уведомлений
- Короткая вибрация

### **Уведомления о просрочке:**
- **Повторяющиеся звук и вибрация** каждые 5 секунд
- **Интенсивная вибрация:** `longArrayOf(0, 1000, 500, 1000, 500, 1000)`
- **Мигание красным светом:** `0xFF0000FF.toInt(), 3000, 3000`

### **Остановка звуков:**
```kotlin
// При нажатии "Принял лекарство"
OverdueCheckService.forceStopSoundAndVibration()

// При отсутствии просроченных лекарств
stopRepeatingSoundAndVibration()
restoreOriginalSettings()
```

## 📱 ДЕЙСТВИЯ ПОЛЬЗОВАТЕЛЯ

### **Кнопки в уведомлениях:**

#### **1. "✅ Принял" / "✅ Выпил"**
- Отмечает лекарство как принятое
- Останавливает звуки и вибрацию
- Скрывает уведомление
- Обновляет UI

#### **2. "⏭ Пропустить"**
- Пропускает прием лекарства
- Скрывает уведомление
- Обновляет UI

#### **3. "⏰ Отложить" (только в современном лейауте)**
- Откладывает уведомление на 15 минут
- Показывает уведомление позже

### **Обработка в MainActivity:**
```kotlin
private fun handleNotificationIntent() {
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
            
            android.widget.Toast.makeText(this, "Лекарства помечены как принятые", 
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
```

## 🚨 ПРОБЛЕМЫ В ТЕКУЩЕЙ СИСТЕМЕ

### **1. Разные временные пороги:**
- Обычные уведомления: в назначенное время
- Просроченные: через 15 минут (но логика разная в разных местах)

### **2. Дублирование уведомлений:**
- Могут показываться и обычные, и просроченные уведомления одновременно

### **3. Сложная логика остановки:**
- Множественные проверки для остановки звуков и вибрации

### **4. Конфликт приоритетов:**
- Разные каналы уведомлений с разными приоритетами

## 💡 РЕКОМЕНДАЦИИ ПО УЛУЧШЕНИЮ

### **1. Унифицировать временные пороги:**
- Использовать единый порог 15 минут везде

### **2. Упростить логику уведомлений:**
- Один тип уведомления с разными приоритетами
- Единая логика остановки

### **3. Улучшить UX:**
- Более понятные тексты уведомлений
- Лучшая группировка просроченных лекарств
- Возможность настройки интервалов уведомлений

### **4. Добавить настройки:**
- Возможность отключить звуки/вибрацию
- Настройка интервалов повторения
- Персонализация текстов уведомлений

## 🎯 ЗАКЛЮЧЕНИЕ

Система уведомлений при просрочке лекарств **достаточно сложная**, но **функциональная**. Основные проблемы связаны с:

1. **Дублированием логики** в разных местах
2. **Разными временными порогами** для определения просрочки
3. **Сложностью управления** звуками и вибрацией
4. **Конфликтами приоритетов** между разными типами уведомлений

**Рекомендуется** унифицировать логику и упростить систему уведомлений для лучшей поддержки и пользовательского опыта. 