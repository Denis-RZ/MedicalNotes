#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Простая проверка и запуск MedicalNotes
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def find_adb():
    """Поиск ADB"""
    possible_paths = [
        os.path.expanduser("~/AppData/Local/Android/Sdk/platform-tools/adb.exe"),
        "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe",
        "C:/Android/Sdk/platform-tools/adb.exe",
        "C:/Program Files/Android/Android Studio/sdk/platform-tools/adb.exe",
        "adb.exe"
    ]
    
    for path in possible_paths:
        path = os.path.expandvars(path)
        if os.path.exists(path):
            print(f"✅ ADB найден: {path}")
            return path
            
    print("❌ ADB не найден")
    return None

def find_apk():
    """Поиск APK"""
    project_dir = Path(__file__).parent
    possible_paths = [
        project_dir / "MedicalNotes-v2.2-fixed.apk",
        project_dir / "MedicalNotes-v2.1-update.apk",
        project_dir / "MedicalNotes-with-alarm-sound.apk",
        project_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    ]
    
    for path in possible_paths:
        if path.exists():
            print(f"✅ APK найден: {path}")
            return path
            
    print("❌ APK не найден")
    return None

def check_devices(adb_path):
    """Проверка устройств"""
    try:
        result = subprocess.run([adb_path, "devices"], 
                              capture_output=True, text=True, encoding='utf-8')
        
        lines = result.stdout.strip().split('\n')[1:]
        devices = []
        
        for line in lines:
            if line.strip() and '\t' in line:
                device_id, status = line.split('\t')
                if status == 'device':
                    devices.append(device_id)
        
        if devices:
            print(f"✅ Найдено устройств: {len(devices)}")
            for device in devices:
                print(f"   - {device}")
            return devices
        else:
            print("❌ Нет подключенных устройств")
            print("💡 Запустите эмулятор или подключите устройство")
            return []
            
    except Exception as e:
        print(f"❌ Ошибка проверки устройств: {e}")
        return []

def install_and_run(adb_path, apk_path, device_id):
    """Установка и запуск приложения"""
    try:
        print(f"📱 Устанавливаем на {device_id}...")
        
        # Устанавливаем APK
        result = subprocess.run([
            adb_path, "-s", device_id, "install", "-r", str(apk_path)
        ], capture_output=True, text=True, encoding='utf-8')
        
        if "Success" in result.stdout:
            print("✅ Установлено успешно")
            
            # Запускаем приложение
            print("🚀 Запускаем приложение...")
            result = subprocess.run([
                adb_path, "-s", device_id, "shell", 
                "am", "start", "-n", "com.medicalnotes.app/.MainActivity"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ Приложение запущено!")
                return True
            else:
                print(f"❌ Ошибка запуска: {result.stderr}")
                return False
        else:
            print(f"❌ Ошибка установки: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка: {e}")
        return False

def main():
    print("🏥 MedicalNotes Quick Check & Run")
    print("=" * 40)
    
    # Проверяем ADB
    adb_path = find_adb()
    if not adb_path:
        return False
    
    # Проверяем APK
    apk_path = find_apk()
    if not apk_path:
        print("💡 Сначала соберите проект или скачайте APK")
        return False
    
    # Проверяем устройства
    devices = check_devices(adb_path)
    if not devices:
        print("\n📋 Что делать:")
        print("1. Запустите Genymotion: python light_emulator.py")
        print("2. Создайте виртуальное устройство")
        print("3. Запустите устройство")
        print("4. Повторите: python check_and_run.py")
        return False
    
    # Устанавливаем и запускаем на первом устройстве
    device_id = devices[0]
    if len(devices) > 1:
        print(f"📱 Найдено несколько устройств, используем: {device_id}")
    
    success = install_and_run(adb_path, apk_path, device_id)
    
    if success:
        print("\n🎉 Готово! Приложение запущено на устройстве.")
        print("💡 Для просмотра логов: adb logcat")
    
    return success

if __name__ == "__main__":
    main() 