#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для сборки Android проекта с локальным Gradle
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_step(step, description):
    print(f"\n{'='*60}")
    print(f"ШАГ {step}: {description}")
    print(f"{'='*60}")

def print_info(message):
    print(f"[INFO] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def setup_local_gradle():
    """Настраивает локальный Gradle"""
    print_step(1, "Настройка локального Gradle")
    
    try:
        # Создаем локальную папку для Gradle
        local_gradle = Path('./.gradle_local')
        local_gradle.mkdir(exist_ok=True)
        
        # Копируем gradle wrapper из рабочего проекта
        source_wrapper = Path('../AndroidVoiceOn/gradle/wrapper')
        target_wrapper = Path('./gradle/wrapper')
        
        if source_wrapper.exists():
            import shutil
            shutil.copytree(source_wrapper, target_wrapper, dirs_exist_ok=True)
            print_success("Gradle wrapper скопирован")
        
        print_success("Локальный Gradle настроен")
        return True
    except Exception as e:
        print_error(f"Ошибка при настройке Gradle: {e}")
        return False

def build_project():
    """Собирает проект"""
    print_step(2, "Сборка проекта")
    
    try:
        # Устанавливаем переменную окружения для локального Gradle
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = str(Path('./.gradle_local').absolute())
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
        print_info("Запуск gradlew clean...")
        clean_result = subprocess.run(
            ['gradlew.bat', 'clean'],
            capture_output=True,
            text=True,
            timeout=60,
            env=env
        )
        
        if clean_result.returncode != 0:
            print_info("Очистка завершена с предупреждениями")
        
        print_info("Запуск gradlew assembleDebug...")
        print_info("Это может занять несколько минут...")
        
        # Запускаем сборку с выводом в реальном времени
        process = subprocess.Popen(
            ['gradlew.bat', 'assembleDebug'],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            universal_newlines=True,
            env=env
        )
        
        # Выводим вывод в реальном времени
        for line in process.stdout:
            print(line.rstrip())
        
        # Ждем завершения процесса
        return_code = process.wait()
        
        if return_code == 0:
            print_success("APK успешно собран!")
            return True
        else:
            print_error(f"Ошибка при сборке. Код возврата: {return_code}")
            return False
            
    except Exception as e:
        print_error(f"Ошибка при сборке: {e}")
        return False

def find_apk():
    """Ищет собранный APK файл"""
    print_step(3, "Поиск APK файла")
    
    apk_path = Path('app/build/outputs/apk/debug/app-debug.apk')
    
    if apk_path.exists():
        size_mb = apk_path.stat().st_size / (1024 * 1024)
        print_success(f"APK найден: {apk_path}")
        print_info(f"Размер: {size_mb:.2f} МБ")
        print_info(f"Полный путь: {apk_path.absolute()}")
        return True
    else:
        print_error("APK файл не найден")
        print_info("Ожидаемый путь: app/build/outputs/apk/debug/app-debug.apk")
        return False

def main():
    print("🚀 Сборка Android проекта MedicalNotes (локальный Gradle)")
    print(f"Время начала: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    if not setup_local_gradle():
        print_error("Настройка Gradle не удалась")
        return False
    
    if not build_project():
        print_error("Сборка не удалась")
        return False
    
    if not find_apk():
        print_error("APK не найден")
        return False
    
    print("\n" + "="*60)
    print_success("🎉 Сборка завершена успешно!")
    print(f"Время завершения: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n[INFO] Сборка прервана пользователем")
        sys.exit(1)
    except Exception as e:
        print_error(f"Неожиданная ошибка: {e}")
        sys.exit(1) 