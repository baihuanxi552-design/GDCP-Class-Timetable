# Android 客户端

源码版本：3.2.7（versionCode 54），包名 `cn.gdcp.timetable`。

客户端不内置个人课表，支持教务个人课表导入、JSON 备份、自建课程、日/周视图和桌面小部件。

## 构建

需要 JDK 17 和 Android SDK 35：

```bash
./gradlew assembleRelease testDebugUnitTest lintRelease
```

Release 默认未签名。正式发布应使用维护者自己的签名密钥；`*.jks`、`*.keystore`、`keystore.properties` 和 `signing.properties` 已被忽略，禁止提交密钥或密码。

## 隐私和许可

登录发生在学校官方 WebView 页面，课程数据仅保存在本机。详见根目录 [PRIVACY.md](../PRIVACY.md)。

项目原创部分采用 `AGPL-3.0-only`。Miuix、Compose、AndroidX 和 Haze 等第三方组件保留各自许可证；完整说明见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) 和 `app/src/main/assets/THIRD_PARTY_NOTICES.txt`。

前台自动检查、手动刷新及会话管理见 [JW-REFRESH.md](JW-REFRESH.md)。

## 手机验证版

`./gradlew assembleDebug` 生成已用调试签名签署的验证版，包名 `cn.gdcp.timetable.preview`，桌面显示“个人课表·验证版”。它可与正式版并行安装，数据独立，需要重新登录或导入备份。不要卸载现有正式版。CI 的验证版密钥仅供测试，不保证不同构建之间可覆盖安装。

验证每次回到前台立即刷新、手动刷新、会话过期提示，以及断网时保留原课表。首次开启后需先登录并确认导入一次。

主分支通过构建后，CI 会发布对应版本的 GitHub Pre-release 手机验证包，不替换正式版或重复覆盖已经发布的同版本验证 APK。
