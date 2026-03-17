package com.example.batterymonitor

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etDeviceName: EditText
    private lateinit var sbBatteryThreshold: SeekBar
    private lateinit var tvBatteryThresholdValue: TextView
    private lateinit var btnSave: Button
    private lateinit var rgThemeMode: android.widget.RadioGroup
    private lateinit var rbThemeSystem: android.widget.RadioButton
    private lateinit var rbThemeLight: android.widget.RadioButton
    private lateinit var rbThemeDark: android.widget.RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 应用主题
        ThemeHelper.applyTheme(this)
        
        setContentView(R.layout.activity_settings)

        etDeviceName = findViewById(R.id.et_device_name)
        sbBatteryThreshold = findViewById(R.id.sb_battery_threshold)
        tvBatteryThresholdValue = findViewById(R.id.tv_battery_threshold_value)
        btnSave = findViewById(R.id.btn_save)
        rgThemeMode = findViewById(R.id.rg_theme_mode)
        rbThemeSystem = findViewById(R.id.rb_theme_system)
        rbThemeLight = findViewById(R.id.rb_theme_light)
        rbThemeDark = findViewById(R.id.rb_theme_dark)
        
        // 设置状态栏和导航栏颜色
        ThemeHelper.setStatusBarAndNavigationBar(this)

        // 加载当前设置
        loadSettings()

        // 监听滑块变化
        sbBatteryThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBatteryThresholdValue.text = "${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 保存按钮点击事件
        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        
        // 加载设备名称
        val deviceName = prefs.getString(AppConfig.KEY_DEVICE_NAME, "")
        etDeviceName.setText(deviceName)
        
        // 加载电量阈值
        val threshold = prefs.getInt(AppConfig.KEY_BATTERY_THRESHOLD, AppConfig.DEFAULT_LOW_BATTERY_THRESHOLD)
        sbBatteryThreshold.progress = threshold
        tvBatteryThresholdValue.text = "${threshold}%"
        
        // 加载主题模式
        val themeMode = AppConfig.getThemeMode(this)
        when (themeMode) {
            AppConfig.THEME_MODE_SYSTEM -> rbThemeSystem.isChecked = true
            AppConfig.THEME_MODE_LIGHT -> rbThemeLight.isChecked = true
            AppConfig.THEME_MODE_DARK -> rbThemeDark.isChecked = true
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 保存设备名称
        val deviceName = etDeviceName.text.toString().trim()
        editor.putString(AppConfig.KEY_DEVICE_NAME, deviceName)
        
        // 保存电量阈值
        val threshold = sbBatteryThreshold.progress
        editor.putInt(AppConfig.KEY_BATTERY_THRESHOLD, threshold)
        
        // 保存主题模式
        val themeMode = when (rgThemeMode.checkedRadioButtonId) {
            R.id.rb_theme_system -> AppConfig.THEME_MODE_SYSTEM
            R.id.rb_theme_light -> AppConfig.THEME_MODE_LIGHT
            R.id.rb_theme_dark -> AppConfig.THEME_MODE_DARK
            else -> AppConfig.THEME_MODE_SYSTEM
        }
        AppConfig.setThemeMode(this, themeMode)
        
        editor.apply()
        
        // 应用新主题
        ThemeHelper.applyTheme(this)
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}