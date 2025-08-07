#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
import time

def parse_timestamp(timestamp_ms):
    """Convert milliseconds timestamp to datetime"""
    return datetime.fromtimestamp(timestamp_ms / 1000)

def analyze_medicine_groups():
    """Analyze the medicine groups and their logic"""
    
    # Parse the test data
    tree = ET.parse('test_data.xml')
    root = tree.getroot()
    
    medicines = []
    for medicine_elem in root.findall('medicine'):
        medicine = {
            'id': medicine_elem.find('id').text,
            'name': medicine_elem.find('name').text,
            'groupId': medicine_elem.find('groupId').text,
            'groupName': medicine_elem.find('groupName').text,
            'groupOrder': int(medicine_elem.find('groupOrder').text),
            'groupStartDate': int(medicine_elem.find('groupStartDate').text),
            'groupFrequency': medicine_elem.find('groupFrequency').text,
            'lastTakenTime': int(medicine_elem.find('lastTakenTime').text),
            'startDate': int(medicine_elem.find('startDate').text)
        }
        medicines.append(medicine)
    
    print("=== АНАЛИЗ ГРУПП ЛЕКАРСТВ ===")
    print()
    
    # Group medicines by groupId
    groups = {}
    for medicine in medicines:
        group_id = medicine['groupId']
        if group_id not in groups:
            groups[group_id] = []
        groups[group_id].append(medicine)
    
    print(f"Найдено групп: {len(groups)}")
    print()
    
    for group_id, group_medicines in groups.items():
        print(f"=== ГРУППА {group_id} ===")
        print(f"Название: {group_medicines[0]['groupName']}")
        print(f"Частота: {group_medicines[0]['groupFrequency']}")
        print(f"Дата начала группы: {parse_timestamp(group_medicines[0]['groupStartDate'])}")
        print()
        
        for medicine in group_medicines:
            print(f"  Лекарство: {medicine['name']}")
            print(f"    Порядок в группе: {medicine['groupOrder']}")
            print(f"    Дата начала: {parse_timestamp(medicine['startDate'])}")
            print(f"    Последний прием: {parse_timestamp(medicine['lastTakenTime'])}")
            print()
    
    print("=== ПРОБЛЕМА ===")
    print("Липетор и Фубуксусат находятся в РАЗНЫХ группах!")
    print("Это означает, что они не синхронизированы для логики 'через день'")
    print()
    
    # Test the logic for specific dates
    test_dates = [
        datetime(2025, 8, 6),
        datetime(2025, 8, 8), 
        datetime(2025, 8, 9),
        datetime(2025, 8, 11),
        datetime(2025, 8, 12),
        datetime(2025, 8, 13)
    ]
    
    print("=== ТЕСТИРОВАНИЕ ЛОГИКИ ПО ДАТАМ ===")
    for test_date in test_dates:
        print(f"\nДата: {test_date.strftime('%Y-%m-%d')}")
        
        for medicine in medicines:
            # Convert group start date to datetime
            group_start = parse_timestamp(medicine['groupStartDate'])
            
            # Calculate days since group start
            days_since_start = (test_date - group_start).days
            
            # For EVERY_OTHER_DAY logic
            if medicine['groupFrequency'] == 'EVERY_OTHER_DAY':
                group_day = days_since_start % 2
                should_take = False
                
                if medicine['groupOrder'] == 1:
                    should_take = group_day == 0  # First medicine on even days (0, 2, 4...)
                elif medicine['groupOrder'] == 2:
                    should_take = group_day == 1  # Second medicine on odd days (1, 3, 5...)
                
                print(f"  {medicine['name']}: день группы {group_day}, порядок {medicine['groupOrder']}, принимать: {should_take}")
    
    print("\n=== РЕШЕНИЕ ===")
    print("Нужно объединить лекарства в одну группу с одинаковой датой начала!")
    print("Липетор и Фубуксусат должны иметь:")
    print("- Одинаковый groupId")
    print("- Одинаковый groupStartDate") 
    print("- Разные groupOrder (1 и 2)")

if __name__ == "__main__":
    analyze_medicine_groups() 