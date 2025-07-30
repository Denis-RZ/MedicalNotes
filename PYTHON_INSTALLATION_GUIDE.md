# Инструкция по установке Python

## Способ 1: Через официальный сайт (Рекомендуется)

1. **Перейдите на официальный сайт Python:**
   - Откройте браузер и перейдите по ссылке: https://www.python.org/downloads/

2. **Скачайте последнюю версию Python:**
   - На главной странице нажмите большую желтую кнопку "Download Python 3.12.x"
   - Или перейдите в раздел "Downloads" и выберите "Windows"

3. **Запустите установщик:**
   - Найдите скачанный файл (обычно в папке Downloads)
   - Запустите файл `python-3.12.x-amd64.exe`

4. **Настройте установку:**
   - **ВАЖНО:** Поставьте галочку "Add Python to PATH"
   - Выберите "Install Now" для стандартной установки
   - Или "Customize installation" для настройки пути установки

5. **Завершите установку:**
   - Дождитесь завершения установки
   - Нажмите "Close"

## Способ 2: Через Microsoft Store

1. **Откройте Microsoft Store:**
   - Нажмите Win + S и введите "Microsoft Store"
   - Или найдите в меню Пуск

2. **Найдите Python:**
   - В поиске введите "Python"
   - Выберите "Python 3.12" от Python Software Foundation

3. **Установите:**
   - Нажмите "Get" или "Install"
   - Дождитесь завершения установки

## Проверка установки

После установки откройте новое окно командной строки или PowerShell и выполните:

```bash
python --version
```

Должна появиться версия Python, например: `Python 3.12.0`

## Если Python не найден в командной строке

1. **Проверьте переменную PATH:**
   - Нажмите Win + R, введите `sysdm.cpl`
   - Перейдите на вкладку "Дополнительно"
   - Нажмите "Переменные среды"
   - В разделе "Системные переменные" найдите "Path"
   - Проверьте, есть ли там путь к Python (обычно `C:\Users\[USERNAME]\AppData\Local\Programs\Python\Python312\` и `C:\Users\[USERNAME]\AppData\Local\Programs\Python\Python312\Scripts\`)

2. **Добавьте Python в PATH вручную:**
   - Если пути нет, добавьте его в переменную PATH
   - Путь обычно: `C:\Users\[USERNAME]\AppData\Local\Programs\Python\Python312\`
   - И: `C:\Users\[USERNAME]\AppData\Local\Programs\Python\Python312\Scripts\`

## Альтернативные способы

### Через Chocolatey (если установлен):
```bash
choco install python
```

### Через Scoop (если установлен):
```bash
scoop install python
```

## После установки

После успешной установки Python вы сможете:
- Запускать Python скрипты
- Использовать pip для установки пакетов
- Работать с Python в вашем проекте MedicalNotes

## Полезные команды

```bash
# Проверка версии Python
python --version

# Проверка версии pip
pip --version

# Установка пакета
pip install package_name

# Обновление pip
python -m pip install --upgrade pip
``` 