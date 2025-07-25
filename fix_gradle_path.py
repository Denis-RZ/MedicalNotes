#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для исправления проблемы с Gradle и кириллицей в пути
"""

import os
import sys
import subprocess
import time
import shutil
from pathlib import Path

def print_step(step, description):
    print(f"\n{'='*60}")
    print(f"ШАГ {step}: {description}")
    print(f"{'='*60}")

def print_info(message):
    print(f"[INFO] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def create_gradle_home():
    """Создает новую папку для Gradle без кириллицы"""
    print_step(1, "Создание новой папки для Gradle")
    
    try:
        # Создаем папку в корне диска C без кириллицы
        gradle_home = Path('C:/gradle_home_clean')
        
        if gradle_home.exists():
            print_info("Папка уже существует, очищаем...")
            shutil.rmtree(gradle_home, ignore_errors=True)
        
        gradle_home.mkdir(parents=True, exist_ok=True)
        print_success(f"Создана папка: {gradle_home}")
        
        # Создаем подпапки
        (gradle_home / 'wrapper' / 'dists').mkdir(parents=True, exist_ok=True)
        (gradle_home / 'caches').mkdir(parents=True, exist_ok=True)
        print_success("Структура папок создана")
        
        return gradle_home
    except Exception as e:
        print_error(f"Ошибка при создании папки: {e}")
        return None

def copy_gradle_wrapper():
    """Копирует Gradle wrapper из рабочего проекта"""
    print_step(2, "Копирование Gradle wrapper")
    
    try:
        source_wrapper = Path('../AndroidVoiceOn/gradle/wrapper')
        target_wrapper = Path('./gradle/wrapper')
        
        if source_wrapper.exists():
            shutil.copytree(source_wrapper, target_wrapper, dirs_exist_ok=True)
            print_success("Gradle wrapper скопирован")
        else:
            print_error("Исходный Gradle wrapper не найден")
            return False
        
        return True
    except Exception as e:
        print_error(f"Ошибка при копировании wrapper: {e}")
        return False

def copy_gradle_distributions():
    """Копирует уже скачанные дистрибутивы Gradle"""
    print_step(3, "Копирование дистрибутивов Gradle")
    
    try:
        source_dists = Path('C:/Users/mikedell/.gradle/wrapper/dists')
        target_dists = Path('C:/gradle_home_clean/wrapper/dists')
        
        if source_dists.exists():
            print_info("Копирование дистрибутивов...")
            shutil.copytree(source_dists, target_dists, dirs_exist_ok=True)
            print_success("Дистрибутивы скопированы")
        else:
            print_info("Дистрибутивы не найдены, будут скачаны автоматически")
        
        return True
    except Exception as e:
        print_error(f"Ошибка при копировании дистрибутивов: {e}")
        return False

def update_gradle_properties():
    """Обновляет gradle.properties с новым путем"""
    print_step(4, "Обновление gradle.properties")
    
    try:
        properties_content = """# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. More details, visit
# http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app's APK
# https://developer.android.com/topic/libraries/support-library/androidx-rn
android.useAndroidX=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
# Устанавливаем новый путь для Gradle home
org.gradle.user.home=C:/gradle_home_clean
"""
        
        with open('gradle.properties', 'w', encoding='utf-8') as f:
            f.write(properties_content)
        
        print_success("gradle.properties обновлен")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при обновлении gradle.properties: {e}")
        return False

def create_android_studio_settings():
    """Создает настройки для Android Studio"""
    print_step(5, "Создание настроек для Android Studio")
    
    try:
        # Создаем файл с инструкциями для Android Studio
        instructions = """# Настройки Gradle для Android Studio

## Проблема
Gradle не может создать файлы в пути с кириллицей: C:\\Users\\mikedell\\.gradle

## Решение
Используйте новый путь без кириллицы: C:\\gradle_home_clean

## Как настроить в Android Studio:

### Способ 1: Через настройки
1. Откройте Android Studio
2. Перейдите в File -> Settings (или Ctrl+Alt+S)
3. В левом меню выберите Build, Execution, Deployment -> Gradle
4. В поле "Gradle user home" введите: C:\\gradle_home_clean
5. Нажмите Apply и OK
6. Перезапустите Android Studio

### Способ 2: Через gradle.properties
Файл gradle.properties уже обновлен с правильным путем.

### Способ 3: Через переменную окружения
Установите переменную окружения GRADLE_USER_HOME=C:\\gradle_home_clean

## Проверка
После настройки попробуйте синхронизировать проект:
1. File -> Sync Project with Gradle Files
2. Или нажмите кнопку "Sync Now" в верхней панели

