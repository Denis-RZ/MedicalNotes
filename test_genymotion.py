#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестирование Genymotion без VirtualBox
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def test_genymotion():
    """Тестирование Genymotion"""
    try:
        print("🧪 Тестируем Genymotion...")
        
        genymotion_path = "C:/Program Files/Genymobile/Genymotion/genymotion.exe"
        
        if not os.path.exists(genymotion_path):
            print("❌ Genymotion не найден")
            return False
        
        print("🚀 Запускаем Genymotion...")
        
        # Запускаем Genymotion
        process = subprocess.Popen([genymotion_path], 
                                 stdout=subprocess.PIPE, 
                                 stderr=subprocess.PIPE,
                                 text=True)
        
        # Ждем немного
        time.sleep(5)
        
        # Проверяем, запустился ли процесс
        if process.poll() is None:
            print("✅ Genymotion запущен успешно")
            print("💡 Если появится ошибка VirtualBox, закройте Genymotion")
            return True
        else:
            stdout, stderr = process.communicate()
            print(f"❌ Genymotion не запустился")
            print(f"Ошибка: {stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка тестирования: {e}")
        return False

def check_virtualbox_error():
    """Проверка ошибок VirtualBox"""
    print("\n🔍 Проверяем возможные проблемы...")
    
    # Проверяем VirtualBox
    virtualbox_paths = [
        "C:/Program Files/Oracle/VirtualBox/VBoxManage.exe",
        "C:/Program Files (x86)/Oracle/VirtualBox/VBoxManage.exe"
    ]
    
    virtualbox_found = False
    for path in virtualbox_paths:
        if os.path.exists(path):
            virtualbox_found = True
            print(f"✅ VirtualBox найден: {path}")
            break
    
    if not virtualbox_found:
        print("❌ VirtualBox не найден")
        print("💡 Genymotion требует VirtualBox")
        print("💡 Но можно попробовать запустить без него")
    
    # Проверяем виртуализацию
    try:
        result = subprocess.run([
            "wmic", "cpu", "get", "VirtualizationFirmwareEnabled"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if "TRUE" in result.stdout:
            print("✅ Виртуализация включена в BIOS")
        else:
            print("❌ Виртуализация отключена в BIOS")
            print("💡 Это может быть проблемой для VirtualBox")
    except:
        print("⚠️ Не удалось проверить виртуализацию")

def main():
    print("🧪 Genymotion Tester")
    print("=" * 30)
    
    # Проверяем проблемы
    check_virtualbox_error()
    
    # Тестируем Genymotion
    if test_genymotion():
        print("\n🎉 Genymotion работает!")
        print("💡 Теперь можете использовать:")
        print("   python fast_emulator.py")
    else:
        print("\n❌ Genymotion не работает")
        print("💡 Попробуйте:")
        print("   1. Установить VirtualBox")
        print("   2. Включить виртуализацию в BIOS")
        print("   3. Или использовать другой эмулятор")

if __name__ == "__main__":
    main() 