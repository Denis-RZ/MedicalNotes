#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Универсальный скрипт для запуска MedicalNotes
Включает сборку, запуск эмулятора и установку приложения
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
        self.adb_path = None
        self.device_id = None
        
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
    
    def build_project(self):
        """Сборка проекта"""
        try:
            print("🔨 Собираем проект...")
            
            # Очищаем проект
            result = subprocess.run(["./gradlew.bat", "clean"], 
                                  cwd=self.project_dir, capture_output=True, text=True)
            
            if result.returncode != 0:
                print(f"❌ Ошибка очистки: {result.stderr}")
                return False
            
            # Собираем Debug APK
            result = subprocess.run(["./gradlew.bat", "assembleDebug"], 
                                  cwd=self.project_dir, capture_output=True, text=True)
            
            if result.returncode != 0:
                print(f"❌ Ошибка сборки: {result.stderr}")
                return False
            
            print("✅ Проект собран успешно")
            return True
            
        except Exception as e:
            print(f"❌ Ошибка сборки: {e}")
            return False
    
    def check_devices(self):
        """Проверка подключенных устройств"""
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
            
            if not devices:
                print("❌ Нет подключенных устройств")
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
    
    def install_and_launch(self):
        """Установка и запуск приложения"""
        try:
            apk_path = self.project_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
            
            if not apk_path.exists():
                print("❌ APK файл не найден")
                return False
            
            print("📱 Устанавливаем MedicalNotes...")
            
            # Удаляем старую версию
            subprocess.run([self.adb_path, "-s", self.device_id, "uninstall", "com.medicalnotes.app"], 
                         capture_output=True)
            
            # Устанавливаем новую версию
            result = subprocess.run([self.adb_path, "-s", self.device_id, "install", "-r", str(apk_path)], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode != 0 or "Success" not in result.stdout:
                print(f"❌ Ошибка установки: {result.stderr}")
                return False
            
            print("✅ Приложение установлено")
            
            # Запускаем приложение
            print("🚀 Запускаем MedicalNotes...")
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
            print(f"❌ Ошибка установки/запуска: {e}")
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
        print("🏥 MedicalNotes Universal Launcher")
        print("=" * 50)
        
        # Проверяем ADB
        if not self.find_adb():
            print("💡 Установите Android SDK или добавьте ADB в PATH")
            return False
        
        # Собираем проект
        if not self.build_project():
            return False
        
        # Проверяем устройства
        if not self.check_devices():
            print("💡 Подключите устройство или запустите эмулятор")
            print("💡 Для запуска эмулятора используйте: python start_emulator.py")
            return False
        
        # Устанавливаем и запускаем приложение
        if not self.install_and_launch():
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