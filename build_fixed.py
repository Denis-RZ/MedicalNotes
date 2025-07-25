#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для сборки с исправленным путем Gradle
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_info(message):
    print(f"[INFO] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def build_project():
    """Собирает проект с исправленным путем Gradle"""
    print("🚀 Сборка проекта с исправленным путем Gradle")
    
    try:
        # Устанавливаем переменную окружения
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\gradle_home_clean'
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
        print_info("Запуск gradlew assembleDebug...")
        
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
    apk_path = Path('app/build/outputs/apk/debug/app-debug.apk')
    
    if apk_path.exists():
        size_mb = apk_path.stat().st_size / (1024 * 1024)
        print_success(f"APK найден: {apk_path}")
        print_info(f"Размер: {size_mb:.2f} МБ")
        return True
    else:
        print_error("APK файл не найден")
        return False

if __name__ == "__main__":
    if build_project():
        find_apk()
    else:
        sys.exit(1)
