#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import datetime

def test_fix():
    print("=== ТЕСТ ИСПРАВЛЕНИЯ ===")
    print()
    
    # Данные из XML (после исправления)
    lipetor_data = {
        'name': 'Липетор',
        'groupStartDate': 1754451744031,
        'groupOrder': 1,
        'lastTakenTime': 0,  # Сброшено при изменении времени
        'takenToday': False
    }
    
    fubuxusat_data = {
        'name': 'Фубуксусат',
        'groupStartDate': 1754451755574,
        'groupOrder': 2,
        'lastTakenTime': 0,  # Сброшено при изменении времени
        'takenToday': False
    }
    
    today = datetime.datetime.now().date()
    print(f"Сегодняшняя дата: {today}")
    print()
    
    for medicine_data in [lipetor_data, fubuxusat_data]:
        print(f"=== ТЕСТ: {medicine_data['name']} ===")
        
        group_start_date = datetime.datetime.fromtimestamp(medicine_data['groupStartDate'] / 1000).date()
        days_since_group_start = (today - group_start_date).days
        group_day = days_since_group_start % 2
        
        print(f"Дата начала группы: {group_start_date}")
        print(f"Дней с начала группы: {days_since_group_start}")
        print(f"День группы (0/1): {group_day}")
        
        # Логика "через день"
        should_take = False
        if medicine_data['groupOrder'] == 1:
            should_take = group_day == 0
        elif medicine_data['groupOrder'] == 2:
            should_take = group_day == 1
        
        print(f"Должно приниматься сегодня: {should_take}")
        
        # Проверяем, было ли принято вчера (теперь lastTakenTime = 0)
        yesterday = today - datetime.timedelta(days=1)
        was_taken_yesterday = False  # lastTakenTime сброшен
        print(f"Принято вчера: {was_taken_yesterday}")
        
        # Финальная логика
        final_result = should_take
        if was_taken_yesterday and not should_take:
            final_result = False
        
        print(f"Финальный результат: {final_result}")
        print(f"Будет показано в списке: {final_result}")
        print()

def main():
    print("🔧 ТЕСТ ИСПРАВЛЕНИЯ ПРОБЛЕМЫ")
    print("=" * 40)
    print()
    
    test_fix()
    
    print("=== ОЖИДАЕМЫЙ РЕЗУЛЬТАТ ===")
    print("После исправления:")
    print("1. lastTakenTime сбрасывается при изменении времени")
    print("2. wasTakenYesterday = false")
    print("3. Лекарства показываются согласно групповой логике")
    print("4. Липетор (порядок 1) в четные дни группы")
    print("5. Фубуксусат (порядок 2) в нечетные дни группы")

if __name__ == "__main__":
    main() 