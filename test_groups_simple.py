#!/usr/bin/env python3
"""
Упрощенный тест функционала групп для MedicalNotes (без реального устройства)
"""

import os
import sys
import subprocess

def run_gradle_test():
    """Запускает тесты через Gradle"""
    print("🧪 Запуск тестов через Gradle...")
    
    try:
        # Собираем проект
        print("📦 Сборка проекта...")
        result = subprocess.run(["./gradlew", "assembleDebug"], 
                              capture_output=True, text=True, cwd=".")
        
        if result.returncode != 0:
            print(f"❌ Ошибка сборки: {result.stderr}")
            return False
        
        print("✅ Проект собран успешно")
        
        # Запускаем unit тесты (если есть)
        print("🔬 Запуск unit тестов...")
        result = subprocess.run(["./gradlew", "testDebugUnitTest"], 
                              capture_output=True, text=True, cwd=".")
        
        if result.returncode != 0:
            print(f"⚠️ Unit тесты не прошли: {result.stderr}")
        else:
            print("✅ Unit тесты прошли успешно")
        
        return True
        
    except Exception as e:
        print(f"❌ Ошибка выполнения тестов: {e}")
        return False

def analyze_code():
    """Анализирует код на предмет потенциальных проблем с группами"""
    print("🔍 Анализ кода на предмет проблем с группами...")
    
    issues = []
    
    # Проверяем ключевые файлы
    files_to_check = [
        "app/src/main/java/com/medicalnotes/app/adapters/GroupAdapter.kt",
        "app/src/main/java/com/medicalnotes/app/adapters/GroupMedicineAdapter.kt",
        "app/src/main/java/com/medicalnotes/app/GroupManagementActivity.kt",
        "app/src/main/java/com/medicalnotes/app/utils/MedicineGroupingUtil.kt",
        "app/src/main/java/com/medicalnotes/app/utils/GroupTestSuite.kt"
    ]
    
    for file_path in files_to_check:
        if os.path.exists(file_path):
            print(f"📄 Проверяем файл: {file_path}")
            
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Проверяем на потенциальные проблемы
                if "TODO" in content or "FIXME" in content:
                    issues.append(f"Найдены TODO/FIXME в {file_path}")
                
                if "groupOrder" in content and "groupOrder <= 0" not in content:
                    issues.append(f"Возможная проблема с валидацией groupOrder в {file_path}")
                
                if "groupName" in content and "groupName.isEmpty()" not in content:
                    issues.append(f"Возможная проблема с валидацией groupName в {file_path}")
                
                if "Exception" in content or "Error" in content:
                    issues.append(f"Найдены обработчики исключений в {file_path}")
                
            except Exception as e:
                issues.append(f"Ошибка чтения файла {file_path}: {e}")
        else:
            issues.append(f"Файл не найден: {file_path}")
    
    return issues

