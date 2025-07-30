#!/usr/bin/env python3
import sys
import os
import subprocess

def check_python_installation():
    print("Проверяем установку Python...")
    
    # Проверяем текущую версию Python
    print(f"Текущая версия Python: {sys.version}")
    print(f"Путь к Python: {sys.executable}")
    
    # Проверяем переменную PATH
    path_dirs = os.environ.get('PATH', '').split(os.pathsep)
    python_paths = []
    
    for directory in path_dirs:
        if 'python' in directory.lower():
            python_paths.append(directory)
    
    if python_paths:
        print("\nНайдены пути к Python в PATH:")
        for path in python_paths:
            print(f"  - {path}")
    else:
        print("\nPython не найден в PATH")
    
    # Проверяем стандартные места установки
    standard_paths = [
        r"C:\Python*",
        r"C:\Program Files\Python*",
        r"C:\Program Files (x86)\Python*",
        r"C:\Users\%USERNAME%\AppData\Local\Programs\Python*",
        r"C:\Users\%USERNAME%\AppData\Local\Microsoft\WindowsApps\python*"
    ]
    
    print("\nПроверяем стандартные места установки...")
    for pattern in standard_paths:
        expanded_pattern = os.path.expandvars(pattern)
        if os.path.exists(expanded_pattern):
            print(f"Найдено: {expanded_pattern}")

if __name__ == "__main__":
    check_python_installation() 