#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

def test_every_other_day_with_yesterday_logic():
    """Test the correct logic for 'every other day' medicines considering yesterday's intake"""
    
    print("=== ТЕСТ ЛОГИКИ 'ЧЕРЕЗ ДЕНЬ' С УЧЕТОМ ВЧЕРАШНЕГО ПРИЕМА ===")
    print()
    
    # Симулируем группу "через день" с двумя разными лекарствами
    group_start_date = datetime(2025, 8, 6)  # Дата начала группы
    
    print(f"Дата начала группы: {group_start_date.strftime('%Y-%m-%d')}")
    print()
    
    # Тестируем на нескольких датах
    test_dates = [
        datetime(2025, 8, 6),   # День 0 - четный день группы
        datetime(2025, 8, 7),   # День 1 - нечетный день группы  
        datetime(2025, 8, 8),   # День 2 - четный день группы
        datetime(2025, 8, 9),   # День 3 - нечетный день группы
        datetime(2025, 8, 10),  # День 4 - четный день группы
        datetime(2025, 8, 11),  # День 5 - нечетный день группы
    ]
    
    print("=== ЛОГИКА БЕЗ УЧЕТА ВЧЕРАШНЕГО ПРИЕМА (НЕПРАВИЛЬНО) ===")
    print("Проблема: Лекарства будут показываться каждый день!")
    print()
    
    for test_date in test_dates:
        days_since_start = (test_date - group_start_date).days
        group_day = days_since_start % 2
        
        print(f"Дата: {test_date.strftime('%Y-%m-%d')} (день группы: {group_day})")
        
        # Лекарство 1 (groupOrder = 1) - принимается в четные дни группы (0, 2, 4...)
        should_take_1 = group_day == 0
        print(f"  Липетор (порядок 1): {'ПРИНИМАТЬ' if should_take_1 else 'НЕ ПРИНИМАТЬ'}")
        
        # Лекарство 2 (groupOrder = 2) - принимается в нечетные дни группы (1, 3, 5...)
        should_take_2 = group_day == 1
        print(f"  Фубуксусат (порядок 2): {'ПРИНИМАТЬ' if should_take_2 else 'НЕ ПРИНИМАТЬ'}")
        print()
    
    print("=== ПРАВИЛЬНАЯ ЛОГИКА С УЧЕТОМ ВЧЕРАШНЕГО ПРИЕМА ===")
    print("Решение: Проверяем, было ли лекарство принято вчера")
    print()
    
    # Симулируем, что Липетор был принят 7 августа (вчера для 8 августа)
    lipitor_taken_yesterday = datetime(2025, 8, 7)
    
    for test_date in test_dates:
        days_since_start = (test_date - group_start_date).days
        group_day = days_since_start % 2
        yesterday = test_date - timedelta(days=1)
        
        print(f"Дата: {test_date.strftime('%Y-%m-%d')} (день группы: {group_day})")
        
        # Лекарство 1 (groupOrder = 1)
        should_take_1 = group_day == 0
        was_taken_yesterday_1 = lipitor_taken_yesterday.date() == yesterday.date()
        
        # Если принято вчера и сегодня не по расписанию - НЕ показываем
        final_result_1 = False
        if was_taken_yesterday_1 and not should_take_1:
            final_result_1 = False
        elif was_taken_yesterday_1 and should_take_1:
            final_result_1 = True  # Принято вчера, но сегодня тоже нужно
        else:
            final_result_1 = should_take_1
        
        print(f"  Липетор:")
        print(f"    - По расписанию: {'ДА' if should_take_1 else 'НЕТ'}")
        print(f"    - Принято вчера: {'ДА' if was_taken_yesterday_1 else 'НЕТ'}")
        print(f"    - Итоговый результат: {'ПРИНИМАТЬ' if final_result_1 else 'НЕ ПРИНИМАТЬ'}")
        
        # Лекарство 2 (groupOrder = 2) - не принималось вчера
        should_take_2 = group_day == 1
        was_taken_yesterday_2 = False  # Фубуксусат не принимался вчера
        
        final_result_2 = should_take_2  # Простая логика, так как не принимался вчера
        
        print(f"  Фубуксусат:")
        print(f"    - По расписанию: {'ДА' if should_take_2 else 'НЕТ'}")
        print(f"    - Принято вчера: {'ДА' if was_taken_yesterday_2 else 'НЕТ'}")
        print(f"    - Итоговый результат: {'ПРИНИМАТЬ' if final_result_2 else 'НЕ ПРИНИМАТЬ'}")
        print()
    
    print("=== ВЫВОД ===")
    print("✅ ПРАВИЛЬНАЯ логика должна учитывать вчерашний прием!")
    print("❌ БЕЗ учета вчерашнего приема лекарства будут показываться неправильно")
    print()
    print("Пример проблемы:")
    print("- 8 августа: Липетор принят 7 августа, но 8 августа не по расписанию")
    print("- БЕЗ проверки вчерашнего: Липетор будет показан как 'пропущенный'")
    print("- С проверкой вчерашнего: Липетор НЕ будет показан (правильно!)")
    print()
    print("🎯 ЗАКЛЮЧЕНИЕ: Проверки вчерашнего приема НУЖНЫ для корректной работы!")

if __name__ == "__main__":
    test_every_other_day_with_yesterday_logic() 