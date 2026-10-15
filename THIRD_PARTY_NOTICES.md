# 第三方软件与归属

本项目原创部分采用 `AGPL-3.0-only`。以下组件和衍生内容保留各自的版权和许可证。本文件用于归属和合规说明，不改变任何第三方许可证。

## 随源码或发布物分发

| 组件/内容 | 版本或来源 | 许可证 | 使用位置 |
| --- | --- | --- | --- |
| HyperCeiler `library/provision` 的 `glow.glsl` / GlowPainter | [ReChronoRain/HyperCeiler](https://github.com/ReChronoRain/HyperCeiler)，见 [GUIDE-NOTICE.md](GUIDE-NOTICE.md) | AGPL-3.0 | `guide-assets/glow.glsl` 原样保留；`guide-flow.js`、`web-guide.js`、`web-guide.css` 为网页适配/衍生部分 |
| progressive-blur `lib/LinearBlur.tsx` | [AndrewPrifer/progressive-blur](https://github.com/AndrewPrifer/progressive-blur) | MIT | `sync.js` 中的渐进模糊思路和适配代码；完整声明见 [PROGRESSIVE-BLUR-LICENSE.txt](PROGRESSIVE-BLUR-LICENSE.txt) |
| Haze | 1.5.4，[chrisbanes/haze](https://github.com/chrisbanes/haze) | Apache-2.0 | Android 本地 AAR：`android/app/libs/haze-android-1.5.4.aar`；许可见 `android/app/libs/HAZE-LICENSE.txt` |
| Miuix | 0.5.1，[compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) | Apache-2.0 | Android UI 依赖 |
| AndroidX / Jetpack Compose | 构建文件所列版本 | Apache-2.0 | Android 运行时依赖 |
| Kotlin / Compose Multiplatform | Kotlin 2.2.10、Compose 1.8.2 | Apache-2.0 | Android 编译与运行时 |
| Gradle Wrapper / Android Gradle Plugin | Wrapper 9.8.1、AGP 8.9.1 | Apache-2.0 | Android 构建工具 |

Android APK 内的归属文本位于 `android/app/src/main/assets/THIRD_PARTY_NOTICES.txt`，Haze 的完整许可证也随 APK 资源和源码分发。Apache License 2.0 的完整文本见 `android/app/libs/HAZE-LICENSE.txt`。

## 仅用于测试

| 组件 | 版本 | 许可证 |
| --- | --- | --- |
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| Hamcrest（JUnit 的传递依赖） | 依赖解析版本 | BSD 3-Clause |

依赖版本的最终权威来源是 `android/build.gradle.kts`、`android/app/build.gradle.kts` 和 Gradle 解析结果。更新依赖时应同步核对并更新本文件。

## 独立实现的参考

Android 欢迎动画参考了 HyperCeiler 的交互和视觉思路，但相关 Kotlin/AGSL 实现为本项目独立代码，不包含其 Java、着色器、图标或其他素材。来源与差异记录在 [android/WELCOME-ANIMATION.md](android/WELCOME-ANIMATION.md)。

各商标、项目名称和产品名称归其各自权利人所有。提及学校名称仅用于说明兼容场景，不表示学校认可、赞助或运营本项目。
