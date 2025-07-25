# Настройки Gradle для Android Studio

## Проблема
Gradle не может создать файлы в пути с кириллицей: C:\Users\mikedell\.gradle

## Решение
Используйте новый путь без кириллицы: C:\gradle_home_clean

## Как настроить в Android Studio:

### Способ 1: Через настройки
1. Откройте Android Studio
2. Перейдите в File -> Settings (или Ctrl+Alt+S)
3. В левом меню выберите Build, Execution, Deployment -> Gradle
4. В поле "Gradle user home" введите: C:\gradle_home_clean
5. Нажмите Apply и OK
6. Перезапустите Android Studio

### Способ 2: Через gradle.properties
Файл gradle.properties уже обновлен с правильным путем.

### Способ 3: Через переменную окружения
Установите переменную окружения GRADLE_USER_HOME=C:\gradle_home_clean

## Проверка
После настройки попробуйте синхронизировать проект:
1. File -> Sync Project with Gradle Files
2. Или нажмите кнопку "Sync Now" в верхней панели

## Если проблема остается
1. File -> Invalidate Caches and Restart
2. Выберите "Invalidate and Restart"