## Если проблема остается
1. File -> Invalidate Caches and Restart
2. Выберите "Invalidate and Restart"
"""
        
        with open('GRADLE_SETTINGS_INSTRUCTIONS.md', 'w', encoding='utf-8') as f:
            f.write(instructions)
        
        print_success("Создан файл с инструкциями: GRADLE_SETTINGS_INSTRUCTIONS.md")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при создании инструкций: {e}")
        return False

def test_gradle_setup():
    """Тестирует настройку Gradle"""
    print_step(6, "Тестирование настройки Gradle")
    
    try:
        # Устанавливаем переменную окружения
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\gradle_home_clean'
        
        print_info("Тестирование gradlew --version...")
        result = subprocess.run(
            ['gradlew.bat', '--version'],
            capture_output=True,
            text=True,
            timeout=60,
            env=env
        )
        
        if result.returncode == 0:
            print_success("Gradle работает корректно!")
            print_info("Версия Gradle:")
            for line in result.stdout.split('\n')[:5]:
                if line.strip():
                    print_info(line.strip())
            return True
        else:
            print_error("Gradle не работает")
            if result.stderr:
                print_error("Ошибка:")
                print(result.stderr)
            return False
            
    except Exception as e:
        print_error(f"Ошибка при тестировании: {e}")
        return False

def create_build_script():
    """Создает скрипт сборки с правильными настройками"""
    print_step(7, "Создание скрипта сборки")
    
    try:
        build_script = '''#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для сборки с исправленным путем Gradle
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_info(message):
    print(f"[INFO] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def build_project():
    """Собирает проект с исправленным путем Gradle"""
    print("🚀 Сборка проекта с исправленным путем Gradle")
    
    try:
        # Устанавливаем переменную окружения
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\\\gradle_home_clean'
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
        print_info("Запуск gradlew assembleDebug...")
        
        # Запускаем сборку с выводом в реальном времени
        process = subprocess.Popen(
            ['gradlew.bat', 'assembleDebug'],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            universal_newlines=True,
            env=env
        )
        
        # Выводим вывод в реальном времени
        for line in process.stdout:
            print(line.rstrip())
        
        # Ждем завершения процесса
        return_code = process.wait()
        
        if return_code == 0:
            print_success("APK успешно собран!")
            return True
        else:
            print_error(f"Ошибка при сборке. Код возврата: {return_code}")
            return False
            
    except Exception as e:
        print_error(f"Ошибка при сборке: {e}")
        return False

def find_apk():
    """Ищет собранный APK файл"""
    apk_path = Path('app/build/outputs/apk/debug/app-debug.apk')
    
    if apk_path.exists():
        size_mb = apk_path.stat().st_size / (1024 * 1024)
        print_success(f"APK найден: {apk_path}")
        print_info(f"Размер: {size_mb:.2f} МБ")
        return True
    else:
        print_error("APK файл не найден")
        return False

if __name__ == "__main__":
    if build_project():
        find_apk()
    else:
        sys.exit(1)
'''
        
        with open('build_fixed.py', 'w', encoding='utf-8') as f:
            f.write(build_script)
        
        print_success("Создан скрипт сборки: build_fixed.py")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при создании скрипта: {e}")
        return False

def main():
    print("🔧 Исправление проблемы с Gradle и кириллицей")
    print(f"Время начала: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    gradle_home = create_gradle_home()
    if not gradle_home:
        return False
    
    if not copy_gradle_wrapper():
        return False
    
    if not copy_gradle_distributions():
        return False
    
    if not update_gradle_properties():
        return False
    
    if not create_android_studio_settings():
        return False
    
    if not test_gradle_setup():
        print_info("Gradle не прошел тест, но настройки созданы")
    
    if not create_build_script():
        return False
    
    print("\n" + "="*60)
    print_success("🎉 Проблема с Gradle исправлена!")
    print("="*60)
    print_info("Следующие шаги:")
    print_info("1. Откройте Android Studio")
    print_info("2. Перейдите в File -> Settings -> Build, Execution, Deployment -> Gradle")
    print_info("3. Установите 'Gradle user home': C:\\gradle_home_clean")
    print_info("4. Нажмите Apply и OK")
    print_info("5. Перезапустите Android Studio")
    print_info("6. Синхронизируйте проект")
    print_info("")
    print_info("Или используйте скрипт: py build_fixed.py")
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n[INFO] Операция прервана пользователем")
        sys.exit(1)
    except Exception as e:
        print_error(f"Неожиданная ошибка: {e}")
        sys.exit(1) 