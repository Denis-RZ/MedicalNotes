#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Простой запуск Genymotion с QEMU
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
            return path
    return None

def find_gmtool():
    """Поиск gmtool"""
    gmtool_paths = [
        "C:/Program Files/Genymobile/Genymotion/gmtool.exe",
        "C:/Program Files (x86)/Genymobile/Genymotion/gmtool.exe"
    ]
    
    for path in gmtool_paths:
        if os.path.exists(path):
            return path
    return None

def configure_qemu(gmtool_path):
    """Настройка QEMU"""
    try:
        print("🔧 Настраиваем QEMU...")
        result = subprocess.run([
            gmtool_path, "config", "--hypervisor", "qemu"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ QEMU настроен")
            return True
        else:
            print(f"❌ Ошибка настройки QEMU: {result.stderr}")
            return False
    except Exception as e:
        print(f"❌ Ошибка: {e}")
        return False

def start_genymotion():
    """Запуск Genymotion"""
    try:
        print("🚀 Запускаем Genymotion...")
        
        genymotion_path = "C:/Program Files/Genymobile/Genymotion/genymotion.exe"
        if not os.path.exists(genymotion_path):
            print("❌ Genymotion не найден")
            return False
        
        # Запускаем Genymotion
        subprocess.Popen([genymotion_path])
        print("✅ Genymotion запущен")
        print("💡 Дождитесь загрузки и создайте устройство вручную")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка запуска: {e}")
        return False

def wait_for_device(adb_path, timeout=300):
    """Ожидание подключения устройства"""
    print("⏳ Ожидаем подключения устройства...")
    
    for i in range(timeout):
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
                print(f"✅ Устройство подключено: {devices[0]}")
                return devices[0]
            
            print(f"⏳ Ожидание... ({i+1}/{timeout})")
            time.sleep(2)
            
        except Exception as e:
            print(f"❌ Ошибка проверки: {e}")
            time.sleep(2)
    
    print("❌ Устройство не подключилось")
    return None

def install_and_run_app(adb_path, device_id):
    """Установка и запуск приложения"""
    try:
        # Ищем APK
        apk_paths = [
            Path("MedicalNotes-v2.2-fixed.apk"),
            Path("MedicalNotes-v2.1-update.apk"),
            Path("MedicalNotes-with-alarm-sound.apk")
        ]
        
        apk_path = None
        for path in apk_paths:
            if path.exists():
                apk_path = path
                break
        
        if not apk_path:
            print("❌ APK не найден")
            return False
        
        print(f"📱 Устанавливаем приложение...")
        
        # Устанавливаем APK
        result = subprocess.run([
            adb_path, "-s", device_id, "install", "-r", str(apk_path)
        ], capture_output=True, text=True, encoding='utf-8')
        
        if "Success" in result.stdout:
            print("✅ Приложение установлено")
            
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

def create_launcher():
    """Создание запускатора"""
    try:
        print("📋 Создаем запускатор...")
        
        launcher_content = """@echo off
echo Запуск MedicalNotes на Genymotion...
python simple_genymotion.py
pause
"""
        
        launcher_path = Path("Запустить_Genymotion_Простой.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Запускатор создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания запускатора: {e}")
        return False

def main():
    print("🚀 Simple Genymotion Launcher")
    print("=" * 35)
    
    # Ищем ADB
    adb_path = find_adb()
    if not adb_path:
        print("❌ ADB не найден")
        return False
    
    print(f"✅ ADB найден: {adb_path}")
    
    # Ищем gmtool
    gmtool_path = find_gmtool()
    if gmtool_path:
        print(f"✅ gmtool найден: {gmtool_path}")
        configure_qemu(gmtool_path)
    else:
        print("⚠️ gmtool не найден, пропускаем настройку QEMU")
    
    # Запускаем Genymotion
    if not start_genymotion():
        return False
    
    print("\n📋 Инструкции:")
    print("1. Дождитесь загрузки Genymotion")
    print("2. Создайте новое устройство (если нет)")
    print("3. Запустите устройство")
    print("4. Дождитесь полной загрузки Android")
    print("5. Нажмите Enter для продолжения...")
    
    input()
    
    # Ждем подключения устройства
    device_id = wait_for_device(adb_path)
    if not device_id:
        print("❌ Не удалось подключить устройство")
        return False
    
    # Устанавливаем и запускаем приложение
    if install_and_run_app(adb_path, device_id):
        # Создаем запускатор
        create_launcher()
        
        print("\n🎉 MedicalNotes запущен на Genymotion!")
        print("💡 Для быстрого запуска используйте:")
        print("   Запустить_Genymotion_Простой.bat")
        
        return True
    else:
        print("❌ Не удалось запустить приложение")
        return False

if __name__ == "__main__":
    main() 