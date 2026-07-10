<p align="center">
  <img src="apps/desktop-dotnet/assets/app-icon-source.png" alt="ShizukuOpenWeather" width="180" />
</p>

<h1 align="center">ShizukuOpenWeather</h1>

<p align="center">
  面向 PC 桌面端的现代天气应用，当前主链路使用 .NET 10 桌面壳、Vue 3 界面、Java/Kotlin 服务层与 SQLite 本地缓存。
</p>

<p align="center">
  <a href="README.en.md">English</a> · <strong>简体中文</strong>
</p>

## 项目简介

ShizukuOpenWeather 是一个以 Windows 桌面端为核心的现代天气应用项目，整体视觉风格参考 Overdrop 一类的卡片化天气产品，但交互和布局重点放在桌面场景。

项目已经从容器内开发副本迁移到本地仓库环境，同时保留了原有的容器校验与 CI/CD 链路，并补上了 Windows 桌面安装包、便携版输出和 GitHub Release 发布能力。

## 当前状态说明

当前仓库的主开发与交付链路是：

- `.NET 10` 桌面壳
- `Kotlin + Jetpack Compose` Android 原生应用
- `Vue 3 + TypeScript` 桌面界面
- `Java / Kotlin` 网络服务与接口层
- `SQLite` 本地缓存与设置存储

仓库里目前仍然保留了一部分 `Rust` 相关目录与 workspace 文件，例如 `crates/`、`Cargo.toml`、`Cargo.lock`，但它们不再是当前桌面端主链路的核心部分，更接近遗留/预留模块。

## 项目定位

- 面向 Windows 与 Android 的天气看板与多地点管理
- 强调现代 UI、卡片化信息组织与个性化设置
- 本地优先，不依赖账号、云同步或复杂部署
- 保留跨语言扩展能力，方便后续桌面端与移动端封装

Android 原生端已经进入实际开发链路，桌面端代码继续保留并独立交付。

## 当前能力

- 多地点天气查看与侧栏磁贴卡片
- 国内区县级、海外城市级地点搜索
- 实时天气、逐小时趋势、7 日预报
- 空气质量、天气预警、地图与雷达信息展示
- 自定义背景、磨砂玻璃效果、卡片显隐与布局偏好
- 本地 SQLite 缓存与本地配置存储
- Windows 桌面程序、便携版输出、安装包输出
- Android 原生天气首页、地点搜索、设置页与自动显隐玻璃导航
- Open-Meteo 免费默认数据源，以及可选的和风天气/高德自定义接入

## 技术栈

### 当前主链路

- `Vue 3` + `TypeScript`
- `Vite`
- `Kotlin` + `Jetpack Compose`
- `.NET 10` WinForms + `WebView2`
- `Java / Kotlin`
- `SQLite`

### 数据来源

- Android 默认天气数据：`Open-Meteo`
- 可选天气数据：`QWeather（和风天气）`
- 可选地理编码：`AMap Web API（高德）`
- 地图底图：`OpenStreetMap`

### 遗留/预留模块

- `Rust` workspace 仍在仓库中，但不是当前 README 所描述的主交付路线

## 仓库结构

```text
ShizukuOpenWeather/
├── .devcontainer/                   # Dev Container 配置
├── .github/workflows/               # GitHub Actions 工作流
├── apps/
│   ├── api/                         # Java / Kotlin 后端服务
│   ├── desktop-dotnet/              # .NET 10 桌面壳、桌面本地主机、安装包资源
│   └── web/                         # Vue 3 + TypeScript 桌面天气界面
│   ├── android/                     # Kotlin + Jetpack Compose Android 应用
├── crates/                          # 遗留 Rust workspace 模块
├── data/                            # 本地开发数据
├── docs/                            # 架构、接口、数据、UI 设计文档
├── scripts/                         # 开发、构建、打包脚本
├── Dockerfile                       # 容器开发镜像
├── docker-compose.yml               # 容器开发环境
├── build.gradle.kts                 # 顶层构建编排
└── README.md
```

## 本地开发

### 推荐环境

- JDK 21
- Node.js 22+
- SQLite3 CLI
- .NET 10 SDK
- Inno Setup 6（用于 Windows 安装包）

### 环境检查
- Android Studio 或 Android SDK 35
- Gradle 8.9

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-dev-env.ps1
```

### 分别启动 API 与 Web 开发服务

```powershell
powershell -ExecutionPolicy Bypass -File scripts/dev-api.ps1
powershell -ExecutionPolicy Bypass -File scripts/dev-web.ps1
```

### 启动现有的组合开发流程

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
```

## Windows 桌面打包

当前仓库已经支持桌面端完整输出，包括程序图标、安装包图标与 Release 资产打包。
## Android 构建

Android 工程位于 `apps/android`，默认使用 Open-Meteo，无需在仓库内提交 API Key。和风天气与高德凭据由用户在设置页填写，并通过 Android Keystore 加密后保存在本机。

```powershell
gradle :apps:android:testDebugUnitTest :apps:android:assembleDebug
```

APK 输出位置：

```text
apps/android/build/outputs/apk/debug/android-debug.apk
```


### 构建桌面程序与安装包

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-desktop-installer.ps1 -Version 0.1.0
```

### 输出内容

构建脚本会生成以下产物：

- `ShizukuWeatherDesktop.exe`：桌面程序发布输出
- `ShizukuOpenWeather-Setup-<version>.exe`：Windows 安装包
- `ShizukuOpenWeather-portable-<version>.zip`：便携版压缩包

安装包默认安装到当前用户目录下，并可创建开始菜单与桌面快捷方式。

## CI/CD

### 现有容器链路

以下链路仍然保留，继续作为开发容器和共享校验流程的基础：

- `.devcontainer/devcontainer.json`
- `.github/workflows/devcontainer-ci-cd.yml`

### Windows 桌面发布链路

### Android 构建与发布链路

- `.github/workflows/android.yml`

该工作流负责 Android 单元测试、APK 构建、CI Artifact 上传，并在标签发布时追加 Android APK。

仓库另外补充了单独的 Windows 桌面打包工作流：

- `.github/workflows/desktop-release.yml`

这样可以在不破坏原有容器 CI/CD 的前提下，单独维护桌面端发布能力。

## Release

项目的发布产物可以在 GitHub Releases 页面查看：

- [Releases](https://github.com/windbreake/ShizukuOpenWeather/releases)

## 说明

GitHub 仓库首页显示的就是根目录 `README.md`。只要把这个文件推到默认分支 `main`，仓库首页内容就会直接更新。
