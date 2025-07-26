#!/usr/bin/env python3
"""
Автоматический тест функционала групп для MedicalNotes
"""

import subprocess
import time
import os

def run_adb_command(command):
    """Выполняет ADB команду"""
    try:
        result = subprocess.run(f"adb {command}", shell=True, capture_output=True, text=True)
        return result.returncode == 0, result.stdout, result.stderr
    except Exception as e:
        return False, "", str(e)

def install_apk():
    """Устанавливает APK на устройство"""
    print("📱 Установка APK...")
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"
    
    if not os.path.exists(apk_path):
        print("❌ APK не найден. Сначала соберите проект.")
        return False
    
    success, stdout, stderr = run_adb_command(f"install -r {apk_path}")
    
    if success:
        print("✅ APK установлен успешно")
        return True
    else:
        print(f"❌ Ошибка установки APK: {stderr}")
        return False

def start_app():
    """Запускает приложение"""
    print("🚀 Запуск приложения...")
    package_name = "com.medicalnotes.app"
    activity_name = "com.medicalnotes.app.MainActivity"
    
    success, stdout, stderr = run_adb_command(f"shell am start -n {package_name}/{activity_name}")
    
    if success:
        print("✅ Приложение запущено")
        return True
    else:
        print(f"❌ Ошибка запуска приложения: {stderr}")
        return False

def click_test_groups_button():
    """Нажимает кнопку тестирования групп"""
    print("🔘 Нажатие кнопки 'ТЕСТ ГРУПП'...")
    
    # Координаты кнопки (примерные, нужно будет уточнить)
    x, y = 500, 1500
    
    success, stdout, stderr = run_adb_command(f"shell input tap {x} {y}")
    
    if success:
        print("✅ Кнопка нажата")
        return True
    else:
        print(f"❌ Ошибка нажатия кнопки: {stderr}")
        return False

def get_logs():
    """Получает логи приложения"""
    print("📋 Получение логов...")
    
    package_name = "com.medicalnotes.app"
    success, stdout, stderr = run_adb_command(f"logcat -d -s {package_name}:*")
    
    if success:
        return stdout
    else:
        print(f"❌ Ошибка получения логов: {stderr}")
        return ""

def analyze_test_results(logs):
    """Анализирует результаты тестов"""
    print("🔍 Анализ результатов тестов...")
    
    lines = logs.split('\n')
    test_results = []
    errors = []
    warnings = []
    
    for line in lines:
        if "=== НАЧАЛО ТЕСТИРОВАНИЯ ГРУПП ===" in line:
            test_results.append("Тестирование началось")
        elif "❌ ОШИБКА" in line:
            errors.append(line.strip())
        elif "⚠️ ВНИМАНИЕ" in line:
            warnings.append(line.strip())
        elif "✅ ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО" in line:
            test_results.append("Все тесты пройдены успешно")
        elif "=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===" in line:
            test_results.append("Тестирование завершено")
    
    print(f"📊 Результаты анализа:")
    print(f"   - Тестовых сообщений: {len(test_results)}")
    print(f"   - Ошибок: {len(errors)}")
    print(f"   - Предупреждений: {len(warnings)}")
    
    if errors:
        print("\n❌ НАЙДЕННЫЕ ОШИБКИ:")
        for error in errors[:10]:  # Показываем первые 10 ошибок
            print(f"   {error}")
    
    if warnings:
        print("\n⚠️ НАЙДЕННЫЕ ПРЕДУПРЕЖДЕНИЯ:")
        for warning in warnings[:10]:  # Показываем первые 10 предупреждений
            print(f"   {warning}")
    
    return len(errors), len(warnings)

def main():
    """Основная функция тестирования"""
    print("🧪 АВТОМАТИЧЕСКОЕ ТЕСТИРОВАНИЕ ФУНКЦИОНАЛА ГРУПП")
    print("=" * 50)
    
    # Проверяем подключение устройства
    print("🔌 Проверка подключения устройства...")
    success, stdout, stderr = run_adb_command("devices")
    
    if "device" not in stdout:
        print("❌ Устройство не подключено. Подключите устройство и включите USB отладку.")
        return
    
    print("✅ Устройство подключено")
    
    # Устанавливаем APK
    if not install_apk():
        return
    
    # Запускаем приложение
    if not start_app():
        return
    
    # Ждем загрузки приложения
    print("⏳ Ожидание загрузки приложения...")
    time.sleep(3)
    
    # Очищаем логи
    run_adb_command("logcat -c")
    
    # Нажимаем кнопку тестирования групп
    if not click_test_groups_button():
        return
    
    # Ждем выполнения тестов
    print("⏳ Ожидание выполнения тестов...")
    time.sleep(5)
    
    # Получаем логи
    logs = get_logs()
    
    # Анализируем результаты
    error_count, warning_count = analyze_test_results(logs)
    
    # Сохраняем логи в файл
    with open("group_test_results.log", "w", encoding="utf-8") as f:
        f.write(logs)
    
    print(f"\n📄 Логи сохранены в файл: group_test_results.log")
    
    # Итоговый результат
    if error_count == 0 and warning_count == 0:
        print("\n🎉 ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО!")
    elif error_count == 0:
        print(f"\n⚠️ ТЕСТЫ ПРОЙДЕНЫ С ПРЕДУПРЕЖДЕНИЯМИ ({warning_count})")
    else:
        print(f"\n❌ НАЙДЕНЫ ОШИБКИ ({error_count}) И ПРЕДУПРЕЖДЕНИЯ ({warning_count})")

if __name__ == "__main__":
    main() 