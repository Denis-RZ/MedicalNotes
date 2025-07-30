#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Автоматическая установка BlueStacks
"""

import os
import sys
import subprocess
import time
import urllib.request
from pathlib import Path

def download_bluestacks():
    """Скачивание BlueStacks"""
    try:
        print("📥 Скачиваем BlueStacks...")
        
        # Скачиваем BlueStacks
        url = "https://cdn3.bluestacks.com/downloads/windows/nxt/5.10.0.1082/5.10.0.1082_BlueStacksInstaller_5.10.0.1082_native.exe"
        filename = "BlueStacks-Installer.exe"
        
        print(f"Скачиваем {filename}...")
        urllib.request.urlretrieve(url, filename)
        
        print(f"✅ {filename} скачан успешно")
        return filename
        
    except Exception as e:
        print(f"❌ Ошибка скачивания: {e}")
        return None

def install_bluestacks(installer_path):
    """Установка BlueStacks"""
    try:
        print("🔧 Устанавливаем BlueStacks...")
        
        # Запускаем установщик
        result = subprocess.run([
            installer_path, "/S"  # Тихая установка
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ BlueStacks установлен успешно")
            return True
        else:
            print(f"❌ Ошибка установки: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка установки: {e}")
        return False

def create_launcher():
    """Создание запускатора"""
    try:
        print("📋 Создаем запускатор...")
        
        launcher_content = """@echo off
echo Запуск MedicalNotes на BlueStacks...
python fast_emulator.py
pause
"""
        
        launcher_path = Path("Запустить_BlueStacks.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Запускатор создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания запускатора: {e}")
        return False

def main():
    print("🔧 BlueStacks Installer")
    print("=" * 30)
    
    # Скачиваем BlueStacks
    installer_path = download_bluestacks()
    if not installer_path:
        return False
    
    # Устанавливаем BlueStacks
    if not install_bluestacks(installer_path):
        return False
    
    # Создаем запускатор
    create_launcher()
    
    print("\n🎉 BlueStacks установлен!")
    print("💡 Теперь можете запускать:")
    print("   python fast_emulator.py")
    print("   или")
    print("   Запустить_BlueStacks.bat")
    
    # Удаляем установщик
    try:
        os.remove(installer_path)
        print(f"✅ Установщик удален: {installer_path}")
    except:
        pass
    
    return True

if __name__ == "__main__":
    main() 