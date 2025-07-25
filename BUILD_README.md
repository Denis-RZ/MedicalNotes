# 🚀 MedicalNotes - Система сборки Android проекта

## 📋 Обзор

Этот проект содержит универсальную систему сборки для Android приложения MedicalNotes с автоматическим исправлением проблем.

## 🛠️ Доступные скрипты

### 1. **build.bat** - Основной скрипт сборки (с цветами)
```bash
build.bat
```
**Возможности:**
- ✅ Debug APK (быстрая сборка)
- ✅ Release APK (продакшн)
- ✅ Clean проект (очистка)
- ✅ Проверка версии Gradle
- ✅ Исправление проблем Gradle
- ✅ Цветной вывод и UTF-8 кодировка

### 2. **build.ps1** - PowerShell версия
```powershell
.\build.ps1
# или с параметрами:
.\build.ps1 debug
.\build.ps1 release
.\build.ps1 clean
.\build.ps1 version
.\build.ps1 fix
.\build.ps1 help
```
**Возможности:**
- ✅ Все функции build.bat
- ✅ Параметры командной строки
- ✅ Улучшенная обработка ошибок
- ✅ Красивое форматирование

### 3. **build_simple.bat** - Простая версия (без цветов)
```bash
build_simple.bat
```
**Для систем без поддержки цветов или совместимости.**

### 4. **build_fix.bat** - Скрипт исправления проблем
```bash
build_fix.bat
```
**Возможности:**
- ✅ Очистка кэша Gradle
- ✅ Пересоздание Gradle wrapper
- ✅ Исправление проблем с кириллицей
- ✅ Проверка Java версии
- ✅ Полная диагностика
- ✅ Все исправления сразу

### 5. **build_fixed.py** - Python скрипт сборки
```bash
py build_fixed.py
```
**Специально для проектов с проблемами кириллицы.**

## 🔧 Настройки Gradle

### gradle.properties
```properties
# Память и производительность
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Стабильность
org.gradle.daemon=false

# Локальный кэш
org.gradle.cache.dir=./.gradle-cache

# Путь без кириллицы
org.gradle.user.home=C:/gradle_home_clean

# Оптимизации
org.gradle.workers.max=8
org.gradle.unsafe.configuration-cache=true
```

## 🚨 Решение проблем

### Проблема: Кириллица в пути пользователя
**Ошибка:** `Could not create parent directory for lock file C:\Users\имя\.gradle`

**Решение:**
1. Запустите `build_fix.bat` → выберите "3"
2. Или запустите `py fix_gradle_path.py`
3. Или вручную в Android Studio: Settings → Gradle → Gradle user home: `C:\gradle_home_clean`

### Проблема: Поврежденный кэш
**Ошибка:** `CorruptedCacheException` или `Failed to create Jar file`

**Решение:**
1. Запустите `build_fix.bat` → выберите "1" (очистка кэша)
2. Или запустите `py build_clean.py`

### Проблема: Не работает gradlew.bat
**Решение:**
1. Запустите `build_fix.bat` → выберите "2" (пересоздание wrapper)
2. Проверьте JAVA_HOME: `build_fix.bat` → выберите "4"

### Проблема: Недостаточно памяти
**Решение:**
1. Увеличьте память в gradle.properties: `org.gradle.jvmargs=-Xmx8192m`
2. Закройте другие приложения
3. Используйте `build_simple.bat` для экономии ресурсов

## 📱 Сборка APK

### Debug APK (для тестирования)
```bash
# Способ 1: Через меню
build.bat
# Выберите "1"

# Способ 2: Прямая команда
gradlew.bat assembleDebug --parallel --max-workers=8

# Способ 3: PowerShell
.\build.ps1 debug
```

### Release APK (для публикации)
```bash
# Способ 1: Через меню
build.bat
# Выберите "2"

# Способ 2: Прямая команда
gradlew.bat assembleRelease --parallel --max-workers=8

# Способ 3: PowerShell
.\build.ps1 release
```

## 📂 Расположение APK файлов

### Debug APK
```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```
app/build/outputs/apk/release/app-release.apk
```

## 🔍 Диагностика

### Полная диагностика
```bash
build_fix.bat
# Выберите "5"
```

### Проверка версий
```bash
# Gradle
gradlew.bat --version

# Java
java -version

# Android Studio
# File → About
```

## 🎯 Рекомендации

### Для быстрой сборки:
1. Используйте `build.bat` → "1" (Debug)
2. Убедитесь, что кэш очищен
3. Закройте Android Studio во время сборки

### Для стабильной сборки:
1. Используйте `build_fix.bat` → "6" (все исправления)
2. Затем `build.bat` → "2" (Release)
3. Проверьте APK в папке outputs

### Для отладки проблем:
1. Запустите `build_fix.bat` → "5" (диагностика)
2. Проверьте логи в консоли
3. Используйте `build_simple.bat` для чистого вывода

## 📞 Поддержка

### Если ничего не помогает:
1. Удалите папку `.gradle` в корне проекта
2. Удалите папку `app/build`
3. Перезапустите Android Studio
4. Выполните Gradle sync
5. Попробуйте сборку снова

### Логи и отладка:
- Gradle логи: `gradlew.bat assembleDebug --info`
- Подробные логи: `gradlew.bat assembleDebug --debug`
- Очистка и сборка: `gradlew.bat clean assembleDebug`

---

**Удачной сборки! 🚀** 