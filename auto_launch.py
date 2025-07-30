#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Автоматический запуск эмулятора с приложением MedicalNotes
"""

import os
import sys
import subprocess
import time
import json
from pathlib import Path

class AutoLauncher:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.adb_path = None
        self.genymotion_path = None
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
    
    def find_genymotion(self):
        """Поиск Genymotion"""
        possible_paths = [
            "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
            "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe",
            os.path.expanduser("~/AppData/Local/Genymobile/Genymotion/genymotion.exe")
        ]
        
        for path in possible_paths:
            if os.path.exists(path):
                self.genymotion_path = path
                print(f"✅ Genymotion найден: {path}")
                return True
                
        print("❌ Genymotion не найден")
        return False
    
    def find_apk(self):
        """Поиск APK"""
        possible_paths = [
            self.project_dir / "MedicalNotes-v2.2-fixed.apk",
            self.project_dir / "MedicalNotes-v2.1-update.apk",
            self.project_dir / "MedicalNotes-with-alarm-sound.apk",
            self.project_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
        ]
        
        for path in possible_paths:
            if path.exists():
                print(f"✅ APK найден: {path}")
                return path
                
        print("❌ APK не найден")
        return None
    
    def start_genymotion(self):
        """Запуск Genymotion"""
        try:
            print("🚀 Запускаем Genymotion...")
            subprocess.Popen([self.genymotion_path])
            print("✅ Genymotion запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска Genymotion: {e}")
            return False
    
    def wait_for_device(self, timeout=300):
        """Ожидание подключения устройства"""
        print("⏳ Ожидаем подключения устройства...")
        print("💡 Убедитесь, что виртуальное устройство запущено в Genymotion")
        
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
    
    def wait_for_boot_complete(self, timeout=120):
        """Ожидание полной загрузки Android"""
        print("⏳ Ожидаем полной загрузки Android...")
        
        for i in range(timeout):
            try:
                result = subprocess.run([
                    self.adb_path, "-s", self.device_id, "shell", 
                    "getprop", "sys.boot_completed"
                ], capture_output=True, text=True, encoding='utf-8')
                
                if result.stdout.strip() == "1":
                    print("✅ Android полностью загружен")
                    return True
                
                print(f"⏳ Загрузка... ({i+1}/{timeout})")
                time.sleep(1)
                
            except Exception as e:
                print(f"❌ Ошибка проверки загрузки: {e}")
                time.sleep(1)
        
        print("❌ Android не загрузился полностью")
        return False
    
    def install_app(self, apk_path):
        """Установка приложения"""
        try:
            print(f"📱 Устанавливаем приложение...")
            
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
    
    def set_auto_start(self):
        """Настройка автозапуска приложения"""
        try:
            print("🔧 Настраиваем автозапуск...")
            
            # Создаем скрипт автозапуска на устройстве
            auto_start_script = f"""
#!/system/bin/sh
# Автозапуск MedicalNotes
sleep 10
am start -n com.medicalnotes.app/.MainActivity
"""
            
            # Записываем скрипт во временный файл
            script_path = self.project_dir / "auto_start.sh"
            with open(script_path, "w") as f:
                f.write(auto_start_script)
            
            # Копируем скрипт на устройство
            result = subprocess.run([
                self.adb_path, "-s", self.device_id, "push", 
                str(script_path), "/data/local/tmp/auto_start.sh"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                # Делаем скрипт исполняемым
                subprocess.run([
                    self.adb_path, "-s", self.device_id, "shell", 
                    "chmod", "755", "/data/local/tmp/auto_start.sh"
                ])
                
                print("✅ Автозапуск настроен")
                
                # Удаляем временный файл
                script_path.unlink()
                return True
            else:
                print(f"❌ Ошибка настройки автозапуска: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка настройки автозапуска: {e}")
            return False
    
    def create_shortcut(self):
        """Создание ярлыка для быстрого запуска"""
        try:
            print("📋 Создаем ярлык для быстрого запуска...")
            
            shortcut_content = f"""@echo off
cd /d "{self.project_dir}"
python auto_launch.py
pause
"""
            
            shortcut_path = self.project_dir / "Запустить_MedicalNotes.bat"
            with open(shortcut_path, "w", encoding="utf-8") as f:
                f.write(shortcut_content)
            
            print(f"✅ Ярлык создан: {shortcut_path}")
            return True
            
        except Exception as e:
            print(f"❌ Ошибка создания ярлыка: {e}")
            return False
    
    def run(self):
        """Основной метод"""
        print("🏥 Auto MedicalNotes Launcher")
        print("=" * 40)
        
        if not self.find_adb():
            return False
        
        if not self.find_genymotion():
            print("❌ Genymotion не найден. Установите его сначала.")
            return False
        
        apk_path = self.find_apk()
        if not apk_path:
            print("💡 Сначала соберите проект или скачайте APK")
            return False
        
        # Запускаем Genymotion
        if not self.start_genymotion():
            return False
        
        print("\n📋 Инструкции:")
        print("1. В открывшемся Genymotion запустите виртуальное устройство")
        print("2. Дождитесь полной загрузки Android")
        print("3. Скрипт автоматически установит и запустит приложение")
        
        # Ждем подключения устройства
        if not self.wait_for_device():
            return False
        
        # Ждем полной загрузки Android
        if not self.wait_for_boot_complete():
            return False
        
        # Устанавливаем приложение
        if not self.install_app(apk_path):
            return False
        
        # Запускаем приложение
        if not self.launch_app():
            return False
        
        # Настраиваем автозапуск
        self.set_auto_start()
        
        # Создаем ярлык
        self.create_shortcut()
        
        print("\n🎉 Готово! Приложение запущено автоматически!")
        print("💡 Для быстрого запуска используйте: Запустить_MedicalNotes.bat")
        print("💡 Для просмотра логов: adb logcat")
        
        return True

def main():
    launcher = AutoLauncher()
    launcher.run()

if __name__ == "__main__":
    main() 