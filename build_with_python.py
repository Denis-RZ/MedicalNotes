#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для сборки Android проекта MedicalNotes
Показывает подробный вывод процесса сборки
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_step(step, description):
    """Выводит шаг процесса"""
    print(f"\n{'='*60}")
    print(f"ШАГ {step}: {description}")
    print(f"{'='*60}")

def print_info(message):
    """Выводит информационное сообщение"""
    print(f"[INFO] {message}")

def print_error(message):
    """Выводит сообщение об ошибке"""
    print(f"[ERROR] {message}")

def print_success(message):
    """Выводит сообщение об успехе"""
    print(f"[SUCCESS] {message}")

def check_requirements():
    """Проверяет требования для сборки"""
    print_step(1, "Проверка требований")
    
    # Создаем папку для Gradle если её нет
    gradle_home = Path('C:/gradle_home')
    if not gradle_home.exists():
        print_info("Создание папки C:/gradle_home...")
        gradle_home.mkdir(parents=True, exist_ok=True)
        print_success("Папка C:/gradle_home создана")
    else:
        print_info("Папка C:/gradle_home уже существует")
    
    # Проверяем наличие Java
    try:
        result = subprocess.run(['java', '-version'], 
                              capture_output=True, text=True, timeout=10)
        if result.returncode == 0:
            print_info("Java найден")
            print_info(result.stderr.split('\n')[0])  # Версия Java
        else:
            print_error("Java не найден или не работает")
            return False
    except Exception as e:
        print_error(f"Ошибка при проверке Java: {e}")
        return False
    
    # Проверяем наличие gradlew
    if not os.path.exists('gradlew.bat'):
        print_error("Файл gradlew.bat не найден")
        return False
    print_info("gradlew.bat найден")
    
    # Проверяем наличие gradle wrapper jar
    if not os.path.exists('gradle/wrapper/gradle-wrapper.jar'):
        print_error("gradle-wrapper.jar не найден")
        return False
    print_info("gradle-wrapper.jar найден")
    
    # Проверяем основные файлы проекта
    required_files = [
        'build.gradle',
        'app/build.gradle',
        'settings.gradle',
        'gradle.properties'
    ]
    
    for file in required_files:
        if not os.path.exists(file):
            print_error(f"Файл {file} не найден")
            return False
        print_info(f"Файл {file} найден")
    
    print_success("Все требования выполнены")
    return True

def clean_project():
    """Очищает проект"""
    print_step(2, "Очистка проекта")
    
    try:
        print_info("Запуск gradlew clean...")
        # Устанавливаем переменную окружения для избежания проблем с кириллицей
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\gradle_home'
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
        result = subprocess.run(['gradlew.bat', 'clean'], 
                              capture_output=True, text=True, timeout=60, env=env)
        
        if result.returncode == 0:
            print_success("Проект очищен")
            if result.stdout:
                print_info("Вывод команды:")
                print(result.stdout)
        else:
            print_error("Ошибка при очистке проекта")
            if result.stderr:
                print_error("Ошибка:")
                print(result.stderr)
            return False
    except subprocess.TimeoutExpired:
        print_error("Таймаут при очистке проекта")
        return False
    except Exception as e:
        print_error(f"Ошибка при очистке: {e}")
        return False
    
    return True

def build_debug_apk():
    """Собирает debug APK"""
    print_step(3, "Сборка debug APK")
    
    try:
        print_info("Запуск gradlew assembleDebug...")
        print_info("Это может занять несколько минут...")
        
        # Устанавливаем переменную окружения для избежания проблем с кириллицей
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\gradle_home'
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
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
    print_step(4, "Поиск APK файла")
    
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
    """Основная функция"""
    print("🚀 Сборка Android проекта MedicalNotes")
    print(f"Время начала: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Проверяем, что мы в правильной директории
    if not os.path.exists('build.gradle'):
        print_error("Файл build.gradle не найден. Убедитесь, что вы в корне проекта.")
        return False
    
    # Проверяем требования
    if not check_requirements():
        print_error("Требования не выполнены. Сборка прервана.")
        return False
    
    # Очищаем проект
    if not clean_project():
        print_error("Ошибка при очистке проекта. Сборка прервана.")
        return False
    
    # Собираем APK
    if not build_debug_apk():
        print_error("Ошибка при сборке APK. Сборка прервана.")
        return False
    
    # Ищем APK
    if not find_apk():
        print_error("APK не найден после сборки.")
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