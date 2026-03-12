package com.example.batterymonitor

import com.example.batterymonitor.BuildConfig

/**
 * 应用配置统一管理类
 * 集中管理所有配置常量，避免重复定义
 */
object AppConfig {
    // 日志标签
    const val TAG_BATTERY_MONITOR = "BatteryMonitor"
    const val TAG_BATTERY_WORKER = "BatteryCheckWorker"
    const val WORK_TAG = "battery_check_work"
    
    // 电量阈值配置
    const val LOW_BATTERY_THRESHOLD = 50
    
    // SharedPreferences 配置
    const val PREF_NAME = "battery_monitor_prefs"
    const val KEY_LAST_NOTIFY_TIME = "last_notify_time"
    const val MIN_NOTIFY_INTERVAL_MS = 20 * 60 * 1000L // 20分钟提醒间隔
    
    // 飞书 Webhook URL（从 BuildConfig 读取，避免硬编码）
    val FEISHU_WEBHOOK_URL: String
        get() = BuildConfig.FEISHU_WEBHOOK_URL
}