#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для открытия проекта в Android Studio
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_info(message):
    print(f"[INFO] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def find_android_studio():
    """Ищет Android Studio"""
    possible_paths = [
        "C:\\Program Files\\Android\\Android Studio\\bin\\studio64.exe",
        "C:\\Program Files (x86)\\Android\\Android Studio\\bin\\studio64.exe",
        "C:\\Users\\mikedell\\AppData\\Local\\Android\\Sdk\\tools\\bin\\studio64.exe",
        "C:\\Users\\mikedell\\AppData\\Local\\Android\\Sdk\\tools\\studio64.exe"
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return path
    
    return None

def open_in_studio():
    """Открывает проект в Android Studio"""
    print("🔧 Открытие проекта в Android Studio")
    
    # Получаем абсолютный путь к проекту
    project_path = Path.cwd().absolute()
    print_info(f"Путь к проекту: {project_path}")
    
    # Ищем Android Studio
    studio_path = find_android_studio()
    if not studio_path:
        print_error("Android Studio не найден")
        print_info("Пожалуйста, установите Android Studio или укажите путь к нему")
        return False
    
    print_info(f"Найден Android Studio: {studio_path}")
    
    try:
        # Открываем проект в Android Studio
        print_info("Открытие проекта в Android Studio...")
        subprocess.Popen([studio_path, str(project_path)])
        
        print_success("Проект открыт в Android Studio!")
        print_info("Инструкции по сборке APK:")
        print_info("1. Дождитесь загрузки проекта")
        print_info("2. Выберите Build -> Build Bundle(s) / APK(s) -> Build APK(s)")
        print_info("3. Или используйте Build -> Make Project")
        print_info("4. APK будет создан в app/build/outputs/apk/debug/")
        
        return True
        
    except Exception as e:
        print_error(f"Ошибка при открытии Android Studio: {e}")
        return False

def create_build_instructions():
    """Создает файл с инструкциями по сборке"""
    instructions = """# Инструкции по сборке APK

## Способ 1: Через Android Studio (рекомендуется)

1. Откройте проект в Android Studio
2. Дождитесь синхронизации Gradle
3. Выберите Build -> Build Bundle(s) / APK(s) -> Build APK(s)
4. APK будет создан в папке: `app/build/outputs/apk/debug/app-debug.apk`

## Способ 2: Через командную строку

1. Откройте терминал в папке проекта
2. Выполните команду: `gradlew.bat assembleDebug`
3. APK будет создан в папке: `app/build/outputs/apk/debug/app-debug.apk`

## Способ 3: Через Gradle панель

1. В Android Studio откройте панель Gradle (справа)
2. Разверните app -> Tasks -> build
3. Дважды кликните на assembleDebug

## Установка APK на устройство

1. Включите режим разработчика на Android устройстве
2. Включите отладку по USB
3. Подключите устройство к компьютеру
4. Скопируйте APK файл на устройство
5. Установите APK через файловый менеджер

## Возможные проблемы

- Если сборка не удается, попробуйте File -> Invalidate Caches and Restart
- Убедитесь, что установлен Android SDK
- Проверьте, что Java версии 8 или выше установлена

## Структура проекта

- `app/src/main/java/` - исходный код Kotlin
- `app/src/main/res/` - ресурсы (макеты, строки, изображения)
- `app/src/main/AndroidManifest.xml` - манифест приложения
- `build.gradle` - настройки сборки
"""
    
    with open('BUILD_INSTRUCTIONS.md', 'w', encoding='utf-8') as f:
        f.write(instructions)
    
    print_success("Создан файл BUILD_INSTRUCTIONS.md с подробными инструкциями")

def main():
    print("="*60)
    print("🚀 Открытие проекта MedicalNotes в Android Studio")
    print("="*60)
    
    if not open_in_studio():
        print_error("Не удалось открыть проект в Android Studio")
        return False
    
    create_build_instructions()
    
    print("\n" + "="*60)
    print_success("Проект готов к сборке!")
    print("="*60)
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n[INFO] Операция прервана пользователем")
        sys.exit(1)
    except Exception as e:
        print_error(f"Неожиданная ошибка: {e}")
        sys.exit(1) 