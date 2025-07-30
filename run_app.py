#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для запуска Android приложения MedicalNotes на Windows
Поддерживает эмулятор и реальное устройство
"""

import os
import sys
import subprocess
import time
import json
from pathlib import Path

class MedicalNotesLauncher:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.apk_path = self.project_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
        self.adb_path = None
        self.device_id = None
        
    def find_adb(self):
        """Поиск ADB в системе"""
        possible_paths = [
            # Android SDK
            os.path.expanduser("~/AppData/Local/Android/Sdk/platform-tools/adb.exe"),
            "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe",
            "C:/Android/Sdk/platform-tools/adb.exe",
            # Android Studio
            "C:/Program Files/Android/Android Studio/sdk/platform-tools/adb.exe",
            # В PATH
            "adb.exe"
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                self.adb_path = path
                print(f"✅ ADB найден: {path}")
                return True
                
        print("❌ ADB не найден. Установите Android SDK или добавьте ADB в PATH")
        return False
    
    def check_devices(self):
        """Проверка подключенных устройств"""
        try:
            result = subprocess.run([self.adb_path, "devices"], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode != 0:
                print(f"❌ Ошибка выполнения ADB: {result.stderr}")
                return False
            
            lines = result.stdout.strip().split('\n')[1:]  # Пропускаем заголовок
            devices = []
            
            for line in lines:
                if line.strip() and '\t' in line:
                    device_id, status = line.split('\t')
                    if status == 'device':
                        devices.append(device_id)
            
            if not devices:
                print("❌ Нет подключенных устройств")
                print("💡 Подключите устройство или запустите эмулятор")
                return False
            
            if len(devices) == 1:
                self.device_id = devices[0]
                print(f"✅ Найдено устройство: {self.device_id}")
                return True
            else:
                print("📱 Найдено несколько устройств:")
                for i, device in enumerate(devices, 1):
                    print(f"  {i}. {device}")
                
                try:
                    choice = int(input("Выберите устройство (номер): ")) - 1
                    if 0 <= choice < len(devices):
                        self.device_id = devices[choice]
                        print(f"✅ Выбрано устройство: {self.device_id}")
                        return True
                    else:
                        print("❌ Неверный номер")
                        return False
                except ValueError:
                    print("❌ Введите число")
                    return False
                    
        except Exception as e:
            print(f"❌ Ошибка проверки устройств: {e}")
            return False
    
    def check_apk(self):
        """Проверка наличия APK файла"""
        if not self.apk_path.exists():
            print(f"❌ APK файл не найден: {self.apk_path}")
            print("💡 Сначала соберите проект: .\\gradlew.bat assembleDebug")
            return False
        
        print(f"✅ APK найден: {self.apk_path}")
        return True
    
    def install_app(self):
        """Установка приложения на устройство"""
        try:
            print("📱 Устанавливаем MedicalNotes...")
            
            # Сначала удаляем старую версию
            subprocess.run([self.adb_path, "-s", self.device_id, "uninstall", "com.medicalnotes.app"], 
                         capture_output=True)
            
            # Устанавливаем новую версию
            result = subprocess.run([self.adb_path, "-s", self.device_id, "install", "-r", str(self.apk_path)], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0 and "Success" in result.stdout:
                print("✅ Приложение установлено успешно")
                return True
            else:
                print(f"❌ Ошибка установки: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка установки: {e}")
            return False
    
    def launch_app(self):
        """Запуск приложения"""
        try:
            print("🚀 Запускаем MedicalNotes...")
            
            # Запускаем главную активность
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
                
        except Exception as e:
            print(f"❌ Ошибка запуска: {e}")
            return False
    
    def show_logs(self):
        """Показ логов приложения"""
        try:
            print("📋 Логи приложения (Ctrl+C для остановки):")
            print("-" * 50)
            
            process = subprocess.Popen([
                self.adb_path, "-s", self.device_id, "logcat", 
                "-s", "MedicalNotes:*", "MainActivity:*", "NotificationManager:*"
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            
            try:
                for line in process.stdout:
                    print(line.strip())
            except KeyboardInterrupt:
                print("\n⏹️ Остановка просмотра логов...")
                process.terminate()
                
        except Exception as e:
            print(f"❌ Ошибка просмотра логов: {e}")
    
    def run(self):
        """Основной метод запуска"""
        print("🏥 MedicalNotes Launcher")
        print("=" * 40)
        
        # Проверяем ADB
        if not self.find_adb():
            return False
        
        # Проверяем устройства
        if not self.check_devices():
            return False
        
        # Проверяем APK
        if not self.check_apk():
            return False
        
        # Устанавливаем приложение
        if not self.install_app():
            return False
        
        # Запускаем приложение
        if not self.launch_app():
            return False
        
        # Спрашиваем про логи
        try:
            show_logs = input("\n📋 Показать логи приложения? (y/n): ").lower().strip()
            if show_logs in ['y', 'yes', 'да', 'д']:
                self.show_logs()
        except KeyboardInterrupt:
            print("\n👋 До свидания!")
        
        return True

def main():
    """Главная функция"""
    launcher = MedicalNotesLauncher()
    
    try:
        success = launcher.run()
        if success:
            print("\n🎉 MedicalNotes успешно запущен!")
        else:
            print("\n❌ Не удалось запустить приложение")
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n👋 Операция прервана пользователем")
    except Exception as e:
        print(f"\n❌ Неожиданная ошибка: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 