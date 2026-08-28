# 🦆 DuckLoop - 循环播报器

一个 Android 循环播报应用，支持背景音乐播放、播报音频录制/导入，以及音频闪避（Audio Ducking）功能。

## ✨ 功能特性

- 🎵 **背景音乐播放** - 支持多首音乐文件，自动循环播放
- 🎙️ **播报音频管理** - 支持导入多个播报音频文件或直接录音
- 🔄 **循环播报** - 前台服务实现后台循环播放，间隔时间可调
- 🔉 **音频闪避** - 播报时自动降低背景音乐音量，播报结束后平滑恢复
- 📱 **后台运行** - 使用前台服务，支持锁屏状态下继续播放
- 🗑️ **文件管理** - 支持清空背景音乐和播报音频列表

## 📁 项目结构

```
DuckLoop/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/Eason/DuckLoop/
│   │       │   ├── MainActivity.kt          # 主界面 Activity
│   │       │   ├── LoopService.kt           # 前台循环播报服务
│   │       │   ├── MusicPlayer.kt           # 背景音乐播放器 (ExoPlayer)
│   │       │   ├── AnnouncePlayer.kt        # 播报音频播放器 (ExoPlayer)
│   │       │   ├── AudioDucker.kt           # 音频闪避处理
│   │       │   └── AudioRecorderManager.kt  # 录音管理器
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml    # 主界面布局
│   │       │   ├── drawable/                # 图标、背景等资源
│   │       │   │   ├── ic_notification.png  # 通知图标
│   │       │   │   ├── ic_custom_logo.png   # 自定义 Logo
│   │       │   │   ├── ic_logo_small.png    # 小 Logo
│   │       │   │   ├── bg_card.xml          # 卡片背景
│   │       │   │   ├── bg_badge.xml         # 徽章背景
│   │       │   │   ├── ic_launcher_foreground.xml
│   │       │   │   └── ic_launcher_background.xml
│   │       │   ├── mipmap-*/                # 启动器图标
│   │       │   └── values/
│   │       │       ├── colors.xml           # 颜色定义
│   │       │       ├── themes.xml           # 主题样式
│   │       │       └── night/colors.xml     # 夜间模式颜色
│   │       └── AndroidManifest.xml          # 应用清单
│   ├── build.gradle.kts                     # App 模块构建配置
│   ├── proguard-rules.pro                   # 混淆规则
│   └── release/
│       ├── DuckLoop_v1.0.apk               # 发布版本
│       └── output-metadata.json            # 构建元数据
├── gradle/
│   └── wrapper/                             # Gradle Wrapper
├── build.gradle.kts                         # 项目根构建配置
├── settings.gradle.kts                      # 项目设置
├── gradle.properties                        # Gradle 属性
├── local.properties                         # 本地配置 (SDK 路径)
├── gradlew / gradlew.bat                    # Gradle Wrapper 脚本
└── README.md                               # 本文件
```

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin |
| **构建** | Gradle (Kotlin DSL) |
| **音频播放** | ExoPlayer (Media3 1.4.0) |
| **录音** | MediaRecorder |
| **UI** | Material Design 3 (1.11.0) |
| **后台服务** | Foreground Service |
| **异步** | Kotlin Coroutines |
| **最低 SDK** | Android 8.0 (API 26) |
| **目标 SDK** | Android 16 (API 36) |

## 📋 权限说明

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 录制播报音频 |
| `FOREGROUND_SERVICE` | 运行前台服务 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 媒体播放类型前台服务 |
| `POST_NOTIFICATIONS` | 显示运行状态通知 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 防止电池优化影响播放 |

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK (API 36)

### 构建与运行

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 Run 按钮运行

### 安装 APK

已有预构建的 APK 文件：
```
app/release/DuckLoop_v1.0.apk
```

## 📖 使用说明

1. **添加背景音乐**
   - 点击「背景音乐」卡片中的「添加」按钮
   - 选择一个或多个音频文件

2. **准备播报音频**
   - **方式一：导入文件** - 点击「播报音频」卡片中的「添加」按钮
   - **方式二：录音** - 点击「录音作为播报」按钮开始录音

3. **调整参数**
   - **间隔时间** - 设置播报之间的等待时间 (0.5s - 30s)
   - **音乐压低** - 设置播报时背景音乐降低的幅度 (0% - 80%)

4. **试听**
   - 点击「试听」按钮预览播报效果

5. **开始循环播报**
   - 点击「开始」按钮启动服务
   - 应用将在后台持续循环播放
   - 点击「停止」按钮或通知栏停止按钮结束

## 🏗️ 架构说明

### 核心组件

- **MainActivity** - 用户界面，管理音频文件选择和参数设置
- **LoopService** - 前台服务，负责后台循环播报逻辑
- **MusicPlayer** - 背景音乐播放器，支持播放列表和循环
- **AnnouncePlayer** - 播报音频播放器，支持顺序播放多个文件
- **AudioDucker** - 音频闪避处理器，实现平滑的音量过渡
- **AudioRecorderManager** - 录音管理器，处理麦克风录音

### 工作流程

```
用户选择音乐/播报 → 设置参数 → 点击开始
       ↓
LoopService 启动 (前台服务)
       ↓
┌─────────────────────────────────────┐
│  循环播放:                         │
│  1. 播放背景音乐                   │
│  2. 等待间隔时间                   │
│  3. 降低音乐音量 (AudioDucker)     │
│  4. 播放播报音频                   │
│  5. 恢复音乐音量                   │
│  6. 重复循环                       │
└─────────────────────────────────────┘
```

## 📝 版本历史

### v2.1.0 (当前版本)
- 🔧 修复 GlobalScope 内存泄漏问题
- 🔧 修复播报循环问题（多文件播放不循环）
- ✨ 添加文件删除功能（清空背景音乐和播报音频）
- 📝 更新项目文档

### v2.0.0
- 支持多文件播报音频
- 录音功能
- 优化音频闪避效果

### v1.0.0
- 初始发布
- 基本循环播报功能

## 📄 许可证

本项目为私人项目，版权所有。

---

**作者**: Eason
**邮箱**: (请补充)
**GitHub**: (请补充)
