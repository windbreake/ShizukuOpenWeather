<p align="center">
  <img src="apps/desktop-dotnet/assets/app-icon-source.png" alt="ShizukuOpenWeather" width="160" />
</p>

<h1 align="center">ShizukuOpenWeather</h1>

<p align="center">
  面向 Windows 与 Android 的现代天气应用。桌面端保留大屏卡片式看板，Android 端使用原生 Kotlin / Jetpack Compose 重构移动体验。
</p>

<p align="center">
  <a href="README.en.md">English</a> · <a href="README.zh-CN.md">简体中文</a> ·
  <a href="https://github.com/windbreake/ShizukuOpenWeather/releases">Releases</a>
</p>

<p align="center">
  <a href="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/android.yml"><img alt="Android CI and Release" src="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/android.yml/badge.svg" /></a>
  <a href="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/desktop-release.yml"><img alt="Desktop Release" src="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/desktop-release.yml/badge.svg" /></a>
  <a href="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/devcontainer-ci-cd.yml"><img alt="Devcontainer CI/CD" src="https://github.com/windbreake/ShizukuOpenWeather/actions/workflows/devcontainer-ci-cd.yml/badge.svg" /></a>
</p>

## 项目定位

ShizukuOpenWeather 是一个本地优先的天气应用项目，视觉方向参考 Overdrop 一类的现代天气产品，但保留当前项目自己的磨砂玻璃、磁贴卡片和天气背景风格。项目已经从早期容器内开发环境迁出，当前仓库保留 Dev Container / CI/CD，同时新增 Windows 安装包、便携版、Android APK 与 GitHub Release 自动化。

当前主线不是 Rust。仓库中仍保留少量历史 Rust workspace 文件用于兼容和备份，但活跃产品链路已经转向：

- Android：`Kotlin` + `Jetpack Compose`
- Windows 桌面：`.NET 10` + `WebView2` + `Vue 3` + `TypeScript`
- 服务与网络层：`Java / Kotlin`
- 本地缓存与设置：`SQLite`

## 功能亮点

- 多地点天气看板，支持侧栏磁贴、卡片化详情和桌面大屏布局
- Android 原生天气首页、地点页、设置页与自动显隐的液态玻璃底部导航栏
- 国内行政区划离线搜索，覆盖省、市、区县、县级市等常见层级
- 海外城市在线搜索，默认使用 Open-Meteo geocoding
- 支持使用 Android 系统定位获取当前位置天气，实际定位来源由设备统一调度 GPS、北斗、Galileo、GLONASS 与网络定位
- 天气默认走 Open-Meteo，避免仓库内硬编码私有 API Key
- 可选配置和风天气、和风图标、高德 Web API 等自定义数据源
- API 凭据不提交到仓库；Android 端由用户在设置页填写，并通过 Android Keystore 加密保存
- 实时天气、逐小时趋势、7 日预报、AQI、天气预警、地图与雷达展示
- 自定义背景、磨砂玻璃强度、卡片显隐、缓存刷新间隔等本地偏好
- Windows 桌面 EXE、Inno Setup 安装包、便携 ZIP 与 Android APK 发布链路

## 数据与隐私

默认模式不需要任何私有天气 API Key。中国地点搜索使用仓库内置的离线行政区划坐标索引来定位城市或区县，再用 Open-Meteo 查询天气；海外地点搜索默认走 Open-Meteo 的免费地理编码接口。

可选 API 凭据只应由用户在本地应用设置中填写，不应写入 README、源码、配置样例或 GitHub Actions 日志。已经泄露过的 Token 应视为失效风险，建议在对应平台轮换。

## 仓库结构

```text
ShizukuOpenWeather/
├── .devcontainer/                 # Dev Container 开发环境
├── .github/workflows/             # Android、桌面端与容器 CI/CD
├── apps/
│   ├── android/                   # Kotlin + Jetpack Compose Android 应用
│   ├── api/                       # Java / Kotlin 服务与接口层
│   ├── desktop-dotnet/            # .NET 10 桌面壳、图标和安装包资源
│   └── web/                       # Vue 3 + TypeScript 桌面天气 UI
├── data/                          # 离线数据与本地开发数据
├── docs/                          # 架构、接口、UI、数据来源和许可证
├── scripts/                       # 开发、校验、打包脚本
├── Dockerfile                     # 容器开发镜像
├── docker-compose.yml             # 容器开发环境
├── build.gradle.kts               # Gradle 顶层构建
└── README.md
```

## 本地开发

推荐环境：

- JDK 21
- Node.js 22+
- Gradle 8.9
- Android SDK 35 或 Android Studio
- .NET 10 SDK
- SQLite3 CLI
- Inno Setup 6，用于 Windows 安装包

环境检查：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

桌面端开发：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

Android 构建：

```powershell
gradle :apps:android:testDebugUnitTest :apps:android:assembleDebug
```

Windows 桌面打包：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-desktop-installer.ps1 -Version 0.1.0
```

## CI/CD

- `Devcontainer CI/CD`：保留原容器开发链路，校验多语言工程与容器构建
- `Android CI and Release`：运行 Android 单元测试、构建 APK，并在标签发布时追加 Release 资产
- `Desktop Release`：打包 Windows 桌面 EXE、安装包和便携版 ZIP

发布产物统一查看：

- [GitHub Releases](https://github.com/windbreake/ShizukuOpenWeather/releases)

## License

项目内第三方数据和资源的许可证说明见 [docs/licenses](docs/licenses)。其中中国行政区划离线索引来自 `city-geo` 数据集，详情见 [docs/licenses/README.md](docs/licenses/README.md)。
