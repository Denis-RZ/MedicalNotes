#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Быстрые эмуляторы без VirtualBox
BlueStacks и NoxPlayer - легкие и быстрые
"""

import os
import sys
import subprocess
import time
import urllib.request
from pathlib import Path

class FastEmulatorLauncher:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.adb_path = None
        self.device_id = None
        
    def find_adb(self):
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
                self.adb_path = path
                print(f"✅ ADB найден: {path}")
                return True
                
        print("❌ ADB не найден")
        return False
    
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
    
    def download_bluestacks(self):
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
    
    def download_noxplayer(self):
        """Скачивание NoxPlayer"""
        try:
            print("📥 Скачиваем NoxPlayer...")
            
            # Скачиваем NoxPlayer
            url = "https://res06.bignox.com/full/20231201/NoxInstaller_7.0.5.9_full.exe"
            filename = "NoxPlayer-Installer.exe"
            
            print(f"Скачиваем {filename}...")
            urllib.request.urlretrieve(url, filename)
            
            print(f"✅ {filename} скачан успешно")
            return filename
            
        except Exception as e:
            print(f"❌ Ошибка скачивания: {e}")
            return None
    
    def start_bluestacks(self):
        """Запуск BlueStacks"""
        bluestacks_path = self.find_bluestacks()
        if not bluestacks_path:
            print("❌ BlueStacks не найден")
            return False
        
        try:
            print("🚀 Запускаем BlueStacks...")
            subprocess.Popen([bluestacks_path])
            print("✅ BlueStacks запущен")
            print("💡 Дождитесь полной загрузки BlueStacks")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска BlueStacks: {e}")
            return False
    
    def start_noxplayer(self):
        """Запуск NoxPlayer"""
        nox_path = self.find_noxplayer()
        if not nox_path:
            print("❌ NoxPlayer не найден")
            return False
        
        try:
            print("🚀 Запускаем NoxPlayer...")
            subprocess.Popen([nox_path])
            print("✅ NoxPlayer запущен")
            print("💡 Дождитесь полной загрузки NoxPlayer")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска NoxPlayer: {e}")
            return False
    
    def wait_for_device(self, timeout=180):
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
                    self.device_id = devices[0]
                    print(f"✅ Устройство подключено: {self.device_id}")
                    return True
                
                print(f"⏳ Ожидание... ({i+1}/{timeout})")
                time.sleep(1)
                
            except Exception as e:
                print(f"❌ Ошибка проверки устройств: {e}")
                time.sleep(1)
        
        print("❌ Устройство не подключилось")
        return False
    
    def install_and_run_app(self):
        """Установка и запуск приложения"""
        try:
            # Ищем APK
            apk_paths = [
                self.project_dir / "MedicalNotes-v2.2-fixed.apk",
                self.project_dir / "MedicalNotes-v2.1-update.apk",
                self.project_dir / "MedicalNotes-with-alarm-sound.apk"
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
                self.adb_path, "-s", self.device_id, "install", "-r", str(apk_path)
            ], capture_output=True, text=True, encoding='utf-8')
            
            if "Success" in result.stdout:
                print("✅ Приложение установлено")
                
                # Запускаем приложение
                print("🚀 Запускаем приложение...")
                result = subprocess.run([
                    self.adb_path, "-s", self.device_id, "shell", 
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
    
    def create_quick_launcher(self, emulator_name):
        """Создание быстрого запуска"""
        try:
            print("📋 Создаем быстрый запуск...")
            
            launcher_content = f"""@echo off
echo Запуск MedicalNotes на {emulator_name}...
python fast_emulator.py
pause
"""
            
            launcher_path = self.project_dir / f"Запустить_{emulator_name}.bat"
            with open(launcher_path, "w", encoding="utf-8") as f:
                f.write(launcher_content)
            
            print(f"✅ Быстрый запуск создан: {launcher_path}")
            return True
            
        except Exception as e:
            print(f"❌ Ошибка создания запуска: {e}")
            return False
    
    def run(self):
        """Основной метод"""
        print("🚀 Fast Emulator Launcher")
        print("=" * 30)
        
        if not self.find_adb():
            return False
        
        # Проверяем доступные эмуляторы
        bluestacks = self.find_bluestacks()
        noxplayer = self.find_noxplayer()
        
        if not bluestacks and not noxplayer:
            print("❌ Не найдено ни одного быстрого эмулятора")
            print("💡 Установите один из эмуляторов:")
            print("   1. BlueStacks (рекомендуется)")
            print("   2. NoxPlayer")
            
            choice = input("Выберите эмулятор для установки (1-2): ")
            
            if choice == "1":
                installer_path = self.download_bluestacks()
                if installer_path:
                    print(f"✅ BlueStacks скачан: {installer_path}")
                    print("💡 Запустите установщик и повторите")
            elif choice == "2":
                installer_path = self.download_noxplayer()
                if installer_path:
                    print(f"✅ NoxPlayer скачан: {installer_path}")
                    print("💡 Запустите установщик и повторите")
            
            return False
        
        # Выбираем эмулятор
        print("\nДоступные эмуляторы:")
        if bluestacks:
            print("1. BlueStacks")
        if noxplayer:
            print("2. NoxPlayer")
        
        try:
            choice = input("Выберите эмулятор (номер): ")
            
            if choice == "1" and bluestacks:
                self.start_bluestacks()
                emulator_name = "BlueStacks"
            elif choice == "2" and noxplayer:
                self.start_noxplayer()
                emulator_name = "NoxPlayer"
            else:
                print("❌ Неверный выбор")
                return False
            
            # Ждем подключения устройства
            if self.wait_for_device():
                # Устанавливаем и запускаем приложение
                if self.install_and_run_app():
                    # Создаем быстрый запуск
                    self.create_quick_launcher(emulator_name)
                    
                    print(f"\n🎉 {emulator_name} готов к использованию!")
                    print("💡 Приложение запущено автоматически")
                    print(f"💡 Для быстрого запуска используйте: Запустить_{emulator_name}.bat")
                    
                    return True
                else:
                    print("❌ Не удалось запустить приложение")
                    return False
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
    launcher = FastEmulatorLauncher()
    launcher.run()

if __name__ == "__main__":
    main() 