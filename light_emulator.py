#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для запуска легких Android эмуляторов
Поддерживает Genymotion, BlueStacks, NoxPlayer
"""

import os
import sys
import subprocess
import time
import json
from pathlib import Path

class LightEmulatorLauncher:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.adb_path = None
        
    def find_adb(self):
        """Поиск ADB в системе"""
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
                self.adb_path = path
                print(f"✅ ADB найден: {path}")
                return True
                
        print("❌ ADB не найден")
        return False
    
    def find_genymotion(self):
        """Поиск Genymotion"""
        possible_paths = [
            "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
            "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe",
            os.path.expanduser("~/AppData/Local/Genymobile/Genymotion/genymotion.exe")
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"✅ Genymotion найден: {path}")
                return path
                
        print("❌ Genymotion не найден")
        return None
    
    def find_bluestacks(self):
        """Поиск BlueStacks"""
        possible_paths = [
            "C:/Program Files/BlueStacks_nxt/HD-Player.exe",
            "C:/Program Files (x86)/BlueStacks_nxt/HD-Player.exe",
            "C:/Program Files/BlueStacks/HD-Player.exe",
            "C:/Program Files (x86)/BlueStacks/HD-Player.exe"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"✅ BlueStacks найден: {path}")
                return path
                
        print("❌ BlueStacks не найден")
        return None
    
    def find_noxplayer(self):
        """Поиск NoxPlayer"""
        possible_paths = [
            "C:/Program Files/Nox/bin/Nox.exe",
            "C:/Program Files (x86)/Nox/bin/Nox.exe"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                print(f"✅ NoxPlayer найден: {path}")
                return path
                
        print("❌ NoxPlayer не найден")
        return None
    
    def install_light_emulator(self):
        """Установка легкого эмулятора"""
        print("📱 Установка легкого эмулятора...")
        print("Доступные варианты:")
        print("1. Genymotion (рекомендуется)")
        print("2. BlueStacks")
        print("3. NoxPlayer")
        
        try:
            choice = input("Выберите эмулятор (1-3): ")
            
            if choice == "1":
                print("📥 Скачиваем Genymotion...")
                # Скачиваем Genymotion
                url = "https://dl.genymotion.com/releases/genymotion-3.5.0/genymotion-3.5.0.exe"
                self.download_file(url, "genymotion-installer.exe")
                print("✅ Genymotion скачан. Запустите установщик.")
                
            elif choice == "2":
                print("📥 Скачиваем BlueStacks...")
                url = "https://cdn3.bluestacks.com/downloads/windows/nxt/5.10.0.1082/5.10.0.1082_BlueStacksInstaller_5.10.0.1082_native.exe"
                self.download_file(url, "bluestacks-installer.exe")
                print("✅ BlueStacks скачан. Запустите установщик.")
                
            elif choice == "3":
                print("📥 Скачиваем NoxPlayer...")
                url = "https://res06.bignox.com/full/20231201/NoxInstaller_7.0.5.9_full.exe"
                self.download_file(url, "noxplayer-installer.exe")
                print("✅ NoxPlayer скачан. Запустите установщик.")
                
        except Exception as e:
            print(f"❌ Ошибка: {e}")
    
    def download_file(self, url, filename):
        """Скачивание файла"""
        try:
            import urllib.request
            print(f"Скачиваем {filename}...")
            urllib.request.urlretrieve(url, filename)
            print(f"✅ {filename} скачан успешно")
        except Exception as e:
            print(f"❌ Ошибка скачивания: {e}")
    
    def start_genymotion(self):
        """Запуск Genymotion"""
        genymotion_path = self.find_genymotion()
        if not genymotion_path:
            print("❌ Genymotion не найден. Установите его сначала.")
            return False
        
        try:
            print("🚀 Запускаем Genymotion...")
            subprocess.Popen([genymotion_path])
            print("✅ Genymotion запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска Genymotion: {e}")
            return False
    
    def start_bluestacks(self):
        """Запуск BlueStacks"""
        bluestacks_path = self.find_bluestacks()
        if not bluestacks_path:
            print("❌ BlueStacks не найден. Установите его сначала.")
            return False
        
        try:
            print("🚀 Запускаем BlueStacks...")
            subprocess.Popen([bluestacks_path])
            print("✅ BlueStacks запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска BlueStacks: {e}")
            return False
    
    def start_noxplayer(self):
        """Запуск NoxPlayer"""
        nox_path = self.find_noxplayer()
        if not nox_path:
            print("❌ NoxPlayer не найден. Установите его сначала.")
            return False
        
        try:
            print("🚀 Запускаем NoxPlayer...")
            subprocess.Popen([nox_path])
            print("✅ NoxPlayer запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска NoxPlayer: {e}")
            return False
    
    def wait_for_device(self, timeout=120):
        """Ожидание подключения устройства"""
        print("⏳ Ожидаем подключения устройства...")
        
        for i in range(timeout):
            try:
                result = subprocess.run([self.adb_path, "devices"], 
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
                time.sleep(1)
                
            except Exception as e:
                print(f"❌ Ошибка проверки устройств: {e}")
                time.sleep(1)
        
        print("❌ Устройство не подключилось")
        return None
    
    def run(self):
        """Основной метод запуска"""
        print("📱 Light Android Emulator Launcher")
        print("=" * 50)
        
        if not self.find_adb():
            return False
        
        # Проверяем доступные эмуляторы
        genymotion = self.find_genymotion()
        bluestacks = self.find_bluestacks()
        noxplayer = self.find_noxplayer()
        
        if not any([genymotion, bluestacks, noxplayer]):
            print("❌ Не найдено ни одного легкого эмулятора")
            print("💡 Установите один из эмуляторов:")
            print("   - Genymotion (рекомендуется)")
            print("   - BlueStacks")
            print("   - NoxPlayer")
            
            install = input("Хотите установить эмулятор? (y/n): ")
            if install.lower() == 'y':
                self.install_light_emulator()
            return False
        
        # Выбираем эмулятор
        print("\nДоступные эмуляторы:")
        if genymotion:
            print("1. Genymotion")
        if bluestacks:
            print("2. BlueStacks")
        if noxplayer:
            print("3. NoxPlayer")
        
        try:
            choice = input("Выберите эмулятор (номер): ")
            
            if choice == "1" and genymotion:
                self.start_genymotion()
            elif choice == "2" and bluestacks:
                self.start_bluestacks()
            elif choice == "3" and noxplayer:
                self.start_noxplayer()
            else:
                print("❌ Неверный выбор")
                return False
            
            # Ждем подключения устройства
            device_id = self.wait_for_device()
            if device_id:
                print("✅ Эмулятор готов к использованию!")
                return True
            else:
                print("❌ Не удалось подключить устройство")
                return False
                
        except KeyboardInterrupt:
            print("\n👋 Операция прервана пользователем")
            return False
        except Exception as e:
            print(f"❌ Ошибка: {e}")
            return False

def main():
    launcher = LightEmulatorLauncher()
    launcher.run()

if __name__ == "__main__":
    main() 