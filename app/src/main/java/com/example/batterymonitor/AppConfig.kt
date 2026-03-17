package com.example.batterymonitor

import android.content.Context
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
    
    // 电量阈值默认值
    const val DEFAULT_LOW_BATTERY_THRESHOLD = 50
    
    // SharedPreferences 配置
    const val PREF_NAME = "battery_monitor_prefs"
    const val KEY_LAST_NOTIFY_TIME = "last_notify_time"
    const val KEY_DEVICE_NAME = "device_name"
    const val KEY_BATTERY_THRESHOLD = "battery_threshold"
    const val MIN_NOTIFY_INTERVAL_MS = 20 * 60 * 1000L // 20分钟提醒间隔
    
    // 飞书 Webhook URL（从 BuildConfig 读取，避免硬编码）
    val FEISHU_WEBHOOK_URL: String
        get() = BuildConfig.FEISHU_WEBHOOK_URL
    
    // 动态获取电量阈值
    fun getBatteryThreshold(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BATTERY_THRESHOLD, DEFAULT_LOW_BATTERY_THRESHOLD)
    }
    
    // 动态获取设备名称
    fun getDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_DEVICE_NAME, "")
        return if (savedName.isNullOrEmpty()) {
            android.os.Build.MODEL
        } else {
            savedName
        }
    }
    
    // 主题模式配置
    const val KEY_THEME_MODE = "theme_mode"
    const val THEME_MODE_SYSTEM = "system"
    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"
    
    // 获取当前主题模式
    fun getThemeMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME_MODE, THEME_MODE_SYSTEM) ?: THEME_MODE_SYSTEM
    }
    
    // 设置主题模式
    fun setThemeMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}