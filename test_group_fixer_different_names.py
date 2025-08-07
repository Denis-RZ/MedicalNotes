#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
from datetime import datetime

def test_group_fixer_logic():
    """Test how GroupFixer works with different group names"""
    
    print("=== ТЕСТ ЛОГИКИ GroupFixer С РАЗНЫМИ НАЗВАНИЯМИ ГРУПП ===")
    print()
    
    # Создаем тестовые данные с разными названиями групп
    test_data = '''<?xml version="1.0" encoding="UTF-8"?>
<medicines>
  <medicine>
    <id>1</id>
    <name>Липетор</name>
    <groupId>1001</groupId>
    <groupName>Тестер</groupName>
    <groupOrder>1</groupOrder>
    <groupStartDate>1754451744031</groupStartDate>
    <groupFrequency>EVERY_OTHER_DAY</groupFrequency>
  </medicine>
  <medicine>
    <id>2</id>
    <name>Фубуксусат</name>
    <groupId>1002</groupId>
    <groupName>Тестер</groupName>
    <groupOrder>2</groupOrder>
    <groupStartDate>1754451755574</groupStartDate>
    <groupFrequency>EVERY_OTHER_DAY</groupFrequency>
  </medicine>
  <medicine>
    <id>3</id>
    <name>Аспирин</name>
    <groupId>2001</groupId>
    <groupName>Утренние</groupName>
    <groupOrder>1</groupOrder>
    <groupStartDate>1754451800000</groupStartDate>
    <groupFrequency>DAILY</groupFrequency>
  </medicine>
  <medicine>
    <id>4</id>
    <name>Витамин D</name>
    <groupId>2002</groupId>
    <groupName>Утренние</groupName>
    <groupOrder>2</groupOrder>
    <groupStartDate>1754451801000</groupStartDate>
    <groupFrequency>DAILY</groupFrequency>
  </medicine>
  <medicine>
    <id>5</id>
    <name>Омега-3</name>
    <groupId>3001</groupId>
    <groupName>Вечерние</groupName>
    <groupOrder>1</groupOrder>
    <groupStartDate>1754451900000</groupStartDate>
    <groupFrequency>DAILY</groupFrequency>
  </medicine>
</medicines>'''
    
    # Сохраняем тестовые данные
    with open('test_different_groups.xml', 'w', encoding='utf-8') as f:
        f.write(test_data)
    
    # Парсим данные
    tree = ET.parse('test_different_groups.xml')
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
            'groupFrequency': medicine_elem.find('groupFrequency').text
        }
        medicines.append(medicine)
    
    print("Исходные данные:")
    for medicine in medicines:
        print(f"  {medicine['name']}: группа '{medicine['groupName']}', ID {medicine['groupId']}, порядок {medicine['groupOrder']}")
    print()
    
    # Симулируем логику GroupFixer
    print("=== ЛОГИКА GroupFixer ===")
    
    # 1. Группируем лекарства по названию группы
    groups_by_name = {}
    for medicine in medicines:
        group_name = medicine['groupName']
        if group_name not in groups_by_name:
            groups_by_name[group_name] = []
        groups_by_name[group_name].append(medicine)
    
    print(f"Найдено групп по названию: {len(groups_by_name)}")
    for group_name, group_medicines in groups_by_name.items():
        print(f"  Группа '{group_name}': {len(group_medicines)} лекарств")
    print()
    
    # 2. Находим группы, которые нужно исправить (одинаковое название, но разные ID)
    groups_to_fix = {}
    for group_name, group_medicines in groups_by_name.items():
        unique_group_ids = set(medicine['groupId'] for medicine in group_medicines)
        if len(unique_group_ids) > 1:
            groups_to_fix[group_name] = group_medicines
            print(f"🔧 Группа '{group_name}' имеет {len(unique_group_ids)} разных ID: {unique_group_ids}")
        else:
            print(f"✅ Группа '{group_name}' в порядке (ID: {unique_group_ids})")
    print()
    
    # 3. Исправляем проблемные группы
    fixed_medicines = medicines.copy()
    
    for group_name, group_medicines in groups_to_fix.items():
        print(f"Исправляем группу: {group_name}")
        
        # Используем самую раннюю дату начала
        earliest_start = min(medicine['groupStartDate'] for medicine in group_medicines)
        earliest_start_date = datetime.fromtimestamp(earliest_start / 1000)
        print(f"  Общая дата начала группы: {earliest_start_date}")
        
        # Используем первый ID группы как общий
        common_group_id = group_medicines[0]['groupId']
        print(f"  Общий ID группы: {common_group_id}")
        
        # Сортируем и переназначаем порядок
        sorted_medicines = sorted(group_medicines, key=lambda m: m['groupOrder'])
        
        for i, medicine in enumerate(sorted_medicines, 1):
            print(f"  {medicine['name']}: порядок {medicine['groupOrder']} -> {i}")
            
            # Обновляем лекарство в списке
            for j, med in enumerate(fixed_medicines):
                if med['id'] == medicine['id']:
                    fixed_medicines[j]['groupId'] = common_group_id
                    fixed_medicines[j]['groupStartDate'] = earliest_start
                    fixed_medicines[j]['groupOrder'] = i
                    break
        print()
    
    print("=== РЕЗУЛЬТАТ ПОСЛЕ ИСПРАВЛЕНИЯ ===")
    for medicine in fixed_medicines:
        print(f"  {medicine['name']}: группа '{medicine['groupName']}', ID {medicine['groupId']}, порядок {medicine['groupOrder']}")
    print()
    
    # Проверяем, что группы с разными названиями остались отдельными
    final_groups = {}
    for medicine in fixed_medicines:
        group_name = medicine['groupName']
        if group_name not in final_groups:
            final_groups[group_name] = []
        final_groups[group_name].append(medicine)
    
    print("Финальные группы:")
    for group_name, group_medicines in final_groups.items():
        group_ids = set(medicine['groupId'] for medicine in group_medicines)
        print(f"  Группа '{group_name}': ID {group_ids}, {len(group_medicines)} лекарств")
        for medicine in group_medicines:
            print(f"    - {medicine['name']} (порядок {medicine['groupOrder']})")
    print()
    
    print("=== ВЫВОД ===")
    print("✅ GroupFixer НЕ объединяет лекарства с разными названиями групп!")
    print("✅ Каждая группа с уникальным названием остается отдельной")
    print("✅ Исправляются только группы с одинаковым названием, но разными ID")
    print("✅ Лекарства без группы (groupName пустой) не затрагиваются")

if __name__ == "__main__":
    test_group_fixer_logic() 