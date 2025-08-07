#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import datetime
import time

def analyze_xml_data():
    print("=== АНАЛИЗ XML ДАННЫХ ===")
    print()
    
    # Данные из XML
    lipetor_data = {
        'id': 1754381301015,
        'name': 'Липетор',
        'startDate': 1754381301006,
        'groupStartDate': 1754451744031,
        'groupOrder': 1,
        'lastTakenTime': 1754473507174,
        'takenToday': False
    }
    
    fubuxusat_data = {
        'id': 1754381353482,
        'name': 'Фубуксусат',
        'startDate': 1754381353472,
        'groupStartDate': 1754451755574,
        'groupOrder': 2,
        'lastTakenTime': 1754471876018,
        'takenToday': False
    }
    
    # Сегодняшняя дата
    today = datetime.datetime.now().date()
    print(f"Сегодняшняя дата: {today}")
    print()
    
    # Анализируем каждое лекарство
    for medicine_data in [lipetor_data, fubuxusat_data]:
        print(f"=== АНАЛИЗ: {medicine_data['name']} ===")
        
        # Конвертируем timestamp в дату
        start_date = datetime.datetime.fromtimestamp(medicine_data['startDate'] / 1000).date()
        group_start_date = datetime.datetime.fromtimestamp(medicine_data['groupStartDate'] / 1000).date()
        last_taken_date = datetime.datetime.fromtimestamp(medicine_data['lastTakenTime'] / 1000).date()
        
        print(f"Дата начала приема: {start_date}")
        print(f"Дата начала группы: {group_start_date}")
        print(f"Последний прием: {last_taken_date}")
        print(f"Порядок в группе: {medicine_data['groupOrder']}")
        print(f"Принято сегодня: {medicine_data['takenToday']}")
        
        # Рассчитываем дни с начала группы
        days_since_group_start = (today - group_start_date).days
        group_day = days_since_group_start % 2  # 0 или 1
        
        print(f"Дней с начала группы: {days_since_group_start}")
        print(f"День группы (0/1): {group_day}")
        
        # Логика "через день"
        should_take = False
        if medicine_data['groupOrder'] == 1:
            should_take = group_day == 0  # Первое лекарство в четные дни
        elif medicine_data['groupOrder'] == 2:
            should_take = group_day == 1  # Второе лекарство в нечетные дни
        
        print(f"Должно приниматься сегодня: {should_take}")
        
        # Проверяем, было ли принято вчера
        yesterday = today - datetime.timedelta(days=1)
        was_taken_yesterday = last_taken_date == yesterday
        print(f"Принято вчера: {was_taken_yesterday}")
        
        # Финальная логика
        final_result = should_take
        if was_taken_yesterday and not should_take:
            final_result = False  # Не показываем, если принято вчера и сегодня не по расписанию
        
        print(f"Финальный результат: {final_result}")
        print()

def analyze_timestamps():
    print("=== АНАЛИЗ TIMESTAMP'ОВ ===")
    print()
    
    # Timestamp'ы из XML
    timestamps = {
        'lipetor_start': 1754381301006,
        'lipetor_group_start': 1754451744031,
        'lipetor_last_taken': 1754473507174,
        'fubuxusat_start': 1754381353472,
        'fubuxusat_group_start': 1754451755574,
        'fubuxusat_last_taken': 1754471876018,
        'updated_at': 1754540857591
    }
    
    for name, ts in timestamps.items():
        dt = datetime.datetime.fromtimestamp(ts / 1000)
        print(f"{name}: {dt.strftime('%Y-%m-%d %H:%M:%S')}")

def main():
    print("🔍 АНАЛИЗ ПРОБЛЕМЫ С ОТОБРАЖЕНИЕМ ЛЕКАРСТВ")
    print("=" * 50)
    print()
    
    analyze_timestamps()
    print()
    analyze_xml_data()
    
    print("=== ВЫВОДЫ ===")
    print("1. Проблема в групповой логике 'через день'")
    print("2. Лекарства имеют разные groupStartDate")
    print("3. Возможно, проблема в том, что при редактировании времени")
    print("   сбрасывается или изменяется логика группового расчета")
    print("4. Нужно проверить, не изменяется ли startDate при редактировании")

if __name__ == "__main__":
    main() 