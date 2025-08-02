import os
import re

def remove_emojis_from_file(file_path):
    """Удаляет эмодзи из файла"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Список эмодзи для удаления
        emojis = ['🔇', '🔔', '✅', '❌', '⚠️', '🎉', '📋', '📱', '💊', '⏰', '📝', '🔧', '🚀', '🚨', '⏸️', '⏭️', '⚠']
        
        # Удаляем каждый эмодзи
        for emoji in emojis:
            content = content.replace(emoji, '')
        
        # Записываем обратно
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"Обработан файл: {file_path}")
        
    except Exception as e:
        print(f"Ошибка обработки {file_path}: {e}")

def process_directory(directory):
    """Обрабатывает все .kt файлы в директории и поддиректориях"""
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.kt'):
                file_path = os.path.join(root, file)
                remove_emojis_from_file(file_path)

if __name__ == "__main__":
    # Обрабатываем все Kotlin файлы в app/src/main/java
    java_dir = "app/src/main/java"
    if os.path.exists(java_dir):
        process_directory(java_dir)
        print("Удаление эмодзи завершено!")
    else:
        print(f"Директория {java_dir} не найдена") 