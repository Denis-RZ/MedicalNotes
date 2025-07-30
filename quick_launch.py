#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Быстрый запуск MedicalNotes на любом доступном устройстве
"""

import os
import sys
import subprocess
import time
from pathlib import Path

class QuickLauncher:
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
    
    def find_apk(self):
        """Поиск APK файла"""
        possible_paths = [
            self.project_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk",
            self.project_dir / "MedicalNotes-v2.2-fixed.apk",
            self.project_dir / "MedicalNotes-v2.1-update.apk",
            self.project_dir / "MedicalNotes-with-alarm-sound.apk"
        ]
        
        for path in possible_paths:
            if path.exists():
                print(f"✅ APK найден: {path}")
                return path
                
        print("❌ APK файл не найден")
        return None
    
    def check_devices(self):
        """Проверка устройств"""
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
    
    def install_app(self, apk_path):
        """Установка приложения"""
        try:
            print(f"📱 Устанавливаем приложение на {self.device_id}...")
            
            result = subprocess.run([
                self.adb_path, "-s", self.device_id, "install", "-r", str(apk_path)
            ], capture_output=True, text=True, encoding='utf-8')
            
            if "Success" in result.stdout:
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
            print("🚀 Запускаем приложение...")
            
            # Запускаем главную активность
            result = subprocess.run([
                self.adb_path, "-s", self.device_id, "shell", 
                "am", "start", "-n", "com.medicalnotes.app/.MainActivity"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ Приложение запущено")
                return True
            else:
                print(f"❌ Ошибка запуска: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка запуска: {e}")
            return False
    
    def show_logs(self):
        """Показать логи приложения"""
        try:
            print("📋 Логи приложения:")
            print("-" * 50)
            
            result = subprocess.run([
                self.adb_path, "-s", self.device_id, "logcat", 
                "-s", "MedicalNotes:*", "AndroidRuntime:E"
            ], capture_output=True, text=True, encoding='utf-8', timeout=10)
            
            if result.stdout:
                print(result.stdout)
            else:
                print("Логи не найдены")
                
        except subprocess.TimeoutExpired:
            print("⏰ Таймаут получения логов")
        except Exception as e:
            print(f"❌ Ошибка получения логов: {e}")
    
    def run(self):
        """Основной метод"""
        print("🏥 Quick MedicalNotes Launcher")
        print("=" * 40)
        
        if not self.find_adb():
            return False
        
        apk_path = self.find_apk()
        if not apk_path:
            print("💡 Сначала соберите проект или скачайте APK")
            return False
        
        if not self.check_devices():
            return False
        
        if not self.install_app(apk_path):
            return False
        
        if not self.launch_app():
            return False
        
        print("✅ Приложение успешно запущено!")
        
        # Показываем логи
        show_logs = input("Показать логи? (y/n): ")
        if show_logs.lower() == 'y':
            self.show_logs()
        
        return True

def main():
    launcher = QuickLauncher()
    launcher.run()

if __name__ == "__main__":
    main() 