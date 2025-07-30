#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Универсальный эмулятор - работает с любым доступным эмулятором
"""

import os
import sys
import subprocess
import time
from pathlib import Path

class UniversalEmulator:
    def __init__(self):
        self.project_dir = Path(__file__).parent
        self.adb_path = None
        self.device_id = None
        self.emulator_path = None
        self.emulator_name = None
        
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
    
    def find_emulators(self):
        """Поиск всех эмуляторов"""
        emulators = {}
        
        # Android Studio AVD
        avd_paths = [
            os.path.expanduser("~/AppData/Local/Android/Sdk/emulator/emulator.exe"),
            "C:/Android/Sdk/emulator/emulator.exe",
            "C:/Program Files/Android/Android Studio/sdk/emulator/emulator.exe"
        ]
        
        for path in avd_paths:
            if os.path.exists(path):
                emulators["Android Studio AVD"] = path
                break
        
        # Genymotion
        genymotion_paths = [
            "C:/Program Files/Genymobile/Genymotion/genymotion.exe",
            "C:/Program Files (x86)/Genymobile/Genymotion/genymotion.exe"
        ]
        
        for path in genymotion_paths:
            if os.path.exists(path):
                emulators["Genymotion"] = path
                break
        
        # BlueStacks
        bluestacks_paths = [
            "C:/Program Files/BlueStacks_nxt/HD-Player.exe",
            "C:/Program Files (x86)/BlueStacks_nxt/HD-Player.exe",
            "C:/Program Files/BlueStacks/HD-Player.exe",
            "C:/Program Files (x86)/BlueStacks/HD-Player.exe"
        ]
        
        for path in bluestacks_paths:
            if os.path.exists(path):
                emulators["BlueStacks"] = path
                break
        
        # NoxPlayer
        nox_paths = [
            "C:/Program Files/Nox/bin/Nox.exe",
            "C:/Program Files (x86)/Nox/bin/Nox.exe"
        ]
        
        for path in nox_paths:
            if os.path.exists(path):
                emulators["NoxPlayer"] = path
                break
        
        # LDPlayer
        ld_paths = [
            "C:/Program Files/LDPlayer/LDPlayer9/ldconsole.exe",
            "C:/Program Files (x86)/LDPlayer/LDPlayer9/ldconsole.exe"
        ]
        
        for path in ld_paths:
            if os.path.exists(path):
                emulators["LDPlayer"] = path
                break
        
        return emulators
    
    def start_android_studio_avd(self, path):
        """Запуск Android Studio AVD"""
        try:
            print("🚀 Запускаем Android Studio AVD...")
            
            # Список доступных AVD
            result = subprocess.run([
                path.replace("emulator.exe", "emulator.exe"), "-list-avds"
            ], capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0 and result.stdout.strip():
                avds = result.stdout.strip().split('\n')
                print(f"📋 Доступные AVD: {avds}")
                
                # Запускаем первый доступный AVD
                avd_name = avds[0]
                print(f"🚀 Запускаем AVD: {avd_name}")
                
                subprocess.Popen([path, "-avd", avd_name])
                print("✅ AVD запущен")
                return True
            else:
                print("❌ Нет доступных AVD")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка запуска AVD: {e}")
            return False
    
    def start_genymotion(self, path):
        """Запуск Genymotion"""
        try:
            print("🚀 Запускаем Genymotion...")
            
            # Настраиваем QEMU если возможно
            gmtool_path = path.replace("genymotion.exe", "gmtool.exe")
            if os.path.exists(gmtool_path):
                try:
                    subprocess.run([gmtool_path, "config", "--hypervisor", "qemu"], 
                                 capture_output=True, text=True, encoding='utf-8')
                    print("✅ QEMU настроен")
                except:
                    pass
            
            # Запускаем Genymotion
            subprocess.Popen([path])
            print("✅ Genymotion запущен")
            print("💡 Создайте устройство вручную")
            return True
            
        except Exception as e:
            print(f"❌ Ошибка запуска Genymotion: {e}")
            return False
    
    def start_bluestacks(self, path):
        """Запуск BlueStacks"""
        try:
            print("🚀 Запускаем BlueStacks...")
            subprocess.Popen([path])
            print("✅ BlueStacks запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска BlueStacks: {e}")
            return False
    
    def start_noxplayer(self, path):
        """Запуск NoxPlayer"""
        try:
            print("🚀 Запускаем NoxPlayer...")
            subprocess.Popen([path])
            print("✅ NoxPlayer запущен")
            return True
        except Exception as e:
            print(f"❌ Ошибка запуска NoxPlayer: {e}")
            return False
    
    def start_ldplayer(self, path):
        """Запуск LDPlayer"""
        try:
            print("🚀 Запускаем LDPlayer...")
            
            # Запускаем LDPlayer через консоль
            result = subprocess.run([path, "launch", "--index", "0"], 
                                  capture_output=True, text=True, encoding='utf-8')
            
            if result.returncode == 0:
                print("✅ LDPlayer запущен")
                return True
            else:
                print(f"❌ Ошибка запуска LDPlayer: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"❌ Ошибка запуска LDPlayer: {e}")
            return False
    
    def start_emulator(self, name, path):
        """Запуск эмулятора по имени"""
        self.emulator_name = name
        self.emulator_path = path
        
        if name == "Android Studio AVD":
            return self.start_android_studio_avd(path)
        elif name == "Genymotion":
            return self.start_genymotion(path)
        elif name == "BlueStacks":
            return self.start_bluestacks(path)
        elif name == "NoxPlayer":
            return self.start_noxplayer(path)
        elif name == "LDPlayer":
            return self.start_ldplayer(path)
        else:
            print(f"❌ Неизвестный эмулятор: {name}")
            return False
    
    def wait_for_device(self, timeout=300):
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
                time.sleep(2)
                
            except Exception as e:
                print(f"❌ Ошибка проверки: {e}")
                time.sleep(2)
        
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
    
    def create_launcher(self):
        """Создание запускатора"""
        try:
            print("📋 Создаем запускатор...")
            
            launcher_content = f"""@echo off
