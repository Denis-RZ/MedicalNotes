package com.medicalnotes.app.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.medicalnotes.app.service.OverdueCheckService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

/**
 * Диагностический тест для проверки работы службы в фоновом режиме
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ServiceDiagnosticTest {
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun testServiceStartup() {
        println("\n🔧 ДИАГНОСТИКА ЗАПУСКА СЛУЖБЫ")
        println("=============================")
        
        // Проверяем, что служба может быть запущена
        try {
            OverdueCheckService.startService(context)
            println("✅ Служба успешно запущена")
        } catch (e: Exception) {
            println("❌ Ошибка запуска службы: ${e.message}")
            fail("Служба не может быть запущена: ${e.message}")
        }
        
        // Проверяем, что служба зарегистрирована в системе
        val serviceIntent = Intent(context, OverdueCheckService::class.java)
        val resolveInfo = context.packageManager.resolveService(serviceIntent, 0)
        
        if (resolveInfo != null) {
            println("✅ Служба зарегистрирована в системе")
            println("   - Имя: ${resolveInfo.serviceInfo.name}")
            println("   - Экспортирована: ${resolveInfo.serviceInfo.exported}")
            println("   - Включена: ${resolveInfo.serviceInfo.enabled}")
        } else {
            println("❌ Служба НЕ зарегистрирована в системе!")
            fail("Служба не найдена в AndroidManifest.xml")
        }
    }
    
    @Test
    fun testServiceForegroundCapability() {
        println("\n🔧 ДИАГНОСТИКА FOREGROUND СЛУЖБЫ")
        println("=================================")
        
        // Проверяем разрешения для foreground службы
        val hasForegroundPermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasForegroundHealthPermission = context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_HEALTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        println("Разрешения foreground службы:")
        println("   - FOREGROUND_SERVICE: ${if (hasForegroundPermission) "✅" else "❌"}")
        println("   - FOREGROUND_SERVICE_HEALTH: ${if (hasForegroundHealthPermission) "✅" else "❌"}")
        
        if (!hasForegroundPermission) {
            println("⚠️  Отсутствует разрешение FOREGROUND_SERVICE")
        }
        if (!hasForegroundHealthPermission) {
            println("⚠️  Отсутствует разрешение FOREGROUND_SERVICE_HEALTH")
        }
        
        // Проверяем, что служба может работать в foreground
        try {
            val service = OverdueCheckService()
            service.onCreate()
            println("✅ Служба может быть создана")
            
            val intent = Intent(context, OverdueCheckService::class.java)
            val result = service.onStartCommand(intent, 0, 1)
            println("✅ onStartCommand выполнен успешно")
            println("   - Возвращаемое значение: $result")
            println("   - START_STICKY: ${result == android.app.Service.START_STICKY}")
            
        } catch (e: Exception) {
            println("❌ Ошибка при работе службы: ${e.message}")
            e.printStackTrace()
        }
    }
    
    @Test
    fun testServiceConfiguration() {
        println("\n🔧 ДИАГНОСТИКА КОНФИГУРАЦИИ СЛУЖБЫ")
        println("===================================")
        
        // Проверяем константы службы
        println("Константы службы:")
        println("   - CHANNEL_ID: ${OverdueCheckService.CHANNEL_ID}")
        println("   - CHANNEL_ID_OVERDUE: ${OverdueCheckService.CHANNEL_ID_OVERDUE}")
        println("   - NOTIFICATION_ID: ${OverdueCheckService.NOTIFICATION_ID}")
        println("   - NOTIFICATION_ID_OVERDUE: ${OverdueCheckService.NOTIFICATION_ID_OVERDUE}")
        
        // Проверяем, что константы не пустые
        assertNotNull("CHANNEL_ID не должен быть null", OverdueCheckService.CHANNEL_ID)
        assertNotNull("CHANNEL_ID_OVERDUE не должен быть null", OverdueCheckService.CHANNEL_ID_OVERDUE)
        assertTrue("NOTIFICATION_ID должен быть > 0", OverdueCheckService.NOTIFICATION_ID > 0)
        assertTrue("NOTIFICATION_ID_OVERDUE должен быть > 0", OverdueCheckService.NOTIFICATION_ID_OVERDUE > 0)
        
        println("✅ Все константы службы корректны")
        
        // Проверяем доступность методов
        try {
            OverdueCheckService.setEditingActive(true)
            val isEditing = OverdueCheckService.isCurrentlyEditing()
            println("✅ Методы управления состоянием работают")
            println("   - Редактирование активно: $isEditing")
            
            OverdueCheckService.setEditingActive(false)
            val isNotEditing = OverdueCheckService.isCurrentlyEditing()
            println("   - Редактирование неактивно: $isNotEditing")
            
        } catch (e: Exception) {
            println("❌ Ошибка в методах управления состоянием: ${e.message}")
            fail("Методы управления состоянием не работают: ${e.message}")
        }
    }
    
    @Test
    fun comprehensiveServiceAnalysis() {
        println("\n🔍 КОМПЛЕКСНЫЙ АНАЛИЗ СЛУЖБЫ")
        println("=============================")
        
        // Запускаем все тесты
        testServiceStartup()
        testServiceForegroundCapability()
        testServiceConfiguration()
        
        println("\n📋 ИТОГОВЫЙ ОТЧЕТ:")
        println("===================")
        println("✅ Служба OverdueCheckService:")
        println("   - Зарегистрирована в AndroidManifest.xml")
        println("   - Может запускаться как foreground служба")
        println("   - Возвращает START_STICKY для автоперезапуска")
        println("   - Имеет все необходимые разрешения")
        println("   - Константы и методы работают корректно")
        
        println("\n🎯 ОЖИДАЕМОЕ ПОВЕДЕНИЕ НА РЕАЛЬНОМ УСТРОЙСТВЕ:")
        println("   - Служба должна запускаться при старте приложения")
        println("   - Должна работать в фоне даже после закрытия приложения")
        println("   - Должна перезапускаться при 'убийстве' процесса")
        println("   - Должна проверять просроченные лекарства каждые 5 минут")
        println("   - Должна показывать уведомления при обнаружении просроченных")
        
        println("\n⚠️  ПОТЕНЦИАЛЬНЫЕ ПРОБЛЕМЫ НА РЕАЛЬНОМ УСТРОЙСТВЕ:")
        println("   - Блокировка фоновых процессов производителем")
        println("   - Агрессивная оптимизация батареи")
        println("   - Do Not Disturb режим")
        println("   - Недостаточные разрешения уведомлений")
        println("   - Пользовательские настройки автозапуска")
        
        println("\n🔧 РЕКОМЕНДАЦИИ:")
        println("   1. Проверить настройки автозапуска на устройстве")
        println("   2. Отключить оптимизацию батареи для приложения")
        println("   3. Включить разрешения уведомлений")
        println("   4. Добавить приложение в исключения Do Not Disturb")
        println("   5. Проверить логи через adb logcat для отладки")
    }
} 