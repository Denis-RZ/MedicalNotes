#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Настройка Genymotion с QEMU вместо VirtualBox
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def find_genymotion():
    """Поиск Genymotion"""
    genymotion_paths = [
        "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
        "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe"
    ]
    
    for path in genymotion_paths:
        if os.path.exists(path):
            print(f"✅ Genymotion найден: {path}")
            return path
    
    print("❌ Genymotion не найден")
    return None

def find_gmtool():
    """Поиск gmtool"""
    gmtool_paths = [
        "C:/Program Files/Genymobile/Genymotion/gmtool.exe",
        "C:/Program Files (x86)/Genymobile/Genymotion/gmtool.exe"
    ]
    
    for path in gmtool_paths:
        if os.path.exists(path):
            print(f"✅ gmtool найден: {path}")
            return path
    
    print("❌ gmtool не найден")
    return None

def configure_qemu(gmtool_path):
    """Настройка QEMU в Genymotion"""
    try:
        print("🔧 Настраиваем QEMU...")
        
        # Настраиваем гипервизор на QEMU
        result = subprocess.run([
            gmtool_path, "config", "--hypervisor", "qemu"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ QEMU настроен успешно")
            return True
        else:
            print(f"❌ Ошибка настройки QEMU: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка настройки QEMU: {e}")
        return False

def create_virtual_device(gmtool_path):
    """Создание виртуального устройства"""
    try:
        print("📱 Создаем виртуальное устройство...")
        
        # Список доступных устройств
        result = subprocess.run([
            gmtool_path, "list", "templates"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("📋 Доступные устройства:")
            print(result.stdout)
            
            # Создаем устройство (используем первое доступное)
            create_result = subprocess.run([
                gmtool_path, "create", "MedicalNotes_Device", "Google Nexus 5 - 6.0.0 - API 23 - 1080x1920"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if create_result.returncode == 0:
                print("✅ Устройство создано успешно")
                return True
            else:
                print(f"❌ Ошибка создания устройства: {create_result.stderr}")
                return False
        else:
            print(f"❌ Ошибка получения списка устройств: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка создания устройства: {e}")
        return False

def start_device(gmtool_path, device_name="MedicalNotes_Device"):
    """Запуск устройства"""
    try:
        print(f"🚀 Запускаем устройство: {device_name}")
        
        result = subprocess.run([
            gmtool_path, "start", device_name
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ Устройство запущено")
            return True
        else:
            print(f"❌ Ошибка запуска устройства: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка запуска устройства: {e}")
        return False

def create_launcher():
    """Создание запускатора"""
    try:
        print("📋 Создаем запускатор...")
        
        launcher_content = """@echo off
echo Запуск MedicalNotes на Genymotion с QEMU...
python setup_qemu.py
pause
"""
        
        launcher_path = Path("Запустить_Genymotion_QEMU.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Запускатор создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания запускатора: {e}")
        return False

def main():
    print("🔧 Genymotion QEMU Setup")
    print("=" * 30)
    
    # Ищем Genymotion
    genymotion_path = find_genymotion()
    if not genymotion_path:
        return False
    
    # Ищем gmtool
    gmtool_path = find_gmtool()
    if not gmtool_path:
        return False
    
    # Настраиваем QEMU
    if not configure_qemu(gmtool_path):
        return False
    
    # Создаем виртуальное устройство
    if not create_virtual_device(gmtool_path):
        return False
    
    # Запускаем устройство
    if not start_device(gmtool_path):
        return False
    
    # Создаем запускатор
    create_launcher()
    
    print("\n🎉 Genymotion настроен с QEMU!")
    print("💡 Теперь можете использовать:")
    print("   python fast_emulator.py")
    print("   или")
    print("   Запустить_Genymotion_QEMU.bat")
    
    return True

if __name__ == "__main__":
    main() 