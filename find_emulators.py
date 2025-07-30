#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Поиск всех эмуляторов на системе
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def find_all_emulators():
    """Поиск всех возможных эмуляторов"""
    emulators = {}
    
    # BlueStacks
    bluestacks_paths = [
        "C:/Program Files/BlueStacks_nxt/HD-Player.exe",
        "C:/Program Files (x86)/BlueStacks_nxt/HD-Player.exe",
        "C:/Program Files/BlueStacks/HD-Player.exe",
        "C:/Program Files (x86)/BlueStacks/HD-Player.exe",
        "C:/Program Files/BlueStacks_nxt/HD-Adb.exe",
        "C:/Program Files (x86)/BlueStacks_nxt/HD-Adb.exe"
    ]
    
    for path in bluestacks_paths:
        if os.path.exists(path):
            emulators["BlueStacks"] = path
            break
    
    # NoxPlayer
    nox_paths = [
        "C:/Program Files/Nox/bin/Nox.exe",
        "C:/Program Files (x86)/Nox/bin/Nox.exe",
        "C:/Program Files/Nox/bin/NoxAdb.exe",
        "C:/Program Files (x86)/Nox/bin/NoxAdb.exe"
    ]
    
    for path in nox_paths:
        if os.path.exists(path):
            emulators["NoxPlayer"] = path
            break
    
    # LDPlayer
    ld_paths = [
        "C:/Program Files/LDPlayer/LDPlayer9/ldconsole.exe",
        "C:/Program Files (x86)/LDPlayer/LDPlayer9/ldconsole.exe",
        "C:/Program Files/LDPlayer/LDPlayer4.0/ldconsole.exe",
        "C:/Program Files (x86)/LDPlayer/LDPlayer4.0/ldconsole.exe"
    ]
    
    for path in ld_paths:
        if os.path.exists(path):
            emulators["LDPlayer"] = path
            break
    
    # MEmu
    memu_paths = [
        "C:/Program Files/MEmu/MEmu.exe",
        "C:/Program Files (x86)/MEmu/MEmu.exe",
        "C:/Program Files/MEmu/MEmuConsole.exe",
        "C:/Program Files (x86)/MEmu/MEmuConsole.exe"
    ]
    
    for path in memu_paths:
        if os.path.exists(path):
            emulators["MEmu"] = path
            break
    
    # Android Studio AVD
    avd_paths = [
        os.path.expanduser("~/AppData/Local/Android/Sdk/emulator/emulator.exe"),
        "C:/Android/Sdk/emulator/emulator.exe",
        "C:/Program Files/Android/Android Studio/sdk/emulator/emulator.exe"
    ]
    
    for path in avd_paths:
        if os.path.exists(path):
            emulators["Android Studio AVD"] = path
            break
    
    # Genymotion
    genymotion_paths = [
        "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
        "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe"
    ]
    
    for path in genymotion_paths:
        if os.path.exists(path):
            emulators["Genymotion"] = path
            break
    
    return emulators

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

def check_devices(adb_path):
    """Проверка подключенных устройств"""
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
        
        return devices
    except:
        return []

def main():
    print("🔍 Поиск эмуляторов на системе")
    print("=" * 40)
    
    # Ищем ADB
    adb_path = find_adb()
    if adb_path:
        print(f"✅ ADB найден: {adb_path}")
    else:
        print("❌ ADB не найден")
    
    # Ищем эмуляторы
    emulators = find_all_emulators()
    
    if not emulators:
        print("\n❌ Эмуляторы не найдены")
        print("\n💡 Рекомендации:")
        print("   1. Установите BlueStacks с официального сайта:")
        print("      https://www.bluestacks.com/")
        print("   2. Или NoxPlayer:")
        print("      https://www.bignox.com/")
        print("   3. Или используйте стандартный Android Studio")
        return
    
    print(f"\n✅ Найдено эмуляторов: {len(emulators)}")
    
    for name, path in emulators.items():
        print(f"   📱 {name}: {path}")
    
    # Проверяем подключенные устройства
    if adb_path:
        devices = check_devices(adb_path)
        if devices:
            print(f"\n✅ Подключенные устройства: {len(devices)}")
            for device in devices:
                print(f"   📱 {device}")
        else:
            print("\n❌ Нет подключенных устройств")
    
    # Создаем запускаторы для найденных эмуляторов
    print("\n📋 Создаем запускаторы...")
    
    for name, path in emulators.items():
        try:
            launcher_content = f"""@echo off
echo Запуск MedicalNotes на {name}...
python fast_emulator.py
pause
"""
            
            launcher_path = Path(f"Запустить_{name.replace(' ', '_')}.bat")
            with open(launcher_path, "w", encoding="utf-8") as f:
                f.write(launcher_content)
            
            print(f"   ✅ {launcher_path}")
            
        except Exception as e:
            print(f"   ❌ Ошибка создания {name}: {e}")
    
    print(f"\n🎉 Готово! Найдено {len(emulators)} эмуляторов")
    print("💡 Для запуска используйте:")
    for name in emulators.keys():
        print(f"   Запустить_{name.replace(' ', '_')}.bat")

if __name__ == "__main__":
    main() 