echo Запуск MedicalNotes на {self.emulator_name}...
python universal_emulator.py
pause
"""
            
            launcher_path = self.project_dir / f"Запустить_{self.emulator_name.replace(' ', '_')}.bat"
            with open(launcher_path, "w", encoding="utf-8") as f:
                f.write(launcher_content)
            
            print(f"✅ Запускатор создан: {launcher_path}")
            return True
            
        except Exception as e:
            print(f"❌ Ошибка создания запускатора: {e}")
            return False
    
    def run(self):
        """Основной метод"""
        print("🚀 Universal Emulator Launcher")
        print("=" * 35)
        
        if not self.find_adb():
            return False
        
        # Ищем эмуляторы
        emulators = self.find_emulators()
        
        if not emulators:
            print("\n❌ Эмуляторы не найдены")
            print("\n💡 Установите один из эмуляторов:")
            print("   1. BlueStacks: https://www.bluestacks.com/")
            print("   2. NoxPlayer: https://www.bignox.com/")
            print("   3. LDPlayer: https://www.ldplayer.net/")
            print("   4. Genymotion: https://www.genymotion.com/")
            return False
        
        print(f"\n✅ Найдено эмуляторов: {len(emulators)}")
        
        # Показываем список эмуляторов
        for i, (name, path) in enumerate(emulators.items(), 1):
            print(f"   {i}. {name}: {path}")
        
        # Выбираем эмулятор
        try:
            choice = input(f"\nВыберите эмулятор (1-{len(emulators)}): ")
            choice = int(choice) - 1
            
            if 0 <= choice < len(emulators):
                emulator_list = list(emulators.items())
                name, path = emulator_list[choice]
                
                print(f"\n🚀 Запускаем {name}...")
                
                # Запускаем эмулятор
                if self.start_emulator(name, path):
                    print(f"\n📋 Инструкции для {name}:")
                    if name == "Genymotion":
                        print("1. Дождитесь загрузки Genymotion")
                        print("2. Создайте новое устройство (если нет)")
                        print("3. Запустите устройство")
                        print("4. Дождитесь полной загрузки Android")
                    elif name == "Android Studio AVD":
                        print("1. Дождитесь полной загрузки AVD")
                        print("2. Разблокируйте экран если нужно")
                    else:
                        print("1. Дождитесь полной загрузки эмулятора")
                        print("2. Разблокируйте экран если нужно")
                    
                    print("3. Нажмите Enter для продолжения...")
                    input()
                    
                    # Ждем подключения устройства
                    if self.wait_for_device():
                        # Устанавливаем и запускаем приложение
                        if self.install_and_run_app():
                            # Создаем запускатор
                            self.create_launcher()
                            
                            print(f"\n🎉 MedicalNotes запущен на {name}!")
                            print(f"💡 Для быстрого запуска используйте:")
                            print(f"   Запустить_{name.replace(' ', '_')}.bat")
                            
                            return True
                        else:
                            print("❌ Не удалось запустить приложение")
                            return False
                    else:
                        print("❌ Не удалось подключить устройство")
                        return False
                else:
                    print(f"❌ Не удалось запустить {name}")
                    return False
            else:
                print("❌ Неверный выбор")
                return False
                
        except (ValueError, KeyboardInterrupt):
            print("\n👋 Операция прервана")
            return False
        except Exception as e:
            print(f"❌ Ошибка: {e}")
            return False

def main():
    launcher = UniversalEmulator()
    launcher.run()

if __name__ == "__main__":
    main() 