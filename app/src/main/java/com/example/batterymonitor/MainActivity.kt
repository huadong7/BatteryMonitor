package com.example.batterymonitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = AppConfig.TAG_BATTERY_MONITOR
    }

    private lateinit var tvBatteryStatus: TextView
    private lateinit var tvLastNotify: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBatteryStatus = findViewById(R.id.tv_battery_status)
        tvLastNotify = findViewById(R.id.tv_last_notify)

        // 显示当前电量
        updateBatteryStatus()

        // 请求忽略电池优化（确保后台任务运行）
        requestIgnoreBatteryOptimization()

        // 启动后台定时任务
        startBatteryMonitorWorker()
    }

    /**
     * 获取当前电量百分比
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * 更新界面显示
     */
    private fun updateBatteryStatus() {
        val level = getBatteryLevel()
        tvBatteryStatus.text = "当前电量: $level%"
        
        // 获取上次通知时间
        val prefs = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        val lastNotifyTime = prefs.getLong(AppConfig.KEY_LAST_NOTIFY_TIME, 0)
        tvLastNotify.text = if (lastNotifyTime > 0) {
            "上次通知: ${formatTime(lastNotifyTime)}"
        } else {
            "上次通知: 暂无"
        }
    }

    /**
     * 请求忽略电池优化
     */
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    /**
     * 启动WorkManager后台任务
     */
    private fun startBatteryMonitorWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
            .build()

        // 每15分钟执行一次
        val workRequest = PeriodicWorkRequestBuilder<BatteryCheckWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(AppConfig.WORK_TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AppConfig.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Log.d(TAG, "Battery monitor worker started, checking every 15 minutes")
        Toast.makeText(this, "电量监控已启动，每15分钟检测一次电量", Toast.LENGTH_SHORT).show()
    }

    /**
     * 格式化时间
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

/**
 * BatteryCheckWorker - 后台电量检查任务
 */
class BatteryCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = AppConfig.TAG_BATTERY_WORKER
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Battery check worker running...")

        // 获取当前电量
        val batteryLevel = getBatteryLevel()
        Log.d(TAG, "Current battery level: $batteryLevel%")

        // 检查是否低于阈值
        if (batteryLevel < AppConfig.LOW_BATTERY_THRESHOLD) {
            Log.d(TAG, "Battery low, checking cooldown...")
            
            // 检查是否在冷却时间内（防重复）
            if (canSendNotification()) {
                val success = sendToFeishu(batteryLevel)
                if (success) {
                    recordNotificationTime()
                    Log.d(TAG, "Notification sent successfully, next notification in 15 minutes")
                } else {
                    Log.e(TAG, "Failed to send notification")
                }
            } else {
                Log.d(TAG, "In cooldown period (15 minutes), skip notification")
            }
        } else {
            Log.d(TAG, "Battery level OK, no action needed")
        }

        return Result.success()
    }

    /**
     * 获取当前电量
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * 检查是否可以发送通知（防重复）
     */
    private fun canSendNotification(): Boolean {
        val prefs = applicationContext.getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        val lastNotifyTime = prefs.getLong(AppConfig.KEY_LAST_NOTIFY_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - lastNotifyTime) >= AppConfig.MIN_NOTIFY_INTERVAL_MS
    }

    /**
     * 记录通知发送时间
     */
    private fun recordNotificationTime() {
        val prefs = applicationContext.getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(AppConfig.KEY_LAST_NOTIFY_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * 发送消息到飞书
     */
    private fun sendToFeishu(batteryLevel: Int): Boolean {
        val webhookUrl = AppConfig.FEISHU_WEBHOOK_URL
        
        // 获取设备名称
        val deviceName = android.os.Build.MODEL
        
        // 计算下次检测时间（15分钟后）
        val nextCheckTime = System.currentTimeMillis() + 15 * 60 * 1000
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val nextCheckTimeStr = sdf.format(java.util.Date(nextCheckTime))
        
        // 飞书消息格式
        val jsonBody = JSONObject().apply {
            put("msg_type", "text")
            put("content", JSONObject().apply {
                put("text", "🔋 电量警报：\n" +
                        "设备名称：$deviceName\n" +
                        "当前电量：$batteryLevel%，低于${AppConfig.LOW_BATTERY_THRESHOLD}%！请及时充电\n" +
                        "下次检测：$nextCheckTimeStr")
            })
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d(TAG, "Feishu response: $responseBody")
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to Feishu: ${e.message}")
            false
        }
    }
}