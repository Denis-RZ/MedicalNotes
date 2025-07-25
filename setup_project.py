#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для автоматической настройки Android проекта MedicalNotes
Настраивает все необходимые компоненты для сборки
"""

import os
import sys
import subprocess
import shutil
import time
from pathlib import Path
import urllib.request
import zipfile

def print_step(step, description):
    """Выводит шаг процесса"""
    print(f"\n{'='*60}")
    print(f"ШАГ {step}: {description}")
    print(f"{'='*60}")

def print_info(message):
    """Выводит информационное сообщение"""
    print(f"[INFO] {message}")

def print_error(message):
    """Выводит сообщение об ошибке"""
    print(f"[ERROR] {message}")

def print_success(message):
    """Выводит сообщение об успехе"""
    print(f"[SUCCESS] {message}")

def check_java():
    """Проверяет и настраивает Java"""
    print_step(1, "Проверка и настройка Java")
    
    try:
        result = subprocess.run(['java', '-version'], 
                              capture_output=True, text=True, timeout=10)
        if result.returncode == 0:
            print_success("Java найден")
            print_info(result.stderr.split('\n')[0])
            return True
        else:
            print_error("Java не найден или не работает")
            return False
    except Exception as e:
        print_error(f"Ошибка при проверке Java: {e}")
        print_info("Убедитесь, что Java установлен и добавлен в PATH")
        return False

def setup_gradle_home():
    """Настраивает папку для Gradle"""
    print_step(2, "Настройка папки Gradle")
    
    gradle_home = Path('C:/gradle_home')
    
    try:
        if not gradle_home.exists():
            print_info("Создание папки C:/gradle_home...")
            gradle_home.mkdir(parents=True, exist_ok=True)
            print_success("Папка C:/gradle_home создана")
        else:
            print_info("Папка C:/gradle_home уже существует")
        
        # Создаем подпапки
        wrapper_dists = gradle_home / 'wrapper' / 'dists'
        wrapper_dists.mkdir(parents=True, exist_ok=True)
        print_success("Структура папок Gradle создана")
        
        return True
    except Exception as e:
        print_error(f"Ошибка при создании папок Gradle: {e}")
        return False

def copy_gradle_from_reference():
    """Копирует Gradle из рабочего проекта"""
    print_step(3, "Копирование Gradle из рабочего проекта")
    
    source_gradle = Path('C:/Users/mikedell/.gradle')
    target_gradle = Path('C:/gradle_home')
    
    if not source_gradle.exists():
        print_error("Папка с Gradle не найдена")
        return False
    
    try:
        print_info("Копирование файлов Gradle...")
        
        # Копируем только необходимые папки
        if (source_gradle / 'wrapper' / 'dists').exists():
            shutil.copytree(
                source_gradle / 'wrapper' / 'dists',
                target_gradle / 'wrapper' / 'dists',
                dirs_exist_ok=True
            )
            print_success("Gradle wrapper dists скопированы")
        
        # Копируем кэш, пропуская заблокированные файлы
        if (source_gradle / 'caches').exists():
            print_info("Копирование кэша (пропуск заблокированных файлов)...")
            
            def copy_with_ignore(src, dst, dirs_exist_ok=False):
                try:
                    shutil.copytree(src, dst, dirs_exist_ok=dirs_exist_ok, ignore=shutil.ignore_patterns('*.lock'))
                except Exception as e:
                    print_info(f"Пропуск заблокированных файлов в {src}")
            
            copy_with_ignore(
                source_gradle / 'caches',
                target_gradle / 'caches',
                dirs_exist_ok=True
            )
            print_success("Gradle кэш скопирован (заблокированные файлы пропущены)")
        
        return True
    except Exception as e:
        print_error(f"Ошибка при копировании Gradle: {e}")
        return False

def download_gradle_wrapper_jar():
    """Скачивает gradle-wrapper.jar если его нет"""
    print_step(4, "Проверка gradle-wrapper.jar")
    
    wrapper_jar = Path('gradle/wrapper/gradle-wrapper.jar')
    
    if wrapper_jar.exists():
        print_success("gradle-wrapper.jar уже существует")
        return True
    
    try:
        print_info("Скачивание gradle-wrapper.jar...")
        
        # Создаем папку если её нет
        wrapper_jar.parent.mkdir(parents=True, exist_ok=True)
        
        # URL для скачивания gradle-wrapper.jar
        url = "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
        
        urllib.request.urlretrieve(url, wrapper_jar)
        print_success("gradle-wrapper.jar скачан")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при скачивании gradle-wrapper.jar: {e}")
        return False

def update_gradle_properties():
    """Обновляет gradle.properties с правильными настройками"""
    print_step(5, "Обновление gradle.properties")
    
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
"""
        
        with open('gradle.properties', 'w', encoding='utf-8') as f:
            f.write(properties_content)
        
        print_success("gradle.properties обновлен")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при обновлении gradle.properties: {e}")
        return False

def update_gradle_wrapper_properties():
    """Обновляет gradle-wrapper.properties"""
    print_step(6, "Обновление gradle-wrapper.properties")
    
    try:
        wrapper_properties_content = """distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https://services.gradle.org/distributions/gradle-8.4-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"""
        
        with open('gradle/wrapper/gradle-wrapper.properties', 'w', encoding='utf-8') as f:
            f.write(wrapper_properties_content)
        
        print_success("gradle-wrapper.properties обновлен")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при обновлении gradle-wrapper.properties: {e}")
        return False

