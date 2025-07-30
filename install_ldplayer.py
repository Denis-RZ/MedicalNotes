#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Установка LDPlayer - быстрого эмулятора
"""

import os
import sys
import subprocess
import time
import urllib.request
from pathlib import Path

def download_ldplayer():
    """Скачивание LDPlayer"""
    try:
        print("📥 Скачиваем LDPlayer...")
        
        # Скачиваем LDPlayer
        url = "https://en.ldplayer.net/dl/en/LDPlayer9.exe"
        filename = "LDPlayer-Installer.exe"
        
        print(f"Скачиваем {filename}...")
        urllib.request.urlretrieve(url, filename)
        
        print(f"✅ {filename} скачан успешно")
        return filename
        
    except Exception as e:
        print(f"❌ Ошибка скачивания: {e}")
        return None

def install_ldplayer(installer_path):
    """Установка LDPlayer"""
    try:
        print("🔧 Устанавливаем LDPlayer...")
        
        # Запускаем установщик
        result = subprocess.run([
            installer_path, "/S"  # Тихая установка
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ LDPlayer установлен успешно")
            return True
        else:
            print(f"❌ Ошибка установки: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка установки: {e}")
        return False

def find_ldplayer():
    """Поиск LDPlayer"""
    ldplayer_paths = [
        "C:/Program Files/LDPlayer/LDPlayer9/ldconsole.exe",
        "C:/Program Files (x86)/LDPlayer/LDPlayer9/ldconsole.exe",
        "C:/Program Files/LDPlayer/LDPlayer4.0/ldconsole.exe",
        "C:/Program Files (x86)/LDPlayer/LDPlayer4.0/ldconsole.exe"
    ]
    
    for path in ldplayer_paths:
        if os.path.exists(path):
            print(f"✅ LDPlayer найден: {path}")
            return path
    
    print("❌ LDPlayer не найден")
    return None

def create_ldplayer_launcher():
    """Создание запускатора для LDPlayer"""
    try:
        print("📋 Создаем запускатор...")
        
        launcher_content = """@echo off
