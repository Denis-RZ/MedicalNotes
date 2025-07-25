# 🚀 **УНИВЕРСАЛЬНЫЙ ПРОМПТ ДЛЯ СБОРКИ ANDROID ПРОЕКТА**

**Скопируй этот текст в новый чат Cursor:**

---

Привет! У меня есть Android проект, который не собирается. Помоги мне создать универсальный скрипт сборки и исправить проблемы.

**Что нужно сделать:**

1. **Создай универсальный скрипт сборки** `build.bat` с поддержкой:
   - Debug сборки (быстрая)
   - Release сборки (для публикации)
   - Очистки проекта
   - Цветного вывода и кодировки UTF-8

2. **Исправь проблемы с Gradle:**
   - Очисти кэш Gradle
   - Настрой локальную папку кэша
   - Исправь проблемы с правами доступа
   - Реши конфликты версий Java

3. **Создай альтернативные скрипты:**
   - PowerShell версию (`build.ps1`)
   - Простую версию без цветов (`build_simple.bat`)
   - Скрипт для исправления проблем (`build_fix.bat`)

4. **Настрой gradle.properties:**
   - Локальный кэш: `org.gradle.cache.dir=./.gradle-cache`
   - Отключи daemon: `org.gradle.daemon=false`
   - Увеличь память: `org.gradle.jvmargs=-Xmx4096m`

**Структура скрипта build.bat:**
```batch
@echo off
chcp 65001 >nul
echo [32m[1mAndroid Project - Build System[0m
echo ================================
set GRADLE_OPTS=-Xmx4096m -XX:MaxPermSize=1024m -Dfile.encoding=UTF-8
set JAVA_OPTS=-Xmx4096m -XX:MaxPermSize=1024m

echo [36mSelect build type:[0m
echo [33m1.[0m Debug APK (fast build)
echo [33m2.[0m Release APK (production)
echo [33m3.[0m Clean project
set /p choice="[36mEnter number (1-3): [0m"

if "%choice%"=="1" goto debug
if "%choice%"=="2" goto release
if "%choice%"=="3" goto clean

:debug
echo [36m[1mBuilding Debug APK...[0m
call gradlew.bat assembleDebug --parallel --max-workers=8
goto end

:release
echo [36m[1mBuilding Release APK...[0m
call gradlew.bat assembleRelease --parallel --max-workers=8
goto end

:clean
echo [36m[1mCleaning project...[0m
call gradlew.bat clean
goto end

:end
pause
```

**Команды для исправления проблем:**
```powershell
# Очистка кэша
Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "app\build" -ErrorAction SilentlyContinue

# Проверка Java версии
java -version

# Прямая сборка без wrapper
gradle assembleDebug
```

**Если не работает gradlew.bat:**
- Попробуй `gradle wrapper` для пересоздания
- Используй `gradle` напрямую
- Проверь переменную `JAVA_HOME`

Создай все эти файлы и помоги мне собрать проект! 🚀

---

**Используй этот промпт в новом чате, и AI поможет настроить сборку для любого Android проекта!** 🎯

---

## 📋 **ДОПОЛНИТЕЛЬНЫЕ ВАРИАНТЫ ПРОМПТА**

### **Вариант 2: Для проектов с кириллицей в пути**
```
Привет! У меня Android проект с кириллицей в пути пользователя (C:\Users\имя\.gradle), 
из-за чего Gradle не может создавать файлы. Помоги исправить эту проблему.

Создай скрипт, который:
1. Создаст новую папку для Gradle без кириллицы (C:\gradle_home_clean)
2. Скопирует все дистрибутивы Gradle
3. Обновит gradle.properties с новым путем
4. Создаст инструкции для Android Studio
```

### **Вариант 3: Для проблем с кэшем**
```
Привет! У меня Android проект с поврежденным кэшем Gradle. 
Ошибки типа "CorruptedCacheException" и "Failed to create Jar file".

Создай скрипт, который:
1. Очистит поврежденные папки кэша (journal-1, metadata-2.106)
2. Создаст локальный кэш в папке проекта
3. Пересоздаст Gradle wrapper
4. Проверит целостность файлов
```

### **Вариант 4: Для проблем с Java**
```
Привет! У меня Android проект с проблемами Java версии. 
Ошибки совместимости между Java 8, 11, 17, 21.

Создай скрипт, который:
1. Проверит установленные версии Java
2. Настроит JAVA_HOME для совместимой версии
3. Обновит gradle.properties для конкретной версии
4. Создаст альтернативные конфигурации
```

---

## 🎯 **КАК ИСПОЛЬЗОВАТЬ**

1. **Скопируй нужный промпт** в новый чат Cursor
2. **Замени описание проблемы** на свою конкретную ситуацию
3. **Добавь детали проекта** (версии Android Studio, Gradle, Java)
4. **Укажи конкретные ошибки** если есть
5. **Запусти промпт** и получи готовые скрипты!

**Результат:** Получишь готовые скрипты сборки и инструкции по исправлению проблем! 🚀 