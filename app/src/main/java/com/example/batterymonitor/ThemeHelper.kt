package com.example.batterymonitor

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题管理工具类
 * 用于处理应用的深色/浅色模式切换
 */
object ThemeHelper {
    
    /**
     * 应用主题模式
     * @param context 上下文
     */
    fun applyTheme(context: Context) {
        val themeMode = AppConfig.getThemeMode(context)
        when (themeMode) {
            AppConfig.THEME_MODE_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            AppConfig.THEME_MODE_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                // 跟随系统
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
        }
    }
    
    /**
     * 为Activity设置状态栏和导航栏颜色
     * @param activity Activity实例
     */
    fun setStatusBarAndNavigationBar(activity: Activity) {
        val isDarkMode = isDarkMode(activity)
        val window = activity.window
        val decorView = window.decorView
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            val controller = decorView.windowInsetsController
            if (controller != null) {
                // 设置状态栏图标颜色
                controller.setSystemBarsAppearance(
                    if (isDarkMode) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                // 设置导航栏图标颜色
                controller.setSystemBarsAppearance(
                    if (isDarkMode) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            // Android 10 及以下
            @Suppress("DEPRECATION")
            if (isDarkMode) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                decorView.systemUiVisibility = 0
            }
        }
    }
    
    /**
     * 检查当前是否为深色模式
     * @param context 上下文
     * @return 是否为深色模式
     */
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}