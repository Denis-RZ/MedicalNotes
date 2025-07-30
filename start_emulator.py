#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для запуска Android эмулятора
"""

import os
import sys
import subprocess
import time
import json
from pathlib import Path

class EmulatorLauncher:
    def __init__(self):
        self.emulator_path = None
        self.avd_manager_path = None
        
    def find_android_tools(self):
        """Поиск инструментов Android SDK"""
        possible_sdk_paths = [
            os.path.expanduser("~/AppData/Local/Android/Sdk"),
            "C:/Users/%USERNAME%/AppData/Local/Android/Sdk",
            "C:/Android/Sdk",
            "C:/Program Files/Android/Android Studio/sdk"
        ]
        
        for sdk_path in possible_sdk_paths:
            sdk_path = os.path.expandvars(sdk_path)
            if os.path.exists(sdk_path):
                emulator_path = os.path.join(sdk_path, "emulator", "emulator.exe")
                avd_manager_path = os.path.join(sdk_path, "cmdline-tools", "latest", "bin", "avdmanager.bat")
                
                if os.path.exists(emulator_path):
                    self.emulator_path = emulator_path
                    print(f"✅ Эмулятор найден: {emulator_path}")
                
                if os.path.exists(avd_manager_path):
                    self.avd_manager_path = avd_manager_path
                    print(f"✅ AVD Manager найден: {avd_manager_path}")
                
                if self.emulator_path and self.avd_manager_path:
                    return True
        
        print("❌ Android SDK не найден")
        return False
    
    def list_avds(self):
        """Список доступных AVD (Android Virtual Devices)"""
        try:
            result = subprocess.run([self.avd_manager_path, "list", "avd"], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode != 0:
                print(f"❌ Ошибка получения списка AVD: {result.stderr}")
                return []
            
            avds = []
            lines = result.stdout.strip().split('\n')
            
            for line in lines:
                if line.strip() and "Name:" in line:
                    avd_name = line.split("Name:")[1].strip()
                    avds.append(avd_name)
            
            return avds
            
        except Exception as e:
            print(f"❌ Ошибка получения списка AVD: {e}")
            return []
    
    def create_avd(self):
        """Создание нового AVD"""
        try:
            print("📱 Создание нового AVD...")
            
            # Создаем AVD с API 26 (Android 8.0)
            result = subprocess.run([
                self.avd_manager_path, "create", "avd",
                "-n", "MedicalNotes_Test",
                "-k", "system-images;android-26;google_apis;x86_64",
                "-d", "pixel_2"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ AVD создан успешно")
                return "MedicalNotes_Test"
            else:
                print(f"❌ Ошибка создания AVD: {result.stderr}")
                return None
                
        except Exception as e:
            print(f"❌ Ошибка создания AVD: {e}")
            return None
    
    def start_emulator(self, avd_name):
        """Запуск эмулятора"""
        try:
            print(f"🚀 Запускаем эмулятор: {avd_name}")
            
            # Запускаем эмулятор в фоне
            process = subprocess.Popen([
                self.emulator_path, "-avd", avd_name,
                "-no-snapshot-load",  # Не загружать снапшот для быстрого запуска
                "-no-boot-anim",      # Отключить анимацию загрузки
                "-gpu", "swiftshader_indirect"  # Использовать программный рендеринг
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            
            print("⏳ Ожидаем запуска эмулятора...")
            
            # Ждем запуска эмулятора
            for i in range(60):  # Максимум 60 секунд
                time.sleep(1)
                
                # Проверяем, запустился ли эмулятор
                try:
                    result = subprocess.run(["adb", "devices"], 
                                          capture_output=True, text=True, encoding='utf-8')
                    
                    if "emulator" in result.stdout:
                        print("✅ Эмулятор запущен!")
                        return True
                        
                except:
                    pass
                
                if i % 10 == 0:
                    print(f"⏳ Ожидание... ({i+1}/60)")
            
            print("❌ Эмулятор не запустился за 60 секунд")
            process.terminate()
            return False
            
        except Exception as e:
            print(f"❌ Ошибка запуска эмулятора: {e}")
            return False
    
    def run(self):
        """Основной метод запуска"""
        print("📱 Android Emulator Launcher")
        print("=" * 40)
        
        # Находим инструменты Android
        if not self.find_android_tools():
            return False
        
        # Получаем список AVD
        avds = self.list_avds()
        
        if not avds:
            print("📱 AVD не найдены. Создаем новый...")
            avd_name = self.create_avd()
            if not avd_name:
                return False
            avds = [avd_name]
        
        # Выбираем AVD
        if len(avds) == 1:
            avd_name = avds[0]
            print(f"✅ Выбран AVD: {avd_name}")
        else:
            print("📱 Доступные AVD:")
            for i, avd in enumerate(avds, 1):
                print(f"  {i}. {avd}")
            
            try:
                choice = int(input("Выберите AVD (номер): ")) - 1
                if 0 <= choice < len(avds):
                    avd_name = avds[choice]
                else:
                    print("❌ Неверный номер")
                    return False
            except ValueError:
                print("❌ Введите число")
                return False
        
        # Запускаем эмулятор
        if self.start_emulator(avd_name):
            print("\n🎉 Эмулятор готов к использованию!")
            print("💡 Теперь можно запустить run_app.py для установки приложения")
            return True
        else:
            return False

def main():
    """Главная функция"""
    launcher = EmulatorLauncher()
    
    try:
        success = launcher.run()
        if not success:
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n👋 Операция прервана пользователем")
    except Exception as e:
        print(f"\n❌ Неожиданная ошибка: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 