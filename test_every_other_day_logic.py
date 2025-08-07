#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

def test_every_other_day_logic():
    """Test the correct logic for 'every other day' medicines"""
    
    print("=== ТЕСТ ЛОГИКИ 'ЧЕРЕЗ ДЕНЬ' ДЛЯ РАЗНЫХ ЛЕКАРСТВ ===")
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
        datetime(2025, 8, 12),  # День 6 - четный день группы
        datetime(2025, 8, 13),  # День 7 - нечетный день группы
    ]
    
    print("=== ПРАВИЛЬНАЯ ЛОГИКА 'ЧЕРЕЗ ДЕНЬ' ===")
    print("Липетор (groupOrder=1): принимается в четные дни группы (0, 2, 4, 6...)")
    print("Фубуксусат (groupOrder=2): принимается в нечетные дни группы (1, 3, 5, 7...)")
    print()
    
    for test_date in test_dates:
        days_since_start = (test_date - group_start_date).days
        group_day = days_since_start % 2  # 0 = четный день, 1 = нечетный день
        
        # Определяем, какие лекарства нужно принимать
        lipetor_should_take = group_day == 0  # Липетор в четные дни
        fubuksusat_should_take = group_day == 1  # Фубуксусат в нечетные дни
        
        print(f"Дата: {test_date.strftime('%Y-%m-%d')} (день группы {group_day})")
        print(f"  Липетор (№1): {'✅ ПРИНИМАТЬ' if lipetor_should_take else '❌ НЕ ПРИНИМАТЬ'}")
        print(f"  Фубуксусат (№2): {'✅ ПРИНИМАТЬ' if fubuksusat_should_take else '❌ НЕ ПРИНИМАТЬ'}")
        print()
    
    print("=== ПРОБЛЕМА В ТЕКУЩЕЙ ЛОГИКЕ ===")
    print("Сейчас в коде есть проверка 'wasTakenYesterday', которая усложняет логику:")
    print()
    print("ПРОБЛЕМНЫЙ КОД:")
    print("if (wasTakenYesterday && !shouldTake) {")
    print("    false  // Не показываем")
    print("} else if (wasTakenYesterday && shouldTake) {")
    print("    true   // Показываем")
    print("}")
    print()
    print("ПРОБЛЕМА: Эта логика создает путаницу!")
    print("Если Липетор принят вчера (четный день), а сегодня нечетный день,")
    print("то он не должен показываться - это правильно.")
    print("Но если Фубуксусат принят вчера (нечетный день), а сегодня четный день,")
    print("то он тоже не должен показываться - это тоже правильно.")
    print()
    print("НО: Эта логика усложняет код и создает баги!")
    print()
    
    print("=== ПРАВИЛЬНОЕ РЕШЕНИЕ ===")
    print("Убрать проверку 'wasTakenYesterday' и использовать простую логику:")
    print()
    print("ПРОСТАЯ ЛОГИКА:")
    print("val groupDay = daysSinceStart % 2")
    print("val shouldTake = when {")
    print("    medicine.groupOrder == 1 -> groupDay == 0  // Липетор в четные дни")
    print("    medicine.groupOrder == 2 -> groupDay == 1  // Фубуксусат в нечетные дни")
    print("    else -> false")
    print("}")
    print()
    print("ПРЕИМУЩЕСТВА:")
    print("✅ Простая и понятная логика")
    print("✅ Нет сложных проверок вчерашнего приема")
    print("✅ Каждое лекарство показывается только в свой день")
    print("✅ Легко тестировать и отлаживать")
    print()
    
    print("=== ПРИМЕР РАБОТЫ ===")
    print("День 0 (четный): Липетор ✅, Фубуксусат ❌")
    print("День 1 (нечетный): Липетор ❌, Фубуксусат ✅")
    print("День 2 (четный): Липетор ✅, Фубуксусат ❌")
    print("День 3 (нечетный): Липетор ❌, Фубуксусат ✅")
    print()
    print("Каждое лекарство принимается строго через день, но в разные дни!")

if __name__ == "__main__":
    test_every_other_day_logic() 