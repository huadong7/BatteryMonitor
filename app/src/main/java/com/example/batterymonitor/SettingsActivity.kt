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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etDeviceName = findViewById(R.id.et_device_name)
        sbBatteryThreshold = findViewById(R.id.sb_battery_threshold)
        tvBatteryThresholdValue = findViewById(R.id.tv_battery_threshold_value)
        btnSave = findViewById(R.id.btn_save)

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
        
        editor.apply()
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}