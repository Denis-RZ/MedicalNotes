#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Установка VirtualBox для Genymotion
"""

import os
import sys
import subprocess
import time
import urllib.request
from pathlib import Path

def find_virtualbox():
    """Поиск VirtualBox"""
    possible_paths = [
        "C:/Program Files/Oracle/VirtualBox/VBoxManage.exe",
        "C:/Program Files (x86)/Oracle/VirtualBox/VBoxManage.exe",
        "VBoxManage.exe"
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            print(f"✅ VirtualBox найден: {path}")
            return True
            
    print("❌ VirtualBox не найден")
    return False

def download_virtualbox():
    """Скачивание VirtualBox"""
    try:
        print("📥 Скачиваем VirtualBox...")
        
        # Скачиваем VirtualBox для Windows
        url = "https://download.virtualbox.org/virtualbox/7.0.18/VirtualBox-7.0.18-162988-Win.exe"
        filename = "VirtualBox-Installer.exe"
        
        print(f"Скачиваем {filename}...")
        urllib.request.urlretrieve(url, filename)
        
        print(f"✅ {filename} скачан успешно")
        return filename
        
    except Exception as e:
        print(f"❌ Ошибка скачивания: {e}")
        return None

def install_virtualbox(installer_path):
    """Установка VirtualBox"""
    try:
        print("🔧 Устанавливаем VirtualBox...")
        
        # Запускаем установщик
        result = subprocess.run([
            installer_path, "--silent", "--ignore-reboot"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ VirtualBox установлен успешно")
            return True
        else:
            print(f"❌ Ошибка установки: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка установки: {e}")
        return False

def enable_virtualization():
    """Включение виртуализации в BIOS"""
    print("🔧 Проверяем виртуализацию...")
    
    try:
        # Проверяем поддержку виртуализации
        result = subprocess.run([
            "wmic", "cpu", "get", "VirtualizationFirmwareEnabled"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if "TRUE" in result.stdout:
            print("✅ Виртуализация включена в BIOS")
            return True
        else:
            print("❌ Виртуализация отключена в BIOS")
            print("💡 Включите виртуализацию в настройках BIOS:")
            print("   1. Перезагрузите компьютер")
            print("   2. Войдите в BIOS (F2, F10, Del)")
            print("   3. Найдите 'Virtualization Technology' или 'Intel VT-x'")
            print("   4. Включите эту опцию")
            print("   5. Сохраните и перезагрузитесь")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка проверки виртуализации: {e}")
        return False

def configure_genymotion():
    """Настройка Genymotion"""
    try:
        print("🔧 Настраиваем Genymotion...")
        
        # Находим путь к Genymotion
        genymotion_paths = [
            "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
            "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe"
        ]
        
        genymotion_path = None
        for path in genymotion_paths:
            if os.path.exists(path):
                genymotion_path = path
                break
        
        if not genymotion_path:
            print("❌ Genymotion не найден")
            return False
        
        # Настраиваем Genymotion для использования VirtualBox
        print("📝 Настраиваем гипервизор...")
        
        # Создаем конфигурационный файл
        config_content = """[Genymotion]
hypervisor=VBox
"""
        
        config_path = Path.home() / ".Genymobile" / "Genymotion" / "genymotion.conf"
        config_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(config_path, "w") as f:
            f.write(config_content)
        
        print(f"✅ Конфигурация создана: {config_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка настройки Genymotion: {e}")
        return False

def create_launcher():
    """Создание запускатора"""
    try:
        print("📋 Создаем запускатор...")
        
        launcher_content = """@echo off
echo Установка VirtualBox и настройка Genymotion...
python install_virtualbox.py
pause
"""
        
        launcher_path = Path("Установить_VirtualBox.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Запускатор создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания запускатора: {e}")
        return False

def main():
    print("🔧 VirtualBox Installer для Genymotion")
    print("=" * 40)
    
    # Проверяем, установлен ли уже VirtualBox
    if find_virtualbox():
        print("✅ VirtualBox уже установлен")
        configure_genymotion()
        return True
    
    # Проверяем виртуализацию
    if not enable_virtualization():
        print("\n⚠️  Сначала включите виртуализацию в BIOS")
        return False
    
    # Скачиваем VirtualBox
    installer_path = download_virtualbox()
    if not installer_path:
        return False
    
    # Устанавливаем VirtualBox
    if not install_virtualbox(installer_path):
        return False
    
    # Настраиваем Genymotion
    configure_genymotion()
    
    # Создаем запускатор
    create_launcher()
    
    print("\n🎉 VirtualBox установлен и настроен!")
    print("💡 Теперь можете запускать Genymotion:")
    print("   python light_emulator.py")
    
    # Удаляем установщик
    try:
        os.remove(installer_path)
        print(f"✅ Установщик удален: {installer_path}")
    except:
        pass
    
    return True

if __name__ == "__main__":
    main() 