#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
from datetime import datetime
import time

def fix_group_inconsistencies():
    """Fix group inconsistencies by merging medicines with same group name"""
    
    # Parse the test data
    tree = ET.parse('test_data.xml')
    root = tree.getroot()
    
    medicines = []
    for medicine_elem in root.findall('medicine'):
        medicine = {
            'element': medicine_elem,
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
    
    print("=== ИСПРАВЛЕНИЕ ГРУППОВЫХ НЕСОГЛАСОВАННОСТЕЙ ===")
    print()
    
    # Group medicines by groupName
    groups_by_name = {}
    for medicine in medicines:
        group_name = medicine['groupName']
        if group_name not in groups_by_name:
            groups_by_name[group_name] = []
        groups_by_name[group_name].append(medicine)
    
    print(f"Найдено групп по названию: {len(groups_by_name)}")
    
    # Find groups that need fixing (same name but different IDs)
    groups_to_fix = {}
    for group_name, group_medicines in groups_by_name.items():
        unique_group_ids = set(medicine['groupId'] for medicine in group_medicines)
        if len(unique_group_ids) > 1:
            groups_to_fix[group_name] = group_medicines
            print(f"Группа '{group_name}' имеет {len(unique_group_ids)} разных ID: {unique_group_ids}")
    
    if not groups_to_fix:
        print("Несоответствий не найдено!")
        return
    
    print()
    print("=== ИСПРАВЛЕНИЕ ===")
    
    for group_name, group_medicines in groups_to_fix.items():
        print(f"Исправляем группу: {group_name}")
        
        # Use the earliest start date as the common group start date
        earliest_start = min(medicine['groupStartDate'] for medicine in group_medicines)
        earliest_start_date = datetime.fromtimestamp(earliest_start / 1000)
        print(f"  Общая дата начала группы: {earliest_start_date}")
        
        # Use the first group ID as the common ID
        common_group_id = group_medicines[0]['groupId']
        print(f"  Общий ID группы: {common_group_id}")
        
        # Reorder medicines by their original groupOrder
        sorted_medicines = sorted(group_medicines, key=lambda m: m['groupOrder'])
        
        # Update each medicine
        for i, medicine in enumerate(sorted_medicines, 1):
            print(f"  {medicine['name']}: порядок {medicine['groupOrder']} -> {i}")
            
            # Update the XML elements
            medicine['element'].find('groupId').text = common_group_id
            medicine['element'].find('groupStartDate').text = str(earliest_start)
            medicine['element'].find('groupOrder').text = str(i)
            
            # Update the medicine dict for verification
            medicine['groupId'] = common_group_id
            medicine['groupStartDate'] = earliest_start
            medicine['groupOrder'] = i
    
    # Save the fixed data
    tree.write('test_data_fixed.xml', encoding='UTF-8', xml_declaration=True)
    print()
    print("Исправленные данные сохранены в test_data_fixed.xml")
    
    # Verify the fix
    print()
    print("=== ПРОВЕРКА ИСПРАВЛЕНИЯ ===")
    
    # Re-parse the fixed data
    tree_fixed = ET.parse('test_data_fixed.xml')
    root_fixed = tree_fixed.getroot()
    
    medicines_fixed = []
    for medicine_elem in root_fixed.findall('medicine'):
        medicine = {
            'id': medicine_elem.find('id').text,
            'name': medicine_elem.find('name').text,
            'groupId': medicine_elem.find('groupId').text,
            'groupName': medicine_elem.find('groupName').text,
            'groupOrder': int(medicine_elem.find('groupOrder').text),
            'groupStartDate': int(medicine_elem.find('groupStartDate').text),
            'groupFrequency': medicine_elem.find('groupFrequency').text,
        }
        medicines_fixed.append(medicine)
    
    # Group by groupId to verify
    groups_fixed = {}
    for medicine in medicines_fixed:
        group_id = medicine['groupId']
        if group_id not in groups_fixed:
            groups_fixed[group_id] = []
        groups_fixed[group_id].append(medicine)
    
    print(f"После исправления групп: {len(groups_fixed)}")
    
    for group_id, group_medicines in groups_fixed.items():
        print(f"Группа {group_id}:")
        for medicine in group_medicines:
            print(f"  {medicine['name']} (порядок {medicine['groupOrder']})")
    
    # Test the logic for specific dates
    test_dates = [
        datetime(2025, 8, 6),
        datetime(2025, 8, 8), 
        datetime(2025, 8, 9),
        datetime(2025, 8, 11),
        datetime(2025, 8, 12),
        datetime(2025, 8, 13)
    ]
    
    print()
    print("=== ТЕСТИРОВАНИЕ ИСПРАВЛЕННОЙ ЛОГИКИ ===")
    for test_date in test_dates:
        print(f"\nДата: {test_date.strftime('%Y-%m-%d')}")
        
        for medicine in medicines_fixed:
            # Convert group start date to datetime
            group_start = datetime.fromtimestamp(medicine['groupStartDate'] / 1000)
            
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

if __name__ == "__main__":
    fix_group_inconsistencies() 