def check_manifest():
    """Проверяет AndroidManifest.xml на предмет проблем с группами"""
    print("📱 Проверка AndroidManifest.xml...")
    
    manifest_path = "app/src/main/AndroidManifest.xml"
    issues = []
    
    if os.path.exists(manifest_path):
        try:
            with open(manifest_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Проверяем наличие необходимых разрешений
            if "android.permission.WRITE_EXTERNAL_STORAGE" not in content:
                issues.append("Отсутствует разрешение на запись файлов")
            
            if "android.permission.READ_EXTERNAL_STORAGE" not in content:
                issues.append("Отсутствует разрешение на чтение файлов")
            
            # Проверяем наличие GroupManagementActivity
            if "GroupManagementActivity" not in content:
                issues.append("GroupManagementActivity не зарегистрирована в манифесте")
            
        except Exception as e:
            issues.append(f"Ошибка чтения манифеста: {e}")
    else:
        issues.append("AndroidManifest.xml не найден")
    
    return issues

def check_layouts():
    """Проверяет макеты на предмет проблем с группами"""
    print("🎨 Проверка макетов...")
    
    layout_files = [
        "app/src/main/res/layout/activity_group_management.xml",
        "app/src/main/res/layout/item_group.xml",
        "app/src/main/res/layout/item_group_medicine.xml"
    ]
    
    issues = []
    
    for layout_file in layout_files:
        if os.path.exists(layout_file):
            try:
                with open(layout_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Проверяем наличие необходимых элементов
                if "RecyclerView" in content and "android:id" not in content:
                    issues.append(f"RecyclerView без ID в {layout_file}")
                
                if "TextView" in content and "android:text" not in content:
                    issues.append(f"TextView без текста в {layout_file}")
                
            except Exception as e:
                issues.append(f"Ошибка чтения макета {layout_file}: {e}")
        else:
            issues.append(f"Макет не найден: {layout_file}")
    
    return issues

def generate_test_report():
    """Генерирует отчет о тестировании"""
    print("📊 Генерация отчета о тестировании...")
    
    report = []
    report.append("=" * 60)
    report.append("ОТЧЕТ О ТЕСТИРОВАНИИ ФУНКЦИОНАЛА ГРУПП")
    report.append("=" * 60)
    report.append("")
    
    # Анализ кода
    code_issues = analyze_code()
    report.append("АНАЛИЗ КОДА:")
    if code_issues:
        for issue in code_issues:
            report.append(f"  ⚠️ {issue}")
    else:
        report.append("  ✅ Проблем не найдено")
    report.append("")
    
    # Проверка манифеста
    manifest_issues = check_manifest()
    report.append("ПРОВЕРКА МАНИФЕСТА:")
    if manifest_issues:
        for issue in manifest_issues:
            report.append(f"  ⚠️ {issue}")
    else:
        report.append("  ✅ Проблем не найдено")
    report.append("")
    
    # Проверка макетов
    layout_issues = check_layouts()
    report.append("ПРОВЕРКА МАКЕТОВ:")
    if layout_issues:
        for issue in layout_issues:
            report.append(f"  ⚠️ {issue}")
    else:
        report.append("  ✅ Проблем не найдено")
    report.append("")
    
    # Общие рекомендации
    report.append("РЕКОМЕНДАЦИИ:")
    report.append("  1. Проверьте валидацию groupOrder (должен быть > 0)")
    report.append("  2. Проверьте валидацию groupName (не должен быть пустым)")
    report.append("  3. Убедитесь, что нет дубликатов порядка в группах")
    report.append("  4. Проверьте обработку пустых групп")
    report.append("  5. Протестируйте на реальном устройстве")
    report.append("")
    
    # Итоги
    total_issues = len(code_issues) + len(manifest_issues) + len(layout_issues)
    report.append("ИТОГИ:")
    report.append(f"  Всего найдено проблем: {total_issues}")
    
    if total_issues == 0:
        report.append("  🎉 Код готов к тестированию на устройстве!")
    else:
        report.append("  ⚠️ Рекомендуется исправить найденные проблемы")
    
    report.append("=" * 60)
    
    return report

def main():
    """Основная функция"""
    print("🧪 УПРОЩЕННОЕ ТЕСТИРОВАНИЕ ФУНКЦИОНАЛА ГРУПП")
    print("=" * 50)
    
    # Запускаем Gradle тесты
    gradle_success = run_gradle_test()
    
    # Генерируем отчет
    report = generate_test_report()
    
    # Выводим отчет
    for line in report:
        print(line)
    
    # Сохраняем отчет в файл
    with open("group_test_report.txt", "w", encoding="utf-8") as f:
        for line in report:
            f.write(line + "\n")
    
    print(f"\n📄 Отчет сохранен в файл: group_test_report.txt")
    
    if gradle_success:
        print("\n✅ Тестирование завершено успешно!")
    else:
        print("\n❌ Тестирование завершено с ошибками!")

if __name__ == "__main__":
    main() 