def update_build_gradle():
    """Обновляет build.gradle с правильными версиями"""
    print_step(7, "Обновление build.gradle")
    
    try:
        build_gradle_content = """// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version '8.1.4' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.10' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
"""
        
        with open('build.gradle', 'w', encoding='utf-8') as f:
            f.write(build_gradle_content)
        
        print_success("build.gradle обновлен")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при обновлении build.gradle: {e}")
        return False

def update_app_build_gradle():
    """Обновляет app/build.gradle"""
    print_step(8, "Обновление app/build.gradle")
    
    try:
        app_build_gradle_content = """plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.medicalnotes.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.medicalnotes.app"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = '1.8'
    }
    buildFeatures {
        viewBinding true
    }
    
    // Оптимизации для ускорения сборки
    // dexOptions устарел, используем автоматическую оптимизацию
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    implementation 'androidx.work:work-runtime-ktx:2.8.1'
    implementation 'androidx.preference:preference-ktx:1.2.1'
    implementation 'androidx.room:room-runtime:2.6.0'
    implementation 'androidx.room:room-ktx:2.6.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.4'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.4'
    implementation 'com.google.code.gson:gson:2.10.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
"""
        
        with open('app/build.gradle', 'w', encoding='utf-8') as f:
            f.write(app_build_gradle_content)
        
        print_success("app/build.gradle обновлен")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при обновлении app/build.gradle: {e}")
        return False

def test_gradle_setup():
    """Тестирует настройку Gradle"""
    print_step(9, "Тестирование настройки Gradle")
    
    try:
        print_info("Запуск gradlew --version...")
        
        # Устанавливаем переменную окружения
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\gradle_home'
        
        result = subprocess.run(
            ['gradlew.bat', '--version'],
            capture_output=True,
            text=True,
            timeout=60,
            env=env
        )
        
        if result.returncode == 0:
            print_success("Gradle работает корректно")
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
        print_error(f"Ошибка при тестировании Gradle: {e}")
        return False

def create_build_script():
    """Создает улучшенный скрипт сборки"""
    print_step(10, "Создание скрипта сборки")
    
    try:
        build_script_content = '''#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Улучшенный скрипт для сборки Android проекта MedicalNotes
"""

import os
import sys
import subprocess
import time
from pathlib import Path

def print_step(step, description):
    print(f"\\n{'='*60}")
    print(f"ШАГ {step}: {description}")
    print(f"{'='*60}")

def print_info(message):
    print(f"[INFO] {message}")

def print_error(message):
    print(f"[ERROR] {message}")

def print_success(message):
    print(f"[SUCCESS] {message}")

def build_project():
    """Собирает проект"""
    print_step(1, "Сборка проекта")
    
    try:
        # Устанавливаем переменную окружения
        env = os.environ.copy()
        env['GRADLE_USER_HOME'] = 'C:\\\\gradle_home'
        print_info(f"Установлен GRADLE_USER_HOME: {env['GRADLE_USER_HOME']}")
        
        print_info("Запуск gradlew assembleDebug...")
        print_info("Это может занять несколько минут...")
        
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
    print_step(2, "Поиск APK файла")
    
    apk_path = Path('app/build/outputs/apk/debug/app-debug.apk')
    
    if apk_path.exists():
        size_mb = apk_path.stat().st_size / (1024 * 1024)
        print_success(f"APK найден: {apk_path}")
        print_info(f"Размер: {size_mb:.2f} МБ")
        print_info(f"Полный путь: {apk_path.absolute()}")
        return True
    else:
        print_error("APK файл не найден")
        print_info("Ожидаемый путь: app/build/outputs/apk/debug/app-debug.apk")
        return False

def main():
    print("🚀 Сборка Android проекта MedicalNotes")
    print(f"Время начала: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    if not build_project():
        print_error("Сборка не удалась")
        return False
    
    if not find_apk():
        print_error("APK не найден")
        return False
    
    print("\\n" + "="*60)
    print_success("🎉 Сборка завершена успешно!")
    print(f"Время завершения: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\\n\\n[INFO] Сборка прервана пользователем")
        sys.exit(1)
    except Exception as e:
        print_error(f"Неожиданная ошибка: {e}")
        sys.exit(1)
'''
        
        with open('build_project.py', 'w', encoding='utf-8') as f:
            f.write(build_script_content)
        
        print_success("Скрипт сборки создан: build_project.py")
        return True
        
    except Exception as e:
        print_error(f"Ошибка при создании скрипта сборки: {e}")
        return False

def main():
    """Основная функция настройки"""
    print("🔧 Настройка Android проекта MedicalNotes")
    print(f"Время начала: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Проверяем, что мы в правильной директории
    if not os.path.exists('build.gradle'):
        print_error("Файл build.gradle не найден. Убедитесь, что вы в корне проекта.")
        return False
    
    steps = [
        check_java,
        setup_gradle_home,
        copy_gradle_from_reference,
        download_gradle_wrapper_jar,
        update_gradle_properties,
        update_gradle_wrapper_properties,
        update_build_gradle,
        update_app_build_gradle,
        test_gradle_setup,
        create_build_script
    ]
    
    for step_func in steps:
        if not step_func():
            print_error(f"Шаг {step_func.__name__} не выполнен. Настройка прервана.")
            return False
    
    print("\n" + "="*60)
    print_success("🎉 Настройка проекта завершена успешно!")
    print(f"Время завершения: {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("="*60)
    print_info("Теперь вы можете запустить сборку командой:")
    print_info("py build_project.py")
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n[INFO] Настройка прервана пользователем")
        sys.exit(1)
    except Exception as e:
        print_error(f"Неожиданная ошибка: {e}")
        sys.exit(1) 