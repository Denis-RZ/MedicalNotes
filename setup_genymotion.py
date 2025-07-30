#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для настройки Genymotion
Создание виртуального устройства и настройка для Android разработки
"""

import os
import sys
import subprocess
import time
import json
from pathlib import Path

class GenymotionSetup:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.genymotion_path = None
        self.adb_path = None
        
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
    
    def start_genymotion(self):
        """Запуск Genymotion"""
        try:
            print("🚀 Запускаем Genymotion...")
            subprocess.Popen([self.genymotion_path])
            print("✅ Genymotion запущен")
            print("💡 В открывшемся окне Genymotion:")
            print("   1. Создайте аккаунт или войдите в существующий")
            print("   2. Нажмите 'Add' для создания нового устройства")
            print("   3. Выберите 'Google Pixel 2' с Android 9.0 (API 28)")
            print("   4. Назовите устройство 'MedicalNotes_Test'")
            print("   5. Нажмите 'Create'")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска Genymotion: {e}")
            return False
    
    def wait_for_device(self, timeout=180):
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
                    print(f"✅ Устройство подключено: {devices[0]}")
                    return devices[0]
                
                print(f"⏳ Ожидание... ({i+1}/{timeout})")
                time.sleep(1)
                
            except Exception as e:
                print(f"❌ Ошибка проверки устройств: {e}")
                time.sleep(1)
        
        print("❌ Устройство не подключилось")
        return None
    
    def enable_developer_options(self, device_id):
        """Включение режима разработчика"""
        try:
            print("🔧 Включаем режим разработчика...")
            
            # Включаем USB отладку
            result = subprocess.run([
                self.adb_path, "-s", device_id, "shell", 
                "settings", "put", "global", "adb_enabled", "1"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ Режим разработчика включен")
                return True
            else:
                print(f"❌ Ошибка включения режима разработчика: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка: {e}")
            return False
    
    def install_google_play(self, device_id):
        """Установка Google Play Services"""
        try:
            print("📱 Устанавливаем Google Play Services...")
            
            # Проверяем, есть ли уже Google Play
            result = subprocess.run([
                self.adb_path, "-s", device_id, "shell", 
                "pm", "list", "packages", "com.android.vending"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if "com.android.vending" in result.stdout:
                print("✅ Google Play уже установлен")
                return True
            
            print("💡 Google Play не найден. Установите его вручную:")
            print("   1. Откройте браузер в эмуляторе")
            print("   2. Скачайте Google Play Services APK")
            print("   3. Установите APK")
            
            return False
                
        except Exception as e:
            print(f"❌ Ошибка проверки Google Play: {e}")
            return False
    
    def test_device(self, device_id):
        """Тестирование устройства"""
        try:
            print("🧪 Тестируем устройство...")
            
            # Проверяем версию Android
            result = subprocess.run([
                self.adb_path, "-s", device_id, "shell", 
                "getprop", "ro.build.version.release"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                android_version = result.stdout.strip()
                print(f"✅ Android версия: {android_version}")
            
            # Проверяем доступное место
            result = subprocess.run([
                self.adb_path, "-s", device_id, "shell", 
                "df", "/data"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ Место на диске доступно")
            
            # Проверяем сеть
            result = subprocess.run([
                self.adb_path, "-s", device_id, "shell", 
                "ping", "-c", "1", "8.8.8.8"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ Сетевое подключение работает")
            
            return True
                
        except Exception as e:
            print(f"❌ Ошибка тестирования: {e}")
            return False
    
    def create_setup_guide(self):
        """Создание руководства по настройке"""
        guide = """
# Руководство по настройке Genymotion

## 1. Создание виртуального устройства

1. Откройте Genymotion
2. Нажмите "Add" (добавить)
3. Выберите устройство: **Google Pixel 2**
4. Выберите Android: **9.0 (API 28)**
5. Назовите устройство: **MedicalNotes_Test**
6. Нажмите "Create"

## 2. Настройка устройства

1. Запустите созданное устройство
2. Дождитесь полной загрузки Android
3. Настройте Wi-Fi подключение
4. Войдите в Google аккаунт (опционально)

## 3. Включение режима разработчика

1. Откройте Настройки
2. Перейдите в "О телефоне"
3. Нажмите 7 раз на "Номер сборки"
4. Вернитесь в Настройки → Для разработчиков
5. Включите "Отладка по USB"

## 4. Установка Google Play Services

1. Откройте браузер в эмуляторе
2. Скачайте Google Play Services APK
3. Установите APK
4. Перезагрузите устройство

## 5. Тестирование

После настройки запустите:
```
python quick_launch.py
```

## Полезные команды

- Проверить устройства: `adb devices`
- Установить APK: `adb install app.apk`
- Запустить приложение: `adb shell am start -n com.medicalnotes.app/.MainActivity`
- Просмотр логов: `adb logcat`
"""
        
        with open("GENYMOTION_SETUP_GUIDE.md", "w", encoding="utf-8") as f:
            f.write(guide)
        
        print("✅ Руководство по настройке создано: GENYMOTION_SETUP_GUIDE.md")
    
    def run(self):
        """Основной метод настройки"""
        print("🔧 Genymotion Setup")
        print("=" * 30)
        
        if not self.find_genymotion():
            print("❌ Genymotion не найден. Установите его сначала.")
            return False
        
        if not self.find_adb():
            print("❌ ADB не найден. Установите Android SDK.")
            return False
        
        # Запускаем Genymotion
        if not self.start_genymotion():
            return False
        
        # Создаем руководство
        self.create_setup_guide()
        
        print("\n📋 Инструкции:")
        print("1. В открывшемся Genymotion создайте виртуальное устройство")
        print("2. Запустите устройство и дождитесь загрузки")
        print("3. Включите режим разработчика")
        print("4. Установите Google Play Services (опционально)")
        print("5. После настройки запустите: python quick_launch.py")
        
        # Ждем подключения устройства
        device_id = self.wait_for_device()
        if device_id:
            print(f"✅ Устройство готово: {device_id}")
            
            # Тестируем устройство
            self.test_device(device_id)
            
            print("\n🎉 Genymotion настроен успешно!")
            print("Теперь можете запускать приложение:")
            print("python quick_launch.py")
            
            return True
        else:
            print("❌ Не удалось подключить устройство")
            print("💡 Проверьте, что устройство запущено в Genymotion")
            return False

def main():
    setup = GenymotionSetup()
    setup.run()

if __name__ == "__main__":
    main() 