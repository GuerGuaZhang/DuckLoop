# 🤝 贡献指南

感谢你对 DuckLoop 项目的关注！本文档将指导你如何参与项目开发。

## 📋 目录

- [开发环境](#开发环境)
- [如何贡献](#如何贡献)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [分支管理](#分支管理)

---

## 🔧 开发环境

### 必需工具

- **Android Studio** Hedgehog (2023.1.1) 或更高版本
- **JDK** 17
- **Android SDK** (API 36)
- **Git**

### 推荐工具

- [ktlint](https://ktlint.github.io/) - Kotlin 代码风格检查
- [Detekt](https://detekt.github.io/detekt/) - Kotlin 静态代码分析

---

## 🎯 如何贡献

### 报告 Bug

1. 在 GitHub Issues 中搜索是否已有相同问题
2. 如果没有，创建新的 Issue
3. 使用 Bug Report 模板
4. 提供详细的复现步骤和设备信息

### 提交功能建议

1. 在 GitHub Issues 中搜索是否已有相同建议
2. 如果没有，创建新的 Issue
3. 使用 Feature Request 模板
4. 详细描述功能需求和使用场景

### 提交代码

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'feat: add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建一个 Pull Request

---

## 📏 代码规范

### Kotlin 代码风格

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用 4 空格缩进
- 行长度限制 120 字符
- 使用有意义的变量和函数命名

### 命名约定

```kotlin
// 类名：PascalCase
class MusicPlayer { }

// 函数名：camelCase
fun playMusic() { }

// 变量名：camelCase
val musicVolume = 1.0f

// 常量：UPPER_SNAKE_CASE
const val CHANNEL_ID = "duckloop_channel"

// 包名：全小写
package com.Eason.DuckLoop
```

### 注释规范

```kotlin
/**
 * 播放背景音乐
 * @param uri 音频文件 URI
 * @return 是否播放成功
 */
fun playMusic(uri: Uri): Boolean {
    // 实现代码
}
```

---

## 📝 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范：

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（既不修复 Bug 也不添加功能） |
| `perf` | 性能优化 |
| `test` | 添加测试 |
| `chore` | 构建过程或辅助工具的变动 |

### 示例

```bash
# 新功能
git commit -m "feat(audio): 添加音频淡入淡出效果"

# Bug 修复
git commit -m "fix(service): 修复前台服务被系统杀死的问题"

# 文档更新
git commit -m "docs: 更新 README 使用说明"
```

---

## 🌿 分支管理

### 分支命名

- `main` - 主分支，稳定版本
- `develop` - 开发分支，最新功能
- `feature/*` - 特性分支
- `fix/*` - Bug 修复分支
- `release/*` - 发布分支
- `hotfix/*` - 紧急修复分支

### 工作流程

1. 从 `develop` 创建特性分支
2. 在特性分支上开发
3. 完成后提交 Pull Request 到 `develop`
4. 代码审查通过后合并
5. 定期从 `develop` 合并到 `main` 发布新版本

---

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行单元测试
./gradlew testDebugUnitTest

# 运行 Android 测试
./gradlew connectedAndroidTest
```

### 编写测试

```kotlin
@Test
fun testMusicPlayerPlay() {
    // Arrange
    val player = MusicPlayer(context)
    
    // Act
    player.play()
    
    // Assert
    assertTrue(player.isPlaying())
}
```

---

## 📞 联系方式

如有任何问题，请通过以下方式联系：

- **GitHub Issues**: [项目 Issues 页面]
- **邮箱**: [请补充]

---

## 📜 许可证

贡献的代码将遵循项目的开源许可证。

感谢你的贡献！🎉
