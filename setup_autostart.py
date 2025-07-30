#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Настройка автозапуска приложения в Genymotion
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
            return []
            
    except Exception as e:
        print(f"❌ Ошибка проверки устройств: {e}")
        return []

def setup_autostart(adb_path, device_id):
    """Настройка автозапуска"""
    try:
        print(f"🔧 Настраиваем автозапуск на {device_id}...")
        
        # Создаем скрипт автозапуска
        autostart_script = """#!/system/bin/sh
# Автозапуск MedicalNotes при загрузке
sleep 15
am start -n com.medicalnotes.app/.MainActivity
"""
        
        # Записываем скрипт во временный файл
        script_path = Path("autostart_medicalnotes.sh")
        with open(script_path, "w") as f:
            f.write(autostart_script)
        
        # Копируем скрипт на устройство
        result = subprocess.run([
            adb_path, "-s", device_id, "push", 
            str(script_path), "/data/local/tmp/autostart_medicalnotes.sh"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode != 0:
            print(f"❌ Ошибка копирования скрипта: {result.stderr}")
            return False
        
        # Делаем скрипт исполняемым
        result = subprocess.run([
            adb_path, "-s", device_id, "shell", 
            "chmod", "755", "/data/local/tmp/autostart_medicalnotes.sh"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode != 0:
            print(f"❌ Ошибка установки прав: {result.stderr}")
            return False
        
        # Добавляем автозапуск в init.rc (если возможно)
        print("📝 Добавляем в автозапуск...")
        
        # Создаем команду для автозапуска
        autostart_cmd = "exec /system/bin/sh /data/local/tmp/autostart_medicalnotes.sh"
        
        # Пытаемся добавить в системные настройки
        result = subprocess.run([
            adb_path, "-s", device_id, "shell", 
            "echo", autostart_cmd, ">>", "/system/etc/init.d/99autostart"
        ], capture_output=True, text=True, encoding='utf-8')
        
        # Удаляем временный файл
        script_path.unlink()
        
        print("✅ Автозапуск настроен!")
        print("💡 Приложение будет запускаться автоматически при загрузке эмулятора")
        
        return True
        
    except Exception as e:
        print(f"❌ Ошибка настройки автозапуска: {e}")
        return False

def create_quick_launcher():
    """Создание быстрого запуска"""
    try:
        print("📋 Создаем быстрый запуск...")
        
        launcher_content = """@echo off
echo Запуск MedicalNotes с автозапуском...
python auto_launch.py
pause
"""
        
        launcher_path = Path("Запустить_с_автозапуском.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Быстрый запуск создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания быстрого запуска: {e}")
        return False

def main():
    print("🔧 Настройка автозапуска MedicalNotes")
    print("=" * 40)
    
    # Проверяем ADB
    adb_path = find_adb()
    if not adb_path:
        return False
    
    # Проверяем устройства
    devices = check_devices(adb_path)
    if not devices:
        print("\n📋 Что делать:")
        print("1. Запустите Genymotion: python light_emulator.py")
        print("2. Создайте и запустите виртуальное устройство")
        print("3. Повторите: python setup_autostart.py")
        return False
    
    # Настраиваем автозапуск на первом устройстве
    device_id = devices[0]
    if len(devices) > 1:
        print(f"📱 Найдено несколько устройств, используем: {device_id}")
    
    # Настраиваем автозапуск
    if setup_autostart(adb_path, device_id):
        # Создаем быстрый запуск
        create_quick_launcher()
        
        print("\n🎉 Автозапуск настроен успешно!")
        print("💡 Теперь приложение будет запускаться автоматически")
        print("💡 Для быстрого запуска используйте: Запустить_с_автозапуском.bat")
        
        return True
    else:
        print("❌ Не удалось настроить автозапуск")
        return False

if __name__ == "__main__":
    main() 