echo Запуск MedicalNotes на LDPlayer...
python ldplayer_launcher.py
pause
"""
        
        launcher_path = Path("Запустить_LDPlayer.bat")
        with open(launcher_path, "w", encoding="utf-8") as f:
            f.write(launcher_content)
        
        print(f"✅ Запускатор создан: {launcher_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания запускатора: {e}")
        return False

def create_ldplayer_script():
    """Создание скрипта для запуска LDPlayer"""
    try:
        print("📋 Создаем скрипт запуска...")
        
        script_content = '''#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Запуск MedicalNotes на LDPlayer
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def find_adb():
    """Поиск ADB"""
    possible_paths = [
        os.path.expanduser("~/AppData/Local/Android/Sdk/platform-tools/adb.exe"),
        "C:/Users/%USERNAME%/AppData/Local/Android/Sdk/platform-tools/adb.exe",
        "C:/Android/Sdk/platform-tools/adb.exe",
        "adb.exe"
    ]
    
    for path in possible_paths:
        path = os.path.expandvars(path)
        if os.path.exists(path):
            return path
    return None

def find_ldplayer():
    """Поиск LDPlayer"""
    ldplayer_paths = [
        "C:/Program Files/LDPlayer/LDPlayer9/ldconsole.exe",
        "C:/Program Files (x86)/LDPlayer/LDPlayer9/ldconsole.exe"
    ]
    
    for path in ldplayer_paths:
        if os.path.exists(path):
            return path
    return None

def start_ldplayer(ldplayer_path):
    """Запуск LDPlayer"""
    try:
        print("🚀 Запускаем LDPlayer...")
        
        # Запускаем LDPlayer
        result = subprocess.run([
            ldplayer_path, "launch", "--index", "0"
        ], capture_output=True, text=True, encoding='utf-8')
        
        if result.returncode == 0:
            print("✅ LDPlayer запущен")
            return True
        else:
            print(f"❌ Ошибка запуска: {result.stderr}")
            return False
            
    except Exception as e:
        print(f"❌ Ошибка запуска: {e}")
        return False

def wait_for_device(adb_path, timeout=180):
    """Ожидание подключения устройства"""
    print("⏳ Ожидаем подключения устройства...")
    
    for i in range(timeout):
        try:
            result = subprocess.run([adb_path, "devices"], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            lines = result.stdout.strip().split('\\n')[1:]
            devices = []
            
            for line in lines:
                if line.strip() and '\\t' in line:
                    device_id, status = line.split('\\t')
                    if status == 'device':
                        devices.append(device_id)
            
            if devices:
                print(f"✅ Устройство подключено: {devices[0]}")
                return devices[0]
            
            print(f"⏳ Ожидание... ({i+1}/{timeout})")
            time.sleep(2)
            
        except Exception as e:
            print(f"❌ Ошибка проверки: {e}")
            time.sleep(2)
    
    print("❌ Устройство не подключилось")
    return None

def install_and_run_app(adb_path, device_id):
    """Установка и запуск приложения"""
    try:
        # Ищем APK
        apk_paths = [
            Path("MedicalNotes-v2.2-fixed.apk"),
            Path("MedicalNotes-v2.1-update.apk"),
            Path("MedicalNotes-with-alarm-sound.apk")
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
            adb_path, "-s", device_id, "install", "-r", str(apk_path)
        ], capture_output=True, text=True, encoding='utf-8')
        
        if "Success" in result.stdout:
            print("✅ Приложение установлено")
            
            # Запускаем приложение
            print("🚀 Запускаем приложение...")
            result = subprocess.run([
                adb_path, "-s", device_id, "shell", 
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

def main():
    print("🚀 LDPlayer Launcher")
    print("=" * 25)
    
    # Ищем ADB
    adb_path = find_adb()
    if not adb_path:
        print("❌ ADB не найден")
        return False
    
    print(f"✅ ADB найден: {adb_path}")
    
    # Ищем LDPlayer
    ldplayer_path = find_ldplayer()
    if not ldplayer_path:
        print("❌ LDPlayer не найден")
        return False
    
    print(f"✅ LDPlayer найден: {ldplayer_path}")
    
    # Запускаем LDPlayer
    if not start_ldplayer(ldplayer_path):
        return False
    
    # Ждем подключения устройства
    device_id = wait_for_device(adb_path)
    if not device_id:
        print("❌ Не удалось подключить устройство")
        return False
    
    # Устанавливаем и запускаем приложение
    if install_and_run_app(adb_path, device_id):
        print("\\n🎉 MedicalNotes запущен на LDPlayer!")
        return True
    else:
        print("❌ Не удалось запустить приложение")
        return False

if __name__ == "__main__":
    main()
'''
        
        script_path = Path("ldplayer_launcher.py")
        with open(script_path, "w", encoding="utf-8") as f:
            f.write(script_content)
        
        print(f"✅ Скрипт создан: {script_path}")
        return True
        
    except Exception as e:
        print(f"❌ Ошибка создания скрипта: {e}")
        return False

def main():
    print("🔧 LDPlayer Installer")
    print("=" * 30)
    
    # Проверяем, установлен ли уже LDPlayer
    if find_ldplayer():
        print("✅ LDPlayer уже установлен")
        create_ldplayer_launcher()
        create_ldplayer_script()
        
        print("\n🎉 LDPlayer готов к использованию!")
        print("💡 Для запуска используйте:")
        print("   python ldplayer_launcher.py")
        print("   или")
        print("   Запустить_LDPlayer.bat")
        
        return True
    
    # Скачиваем LDPlayer
    installer_path = download_ldplayer()
    if not installer_path:
        return False
    
    # Устанавливаем LDPlayer
    if not install_ldplayer(installer_path):
        return False
    
    # Создаем запускаторы
    create_ldplayer_launcher()
    create_ldplayer_script()
    
    print("\n🎉 LDPlayer установлен!")
    print("💡 Для запуска используйте:")
    print("   python ldplayer_launcher.py")
    print("   или")
    print("   Запустить_LDPlayer.bat")
    
    # Удаляем установщик
    try:
        os.remove(installer_path)
        print(f"✅ Установщик удален: {installer_path}")
    except:
        pass
    
    return True

if __name__ == "__main__":
    main() 