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
        
        // 应用主题
        ThemeHelper.applyTheme(this)
        
        setContentView(R.layout.activity_main)

        // 初始化AppContextProvider
        AppContextProvider.init(this)
        
        // 设置状态栏和导航栏颜色
        ThemeHelper.setStatusBarAndNavigationBar(this)

        tvBatteryStatus = findViewById(R.id.tv_battery_status)
        tvLastNotify = findViewById(R.id.tv_last_notify)
        val btnLogs = findViewById<android.widget.Button>(R.id.btn_logs)
        val btnSettings = findViewById<android.widget.Button>(R.id.btn_settings)

        // 显示当前电量
        updateBatteryStatus()

        // 请求忽略电池优化（确保后台任务运行）
        requestIgnoreBatteryOptimization()

        // 启动后台定时任务
        startBatteryMonitorWorker()

        // 日志按钮点击事件
        btnLogs.setOnClickListener {
            showLogsDialog()
        }

        // 设置按钮点击事件
        btnSettings.setOnClickListener {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        LogUtils.log(TAG, "应用启动")
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

        // 每15分钟执行一次，flex interval设置为1分钟以提高执行精度
        val workRequest = PeriodicWorkRequestBuilder<BatteryCheckWorker>(
            15, TimeUnit.MINUTES,
            1, TimeUnit.MINUTES // flex interval: 在周期结束前1分钟内执行
        )
            .setConstraints(constraints)
            .addTag(AppConfig.WORK_TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AppConfig.WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        LogUtils.log(TAG, "Battery monitor worker started, checking every 15 minutes")
        Toast.makeText(this, "电量监控已启动，每15分钟检测一次电量", Toast.LENGTH_SHORT).show()
    }

    /**
     * 格式化时间
     */
    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * 显示日志对话框
     */
    private fun showLogsDialog() {
        val logContent = LogUtils.getLogContent()
        
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("电量监控日志")
        
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this)
        textView.text = logContent
        textView.setPadding(16, 16, 16, 16)
        textView.textSize = 12f
        scrollView.addView(textView)
        
        builder.setView(scrollView)
        builder.setPositiveButton("确定", null)
        builder.setNegativeButton("清空日志") { _, _ ->
            LogUtils.clearLogs()
            LogUtils.log(TAG, "日志已清空")
        }
        
        builder.create().show()
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
        LogUtils.log(TAG, "Battery check worker running...")

        // 获取当前电量
        val batteryLevel = getBatteryLevel()
        LogUtils.log(TAG, "Current battery level: $batteryLevel%")

        // 动态获取电量阈值
        val threshold = AppConfig.getBatteryThreshold(applicationContext)
        LogUtils.log(TAG, "Current battery threshold: $threshold%")
        
        // 检查是否低于阈值
        if (batteryLevel < threshold) {
            LogUtils.log(TAG, "Battery low, checking cooldown...")
            
            // 检查是否在冷却时间内（防重复）
            if (canSendNotification()) {
                val success = sendToFeishu(batteryLevel)
                if (success) {
                    recordNotificationTime()
                    LogUtils.log(TAG, "Notification sent successfully, next notification in 15 minutes")
                } else {
                    LogUtils.logError(TAG, "Failed to send notification")
                }
            } else {
                LogUtils.log(TAG, "In cooldown period (15 minutes), skip notification")
            }
        } else {
            LogUtils.log(TAG, "Battery level OK, no action needed")
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
     * 暂时注释掉间隔限制，每次检测到低电量都会发送通知
     */
    private fun canSendNotification(): Boolean {
        // 暂时注释掉防重复逻辑
        // val prefs = applicationContext.getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        // val lastNotifyTime = prefs.getLong(AppConfig.KEY_LAST_NOTIFY_TIME, 0)
        // val currentTime = System.currentTimeMillis()
        // return (currentTime - lastNotifyTime) >= AppConfig.MIN_NOTIFY_INTERVAL_MS
        
        return true // 总是返回true，允许每次发送通知
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
        
        // 获取设备名称（从设置中读取，或使用默认设备型号）
        val deviceName = AppConfig.getDeviceName(applicationContext)
        LogUtils.log(TAG, "Device name: $deviceName")
        
        // 计算下次检测时间（15分钟后）
        val nextCheckTime = System.currentTimeMillis() + 15 * 60 * 1000
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val nextCheckTimeStr = sdf.format(java.util.Date(nextCheckTime))
        
        // 飞书消息格式
        val jsonBody = JSONObject().apply {
            put("msg_type", "text")
            put("content", JSONObject().apply {
                // 动态获取电量阈值
                val threshold = AppConfig.getBatteryThreshold(applicationContext)
                put("text", "🔋 电量警报：\n" +
                        "设备名称：$deviceName\n" +
                        "当前电量：$batteryLevel%，低于${threshold}%！请及时充电\n" +
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
            LogUtils.log(TAG, "Feishu response: $responseBody")
            response.isSuccessful
        } catch (e: Exception) {
            LogUtils.logError(TAG, "Failed to send to Feishu: ${e.message}", e)
            false
        }
    }
}