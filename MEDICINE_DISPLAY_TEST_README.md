# Тесты для анализа проблемы с отображением лекарств

Этот набор тестов предназначен для анализа проблемы с отображением данных лекарств в форме на сегодня в приложении MedicalNotes.

## Описание проблемы

Пользователь сообщает, что данные лекарств из XML не отображаются в форме лекарств на сегодня. Необходимо понять, на каком этапе происходит проблема:

1. **Загрузка данных** - правильно ли загружаются данные из XML
2. **Логика DosageCalculator** - корректно ли работает расчет лекарств на сегодня
3. **MainViewModel** - правильно ли работает логика загрузки лекарств
4. **Адаптер** - корректно ли отображаются данные в UI
5. **Групповая логика** - правильно ли работает логика групп "через день"

## Файлы тестов

### 1. `test_medicine_display_analysis.kt`
Основной тест для полного анализа проблемы. Включает:
- Анализ данных из XML
- Тестирование логики DosageCalculator
- Тестирование логики MainViewModel
- Тестирование логики адаптера
- Тестирование групповой логики
- Генерация отчета

### 2. `test_specific_dates.kt`
Тест для анализа конкретных дат из XML данных. Включает:
- Анализ дат начала приема
- Анализ дат последнего приема
- Анализ дат начала групп
- Тестирование групповой логики для конкретных дат

### 3. `test_medicine_display_data.kt`
Тест для анализа данных лекарств из XML

### 4. `test_main_viewmodel_logic.kt`
Тест логики MainViewModel для загрузки лекарств

### 5. `test_adapter_display_logic.kt`
Тест логики адаптера и отображения

## Данные для тестирования

Тесты используют данные из предоставленного XML:

```xml
<medicines>
  <medicine>
    <id>1754381301015</id>
    <name>Липетор</name>
    <dosage>20</dosage>
    <quantity>44</quantity>
    <remainingQuantity>44</remainingQuantity>
    <medicineType>Tablets</medicineType>
    <time>17:41</time>
    <frequency>EVERY_OTHER_DAY</frequency>
    <startDate>1754381301006</startDate>
    <isActive>true</isActive>
    <takenToday>false</takenToday>
    <lastTakenTime>1754473507174</lastTakenTime>
    <takenAt>0</takenAt>
    <isMissed>false</isMissed>
    <missedCount>0</missedCount>
    <isOverdue>false</isOverdue>
    <groupId>1754451744031</groupId>
    <groupName>Тестер</groupName>
    <groupOrder>1</groupOrder>
    <groupStartDate>1754451744031</groupStartDate>
    <groupFrequency>EVERY_OTHER_DAY</groupFrequency>
    <multipleDoses>false</multipleDoses>
    <doseTimes>17:41</doseTimes>
    <customDays></customDays>
    <updatedAt>1754540857591</updatedAt>
  </medicine>
  <medicine>
    <id>1754381353482</id>
    <name>Фубуксусат</name>
    <dosage>Полтоблетки</dosage>
    <quantity>34</quantity>
    <remainingQuantity>34</remainingQuantity>
    <medicineType>Tablets</medicineType>
    <time>16:15</time>
    <frequency>EVERY_OTHER_DAY</frequency>
    <startDate>1754381353472</startDate>
    <isActive>true</isActive>
    <takenToday>false</takenToday>
    <lastTakenTime>1754471876018</lastTakenTime>
    <takenAt>0</takenAt>
    <isMissed>false</isMissed>
    <missedCount>0</missedCount>
    <isOverdue>false</isOverdue>
    <groupId>1754451755574</groupId>
    <groupName>Тестер</groupName>
    <groupOrder>2</groupOrder>
    <groupStartDate>1754451755574</groupStartDate>
    <groupFrequency>EVERY_OTHER_DAY</groupFrequency>
    <multipleDoses>false</multipleDoses>
    <doseTimes>16:15</doseTimes>
    <customDays></customDays>
    <updatedAt>1754540857591</updatedAt>
  </medicine>
</medicines>
```

## Запуск тестов

### Основной тест
```bash
test_medicine_display.bat
```

### Тест конкретных дат
```bash
test_specific_dates.bat
```

## Что проверяют тесты

### 1. Анализ данных из XML
- Корректность загрузки данных
- Проверка обязательных полей
- Анализ дат и временных меток

### 2. Логика DosageCalculator
- Метод `shouldTakeMedicine()` - должно ли лекарство приниматься сегодня
- Метод `getMedicineStatus()` - статус лекарства
- Метод `getActiveMedicinesForDate()` - активные лекарства на дату

### 3. Групповая логика
- Логика групп "через день"
- Расчет дней группы
- Проверка порядка лекарств в группе
- Учет последнего приема

### 4. MainViewModel
- Симуляция метода `loadTodayMedicines()`
- Валидация групп
- Получение активных лекарств

### 5. Адаптер
- Симуляция отображения элементов
- Проверка данных для отображения
- Анализ возможных проблем с UI

## Возможные причины проблемы

### 1. Групповая логика
- Неправильный расчет дней группы
- Ошибка в логике "через день"
- Проблемы с валидацией групп

### 2. Даты
- Неправильное преобразование timestamp в даты
- Проблемы с часовыми поясами
- Ошибки в расчете дней с начала приема

### 3. Флаги состояния
- Неправильное значение `takenToday`
- Проблемы с `lastTakenTime`
- Ошибки в статусе `isActive`

### 4. Логика DosageCalculator
- Ошибки в методе `shouldTakeMedicine()`
- Проблемы с групповой логикой
- Неправильная фильтрация активных лекарств

## Интерпретация результатов

### Если лекарства не отображаются:
1. Проверьте логику `DosageCalculator.shouldTakeMedicine()`
2. Проверьте групповую логику
3. Проверьте даты начала приема
4. Проверьте флаг `takenToday`

### Если лекарства отображаются, но неправильно:
1. Проверьте логику адаптера
2. Проверьте данные для отображения
3. Проверьте UI компоненты

## Файлы результатов

- `test_analysis_results.txt` - результаты основного теста
- `test_dates_results.txt` - результаты теста конкретных дат

## Требования

- Kotlin compiler (kotlinc)
- Java Runtime Environment (JRE)
- Доступ к классам приложения (Medicine, DosageCalculator и др.)

## Примечания

- Тесты используют упрощенные версии некоторых методов для изоляции от Android-зависимостей
- Результаты могут отличаться в зависимости от текущей даты
- Для точного анализа необходимо учитывать временную зону системы 