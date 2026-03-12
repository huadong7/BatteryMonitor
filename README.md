# 🔋 Android 电量监控应用

一个轻量级的 Android 电量监控应用，当电量低于阈值时自动发送飞书机器人提醒。

## 📱 功能特性

- ✅ 后台监控：使用 WorkManager 每 15 分钟检查一次电量
- ✅ 低电量提醒：电量低于阈值时自动发送飞书通知
- ✅ 防重复：每 15 分钟最多发送一次提醒
- ✅ 简单易用：一键启动，无需复杂配置
- ✅ 清晰的通知格式：包含设备名称、电量状态和下次检测时间

## 🛠 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| WorkManager | 后台定时任务（15分钟间隔） |
| BatteryManager | 获取电量信息 |
| OkHttp | HTTP 网络请求 |
| Min SDK | Android 8.0 (API 26) |

## 🚀 快速开始

### 1. 配置飞书 Webhook

在项目根目录的 `local.properties` 文件中添加飞书机器人 Webhook 地址：

```properties
# 飞书 Webhook URL（敏感信息，不要提交到版本控制）
feishu.webhook.url=https://open.feishu.cn/open-apis/bot/v2/hook/your-webhook-url
```

**获取飞书 Webhook 地址：**
1. 打开飞书群聊 → 设置 → 群机器人
2. 添加机器人 → 选择 "自定义机器人"
3. 复制 Webhook 地址

### 2. 编译运行

```bash
# 在 Android Studio 中打开项目
# 或使用命令行
cd BatteryMonitor
./gradlew assembleDebug
```

### 3. 安装 APK

将生成的 APK 安装到手机上：

```
app/build/outputs/apk/debug/app-debug.apk
```

### 4. 授权设置

首次运行时会弹出"忽略电池优化"授权，建议允许以确保后台任务稳定运行。

## 📋 项目结构

```
BatteryMonitor/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/batterymonitor/
│   │   │   ├── MainActivity.kt          # 主界面
│   │   │   ├── BatteryCheckWorker.kt     # 后台电量检查任务
│   │   │   └── AppConfig.kt              # 配置管理类
│   │   ├── res/
│   │   │   └── layout/
│   │   │       └── activity_main.xml    # 布局文件
│   │   └── AndroidManifest.xml          # 权限声明
│   ├── build.gradle.kts                 # 依赖配置
│   └── local.properties                 # 本地配置（包含敏感信息）
├── .gitignore                           # Git 忽略文件
└── README.md                            # 项目说明
```

## ⚙️ 可调参数

所有配置参数集中在 `AppConfig.kt` 文件中：

| 参数 | 位置 | 默认值 | 说明 |
|------|------|--------|------|
| 电量阈值 | AppConfig.kt | 50% | 低于此值触发提醒 |
| 检查间隔 | MainActivity.kt | 15分钟 | WorkManager 间隔 |
| 防重复间隔 | AppConfig.kt | 15分钟 | 最小通知间隔 |

## 📝 权限说明

| 权限 | 用途 |
|------|------|
| INTERNET | 发送飞书通知 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 允许后台运行 |

## 🐛 常见问题

**Q: 为什么收不到通知？**
1. 检查 `local.properties` 中的 Webhook 地址是否正确
2. 确保手机网络畅通
3. 检查是否允许了"忽略电池优化"
4. 检查电量是否确实低于阈值

**Q: 通知太频繁？**
- 默认 15 分钟内不会重复发送，可在 `AppConfig.kt` 中调整 `MIN_NOTIFY_INTERVAL_MS`

**Q: 想调整电量阈值？**
- 修改 `AppConfig.kt` 中的 `LOW_BATTERY_THRESHOLD` 变量的值

**Q: 想调整检查间隔？**
- 修改 `MainActivity.kt` 中的 `PeriodicWorkRequestBuilder` 的时间参数

## 📄 通知格式

当电量低于阈值时，飞书机器人会发送以下格式的通知：

```
🔋 电量警报：
设备名称：[设备型号]
当前电量：[电量]%，低于[阈值]%！请及时充电
下次检测：[下次检测时间]
```

## 🔒 安全注意事项

- `local.properties` 文件包含敏感信息（飞书 Webhook URL），已在 `.gitignore` 中排除
- 请勿将包含敏感信息的配置文件提交到版本控制
- 定期轮换飞书机器人 Webhook URL 以提高安全性

## 📄  License

MIT License