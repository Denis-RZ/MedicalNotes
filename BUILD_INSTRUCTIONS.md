# Инструкции по сборке APK

## Способ 1: Через Android Studio (рекомендуется)

1. Откройте проект в Android Studio
2. Дождитесь синхронизации Gradle
3. Выберите Build -> Build Bundle(s) / APK(s) -> Build APK(s)
4. APK будет создан в папке: `app/build/outputs/apk/debug/app-debug.apk`

## Способ 2: Через командную строку

1. Откройте терминал в папке проекта
2. Выполните команду: `gradlew.bat assembleDebug`
3. APK будет создан в папке: `app/build/outputs/apk/debug/app-debug.apk`

## Способ 3: Через Gradle панель

1. В Android Studio откройте панель Gradle (справа)
2. Разверните app -> Tasks -> build
3. Дважды кликните на assembleDebug

## Установка APK на устройство

1. Включите режим разработчика на Android устройстве
2. Включите отладку по USB
3. Подключите устройство к компьютеру
4. Скопируйте APK файл на устройство
5. Установите APK через файловый менеджер

## Возможные проблемы

- Если сборка не удается, попробуйте File -> Invalidate Caches and Restart
- Убедитесь, что установлен Android SDK
- Проверьте, что Java версии 8 или выше установлена

## Структура проекта

- `app/src/main/java/` - исходный код Kotlin
- `app/src/main/res/` - ресурсы (макеты, строки, изображения)
- `app/src/main/AndroidManifest.xml` - манифест приложения
- `build.gradle` - настройки сборки
